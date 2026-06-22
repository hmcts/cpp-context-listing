# Testcontainers Migration Plan — cpp-context-listing

## 1. Overview

This document provides a precise implementation plan for migrating `cpp-context-listing` integration tests from `cpp-developers-docker` + `runIntegrationTests.sh` to **Testcontainers** — a Java-native, self-contained approach where all infrastructure is managed programmatically from test code.

### Goal
Eliminate all dependencies on `cpp-developers-docker`, external shell scripts, and pre-provisioned Docker environments. Integration tests should be runnable with a single Maven command: `mvn verify -Plisting-integration-test`.

### Prerequisites
- Docker Desktop (or Podman with Testcontainers Cloud)
- Java 17 + Maven (already required)
- No other tooling needed

---

## 2. Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    JUnit 5 Test Execution                     │
│                                                               │
│  @Testcontainers                                              │
│  ┌────────────────────────────────────────────────────────┐   │
│  │           Docker Network: listing-test-net              │   │
│  │                                                         │   │
│  │  ┌──────────┐  ┌──────────┐  ┌───────────────────┐    │   │
│  │  │PostgreSQL │  │ Artemis  │  │    Wildfly 26      │    │   │
│  │  │   15      │  │  2.18    │  │  listing-service   │    │   │
│  │  │           │  │          │  │     .war            │    │   │
│  │  │ 3 DBs:   │  │ Topics:  │  │                     │    │   │
│  │  │ eventstore│  │ listing  │  │  Dynamic port       │    │   │
│  │  │ viewstore │  │ .event   │  │  (mapped to host)   │    │   │
│  │  │ system    │  │ public   │  │                     │    │   │
│  │  │           │  │ .event   │  │                     │    │   │
│  │  └──────────┘  └──────────┘  └───────────────────┘    │   │
│  │                                                         │   │
│  │  ┌──────────┐  ┌──────────────────────────────────┐    │   │
│  │  │ WireMock │  │   Liquibase (programmatic,        │    │   │
│  │  │  stubs   │  │   runs in @BeforeAll)              │    │   │
│  │  └──────────┘  └──────────────────────────────────┘    │   │
│  └────────────────────────────────────────────────────────┘   │
│                                                               │
│  Tests use dynamic ports → no port conflicts                  │
│  Containers destroyed after test suite → clean state          │
└──────────────────────────────────────────────────────────────┘
```

---

## 3. File Structure — Changes Required

```
cpp-context-listing/
├── listing-integration-test/
│   ├── pom.xml                                    # MODIFY: Add Testcontainers dependencies
│   └── src/test/java/uk/gov/moj/cpp/listing/
│       ├── integration/
│       │   ├── AbstractIT.java                    # MODIFY: Replace cpp-developers-docker setup
│       │   ├── ListCourtHearingIT.java            # MODIFY: Use new base class
│       │   └── ...other IT classes...             # MODIFY: Use new base class
│       └── containers/                            # NEW: Container configuration package
│           ├── ListingTestEnvironment.java         # NEW: Singleton container orchestration
│           ├── PostgresContainerConfig.java        # NEW: PostgreSQL + Liquibase setup
│           ├── ArtemisContainerConfig.java         # NEW: Artemis broker setup
│           ├── WildflyContainerConfig.java         # NEW: Wildfly + WAR deployment
│           └── WiremockContainerConfig.java        # NEW: WireMock stubs setup
```

---

## 4. Implementation Steps

### Step 1: Add Maven Dependencies

**File**: `listing-integration-test/pom.xml`

Add to the `<dependencies>` section:

```xml
<!-- Testcontainers BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>1.19.7</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Testcontainers dependencies -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

> **Note**: If the parent POM (`service-parent-pom`) already manages a `testcontainers-bom`, use that version. Otherwise, add the BOM in the integration-test module's `<dependencyManagement>`.

### Step 2: Create ListingTestEnvironment (Singleton Container Orchestration)

**File**: `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/containers/ListingTestEnvironment.java`

