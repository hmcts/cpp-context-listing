package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub;
import uk.gov.moj.cpp.listing.utils.QueueUtil;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


public class GroupCasesIT extends AbstractIT {

    private static final String LIST_COURT_HEARING_JSON = "list-court-hearing-group-cases";
    private static final String LIST_COURT_HEARING_GROUP_CASES_PART_CASES_JSON = "list-court-hearing-group-cases-part-cases.json";
    private static final String PUBLIC_PROGRESSION_CASE_REMOVED_FROM_GROUP_CASES_JSON = "public.progression.case-removed-from-group-cases.json";
    private static final String MEDIA_TYPE_SEARCH_HEARING_JSON = "application/vnd.listing.search.hearing+json";

    private final UUID hearingId = randomUUID();
    private final UUID groupId = randomUUID();
    private final UUID hearingTypeId = randomUUID();
    private final UUID courtCentreId = getRandomCourtCenterId();
    private final LocalDate startDate = ItClock.today();
    private LocalTime defaultStartTime = LocalTime.parse("10:00");
    private final ZonedDateTime hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);
    private long defaultDuration = 20;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private JmsMessageProducerClient publicMessageProducer;
    private final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps();

    @BeforeEach
    public void setup() {
        publicMessageProducer = QueueUtil.publicEvents.createPublicProducer();
    }

    @Test
    public void shouldSaveGroupCasesWithFilteringMembers() throws IOException {
        final UUID masterCaseId = randomUUID();
        final UUID newGroupMasterCaseId = randomUUID();

        stubGetReferenceDataCourtCentreById(courtCentreId);
        CourtSchedulerServiceStub.stubSearchBookHearingSlotsForCrown(
                hearingId.toString(), courtCentreId.toString(), "28b922c3-0396-3c68-970f-5b805c7ab1bb");
        postListCourtHearingCommand(masterCaseId);

        JsonPath jsonResponse = listCourtHearingSteps.getHearingConfirmedPublicEventPayload();
        assertPublicHearingConfirmed(jsonResponse, masterCaseId);
        assertViewStoreUpdated(masterCaseId, Collections.emptyList());

        publishCaseRemovedFromGroupCasesEvent(masterCaseId, masterCaseId, newGroupMasterCaseId);
        assertViewStoreUpdated(newGroupMasterCaseId, Arrays.asList(masterCaseId));
    }

    private void postListCourtHearingCommand(final UUID masterCaseId) throws IOException {
        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps
                .preparePayloadToListCourtHearingForGroupCases(LIST_COURT_HEARING_JSON,
                        getPayloadValues(), groupId, masterCaseId);

        listCourtHearingSteps.listCourtHearing(listCourtHearingJsonObject, courtCentreId, hearingTypeId);
    }

    private void publishCaseRemovedFromGroupCasesEvent(final UUID masterCaseId, final UUID removedCaseId,
                                                       final UUID newGroupMasterCaseId) throws IOException {
        final JsonObject caseRemovedFromGroupCasesJson = listCourtHearingSteps
                .preparePayloadCaseRemovedFromGroupCases(PUBLIC_PROGRESSION_CASE_REMOVED_FROM_GROUP_CASES_JSON,
                        LIST_COURT_HEARING_GROUP_CASES_PART_CASES_JSON,
                        groupId, masterCaseId, removedCaseId, newGroupMasterCaseId);

        sendMessage(publicMessageProducer,
                "public.progression.case-removed-from-group-cases",
                caseRemovedFromGroupCasesJson,
                metadataBuilder().withId(randomUUID())
                        .withName("public.progression.case-removed-from-group-cases")
                        .withUserId(randomUUID().toString())
                        .build());
    }

    private void assertPublicHearingConfirmed(final JsonPath jsonResponse, final UUID masterCaseId) {
        assertThat(jsonResponse.get("confirmedHearing.id"), is(hearingId.toString()));
        assertThat(jsonResponse.get("confirmedHearing.isGroupProceedings"), is(true));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases"), hasSize(1));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].id"), is(masterCaseId.toString()));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].isCivil"), is(true));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].isGroupMember"), is(true));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].isGroupMaster"), is(true));
        assertThat(jsonResponse.get("confirmedHearing.prosecutionCases[0].groupId"), is(groupId.toString()));
    }

    private void assertViewStoreUpdated(final UUID masterCaseId, final List<UUID> removedCaseIds) {
        final int filteredCases = 1 + removedCaseIds.size();
        final List<Matcher> groupCasesMatcher = new ArrayList<>();

        groupCasesMatcher.add(withJsonPath("$.id", equalTo(hearingId.toString())));
        groupCasesMatcher.add(withJsonPath("$.numberOfGroupCases", equalTo(1000)));

        for (int i = 0; i < removedCaseIds.size(); i++) {
            groupCasesMatcher.add(withJsonPath("$.listedCases[" + (i) + "].isCivil", is(true)));
            groupCasesMatcher.add(withJsonPath("$.listedCases[" + (i) + "].groupId", is(groupId.toString())));
            groupCasesMatcher.add(withJsonPath("$.listedCases[" + (i) + "].isGroupMember", is(false)));
            groupCasesMatcher.add(withJsonPath("$.listedCases[" + (i) + "].isGroupMaster", is(false)));
        }

        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].id", is(masterCaseId.toString())));
        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].isCivil", is(true)));
        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].groupId", is(groupId.toString())));
        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].isGroupMember", is(true)));
        groupCasesMatcher.add(withJsonPath("$.listedCases[" + (filteredCases - 1) + "].isGroupMaster", is(true)));

        verifyHearingInViewStore(groupCasesMatcher);
    }

    private void verifyHearingInViewStore(final List<Matcher> groupCasesMatcher) {
        final String url = generateUrlForFindingAHearingById(hearingId.toString());
        final ResponseData response = pollWithDefaults(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK),
                        payload().isJson(
                                allOf(groupCasesMatcher.toArray(new Matcher[groupCasesMatcher.size()]))));
    }

    private String generateUrlForFindingAHearingById(final String rawId) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearing"),
                        rawId
                ));
    }

    private Map<String, String> getPayloadValues() {
        final Map<String, String> payloadValues = new HashMap<>();

        payloadValues.put("hearingId", hearingId.toString());
        payloadValues.put("courtCentreId", courtCentreId.toString());
        payloadValues.put("startDate", startDate.toString());
        payloadValues.put("hearingStartTime", hearingStartTime.format(formatter));
        payloadValues.put("estimatedMinutes", String.valueOf(defaultDuration));
        payloadValues.put("hearingTypeId", hearingTypeId.toString());

        return payloadValues;
    }
}
