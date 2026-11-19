package uk.gov.moj.cpp.listing.common.client;

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Outbound HTTP configuration (§6), bound from {@code listing.clients.*} (see listing-api's
 * application.yaml for the {@code LISTING_*} environment variables).
 *
 * @param baseUrl                the CPP gateway that serves the other contexts' REST APIs
 *                               (referencedata, progression, defence, usersgroups, systemdocgenerator);
 *                               the framework's per-service context roots are appended to it
 * @param courtschedulerBaseUrl  listingcourtscheduler's REST root (legacy {@code courtscheduler.base.url})
 * @param hearingQueryBaseUrl    the hearing context's query REST root (legacy {@code hearing.query.base.url};
 *                               default: {@code <baseUrl>/hearing-service/query/api/rest/hearing})
 * @param progressionCommandBaseUrl progression's command REST root (legacy {@code progression.base.url};
 *                               default: {@code <baseUrl>/progression-service/command/api/rest/progression})
 * @param systemUserId           the CJSCPPUID sent on calls the framework made "as admin" / as the
 *                               context system user
 * @param connectTimeoutSeconds  connect timeout
 * @param readTimeoutSeconds     read timeout
 */
@ConfigurationProperties(prefix = "listing.clients")
public record ListingClientsProperties(String baseUrl,
                                       String courtschedulerBaseUrl,
                                       UUID systemUserId,
                                       Integer connectTimeoutSeconds,
                                       Integer readTimeoutSeconds,
                                       String hearingQueryBaseUrl,
                                       String progressionCommandBaseUrl) {

    /** Without the P2.3.3 hearing-query and progression-command roots (their defaults apply). */
    public ListingClientsProperties(final String baseUrl, final String courtschedulerBaseUrl, final UUID systemUserId,
                                    final Integer connectTimeoutSeconds, final Integer readTimeoutSeconds) {
        this(baseUrl, courtschedulerBaseUrl, systemUserId, connectTimeoutSeconds, readTimeoutSeconds, null, null);
    }

    @ConstructorBinding
    public ListingClientsProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8080" : stripTrailingSlash(baseUrl);
        courtschedulerBaseUrl = courtschedulerBaseUrl == null || courtschedulerBaseUrl.isBlank()
                ? baseUrl + "/listingcourtscheduler-api/rest/courtscheduler"
                : stripTrailingSlash(courtschedulerBaseUrl);
        // Deployed context roots, as the legacy ITs stub them (haproxy rewrote the generated clients'
        // /<context>-query-api and /<context>-command-api roots to /<context>-service).
        hearingQueryBaseUrl = hearingQueryBaseUrl == null || hearingQueryBaseUrl.isBlank()
                ? baseUrl + "/hearing-service/query/api/rest/hearing"
                : stripTrailingSlash(hearingQueryBaseUrl);
        progressionCommandBaseUrl = progressionCommandBaseUrl == null || progressionCommandBaseUrl.isBlank()
                ? baseUrl + "/progression-service/command/api/rest/progression"
                : stripTrailingSlash(progressionCommandBaseUrl);
        connectTimeoutSeconds = connectTimeoutSeconds == null ? 5 : connectTimeoutSeconds;
        readTimeoutSeconds = readTimeoutSeconds == null ? 30 : readTimeoutSeconds;
    }

    public UUID requiredSystemUserId() {
        if (systemUserId == null) {
            throw new IllegalStateException("listing.clients.system-user-id (LISTING_SYSTEM_USER_ID) is not configured");
        }
        return systemUserId;
    }

    private static String stripTrailingSlash(final String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