```java
package uk.gov.moj.cpp.listing.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;

/**
 * Singleton test environment that starts all containers once for the entire
 * test suite. Uses the "singleton container" pattern recommended by Testcontainers.
 *
 * Containers are shared across all IT classes and torn down when the JVM exits.
 */
public final class ListingTestEnvironment {

    private static final Network NETWORK = Network.newNetwork();

    // PostgreSQL with 3 databases
    private static final PostgreSQLContainer<?> POSTGRES = PostgresContainerConfig.create(NETWORK);

    // Apache Artemis JMS broker
    private static final GenericContainer<?> ARTEMIS = ArtemisContainerConfig.create(NETWORK);

    // WireMock for external service stubs
    private static final GenericContainer<?> WIREMOCK = WiremockContainerConfig.create(NETWORK);

    // Wildfly with listing-service WAR
    private static final GenericContainer<?> WILDFLY = WildflyContainerConfig.create(NETWORK);

    private static boolean started = false;

    private ListingTestEnvironment() {}

    /**
     * Starts all containers if not already running.
     * Called from AbstractIT @BeforeAll.
     */
    public static synchronized void start() {
        if (started) return;

        // Start infrastructure containers in parallel
        Startables.deepStart(POSTGRES, ARTEMIS, WIREMOCK).join();

        // Run Liquibase migrations after PostgreSQL is ready
        PostgresContainerConfig.runLiquibaseMigrations(POSTGRES);

        // Start Wildfly after all dependencies are ready
        WILDFLY.start();

        // Wait for deployment health check
        waitForDeployment();

        started = true;
    }

    private static void waitForDeployment() {
        // Wildfly readiness is handled by the container's wait strategy
        // configured in WildflyContainerConfig
    }

    public static String getWildflyBaseUrl() {
        return String.format("http://%s:%d",
            WILDFLY.getHost(),
            WILDFLY.getMappedPort(8080));
    }

    public static String getPostgresJdbcUrl(String database) {
        return String.format("jdbc:postgresql://%s:%d/%s",
            POSTGRES.getHost(),
            POSTGRES.getMappedPort(5432),
            database);
    }

    public static String getArtemisUrl() {
        return String.format("tcp://%s:%d",
            ARTEMIS.getHost(),
            ARTEMIS.getMappedPort(61616));
    }

    public static String getWiremockUrl() {
        return String.format("http://%s:%d",
            WIREMOCK.getHost(),
            WIREMOCK.getMappedPort(8080));
    }

    public static PostgreSQLContainer<?> getPostgres() { return POSTGRES; }
    public static GenericContainer<?> getArtemis() { return ARTEMIS; }
    public static GenericContainer<?> getWildfly() { return WILDFLY; }
    public static GenericContainer<?> getWiremock() { return WIREMOCK; }
}
```

### Step 3: Create PostgreSQL Container Configuration

**File**: `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/containers/PostgresContainerConfig.java`

