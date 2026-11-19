package uk.gov.moj.cpp.listing.xhibit.webdav;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code listing.xhibit.webdav.*} (application-xhibit.yml): the legacy {@code webdav.outbound.url},
 * {@code webdav.user} and {@code webdav.password} JNDI values, with the same defaults.
 */
@ConfigurationProperties(prefix = "listing.xhibit.webdav")
public class XhibitWebDavProperties {

    private String outboundUrl = "http://localhost:8080/xhibit-gateway/send-to-xhibit/";
    private String user = "listing";
    private String password = "listing";
    private int connectTimeoutSeconds = 5;
    private int requestTimeoutSeconds = 60;

    public String getOutboundUrl() {
        return outboundUrl;
    }

    public void setOutboundUrl(final String outboundUrl) {
        this.outboundUrl = outboundUrl;
    }

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(final int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(final int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }
}
