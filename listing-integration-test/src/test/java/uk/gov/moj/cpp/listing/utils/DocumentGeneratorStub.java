package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.*;
import static javax.ws.rs.core.Response.Status.OK;

import java.util.List;

import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.client.ValueMatchingStrategy;

public class DocumentGeneratorStub {

    public static final String PATH = "/system-documentgenerator-api/rest/documentgenerator/render";

    public static void stubDocumentCreate(final String documentText) {
        stubDocumentCreate(documentText, OK);
    }

    public static void stubDocumentCreate(final String documentText, final Status expectedStatus) {
        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.system.documentgenerator.render+json"))
                .willReturn(aResponse().withStatus(expectedStatus.getStatusCode())
                        .withBody(documentText.getBytes())));
    }

    public static void stubDocumentCreateWithRequestBody(final String documentText, final String requestContainsString) {
        stubFor(post(urlPathMatching(PATH))
                .withHeader(CONTENT_TYPE, equalTo("application/vnd.system.documentgenerator.render+json"))
                .withRequestBody(containing(requestContainsString))
                .willReturn(aResponse().withStatus(OK.getStatusCode())
                        .withBody(documentText.getBytes())));
    }
}