```java
package uk.gov.moj.cpp.listing.containers;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public final class PostgresContainerConfig {

    static final String DB_USER = "listing";
    static final String DB_PASSWORD = "listing";
    static final String EVENTSTORE_DB = "listingeventstore";
    static final String VIEWSTORE_DB = "listingviewstore";
    static final String SYSTEM_DB = "listingsystem";

    private PostgresContainerConfig() {}

    public static PostgreSQLContainer<?> create(final Network network) {
        return new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases("postgres")
            .withDatabaseName("postgres")
            .withUsername("postgres")
            .withPassword("postgres")
            .withInitScript("db/init.sql");
    }

    /**
     * Runs all 6 Liquibase migration sets against the PostgreSQL container.
     * This replaces the shell-based runLiquibase() function from cpp-developers-docker.
     */
    public static void runLiquibaseMigrations(final PostgreSQLContainer<?> postgres) {
        final String host = postgres.getHost();
        final int port = postgres.getMappedPort(5432);

        // Event Store migrations (eventstore database)
        runLiquibaseJar("event-repository-liquibase.jar", host, port, EVENTSTORE_DB);
        runLiquibaseJar("aggregate-snapshot-repository-liquibase.jar", host, port, EVENTSTORE_DB);

        // View Store migrations (viewstore database)
        runLiquibaseJar("event-buffer-liquibase.jar", host, port, VIEWSTORE_DB);
        runLiquibaseJar("event-tracking-liquibase.jar", host, port, VIEWSTORE_DB);

        // Context-specific viewstore (uses Maven liquibase plugin)
        runMavenLiquibase(host, port, VIEWSTORE_DB);

        // System migrations
        runLiquibaseJar("framework-system-liquibase.jar", host, port, SYSTEM_DB);
    }

    private static void runLiquibaseJar(
            final String jarName,
            final String host,
            final int port,
            final String database) {
        try {
            final String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/%s", host, port, database);

            ProcessBuilder pb = new ProcessBuilder(
                "java", "-jar",
                getJarPath(jarName),
                "--url=" + jdbcUrl,
                "--username=" + DB_USER,
                "--password=" + DB_PASSWORD,
                "--logLevel=info",
                "update"
            );
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(
                    "Liquibase migration failed for " + jarName + " (exit code: " + exitCode + ")");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to run Liquibase: " + jarName, e);
        }
    }

    private static void runMavenLiquibase(
            final String host,
            final int port,
            final String database) {
        try {
            final String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/%s", host, port, database);

            ProcessBuilder pb = new ProcessBuilder(
                "mvn", "-f",
                "listing-viewstore/listing-viewstore-liquibase/pom.xml",
                "-Dliquibase.url=" + jdbcUrl,
                "-Dliquibase.username=" + DB_USER,
                "-Dliquibase.password=" + DB_PASSWORD,
                "-Dliquibase.logLevel=info",
                "resources:resources", "liquibase:update"
            );
            pb.directory(new java.io.File(System.getProperty("user.dir")).getParentFile());
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(
                    "Viewstore Liquibase migration failed (exit code: " + exitCode + ")");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to run viewstore Liquibase", e);
        }
    }

    private static String getJarPath(final String jarName) {
        // JARs downloaded by Maven to target/liquibase-jars/ during build
        return System.getProperty("user.dir") + "/../target/liquibase-jars/" + jarName;
    }
}
```

**New test resource file**: `listing-integration-test/src/test/resources/db/init.sql`

```sql
-- Create listing user
CREATE USER listing WITH PASSWORD 'listing';

-- Event Store database
CREATE DATABASE listingeventstore WITH OWNER=postgres;
GRANT ALL PRIVILEGES ON DATABASE listingeventstore TO listing;

-- View Store database
CREATE DATABASE listingviewstore WITH OWNER=postgres;
GRANT ALL PRIVILEGES ON DATABASE listingviewstore TO listing;

-- System database
CREATE DATABASE listingsystem WITH OWNER=postgres;
GRANT ALL PRIVILEGES ON DATABASE listingsystem TO listing;

-- Grant schema permissions (requires connecting to each DB)
\c listingeventstore
GRANT ALL ON SCHEMA public TO listing;
\c listingviewstore
GRANT ALL ON SCHEMA public TO listing;
\c listingsystem
GRANT ALL ON SCHEMA public TO listing;
```

### Step 4: Create Artemis Container Configuration

**File**: `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/containers/ArtemisContainerConfig.java`

```java
package uk.gov.moj.cpp.listing.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public final class ArtemisContainerConfig {

    private ArtemisContainerConfig() {}

    public static GenericContainer<?> create(final Network network) {
        return new GenericContainer<>("apache/activemq-artemis:2.18.0")
            .withNetwork(network)
            .withNetworkAliases("artemis")
            .withExposedPorts(61616, 8161)
            .withEnv("ARTEMIS_USER", "admin")
            .withEnv("ARTEMIS_PASSWORD", "admin")
            .withEnv("ANONYMOUS_LOGIN", "true")
            .waitingFor(Wait.forListeningPort());
    }
}
```

