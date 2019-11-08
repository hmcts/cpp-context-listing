package uk.gov.moj.cpp.listing.common.xhibit;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;

import com.github.sardine.Sardine;

@Named
public class XhibitSessionFactory {

    @Inject
    private XhibitSessionConnectionParameters xhibitSessionConnectionParameters;

    @Inject
    private SardineClientFactory sardineClientFactory;

    @Inject
    private UrlFactory urlFactory;


    public XhibitSession createSession() {

        final URL outboundUrl = urlFactory.create(xhibitSessionConnectionParameters.getOutboundUrl());

        final Sardine client = sardineClientFactory.createSardineClient(
                xhibitSessionConnectionParameters.getUser(),
                xhibitSessionConnectionParameters.getPassword());

        client.enablePreemptiveAuthentication(outboundUrl);

        return new XhibitSession(outboundUrl, client);
    }
}
