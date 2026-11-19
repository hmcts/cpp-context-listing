package uk.gov.moj.cpp.listing.legacyharness;

/**
 * Where the legacy {@code *IT} suite finds the Spring stack (MBD refactor P2.3.0, §9, ADR 014): the
 * docker-compose IT stack of listing-integration-test-spring plus the legacy overlay
 * ({@code docker/docker-compose.legacy.yml}: a gateway that rewrites {@code /<ctx>-query-api/...} to
 * {@code /<ctx>-service/...} as cpp-developers-docker's haproxy did, and the Artemis console).
 *
 * <p>Replaces the fixed {@code localhost:8080} / {@code :61616} / {@code :8161} of the
 * WildFly + cpp-developers-docker harness. Every value is a system property set by the Gradle test
 * task, with the compose defaults.
 */
public final class LegacyStack {

    private LegacyStack() {
    }

    /** The app behind the gateway, without a trailing slash (was {@code http://localhost:8080}). */
    public static String baseUri() {
        return System.getProperty("legacy.baseUri", "http://localhost:8192");
    }

    public static String wireMockHost() {
        return System.getProperty("legacy.wiremock.host", "localhost");
    }

    public static int wireMockPort() {
        return Integer.getInteger("legacy.wiremock.port", 8191);
    }

    /** Artemis core URL with the IT broker's credentials (was {@code tcp://localhost:61616}, anonymous). */
    public static String artemisUrl() {
        return System.getProperty("legacy.artemis.url", "tcp://localhost:61626?user=listing&password=listing");
    }

    /** Artemis Jolokia endpoint (was {@code http://localhost:8161/console/jolokia/}). */
    public static String jolokiaUrl() {
        return System.getProperty("legacy.jolokia.url", "http://localhost:8171/console/jolokia/");
    }

    public static String jolokiaUser() {
        return System.getProperty("legacy.jolokia.user", "listing");
    }

    public static String jolokiaPassword() {
        return System.getProperty("legacy.jolokia.password", "listing");
    }

    /** The broker name in the Artemis MBean names (was {@code "default"}). */
    public static String brokerName() {
        return System.getProperty("legacy.artemis.brokerName", "0.0.0.0");
    }

    public static String dbUrl() {
        return System.getProperty("legacy.db.url", "jdbc:postgresql://localhost:55436/listing");
    }

    public static String dbUser() {
        return System.getProperty("legacy.db.user", "listing");
    }

    public static String dbPassword() {
        return System.getProperty("legacy.db.password", "listing");
    }
}