### Step 5: Create Wildfly Container Configuration

**File**: `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/containers/WildflyContainerConfig.java`

```java
package uk.gov.moj.cpp.listing.containers;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.nio.file.Path;
import java.time.Duration;

public final class WildflyContainerConfig {

    private static final String WILDFLY_HOME = "/opt/jboss/wildfly";
    private static final String DEPLOYMENTS = WILDFLY_HOME + "/standalone/deployments";

    private WildflyContainerConfig() {}

    public static GenericContainer<?> create(final Network network) {
        // Find the WAR file from the Maven build output
        final Path warPath = findWarFile();
        final Path standaloneXmlPath = Path.of(
            System.getProperty("user.dir"), "src/test/resources/wildfly/standalone-testcontainers.xml");

        return new GenericContainer<>("jboss/wildfly:26.1.3.Final-jdk17")
            .withNetwork(network)
            .withNetworkAliases("wildfly")
            .withExposedPorts(8080, 8787, 9990)
            .withFileSystemBind(
                warPath.toString(),
                DEPLOYMENTS + "/listing-service.war",
                BindMode.READ_ONLY)
            .withFileSystemBind(
                standaloneXmlPath.toString(),
                WILDFLY_HOME + "/standalone/configuration/standalone.xml",
                BindMode.READ_ONLY)
            .withEnv("JAVA_OPTS", String.join(" ",
                "-agentlib:jdwp=transport=dt_socket,address=*:8787,server=y,suspend=n",
                "-Djboss.bind.address=0.0.0.0",
                "-Djboss.bind.address.management=0.0.0.0"
            ))
            .withCommand(WILDFLY_HOME + "/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0")
            .waitingFor(Wait.forHttp("/listing-service/internal/metrics/ping")
                .forPort(8080)
                .forResponsePredicate(response -> response.contains("pong"))
                .withStartupTimeout(Duration.ofMinutes(3)));
    }

    private static Path findWarFile() {
        final Path serviceTarget = Path.of(
            System.getProperty("user.dir"), "..", "listing-service", "target");
        try {
            return java.nio.file.Files.list(serviceTarget)
                .filter(p -> p.getFileName().toString().endsWith(".war"))
                .filter(p -> !p.getFileName().toString().contains("original"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                    "listing-service WAR not found in " + serviceTarget +
                    ". Run 'mvn install -DskipTests' first."));
        } catch (Exception e) {
            throw new RuntimeException("Cannot find WAR file", e);
        }
    }
}
```

### Step 6: Create WireMock Container Configuration

**File**: `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/containers/WiremockContainerConfig.java`

```java
package uk.gov.moj.cpp.listing.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public final class WiremockContainerConfig {

    private WiremockContainerConfig() {}

    public static GenericContainer<?> create(final Network network) {
        return new GenericContainer<>("wiremock/wiremock:3.3.1")
            .withNetwork(network)
            .withNetworkAliases("wiremock")
            .withExposedPorts(8080)
            .withCommand("--verbose", "--global-response-templating")
            .withClasspathResourceMapping(
                "wiremock/mappings",
                "/home/wiremock/mappings",
                org.testcontainers.containers.BindMode.READ_ONLY)
            .waitingFor(Wait.forHttp("/__admin/mappings")
                .forStatusCode(200));
    }
}
```

### Step 7: Modify AbstractIT Base Class

**File**: `listing-integration-test/src/test/java/uk/gov/moj/cpp/listing/integration/AbstractIT.java`

The existing `AbstractIT` extends `RestClient` and configures base URLs from `endpoint.properties`. The key changes:

