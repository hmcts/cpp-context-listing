package uk.gov.moj.cpp.listing.event.processor.xhibit;

import uk.gov.justice.services.common.configuration.Value;

import javax.inject.Inject;

public class XhibitSessionConnectionParameters {

    @Inject
    @Value(key = "xhibit.outbound.url", defaultValue = "http://localhost")
    private String outboundUrl;

    @Inject
    @Value(key = "xhibit.user", defaultValue = "user")
    private String user;

    @Inject
    @Value(key = "xhibit.password", defaultValue = "password")
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
