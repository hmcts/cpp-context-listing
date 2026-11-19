package uk.gov.moj.cpp.listing.common.client;

import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbound HTTP configuration (§6), bound from {@code listing.clients.*} (see listing-api's
 * application.yaml for the {@code LISTING_*} environment variables).
 *
 * @param baseUrl                the CPP gateway that serves the other contexts' REST APIs
 *                               (referencedata, progression, defence, usersgroups, systemdocgenerator);
 *                               the framework's per-service context roots are appended to it
 * @param courtschedulerBaseUrl  listingcourtscheduler's REST root (legacy {@code courtscheduler.base.url})
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
                                       Integer readTimeoutSeconds) {

    public ListingClientsProperties {
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://localhost:8080" : stripTrailingSlash(baseUrl);
        courtschedulerBaseUrl = courtschedulerBaseUrl == null || courtschedulerBaseUrl.isBlank()
                ? baseUrl + "/listingcourtscheduler-api/rest/courtscheduler"
                : stripTrailingSlash(courtschedulerBaseUrl);
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