```java
package uk.gov.moj.cpp.listing.integration;

import uk.gov.moj.cpp.listing.containers.ListingTestEnvironment;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for all integration tests.
 * Replaces the cpp-developers-docker dependency with Testcontainers.
 */
public abstract class AbstractIT extends RestClient {

    @BeforeAll
    static void startEnvironment() {
        // Starts containers only once (singleton pattern)
        ListingTestEnvironment.start();

        // Override the base URL to use Testcontainers' dynamic port
        System.setProperty("INTEGRATION_HOST_KEY", "localhost");
        System.setProperty("INTEGRATION_BASE_URL",
            ListingTestEnvironment.getWildflyBaseUrl());
    }

    @BeforeEach
    void cleanDatabases() {
        // Existing database cleanup logic remains unchanged
        // Uses JDBC to truncate viewstore tables before each test
        cleanEventStoreTables();
        cleanViewStoreTables();
    }

    // Existing helper methods remain:
    // - setupWiremockStubs()
    // - cleanEventStoreTables()
    // - cleanViewStoreTables()
    // - getCppUidHeader()

    @Override
    protected String getBaseUri() {
        // Dynamic URL from Testcontainers instead of static endpoint.properties
        return ListingTestEnvironment.getWildflyBaseUrl();
    }
}
```

### Step 8: Create Wildfly Standalone Configuration for Testcontainers

**File**: `listing-integration-test/src/test/resources/wildfly/standalone-testcontainers.xml`

This is a Wildfly `standalone.xml` with datasources and JMS configured to use Testcontainers network aliases:

Key sections to configure:

```xml
<!-- Datasource: Event Store -->
<datasource jndi-name="java:/app/listing-service/DS.eventstore"
            pool-name="listingeventstore">
    <connection-url>jdbc:postgresql://postgres:5432/listingeventstore</connection-url>
    <driver>postgresql</driver>
    <security>
        <user-name>listing</user-name>
        <password>listing</password>
    </security>
</datasource>

<!-- Datasource: View Store -->
<datasource jndi-name="java:/app/listing-service/DS.viewstore"
            pool-name="listingviewstore">
    <connection-url>jdbc:postgresql://postgres:5432/listingviewstore</connection-url>
    <driver>postgresql</driver>
    <security>
        <user-name>listing</user-name>
        <password>listing</password>
    </security>
</datasource>

<!-- Datasource: System -->
<datasource jndi-name="java:/app/listing-service/DS.system"
            pool-name="listingsystem">
    <connection-url>jdbc:postgresql://postgres:5432/listingsystem</connection-url>
    <driver>postgresql</driver>
    <security>
        <user-name>listing</user-name>
        <password>listing</password>
    </security>
</datasource>

<!-- JMS: Artemis connection -->
<pooled-connection-factory name="activemq-ra"
                          entries="java:/JmsXA java:jboss/DefaultJMSConnectionFactory"
                          connectors="remote-artemis"
                          transaction="xa"/>

<!-- Remote Artemis connector -->
<remote-connector name="remote-artemis"
                  socket-binding="messaging-remote"/>

<!-- Socket binding for Artemis -->
<outbound-socket-binding name="messaging-remote">
    <remote-destination host="artemis" port="61616"/>
</outbound-socket-binding>
```

> **Important**: Extract the full `standalone.xml` from `cpp-developers-docker/containers/wildfly/configuration/standalone.xml` and replace hostname references:
> - `cpp-postgres` → `postgres`
> - `cpp-artemis` → `artemis`
> - `localhost:8080` (HAProxy) → `wildfly:8080` (direct)

### Step 9: Add Liquibase JAR Download to Build

**File**: `pom.xml` (root) — add to the existing `listing-integration-test` profile or create a new one:

```xml
<profile>
    <id>testcontainers-prepare</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>copy-liquibase-jars</id>
                        <phase>generate-test-resources</phase>
                        <goals><goal>copy</goal></goals>
                        <configuration>
                            <outputDirectory>${project.basedir}/target/liquibase-jars</outputDirectory>
                            <artifactItems>
                                <artifactItem>
                                    <groupId>uk.gov.justice.event-store</groupId>
                                    <artifactId>event-repository-liquibase</artifactId>
                                    <version>${event-store.version}</version>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>uk.gov.justice.event-store</groupId>
                                    <artifactId>aggregate-snapshot-repository-liquibase</artifactId>
                                    <version>${event-store.version}</version>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>uk.gov.justice.services</groupId>
                                    <artifactId>event-buffer-liquibase</artifactId>
                                    <version>${framework.version}</version>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>uk.gov.justice.services</groupId>
                                    <artifactId>framework-system-liquibase</artifactId>
                                    <version>${framework.version}</version>
                                </artifactItem>
                                <artifactItem>
                                    <groupId>uk.gov.justice.event-store</groupId>
                                    <artifactId>event-tracking-liquibase</artifactId>
                                    <version>${event-store.version}</version>
                                </artifactItem>
                            </artifactItems>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</profile>
```

