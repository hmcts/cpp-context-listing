# Tilt.dev Migration Plan — cpp-context-listing

## 1. Overview

This document provides a precise implementation plan for migrating `cpp-context-listing` from `cpp-developers-docker` + `runIntegrationTests.sh` to a self-contained **Tilt.dev** local development and integration test environment.

### Goal
Replace all `cpp-developers-docker` dependencies with a Tiltfile and Kubernetes manifests owned by the `cpp-context-listing` repository.

### Prerequisites
- Docker Desktop (or equivalent container runtime)
- `kind` (Kubernetes in Docker) — local K8s cluster
- `kubectl` — Kubernetes CLI
- `ctlptl` — cluster lifecycle tool (recommended by Tilt)
- `tilt` CLI — v0.33+ (latest stable)
- `helm` — for chart-based deployments (optional, can use raw manifests)

---

## 2. Architecture

```
┌─────────────────────────────────────────────────────┐
│                 kind K8s Cluster                     │
│                                                      │
│  ┌──────────┐  ┌──────────┐  ┌───────────────────┐  │
│  │PostgreSQL │  │ Artemis  │  │    Wildfly 26      │  │
│  │   15      │  │  2.18    │  │  listing-service   │  │
│  │           │  │          │  │     .war            │  │
│  │ 3 DBs:   │  │ Topics:  │  │                     │  │
│  │ eventstore│  │ listing  │  │ Port: 8080         │  │
│  │ viewstore │  │ .event   │  │ Debug: 8787        │  │
│  │ system    │  │ public   │  │                     │  │
│  │           │  │ .event   │  │                     │  │
│  └──────────┘  └──────────┘  └───────────────────┘  │
│                                                      │
│  ┌──────────┐  ┌──────────────────────────────────┐  │
│  │ WireMock │  │     Liquibase Job (init)          │  │
│  │  stubs   │  │  6 migration sets on startup      │  │
│  └──────────┘  └──────────────────────────────────┘  │
│                                                      │
│  Port-forwards: localhost:8080 → wildfly             │
│                 localhost:5432 → postgres             │
│                 localhost:8787 → wildfly debug        │
│                 localhost:61616 → artemis             │
└─────────────────────────────────────────────────────┘
```

---

## 3. File Structure to Create

```
cpp-context-listing/
├── tilt/                                    # NEW: All Tilt configuration
│   ├── Tiltfile                             # Main Tilt orchestration file
│   ├── k8s/                                 # Kubernetes manifests
│   │   ├── postgres.yaml                    # PostgreSQL StatefulSet + Service
│   │   ├── postgres-init-configmap.yaml     # DB init SQL (3 databases + user)
│   │   ├── artemis.yaml                     # Artemis Deployment + Service
│   │   ├── wildfly.yaml                     # Wildfly Deployment + Service
│   │   ├── wiremock.yaml                    # WireMock Deployment + Service
│   │   └── liquibase-job.yaml              # Liquibase Job (runs once)
│   ├── docker/                              # Custom Docker images for Tilt
│   │   ├── Dockerfile.wildfly               # Wildfly with listing-service WAR
│   │   └── Dockerfile.liquibase             # Liquibase runner with all JARs
│   ├── config/                              # Service configuration
│   │   ├── standalone-local.xml             # Wildfly standalone config for local dev
│   │   ├── wiremock-stubs/                  # WireMock JSON stub mappings
│   │   │   ├── court-scheduler-stubs.json
│   │   │   ├── reference-data-stubs.json
│   │   │   ├── progression-stubs.json
│   │   │   ├── users-groups-stubs.json
│   │   │   └── prosecution-case-stubs.json
│   │   └── artemis/
│   │       └── broker.xml                   # Artemis broker configuration
│   └── scripts/
│       ├── setup-cluster.sh                 # One-time kind cluster creation
│       └── run-integration-tests.sh         # Tilt-based integration test runner
```

---

## 4. Implementation Steps

### Step 1: Install Prerequisites

