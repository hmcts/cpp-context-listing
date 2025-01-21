package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.waitForPutStubToBeReady;

import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.commons.collections.CollectionUtils;

public class WebDavStub {

    public static final String XHIBIT_GATEWAY_SEND_TO_XHIBIT_PATH_REG_EX = "/xhibit-gateway/send-to-xhibit/.*\\.xml";

    public static void acceptCourtListXmlFile(final Response.Status status) {
        stubFor(put(urlPathMatching(XHIBIT_GATEWAY_SEND_TO_XHIBIT_PATH_REG_EX))
                .willReturn(aResponse()
                        .withStatus(status.getStatusCode())
                        .withHeader("CPPID", randomUUID().toString())));

        waitForPutStubToBeReady("/xhibit-gateway/send-to-xhibit/waitForPutStubToBeReady.xml", APPLICATION_XML, status);
    }

    public static Optional<String> getSentXml() {
        final List<LoggedRequest> putRequests = findAll(putRequestedFor(urlPathMatching(XHIBIT_GATEWAY_SEND_TO_XHIBIT_PATH_REG_EX)));
        if (CollectionUtils.isEmpty(putRequests)) {
            return Optional.empty();
        }
        final LoggedRequest loggedRequest = putRequests.get(putRequests.size() - 1);
        return ofNullable(loggedRequest.getBodyAsString());
    }
}