---

## 5. Alternative Approach: Embedded Liquibase (Recommended Simplification)

Instead of running Liquibase as external JARs via `ProcessBuilder`, a cleaner approach is to use Liquibase as a Java dependency and run migrations programmatically:

```xml
<!-- Add to listing-integration-test pom.xml -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
    <version>4.25.1</version>
    <scope>test</scope>
</dependency>
```

Then in `PostgresContainerConfig.java`:

```java
public static void runLiquibaseMigrations(final PostgreSQLContainer<?> postgres) {
    // For framework JARs (event-repository, event-buffer, etc.):
    // These are already on the classpath via Maven dependencies.
    // Extract their Liquibase changelogs and run programmatically.

    try (Connection conn = DriverManager.getConnection(
            postgres.getJdbcUrl().replace("/postgres", "/listingeventstore"),
            DB_USER, DB_PASSWORD)) {

        Database database = DatabaseFactory.getInstance()
            .findCorrectDatabaseImplementation(new JdbcConnection(conn));

        Liquibase liquibase = new Liquibase(
            "liquibase/event-store-changelog.xml",
            new ClassLoaderResourceAccessor(),
            database);

        liquibase.update("");
    }

    // Repeat for viewstore and system databases
}
```

> **Note**: This approach depends on the framework Liquibase JARs having accessible changelog resources on the classpath. If changelogs are packaged as self-executing JARs only, the `ProcessBuilder` approach in Step 3 is the fallback.

---

## 6. Running Tests

### Local Development

```bash
# Step 1: Build the WAR (only needed once, or after code changes)
mvn install -DskipTests -pl listing-service -am

# Step 2: Run integration tests (Testcontainers handles everything else)
mvn verify -pl listing-integration-test -Plisting-integration-test -DINTEGRATION_HOST_KEY=localhost
```

### CI/CD (Azure DevOps)

```yaml
# azure-pipelines.yaml - Integration test stage
- stage: IntegrationTest_Testcontainers
  jobs:
  - job: TestcontainersIT
    pool:
      name: "MDV-ADO-AGENT-AKS-01"
      demands:
        - identifier -equals centos8-j17
    steps:
    - script: |
        # Docker must be available on the agent
        docker info

        # Full build
        mvn clean install -DskipTests

        # Integration tests — Testcontainers manages containers automatically
        mvn verify -pl listing-integration-test \
          -Plisting-integration-test \
          -DINTEGRATION_HOST_KEY=localhost
      displayName: 'Run Testcontainers Integration Tests'
```

No `CPP_DOCKER_DIR` environment variable, no `cpp-developers-docker` checkout, no separate Docker Compose setup.

---

## 7. Migration Execution Order

| Step | Action | Deliverable | Effort |
|------|--------|-------------|--------|
| 1 | Add Testcontainers dependencies to `listing-integration-test/pom.xml` | pom.xml changes | 0.5 day |
| 2 | Create `containers/` package with 5 configuration classes | Java source files | 2 days |
| 3 | Create `db/init.sql` test resource for PostgreSQL init | SQL file | 0.5 day |
| 4 | Extract and adapt `standalone-testcontainers.xml` from cpp-developers-docker | XML config | 1 day |
| 5 | Extract WireMock stubs to `src/test/resources/wiremock/mappings/` | JSON files | 1 day |
| 6 | Modify `AbstractIT` to use `ListingTestEnvironment` | Java changes | 1 day |
| 7 | Configure Liquibase JAR download or embedded execution | pom.xml + Java | 1 day |
| 8 | Test full integration test suite with Testcontainers | Passing tests | 2 days |
| 9 | Update CI/CD pipeline to remove cpp-developers-docker dependency | Pipeline YAML | 0.5 day |
| 10 | Document new approach for team | README update | 0.5 day |
| **Total** | | | **~10 days** |