Create `tilt/scripts/setup-cluster.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

echo "=== Setting up Tilt.dev environment for cpp-context-listing ==="

# Check prerequisites
command -v docker >/dev/null 2>&1 || { echo "Docker required. Install Docker Desktop."; exit 1; }
command -v kind >/dev/null 2>&1 || { echo "kind required. Install: brew install kind"; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo "kubectl required. Install: brew install kubectl"; exit 1; }
command -v tilt >/dev/null 2>&1 || { echo "tilt required. Install: curl -fsSL https://raw.githubusercontent.com/tilt-dev/tilt/master/scripts/install.sh | bash"; exit 1; }

# Create kind cluster with port mappings
cat <<EOF | kind create cluster --name listing-dev --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  extraPortMappings:
  - containerPort: 30080
    hostPort: 8080
    protocol: TCP
  - containerPort: 30432
    hostPort: 5432
    protocol: TCP
  - containerPort: 30787
    hostPort: 8787
    protocol: TCP
  - containerPort: 30616
    hostPort: 61616
    protocol: TCP
EOF

echo "=== Cluster 'listing-dev' created. Run 'tilt up' from tilt/ directory ==="
```

### Step 2: Create Kubernetes Manifests

#### 2a. PostgreSQL (`tilt/k8s/postgres.yaml`)

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-data
spec:
  accessModes: [ReadWriteOnce]
  resources:
    requests:
      storage: 1Gi
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_PASSWORD
          value: postgres
        - name: POSTGRES_USER
          value: postgres
        volumeMounts:
        - name: init-sql
          mountPath: /docker-entrypoint-initdb.d
        readinessProbe:
          exec:
            command: ["pg_isready", "-U", "postgres"]
          initialDelaySeconds: 5
          periodSeconds: 5
      volumes:
      - name: init-sql
        configMap:
          name: postgres-init
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
spec:
  type: NodePort
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
    nodePort: 30432
```

#### 2b. PostgreSQL Init ConfigMap (`tilt/k8s/postgres-init-configmap.yaml`)

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-init
data:
  init.sql: |
    -- Create listing user
    CREATE USER listing WITH PASSWORD 'listing';

    -- Event Store database
    CREATE DATABASE listingeventstore WITH OWNER=postgres;
    \c listingeventstore
    GRANT ALL ON SCHEMA public TO listing;

    -- View Store database
    CREATE DATABASE listingviewstore WITH OWNER=postgres;
    \c listingviewstore
    GRANT ALL ON SCHEMA public TO listing;

    -- System database
    CREATE DATABASE listingsystem WITH OWNER=postgres;
    \c listingsystem
    GRANT ALL ON SCHEMA public TO listing;
```

#### 2c. Artemis (`tilt/k8s/artemis.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: artemis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: artemis
  template:
    metadata:
      labels:
        app: artemis
    spec:
      containers:
      - name: artemis
        image: apache/activemq-artemis:2.18.0
        ports:
        - containerPort: 61616
        - containerPort: 8161
        env:
        - name: ARTEMIS_USER
          value: admin
        - name: ARTEMIS_PASSWORD
          value: admin
        - name: ANONYMOUS_LOGIN
          value: "true"
        readinessProbe:
          tcpSocket:
            port: 61616
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: artemis
spec:
  type: NodePort
  selector:
    app: artemis
  ports:
  - name: openwire
    port: 61616
    targetPort: 61616
    nodePort: 30616
  - name: console
    port: 8161
    targetPort: 8161
```

#### 2d. Wildfly (`tilt/k8s/wildfly.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wildfly
spec:
  replicas: 1
  selector:
    matchLabels:
      app: wildfly
  template:
    metadata:
      labels:
        app: wildfly
    spec:
      initContainers:
      - name: wait-for-postgres
        image: busybox:1.36
        command: ['sh', '-c', 'until nc -z postgres 5432; do echo waiting for postgres; sleep 2; done']
      - name: wait-for-artemis
        image: busybox:1.36
        command: ['sh', '-c', 'until nc -z artemis 61616; do echo waiting for artemis; sleep 2; done']
      containers:
      - name: wildfly
        image: listing-wildfly
        ports:
        - containerPort: 8080
        - containerPort: 8787
        - containerPort: 9990
        env:
        - name: JAVA_OPTS
          value: >-
            -agentlib:jdwp=transport=dt_socket,address=*:8787,server=y,suspend=n
            -Djboss.bind.address=0.0.0.0
            -Djboss.bind.address.management=0.0.0.0
        readinessProbe:
          httpGet:
            path: /listing-service/internal/metrics/ping
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
          timeoutSeconds: 5
        livenessProbe:
          httpGet:
            path: /listing-service/internal/metrics/ping
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 15
---
apiVersion: v1
kind: Service
metadata:
  name: wildfly
