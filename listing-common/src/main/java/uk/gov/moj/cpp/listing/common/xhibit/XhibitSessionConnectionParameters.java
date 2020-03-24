package uk.gov.moj.cpp.listing.common.xhibit;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

public class XhibitSessionConnectionParameters {

    @Inject
    @Value(key = "webdav.outbound.url", defaultValue = "http://localhost:8080/xhibit-gateway/send-to-xhibit/")
    private String outboundUrl;

    @Inject
    @Value(key = "webdav.user", defaultValue = "listing")
    private String user;

    @Inject
    @Value(key = "webdav.password", defaultValue = "listing")
    private String password;

    public String getOutboundUrl() {
        return outboundUrl;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
