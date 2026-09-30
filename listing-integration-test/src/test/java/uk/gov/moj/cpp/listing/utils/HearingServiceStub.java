package uk.gov.moj.cpp.listing.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.apache.http.HttpStatus.SC_OK;

import java.util.UUID;

/**
 * Stubs the hearing context's PTPH-detail query (LPT-2401/2404), which listing calls when a
 * trial is listed from a seeding hearing (LPT-2405).
 * <p>
 * The hearing context returns {@code finalised: false} with null fields when no record
 * exists, so "nothing saved" is a successful response rather than an error.
 */
public class HearingServiceStub {

    /**
     * The generated remote client targets {@code /hearing-query-api/query/api/rest/hearing},
     * but haproxy rewrites {@code /<context>-<anything>/} to {@code /<context>-service/} for
     * paths containing {@code query-api}, so the request reaches WireMock as
     * {@code /hearing-service/...}. Stub the rewritten path, not the client's base uri.
     */
    private static final String HEARING_QUERY_PTPH_DETAIL_URL = "/hearing-service/query/api/rest/hearing/hearings/.*/ptph-detail";
    private static final String HEARING_QUERY_PTPH_DETAIL_MEDIA_TYPE = "application/vnd.hearing.get-ptph-detail+json";

    public static void stubFinalisedPtphDetail(final String tier, final String listType, final String keyReason) {
        stubPtphDetail(String.format(
                "{\"tier\":\"%s\",\"listType\":\"%s\",\"keyReason\":\"%s\",\"finalised\":true}",
                tier, listType, keyReason));
    }

    public static void stubNotFinalisedPtphDetail(final String tier, final String listType) {
        stubPtphDetail(String.format(
                "{\"tier\":\"%s\",\"listType\":\"%s\",\"finalised\":false}", tier, listType));
    }

    public static void stubNoPtphDetail() {
        stubPtphDetail("{\"finalised\":false}");
    }

    private static void stubPtphDetail(final String body) {
        stubFor(get(urlMatching(HEARING_QUERY_PTPH_DETAIL_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", HEARING_QUERY_PTPH_DETAIL_MEDIA_TYPE)
                        .withBody(body)));
    }
}