spec:
  type: NodePort
  selector:
    app: wildfly
  ports:
  - name: http
    port: 8080
    targetPort: 8080
    nodePort: 30080
  - name: debug
    port: 8787
    targetPort: 8787
    nodePort: 30787
  - name: admin
    port: 9990
    targetPort: 9990
```

#### 2e. WireMock (`tilt/k8s/wiremock.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: wiremock
spec:
  replicas: 1
  selector:
    matchLabels:
      app: wiremock
  template:
    metadata:
      labels:
        app: wiremock
    spec:
      containers:
      - name: wiremock
        image: wiremock/wiremock:3.3.1
        ports:
        - containerPort: 8080
        args: ["--verbose", "--global-response-templating"]
        volumeMounts:
        - name: stubs
          mountPath: /home/wiremock/mappings
      volumes:
      - name: stubs
        configMap:
          name: wiremock-stubs
---
apiVersion: v1
kind: Service
metadata:
  name: wiremock
spec:
  selector:
    app: wiremock
  ports:
  - port: 8090
    targetPort: 8080
```

#### 2f. Liquibase Job (`tilt/k8s/liquibase-job.yaml`)

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: liquibase-init
spec:
  backoffLimit: 3
  template:
    spec:
      initContainers:
      - name: wait-for-postgres
        image: busybox:1.36
        command: ['sh', '-c', 'until nc -z postgres 5432; do echo waiting for postgres; sleep 2; done']
      containers:
      - name: liquibase
        image: listing-liquibase
        env:
        - name: DB_HOST
          value: postgres
        - name: DB_PORT
          value: "5432"
        - name: DB_USER
          value: listing
        - name: DB_PASSWORD
          value: listing
      restartPolicy: Never
```

### Step 3: Create Custom Docker Images

#### 3a. Wildfly Image (`tilt/docker/Dockerfile.wildfly`)

```dockerfile
FROM jboss/wildfly:26.1.3.Final-jdk17

ARG WILDFLY_HOME=/opt/jboss/wildfly

# Copy Wildfly standalone configuration (datasources, JMS, etc.)
COPY config/standalone-local.xml ${WILDFLY_HOME}/standalone/configuration/standalone.xml

# Copy listing-service WAR (built by Maven)
COPY target/listing-service-*.war ${WILDFLY_HOME}/standalone/deployments/listing-service.war

# Enable debug mode and bind to all interfaces
ENV JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,address=*:8787,server=y,suspend=n"

EXPOSE 8080 8787 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
```

#### 3b. Liquibase Image (`tilt/docker/Dockerfile.liquibase`)

```dockerfile
FROM eclipse-temurin:17-jdk

WORKDIR /liquibase

# Copy all Liquibase JARs (built/downloaded by Maven)
COPY target/liquibase-jars/*.jar /liquibase/

# Copy migration script
COPY tilt/scripts/run-liquibase-all.sh /liquibase/run.sh
RUN chmod +x /liquibase/run.sh

ENTRYPOINT ["/liquibase/run.sh"]
```

### Step 4: Create Standalone Wildfly Configuration

Create `tilt/config/standalone-local.xml` — a simplified Wildfly configuration with:

- **Datasources** pointing to K8s PostgreSQL service:
  - `java:/app/listing-service/DS.eventstore` → `jdbc:postgresql://postgres:5432/listingeventstore`
  - `java:/app/listing-service/DS.viewstore` → `jdbc:postgresql://postgres:5432/listingviewstore`
  - `java:/app/listing-service/DS.system` → `jdbc:postgresql://postgres:5432/listingsystem`
- **JMS** connection factory pointing to Artemis: `tcp://artemis:61616`
- **Topics**: `listing.event`, `public.event`
- **Logging**: Configured for local development (DEBUG level)

> **Note**: Extract the relevant datasource and messaging subsystem sections from the existing `cpp-developers-docker/containers/wildfly/configuration/standalone.xml` and adapt the hostnames to K8s service names.

### Step 5: Create the Tiltfile

Create `tilt/Tiltfile`:

