package uk.gov.moj.cpp.listing.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.query.api.HearingQueryApi;
import uk.gov.moj.cpp.listing.support.PostgresContainerSupport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** {@code listing.envelope.enabled=true} restores courtscheduler's {@code _metadata} envelope. */
@SpringBootTest(properties = {"listing.p20-skeleton.enabled=true", "listing.envelope.enabled=true",
        "listing.messaging.enabled=false"})
@AutoConfigureMockMvc
class EnvelopeEnabledTest extends PostgresContainerSupport {

    /** The real query service is mocked: these tests are about the web plumbing, not the query. */
    @MockitoBean
    private HearingQueryApi hearingQueryApi;

    @BeforeEach
    void stubHearingQuery() {
        when(hearingQueryApi.searchForHearingById(any())).thenAnswer(invocation ->
                Json.createObjectBuilder().add("id", ((JsonObject) invocation.getArgument(0)).getString("id")).build());
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void successBodyIsWrappedInMetadataEnvelope() throws Exception {
        mvc.perform(get("/listing-service/query/api/rest/listing/hearings/h1").contextPath("/listing-service")
                        .header("CJSCPPUID", "11111111-1111-1111-1111-111111111111")
                        .accept("application/vnd.listing.search.hearing+json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._metadata.name").value("listing.search.hearing"))
                .andExpect(jsonPath("$._metadata.userId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.id").value("h1"));
    }
}
