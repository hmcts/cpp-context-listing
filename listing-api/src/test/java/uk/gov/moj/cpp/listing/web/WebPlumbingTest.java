package uk.gov.moj.cpp.listing.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * The web plumbing in one Spring context, with the P2.0 skeleton command endpoint, the query
 * service mocked (P2.1) and authz off
 * (authz is proven end-to-end in the integration tests against WireMock).
 */
@SpringBootTest(properties = {"listing.p20-skeleton.enabled=true", "management.health.jms.enabled=false"})
@AutoConfigureMockMvc
class WebPlumbingTest extends PostgresContainerSupport {

    /** The real query service is mocked: these tests are about the web plumbing, not the query. */
    @MockitoBean
    private HearingQueryApi hearingQueryApi;

    @BeforeEach
    void stubHearingQuery() {
        when(hearingQueryApi.searchForHearingById(any())).thenAnswer(invocation ->
                Json.createObjectBuilder().add("id", ((JsonObject) invocation.getArgument(0)).getString("id")).build());
    }

    private static final String CTX = "/listing-service";
    private static final String HEARING = CTX + "/query/api/rest/listing/hearings/2f6f2b0a-0b0c-4f43-9d1c-5f4e7f6d8a01";
    private static final String NOTE = CTX + "/command/api/rest/listing/listing-note";
    private static final String SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    private static final String CREATE_NOTE = "application/vnd.listing.command.create-listing-note+json";
    private static final String VALID_NOTE =
            "{\"hearingDate\":\"2020-01-09\",\"courtRoomId\":\"a299cf49-d14a-496f-895f-59c46859b4a9\",\"noteDescription\":\"n\"}";

    @Autowired
    private MockMvc mvc;

    private static MockHttpServletRequestBuilder inContext(final MockHttpServletRequestBuilder builder) {
        return builder.contextPath(CTX).header("CJSCPPUID", "11111111-1111-1111-1111-111111111111");
    }

    @Test
    void queryResponseIsPayloadOnlyWithCppidHeader() throws Exception {
        mvc.perform(inContext(get(HEARING)).accept(SEARCH_HEARING))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString(SEARCH_HEARING.replace("+json", ""))))
                .andExpect(header().exists("CPPID"))
                .andExpect(jsonPath("$.id").value("2f6f2b0a-0b0c-4f43-9d1c-5f4e7f6d8a01"))
                .andExpect(jsonPath("$._metadata").doesNotExist());
    }

    @Test
    void unknownAcceptIs406WithErrorBody() throws Exception {
        mvc.perform(inContext(get(HEARING)).accept("application/json"))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(inContext(get(HEARING)).accept("application/vnd.listing.search.hearings+json"))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void severalVendorTypesInAcceptAre406() throws Exception {
        mvc.perform(inContext(get(HEARING)).header("Accept",
                        "application/vnd.listing.search.hearings+json, " + SEARCH_HEARING))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    void unknownPathIs404AndWrongMethodIs405() throws Exception {
        mvc.perform(inContext(get(CTX + "/query/api/rest/listing/no-such-thing")).accept(SEARCH_HEARING))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
        mvc.perform(inContext(post(HEARING)).contentType(CREATE_NOTE).content(VALID_NOTE))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void wrongContentTypeIs415() throws Exception {
        mvc.perform(inContext(post(NOTE)).contentType("application/vnd.listing.command.edit-listing-note+json")
                        .content("{\"noteDescription\":\"n\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void validCommandBodyPassesSchemaValidation() throws Exception {
        mvc.perform(inContext(post(NOTE)).contentType(CREATE_NOTE).content(VALID_NOTE))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void schemaInvalidBodyIs400WithErrorAndValidationErrors() throws Exception {
        mvc.perform(inContext(post(NOTE)).contentType(CREATE_NOTE).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("JSON schema validation has failed")))
                .andExpect(jsonPath("$.validationErrors.message").exists())
                .andExpect(jsonPath("$.validationErrors.causingExceptions").isArray());
        mvc.perform(inContext(post(NOTE)).contentType(CREATE_NOTE)
                        .content(VALID_NOTE.replace("a299cf49-d14a-496f-895f-59c46859b4a9", "x")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("courtRoomId")));
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(inContext(post(NOTE)).contentType(CREATE_NOTE).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Request body is not valid JSON"));
    }

    @Test
    void actuatorHealthIsOutsideTheApiRoots() throws Exception {
        mvc.perform(get(CTX + "/actuator/health").contextPath(CTX))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("UP")))
                .andExpect(content().string(not(containsString("_metadata"))));
    }
}