```python
# Tiltfile for cpp-context-listing local development

# ============================================================
# Configuration
# ============================================================
config.define_string('war-path', args=True)
cfg = config.parse()
war_path = cfg.get('war-path', '../listing-service/target')

# ============================================================
# Infrastructure: PostgreSQL
# ============================================================
k8s_yaml('k8s/postgres-init-configmap.yaml')
k8s_yaml('k8s/postgres.yaml')
k8s_resource('postgres',
    port_forwards=['5432:5432'],
    labels=['infra']
)

# ============================================================
# Infrastructure: Artemis
# ============================================================
k8s_yaml('k8s/artemis.yaml')
k8s_resource('artemis',
    port_forwards=['61616:61616', '8161:8161'],
    labels=['infra']
)

# ============================================================
# Infrastructure: WireMock
# ============================================================
k8s_yaml('k8s/wiremock.yaml')
k8s_resource('wiremock',
    port_forwards=['8090:8080'],
    labels=['infra']
)

# ============================================================
# Database Migrations: Liquibase Job
# ============================================================
docker_build('listing-liquibase',
    context='..',
    dockerfile='tilt/docker/Dockerfile.liquibase'
)
k8s_yaml('k8s/liquibase-job.yaml')
k8s_resource('liquibase-init',
    resource_deps=['postgres'],
    labels=['setup']
)

# ============================================================
# Application: Wildfly + listing-service
# ============================================================
docker_build('listing-wildfly',
    context='..',
    dockerfile='tilt/docker/Dockerfile.wildfly',
    live_update=[
        # Sync WAR file changes for hot reload
        sync(war_path, '/opt/jboss/wildfly/standalone/deployments/'),
    ]
)
k8s_yaml('k8s/wildfly.yaml')
k8s_resource('wildfly',
    port_forwards=['8080:8080', '8787:8787', '9990:9990'],
    resource_deps=['postgres', 'artemis', 'liquibase-init'],
    labels=['app']
)

# ============================================================
# Local Maven Build (trigger on source change)
# ============================================================
local_resource('maven-build',
    cmd='cd .. && mvn install -pl listing-service -am -DskipTests -q',
    deps=[
        '../listing-command',
        '../listing-domain',
        '../listing-event',
        '../listing-query',
        '../listing-viewstore',
        '../listing-service',
        '../listing-common',
        '../listing-healthchecks',
    ],
    labels=['build'],
    resource_deps=[]
)
```

### Step 6: Create Integration Test Runner Script

Create `tilt/scripts/run-integration-tests.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TILT_DIR="$(dirname "$SCRIPT_DIR")"
PROJECT_DIR="$(dirname "$TILT_DIR")"

echo "=== Building listing-service WAR ==="
cd "$PROJECT_DIR"
mvn install -DskipTests -q

echo "=== Starting Tilt infrastructure ==="
cd "$TILT_DIR"
tilt ci -- 2>&1 &
TILT_PID=$!

# Wait for Wildfly healthcheck
echo "=== Waiting for listing-service to be healthy ==="
TIMEOUT=180
ELAPSED=0
until curl -sf http://localhost:8080/listing-service/internal/metrics/ping | grep -q pong; do
    sleep 5
    ELAPSED=$((ELAPSED + 5))
    if [ $ELAPSED -ge $TIMEOUT ]; then
        echo "ERROR: Timeout waiting for listing-service"
        kill $TILT_PID 2>/dev/null
        exit 1
    fi
    echo "  Waiting... ($ELAPSED/$TIMEOUT seconds)"
done
echo "=== listing-service is healthy ==="

echo "=== Running integration tests ==="
cd "$PROJECT_DIR"
mvn -B verify -pl listing-integration-test \
    -Plisting-integration-test \
    -DINTEGRATION_HOST_KEY=localhost

TEST_EXIT=$?

echo "=== Tearing down Tilt ==="
kill $TILT_PID 2>/dev/null
tilt down --delete-namespaces 2>/dev/null

exit $TEST_EXIT
```

### Step 7: Create Liquibase Runner Script

Create `tilt/scripts/run-liquibase-all.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-listing}"
DB_PASSWORD="${DB_PASSWORD:-listing}"

run_migration() {
    local jar=$1
    local db=$2
    echo "Running migration: $jar → $db"
    java -jar "/liquibase/$jar" \
        --url="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${db}" \
        --username="${DB_USER}" \
        --password="${DB_PASSWORD}" \
        --logLevel=info \
        update
}

# Event Store migrations
run_migration "event-repository-liquibase.jar" "listingeventstore"
run_migration "aggregate-snapshot-repository-liquibase.jar" "listingeventstore"

# View Store migrations
run_migration "event-buffer-liquibase.jar" "listingviewstore"
run_migration "event-tracking-liquibase.jar" "listingviewstore"
run_migration "listing-viewstore-liquibase.jar" "listingviewstore"

# System migrations
run_migration "framework-system-liquibase.jar" "listingsystem"

echo "=== All Liquibase migrations complete ==="
```

