package uk.gov.moj.cpp.listing.utils;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.text.MessageFormat.format;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.steps.data.Judge;
import uk.gov.moj.cpp.listing.steps.data.CourtReferenceData;

import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;

public class ReferenceDataStub {

    private static final String FIELD_GENERIC_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_COURT_ROOMS = "courtRooms";

    private static final String FIELD_TITLE = "title";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";


    private static final String REFERENCE_DATA_RESULT_DEFINITIONS_QUERY_URL = "/referencedata-query-api/query/api/rest/referencedata/result/definitions";
    private static final String REFERENCE_DATA_RESULT_DEFINITIONS_KEYWORDS_QUERY_URL = "/referencedata-query-api/query/api/rest/referencedata/result/definition-keyword-synonyms";
    private static final String REFERENCE_DATA_RESULT_PROMPTS_QUERY_URL = "/referencedata-query-api/query/api/rest/referencedata/result/prompts";
    private static final String REFERENCE_DATA_COURT_CENTRE_QUERY_URL = "/referencedata-query-api/query/api/rest/referencedata/court/centres/{0}";
    private static final String REFERENCE_DATA_JUDGE_QUERY_URL = "/referencedata-query-api/query/api/rest/referencedata/court/judges/{0}";
    private static final String REFERENCE_DATA_RESULT_PROMPT_FIXED_LISTS_QUERY_URL = "/referencedata-query-api/query/api/rest/referencedata/result/prompt-fixedlists";
    private static final String REFERENCE_DATA_RESULT_PROMPTS_KEYWORDS_QUERY_URL = "/referencedata-query-api/query/api/rest/referencedata/result/prompt-keyword-synonyms";


    private static final String REFERENCE_DATA_RESULT_DEFINITIONS_MEDIA_TYPE = "application/vnd.referencedata.result.get-all-definitions+json";
    private static final String REFERENCE_DATA_RESULT_DEFINITIONS_KEYWORDS_MEDIA_TYPE = "application/vnd.referencedata.result.get-all-definition-keyword-synonyms";
    private static final String REFERENCE_DATA_RESULT_PROMPTS_MEDIA_TYPE = "application/vnd.referencedata.result.get-all-prompts+json";
    private static final String REFERENCE_DATA_JUDGE_MEDIA_TYPE = "application/vnd.referencedata.get-judge+json";
    private static final String REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE = "application/vnd.referencedata.get.court-centre+json";
    private static final String REFERENCE_DATA_RESULT_PROMPT_FIXED_LISTS_MEDIA_TYPE = "application/vnd.referencedata.result.get-all-prompt-fixedlists+json";
    private static final String REFERENCE_DATA_RESULT_PROMPTS_KEYWORDS_MEDIA_TYPE = "application/vnd.referencedata.result.get-all-prompt-keyword-synonyms+json";


    public static void stubForReferenceDataResults(){
        stubGetReferenceDataResultDefinitions();
        stubGetReferenceDataResultDefinitionsKeywords();
        stubGetReferenceDataResultPrompts();
        stubGetReferenceDataResultPromptsKeywords();
        stubGetReferenceDataResultPromptFixedLists();
    }

