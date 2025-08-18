package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.progression.courts.CaseLinked.caseLinked;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.progression.courts.CaseLinked;
import uk.gov.justice.progression.courts.Cases;
import uk.gov.justice.progression.courts.LinkActionType;
import uk.gov.justice.progression.courts.LinkedToCases;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.Arrays;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class CaseLinkedIT extends AbstractIT {
    private static final UUID CASE_TO_BE_LINKED_1 = randomUUID();
    private static final String CASE_URN_TO_BE_LINKED_1 = STRING.next();
    private static final UUID CASE_TO_BE_LINKED_2 = randomUUID();
    private static final String CASE_URN_TO_BE_LINKED_2 = STRING.next();

    private static final String PUBLIC_EVENT_PROGRESSION_CASE_LINKED = "public.progression.case-linked";
    private static final String MEDIA_TYPE_SEARCH_HEARING_JSON = "application/vnd.listing.search.hearing+json";

    private JmsMessageProducerClient publicEventProgressionCaseLinked;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    @BeforeEach
    void setup() {
        publicEventProgressionCaseLinked = publicEvents.createPublicProducer();
    }

    @Test
    @Disabled("Will be fixed with SPRDT-181")
    void shouldUpdateLinkedCases() {
        final HearingsData hearingsData = listCourtHearing();
        final UUID hearingId = hearingsData.getHearingData().get(0).getId();
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final String caseUrn = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseReference();
        final int numberOfListedCases = hearingsData.getHearingData().get(0).getListedCases().size();

        //LINK EVENT
        publishCasesLinkedEvent(createCaseLinkedEvent(caseId, caseUrn));

        final Matcher[] caseLinkedEventMatchers = new Matcher[]{
                withJsonPath("$.id", equalTo(hearingId.toString())),
                withJsonPath("$.listedCases.length()", equalTo(numberOfListedCases)),
                anyOf(createLinkedCaseMatcher(0), createLinkedCaseMatcher(1))
        };

        verifyHearing(hearingId, caseLinkedEventMatchers);

        //UNLINK EVENT
        publishCasesLinkedEvent(createCaseUnlinkedEvent(caseId, caseUrn));
        final Matcher[] caseUnlinkedEventMatchers = new Matcher[]{
                withJsonPath("$.id", equalTo(hearingId.toString())),
                withJsonPath("$.listedCases.length()", equalTo(numberOfListedCases)),
                anyOf(createUnlinkedCaseMatcher(0), createUnlinkedCaseMatcher(1))
        };
        verifyHearing(hearingId, caseUnlinkedEventMatchers);

    }

    private Matcher createLinkedCaseMatcher(final int caseIndex) {
        final String prefix = String.format("$.listedCases[%s].linkedCases", caseIndex);
        return allOf(
                withJsonPath(prefix + ".length()", equalTo(2)),
                withJsonPath(prefix + "[0].caseId", anyOf(equalTo(CASE_TO_BE_LINKED_1.toString()), equalTo(CASE_TO_BE_LINKED_2.toString()))),
                withJsonPath(prefix + "[0].caseUrn", anyOf(equalTo(CASE_URN_TO_BE_LINKED_1), equalTo(CASE_URN_TO_BE_LINKED_2))),
                withJsonPath(prefix + "[1].caseId", anyOf(equalTo(CASE_TO_BE_LINKED_1.toString()), equalTo(CASE_TO_BE_LINKED_2.toString()))),
                withJsonPath(prefix + "[1].caseUrn", anyOf(equalTo(CASE_URN_TO_BE_LINKED_1), equalTo(CASE_URN_TO_BE_LINKED_2)))
        );
    }

    private Matcher createUnlinkedCaseMatcher(final int caseIndex) {
        final String prefix = String.format("$.listedCases[%s].linkedCases", caseIndex);
        return allOf(
                withJsonPath(prefix + ".length()", equalTo(1)),
                withJsonPath(prefix + "[0].caseId", equalTo(CASE_TO_BE_LINKED_1.toString())),
                withJsonPath(prefix + "[0].caseUrn", equalTo(CASE_URN_TO_BE_LINKED_1))
        );
    }

    private String verifyHearing(final UUID hearingId, final Matcher[] matchers) {
        final String url = generateUrlForFindingAHearingById(hearingId.toString());
        final ResponseData response = pollWithDefaults(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK),
                        payload().isJson(
                                allOf(matchers)));

        return response.getPayload();
    }

    private String generateUrlForFindingAHearingById(final String rawId) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearing"),
                        rawId
                ));
    }

    private CaseLinked createCaseLinkedEvent(final UUID caseId, final String caseUrn) {
        return caseLinked()
                .withLinkActionType(LinkActionType.LINK)
                .withCases(Arrays.asList(
                                Cases.cases()
                                        .withCaseId(caseId)
                                        .withCaseUrn(caseUrn)
                                        .withLinkedToCases(
                                                Arrays.asList(
                                                        LinkedToCases.linkedToCases()
                                                                .withCaseId(CASE_TO_BE_LINKED_1)
                                                                .withCaseUrn(CASE_URN_TO_BE_LINKED_1)
                                                                .build(),
                                                        LinkedToCases.linkedToCases()
                                                                .withCaseId(CASE_TO_BE_LINKED_2)
                                                                .withCaseUrn(CASE_URN_TO_BE_LINKED_2)
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                ).build();
    }

    private CaseLinked createCaseUnlinkedEvent(final UUID caseId, final String caseUrn) {
        return caseLinked()
                .withLinkActionType(LinkActionType.UNLINK)
                .withCases(Arrays.asList(
                                Cases.cases()
                                        .withCaseId(caseId)
                                        .withCaseUrn(caseUrn)
                                        .withLinkedToCases(
                                                Arrays.asList(
                                                        LinkedToCases.linkedToCases()
                                                                .withCaseId(CASE_TO_BE_LINKED_2)
                                                                .withCaseUrn(CASE_URN_TO_BE_LINKED_2)
                                                                .build()
                                                )
                                        )
                                        .build()
                        )
                ).build();
    }


    private void publishCasesLinkedEvent(final CaseLinked caseLinked) {
        final JsonObject caseLinkedObject = (JsonObject) objectToJsonValueConverter.convert(caseLinked);

        sendMessage(
                publicEventProgressionCaseLinked,
                PUBLIC_EVENT_PROGRESSION_CASE_LINKED,
                caseLinkedObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_PROGRESSION_CASE_LINKED).withUserId(randomUUID().toString()).build());
    }

    private HearingsData listCourtHearing() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        return hearingsData;
    }
}