---

## 5. Changes to Existing Files

### 5.1 Maven pom.xml Changes

Add a Maven profile to download Liquibase JARs for the Tilt setup:

**File**: `pom.xml` (root)

```xml
<profile>
    <id>tilt-prepare</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-dependency-plugin</artifactId>
                <executions>
                    <execution>
                        <id>copy-liquibase-jars</id>
                        <phase>package</phase>
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

### 5.2 Integration Test Configuration

**File**: `listing-integration-test/src/test/resources/endpoint.properties`

No changes needed — tests already use `INTEGRATION_HOST_KEY=localhost` and port `8080`.

### 5.3 runIntegrationTests.sh

The existing `runIntegrationTests.sh` can remain for backward compatibility. Create the new Tilt-based script alongside it. Eventually, the old script can be deprecated.

---

## 6. Migration Execution Order

| Step | Action | Deliverable | Effort |
|------|--------|-------------|--------|
| 1 | Install Tilt, kind, kubectl on dev machines | Setup guide | 0.5 day |
| 2 | Create `tilt/k8s/` manifests (postgres, artemis, wiremock) | YAML files | 1 day |
| 3 | Extract and adapt `standalone-local.xml` from cpp-developers-docker | Wildfly config | 1 day |
| 4 | Create Dockerfiles for Wildfly and Liquibase | Docker images | 0.5 day |
| 5 | Create the Tiltfile with resource dependencies | Tiltfile | 1 day |
| 6 | Add `tilt-prepare` Maven profile for Liquibase JARs | pom.xml change | 0.5 day |
| 7 | Extract WireMock stubs to static JSON files | ConfigMap data | 1 day |
| 8 | Test `tilt up` → full service startup | Working environment | 1 day |
| 9 | Test integration tests against Tilt environment | Passing tests | 1 day |
| 10 | Create `run-integration-tests.sh` for CI | CI script | 0.5 day |
| 11 | Document setup for team | Developer guide | 0.5 day |
| **Total** | | | **~8 days** |

---

## 7. CI/CD Integration (Azure DevOps)

To run Tilt-based integration tests in Azure DevOps:

```yaml
# azure-pipelines.yaml addition
- stage: IntegrationTest_Tilt
  jobs:
  - job: TiltIntegration
    pool:
      name: "MDV-ADO-AGENT-AKS-01"
      demands:
        - identifier -equals centos8-j17
    steps:
    - script: |
        # Install kind
        curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
        chmod +x ./kind && sudo mv ./kind /usr/local/bin/kind

        # Install tilt
        curl -fsSL https://raw.githubusercontent.com/tilt-dev/tilt/master/scripts/install.sh | bash

        # Create cluster
        kind create cluster --name listing-it

        # Build and test
        mvn install -DskipTests
        cd tilt && tilt ci
      displayName: 'Run Tilt Integration Tests'
    - script: |
        kind delete cluster --name listing-it
      displayName: 'Cleanup'
      condition: always()
```

---

## 8. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Kind cluster startup time (30-60s) | Slower test feedback | Use persistent cluster; only restart pods |
| Wildfly image size (~1.5GB) | Slow first build | Pre-cache in CI; use `live_update` for iterative builds |
| K8s learning curve | Team adoption friction | Provide setup scripts + documentation; Tilt dashboard helps |
| Port conflicts with cpp-developers-docker | Cannot run both | Use different NodePort ranges; document conflict resolution |
| Standalone.xml drift | Config mismatch with production | Extract from cpp-developers-docker and track changes |
| Liquibase JAR version coupling | Migrations may fail | Pin versions in Maven profile; test on version updates |

---

## 9. Rollback Plan

The migration is additive — the existing `runIntegrationTests.sh` remains functional. If Tilt adoption fails:

1. Delete the `tilt/` directory
2. Remove the `tilt-prepare` Maven profile
3. Continue using `runIntegrationTests.sh` + `cpp-developers-docker`

No existing functionality is modified or removed during the migration.