    public static void stubGetReferenceDataCourtCentre(final CourtReferenceData courtReferenceData) {
        InternalEndpointMockUtils.stubPingFor("referencedata-query-api");

        stubFor(get(urlPathEqualTo(format(REFERENCE_DATA_COURT_CENTRE_QUERY_URL, courtReferenceData.getCourtCentreId())))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE)
                        .withBody(getCourtCentreJsonBuilder(courtReferenceData).build().toString())));


        waitForStubToBeReady(format(REFERENCE_DATA_COURT_CENTRE_QUERY_URL, courtReferenceData.getCourtCentreId()), REFERENCE_DATA_COURT_CENTRE_MEDIA_TYPE);
    }

    public static void stubGetReferenceDataJudge(final Judge judge) {
        InternalEndpointMockUtils.stubPingFor("referencedata-query-api");

        stubFor(get(urlPathEqualTo(format(REFERENCE_DATA_JUDGE_QUERY_URL, judge.getJudgeId())))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_JUDGE_MEDIA_TYPE)
                        .withBody(getJudgeJsonBuilder(judge).build().toString())));

        waitForStubToBeReady(format(REFERENCE_DATA_JUDGE_QUERY_URL, judge.getJudgeId()), REFERENCE_DATA_JUDGE_MEDIA_TYPE);
    }



    private static void stubGetReferenceDataResultDefinitions() {
        InternalEndpointMockUtils.stubPingFor("referencedata-query-api");

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_RESULT_DEFINITIONS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_RESULT_DEFINITIONS_MEDIA_TYPE)
                        .withBody(getPayload("referencedata.result.definitions.json"))));

        waitForStubToBeReady(REFERENCE_DATA_RESULT_DEFINITIONS_QUERY_URL, REFERENCE_DATA_RESULT_DEFINITIONS_MEDIA_TYPE);
    }

    private static void stubGetReferenceDataResultDefinitionsKeywords() {
        InternalEndpointMockUtils.stubPingFor("referencedata-query-api");

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_RESULT_DEFINITIONS_KEYWORDS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_RESULT_DEFINITIONS_KEYWORDS_MEDIA_TYPE)
                        .withBody(getPayload("referencedata.result.definition-keyword-synonyms.json"))));

        waitForStubToBeReady(REFERENCE_DATA_RESULT_DEFINITIONS_KEYWORDS_QUERY_URL, REFERENCE_DATA_RESULT_DEFINITIONS_KEYWORDS_MEDIA_TYPE);
    }

    private static void stubGetReferenceDataResultPrompts() {
        InternalEndpointMockUtils.stubPingFor("referencedata-query-api");

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_RESULT_PROMPTS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_RESULT_PROMPTS_MEDIA_TYPE)
                        .withBody(getPayload("referencedata.result.prompts.json"))));

        waitForStubToBeReady(REFERENCE_DATA_RESULT_PROMPTS_QUERY_URL, REFERENCE_DATA_RESULT_PROMPTS_MEDIA_TYPE);
    }

    private static void stubGetReferenceDataResultPromptFixedLists() {
        InternalEndpointMockUtils.stubPingFor("referencedata-query-api");

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_RESULT_PROMPT_FIXED_LISTS_QUERY_URL))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_RESULT_PROMPT_FIXED_LISTS_MEDIA_TYPE)
                        .withBody(getPayload("referencedata.result.prompt-fixedlists.json"))));

        waitForStubToBeReady(REFERENCE_DATA_RESULT_PROMPT_FIXED_LISTS_QUERY_URL, REFERENCE_DATA_RESULT_PROMPT_FIXED_LISTS_MEDIA_TYPE);
    }

    private static void stubGetReferenceDataResultPromptsKeywords() {
        InternalEndpointMockUtils.stubPingFor("referencedata-query-api");

        stubFor(get(urlPathEqualTo(REFERENCE_DATA_RESULT_PROMPTS_KEYWORDS_QUERY_URL ))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", REFERENCE_DATA_RESULT_PROMPTS_KEYWORDS_MEDIA_TYPE)
                        .withBody(getPayload("referencedata.result.prompt-keyword-synonyms.json"))));

        waitForStubToBeReady(REFERENCE_DATA_RESULT_PROMPTS_KEYWORDS_QUERY_URL, REFERENCE_DATA_RESULT_PROMPTS_KEYWORDS_MEDIA_TYPE);
    }


    private static JsonObjectBuilder getCourtCentreJsonBuilder(final CourtReferenceData rd) {
        final JsonArrayBuilder courtRooms = createArrayBuilder()
                .add(createObjectBuilder()
                        .add(FIELD_GENERIC_ID, rd.getCourtRoomId().toString())
                        .add(FIELD_NAME, rd.getCourtRoomName()))
                .add(createObjectBuilder()
                        .add(FIELD_GENERIC_ID, UUID.randomUUID().toString())
                        .add(FIELD_NAME, RandomGenerator.STRING.next()));

        return createObjectBuilder()
                .add(FIELD_GENERIC_ID, rd.getCourtCentreId().toString())
                .add(FIELD_NAME, rd.getCourtCentreName())
                .add(FIELD_COURT_ROOMS, courtRooms.build());
    }



    private static JsonObjectBuilder getJudgeJsonBuilder(final Judge judge) {
        return createObjectBuilder()
                .add(FIELD_GENERIC_ID, judge.getJudgeId().toString())
                .add(FIELD_TITLE, judge.getJudgeTitle())
                .add(FIELD_FIRST_NAME, judge.getJudgeFirstName())
                .add(FIELD_LAST_NAME, judge.getJudgeLastName());

    }



}
