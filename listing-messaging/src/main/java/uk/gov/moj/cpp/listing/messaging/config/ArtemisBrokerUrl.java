package uk.gov.moj.cpp.listing.messaging.config;

import java.util.List;
import java.util.StringJoiner;

/**
 * Builds the Artemis core-client URL from {@link MessagingProperties.Artemis}, the same way
 * {@code cp-audit-filter-springboot}'s {@code ArtemisAuditAutoConfiguration} builds it from
 * {@code cp.audit.*} (read from the 1.0.5 jar): one {@code tcp://host:port} per configured host
 * (a list in parentheses when there are several), then {@code ha}, the reconnect/retry tuning and,
 * when SSL is on, the SSL transport parameters.
 */
final class ArtemisBrokerUrl {

    private ArtemisBrokerUrl() {
    }

    static String of(final MessagingProperties.Artemis artemis) {
        final List<String> hosts = artemis.getHosts() == null ? List.of() : artemis.getHosts().stream()
                .filter(host -> host != null && !host.isBlank())
                .map(String::trim)
                .toList();
        if (hosts.isEmpty()) {
            throw new IllegalStateException("listing.messaging.artemis.hosts must contain at least one broker host");
        }
        if (artemis.getPort() <= 0) {
            throw new IllegalStateException("listing.messaging.artemis.port must be a positive integer");
        }

        final StringJoiner servers = new StringJoiner(",");
        hosts.forEach(host -> servers.add("tcp://" + host + ":" + artemis.getPort()));
        final String serverList = hosts.size() == 1 ? servers.toString() : "(" + servers + ")";

        final MessagingProperties.Jms jms = artemis.getJms();
        final StringJoiner params = new StringJoiner("&");
        params.add("ha=" + artemis.isHighAvailability());
        params.add("reconnectAttempts=" + jms.getReconnectAttempts());
        params.add("initialConnectAttempts=" + jms.getInitialConnectAttempts());
        params.add("retryInterval=" + jms.getRetryIntervalMs());
        params.add("retryIntervalMultiplier=" + jms.getRetryMultiplier());
        params.add("maxRetryInterval=" + jms.getMaxRetryIntervalMs());
        params.add("connectionTTL=" + jms.getConnectionTtlMs());
        params.add("callTimeout=" + jms.getCallTimeoutMs());
        if (artemis.isSslEnabled()) {
            params.add("sslEnabled=true");
            params.add("verifyHost=" + artemis.isVerifyHost());
            addIfSet(params, "trustStorePath", artemis.getTruststore());
            addIfSet(params, "trustStorePassword", artemis.getTruststorePassword());
            if (artemis.isClientAuthRequired()) {
                if (isBlank(artemis.getKeystore()) || isBlank(artemis.getKeystorePassword())) {
                    throw new IllegalStateException(
                            "client-auth-required=true requires listing.messaging.artemis.keystore and keystore-password");
                }
            }
            addIfSet(params, "keyStorePath", artemis.getKeystore());
            addIfSet(params, "keyStorePassword", artemis.getKeystorePassword());
        }
        return serverList + "?" + params;
    }

    private static void addIfSet(final StringJoiner params, final String name, final String value) {
        if (!isBlank(value)) {
            params.add(name + "=" + value);
        }
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