---

## 8. Handling Specific Listing Complexities

### 8.1 Six Liquibase Migration Sets

The listing context requires 6 separate Liquibase migrations against 3 databases. This is the most complex part:

| Migration JAR | Target Database | Source |
|--------------|----------------|--------|
| `event-repository-liquibase` | `listingeventstore` | Framework (event-store) |
| `aggregate-snapshot-repository-liquibase` | `listingeventstore` | Framework (event-store) |
| `event-buffer-liquibase` | `listingviewstore` | Framework |
| `event-tracking-liquibase` | `listingviewstore` | Framework (event-store) |
| `listing-viewstore-liquibase` | `listingviewstore` | Context-specific |
| `framework-system-liquibase` | `listingsystem` | Framework |

**Strategy**: Download JARs during `generate-test-resources` phase; execute in `@BeforeAll`.

### 8.2 WireMock Stub Setup

Current tests programmatically configure WireMock stubs in `AbstractIT.setupWiremockStubs()`. Two options:

1. **Keep programmatic setup** (easiest): WireMock container starts empty; stubs configured via REST API in `@BeforeAll`
2. **Static JSON mappings** (cleaner): Export stubs to JSON files under `src/test/resources/wiremock/mappings/`

Recommendation: **Option 1** — minimal changes to existing test code.

### 8.3 JMS Integration

Tests that use `JmsResourceManagementExtension` need the Artemis container URL. The extension currently uses JNDI to look up the connection factory from Wildfly. Since Wildfly connects to Artemis via its `standalone.xml` configuration, and the Testcontainers Artemis uses the network alias `artemis`, no test code changes are needed — only the `standalone-testcontainers.xml` needs correct Artemis hostname.

### 8.4 Payload-Based Testing Framework

The `PayloadGenerator` and payload-based test helpers load JSON from `test-data/` directories. These are classpath resources and are **unaffected** by the Testcontainers migration.

---

## 9. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Wildfly startup time in container (30-90s) | Slower test suite | Singleton pattern ensures one startup per suite |
| Docker image pull time (first run) | Slow first execution | Use `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` |
| Liquibase JAR compatibility | Migrations may fail | Pin versions matching `framework.version` and `event-store.version` |
| CI agent Docker availability | Tests won't run | Verify Docker daemon on CI agents; consider Testcontainers Cloud |
| Port conflicts | Container start failure | Dynamic port allocation (Testcontainers default) avoids conflicts |
| standalone.xml drift | Config mismatch | Track changes to cpp-developers-docker standalone.xml |
| Memory usage | Docker + JVM pressure | Configure Docker Desktop with 8GB+ RAM; tune Wildfly JVM args |

---

## 10. Testcontainers Configuration for Faster Development

Create `listing-integration-test/src/test/resources/testcontainers.properties`:

```properties
# Enable reusable containers (containers persist between test runs)
# Avoids cold-start penalty during development
testcontainers.reuse.enable=true
```

And in each container configuration, add `.withReuse(true)`:

```java
return new PostgreSQLContainer<>("postgres:15-alpine")
    .withNetwork(network)
    .withNetworkAliases("postgres")
    .withReuse(true)  // Keep container between test runs
    // ... rest of config
```

> **Warning**: Reusable containers should NOT be used in CI/CD — only for local development. In CI, use fresh containers for test isolation.

---

## 11. Rollback Plan

The migration is additive:

1. Existing `runIntegrationTests.sh` remains functional
2. Existing `AbstractIT` can be preserved alongside the new one
3. If Testcontainers adoption fails, revert the pom.xml changes and `containers/` package

No existing infrastructure is modified or removed.
