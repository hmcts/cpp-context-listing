package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.SequenceHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Filter;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class HearingDaysIT extends AbstractIT {

    private static final String LIST_COURT_HEARING_JSON = "list-court-hearing.json";
    private static final String UPDATE_HEARING_FOR_LISTING_JSON = "update-hearing-for-listing.json";
    private static final String UPDATE_HEARING_FOR_LISTING_SPLIT_JSON = "update-hearing-for-listing-split.json";
    private static final String UPDATE_ALLOCATED_HEARING_FOR_LISTING_JSON = "update-allocated-hearing-for-listing.json";
    private static final String MEDIA_TYPE_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE = "application/vnd.listing.correct-hearing-days-without-court-centre+json";
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String ALPHABETICAL = "Alphabetical";
    private static final String PUBLIC = "Public";
    public static final String STANDARD = "Standard";

    private UUID hearingId;
    private UUID caseId;
    private UUID courtCentreId;
    private UUID courtRoomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private ZonedDateTime hearingStartTime;
    private String caseUrn;
    private LocalTime defaultStartTime = LocalTime.parse("10:00");
    private int defaultDuration = 20;
    private UUID otherCourtCentreId;
    private UUID otherCourtRoomId;


    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Test
    public void testHearingDaysWithCourtCentre() throws IOException {
        stubGetAvailableHearingSlots();
        startDate = LocalDate.now();
        endDate = LocalDate.now().plusDays(1);
        hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);
        hearingId = randomUUID();
        caseId = randomUUID();
        courtCentreId = randomUUID();
        stubGetReferenceDataCourtCentreById(courtCentreId);
        courtRoomId = randomUUID();
        caseUrn = "TVL16116BT1UU";

        otherCourtCentreId = randomUUID();
        otherCourtRoomId = randomUUID();

        final Filter idFilter = filter(where("id").is(hearingId.toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        //List
        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps();
        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps.preparePayloadToListCourtHearing(LIST_COURT_HEARING_JSON, getPayloadValues("listing"));

        listCourtHearingSteps.listCourtHearing(listCourtHearingJsonObject, Optional.empty(), Optional.empty());

        //Query for unallocated
        final Matcher[] unallocatedMatchers = {withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].hearingDate", hasItem(startDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString()))};
        listCourtHearingSteps.verifyUnallocatedHearingFound(unallocatedMatchers);

        //Allocate to different courts
        UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps();
        final JsonObject updateHearingJsonObject = updateHearingSteps.preparePayloadToUpdateHearing(UPDATE_HEARING_FOR_LISTING_JSON, getPayloadValues("allocating"));

        final JsonNode hearingNode = populateNonDefaultDays(otherCourtCentreId, otherCourtRoomId, updateHearingJsonObject, randomUUID().toString());
        updateHearingSteps.updateHearingForListing(objectMapper.treeToValue(hearingNode, JsonObject.class), hearingId);

        //Query for allocated for each court
        final Matcher[] allocatedMatchers = getAllocatedMatchers(hearingIdFilter);

        updateHearingSteps.verifyAllocatedHearingFound(courtCentreId, courtRoomId, startDate.toString(), allocatedMatchers);
        updateHearingSteps.verifyAllocatedHearingFound(otherCourtCentreId, otherCourtRoomId, endDate.toString(), allocatedMatchers);

        //Extend end date for allocated hearing
        final LocalDate extendedDate = endDate.plusDays(1);
        final Map<String, String> valueMap = getPayloadValues("updating");
        valueMap.put("endDate", extendedDate.toString());
        final String courtScheduleId = randomUUID().toString();
        valueMap.put("courtScheduleId", courtScheduleId);
        final JsonObject updateAllocatedHearingJsonObject = updateHearingSteps.preparePayloadToUpdateHearing(UPDATE_ALLOCATED_HEARING_FOR_LISTING_JSON, valueMap);

        final JsonNode jsonNode = populateNonDefaultDays(otherCourtCentreId, otherCourtRoomId, updateAllocatedHearingJsonObject, courtScheduleId);
        updateHearingSteps.updateHearingForListing(objectMapper.treeToValue(jsonNode, JsonObject.class), hearingId);

        //Range Search for each court
        updateHearingSteps.verifyAllocatedHearingFoundByRangeSearch(courtCentreId, startDate.toString(), allocatedMatchers);
        updateHearingSteps.verifyAllocatedHearingFoundByRangeSearch(otherCourtCentreId, endDate.toString(), allocatedMatchers);
        updateHearingSteps.verifyAllocatedHearingFoundByRangeSearch(otherCourtCentreId, extendedDate.toString(), allocatedMatchers);

        //Sequencing command
        SequenceHearingSteps sequenceHearingSteps = new SequenceHearingSteps();
        sequenceHearingSteps.sequenceHearing(getSequencedHearingDay(startDate, 1), hearingId);
        sequenceHearingSteps.sequenceHearing(getSequencedHearingDay(endDate, 2), hearingId);

        final Matcher[] sequenceMatchers = getSequenceMatchers(hearingIdFilter);

        sequenceHearingSteps.verifyHearingDaysAreSequencedForHearing(courtCentreId, courtRoomId, startDate.toString(), sequenceMatchers);
        sequenceHearingSteps.verifyHearingDaysAreSequencedForHearing(otherCourtCentreId, otherCourtRoomId, endDate.toString(), sequenceMatchers);

        //Check CourtList getting generated
        CourtListSteps courtListSteps = new CourtListSteps();
        courtListSteps.verifyCourtListGenerated(courtCentreId, PUBLIC, startDate.toString(), endDate.toString());
        courtListSteps.verifyCourtListGenerated(courtCentreId, STANDARD, startDate.toString(), endDate.toString());
        courtListSteps.verifyCourtListGenerated(courtCentreId, ALPHABETICAL, startDate.toString(), endDate.toString());
    }

    @Test
    public void testHearingDaysWithCourtCentreForSplit() throws IOException {
        stubGetAvailableHearingSlots();
        startDate = LocalDate.now();
        endDate = LocalDate.now().plusDays(1);
        hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);
        hearingId = randomUUID();
        caseId = randomUUID();
        courtCentreId = randomUUID();
        courtRoomId = randomUUID();
        caseUrn = "TVL16116BT1UU";

        otherCourtCentreId = randomUUID();
        otherCourtRoomId = randomUUID();
        final HearingsData hearingsData = HearingsData.singleHearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData) ;
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final HearingData hearingData = hearingsData.getHearingData().get(0);

        UpdateHearingSteps updateHearingStepsSplit = new UpdateHearingSteps();

        final JsonObject updateHearingJsonObjectSplit =
                updateHearingStepsSplit.preparePayloadToUpdateHearing(UPDATE_HEARING_FOR_LISTING_SPLIT_JSON,
                getSplitPayloadValues(hearingData.getId().toString(), hearingData.getListedCases().get(0).getCaseId().toString(),
                        hearingData.getCourtCentreId().toString(), hearingData.getCourtRoomId().toString(),
                        hearingData.getHearingStartDate().plusDays(1).toString(), hearingData.getHearingEndDate().plusDays(2).toString(),
                        hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString(),
                        hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(1).getOffenceId().toString(), randomUUID().toString()));


        final JsonNode jsonNode = populateNonDefaultDays(otherCourtCentreId, otherCourtRoomId, updateHearingJsonObjectSplit, randomUUID().toString());
        updateHearingStepsSplit.updateHearingForListing(objectMapper.treeToValue(jsonNode, JsonObject.class), hearingData.getId());
        updateHearingStepsSplit.verifyHearingRequestedForListingEvent(2);
        updateHearingStepsSplit.verifyHearingRequestedForListingInPublicMQ();

    }

    private Map<String, String> getSplitPayloadValues(final String hearingId,
                                                      final String caseId,
                                                      final String courtCentreId,
                                                      final String courtRoomId,
                                                      final String startDate,
                                                      final String endDate,
                                                      final String defendantId,
                                                      final String offenceId, final String courtScheduleId) {

        return new HashMap<>() {{
            put("hearingId", hearingId);
            put("caseId", caseId);
            put("courtCentreId", courtCentreId);
            put("courtRoomId", courtRoomId);
            put("courtScheduleId", courtScheduleId);
            put("startDate", startDate);
            put("defendantId", defendantId);
            put("offenceId", offenceId);
            put("firstNonDefaultDayStartDate", startDate);
            put("secondNonDefaultDayStartDate", endDate);
            put("endDate", endDate);
        }};
    }

    @Test
    public void testHearingDaysCorrectedWithCourtCentre() throws IOException, JMSException {
        startDate = LocalDate.now();
        hearingId = randomUUID();
        caseId = randomUUID();
        courtCentreId = randomUUID();
        courtRoomId = randomUUID();
        caseUrn = "TVL16116BT1UU";
        hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);

        final Filter idFilter = filter(where("id").is(hearingId.toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        //List
        ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps();
        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps.preparePayloadToListCourtHearing(LIST_COURT_HEARING_JSON, getPayloadValues("listing"));

        listCourtHearingSteps.listCourtHearing(listCourtHearingJsonObject, Optional.empty(), Optional.empty());

        //Query for unallocated
        final Matcher[] matchers = {withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].hearingDate", hasItem(startDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString()))};
        listCourtHearingSteps.verifyUnallocatedHearingFound(matchers);

        //Trigger command to correct hearing days with court centre
        final String correctHearingDaysWithCourtCentre = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty("listing.command.correct-hearing-day-without-court-centre")));

        final JsonObject payload = createObjectBuilder()
                .add("hearingDays", createArrayBuilder().add(populateCorrectedHearingDays()))
                .add("id", hearingId.toString()).build();

        final JmsMessageConsumerClient messageConsumerClient = newPublicJmsMessageConsumerClientProvider()
                .withEventNames( "public.events.listing.hearing-days-without-court-centre-corrected").getMessageConsumerClient();

        final Response response = restClient.postCommand(correctHearingDaysWithCourtCentre, MEDIA_TYPE_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE,
                payload.toString(), getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));

        final Matcher[] unallocatedMatchers = {withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].hearingDate", hasItem(startDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtRoomId", hasItem(courtRoomId.toString()))};
        listCourtHearingSteps.verifyUnallocatedHearingFound(unallocatedMatchers);

        final JsonPath message = messageConsumerClient.retrieveMessageAsJsonPath().get();
        assertThat(message.get("id"), is(hearingId.toString()));
        assertThat(message.get("hearingDays[0].sittingDay"), is(hearingStartTime.format(formatter)));
        assertThat(message.get("hearingDays[0].courtCentreId"), is(courtCentreId.toString()));
        assertThat(message.get("hearingDays[0].courtRoomId"), is(courtRoomId.toString()));
        assertThat(message.get("hearingDays[0].listedDurationMinutes"), is(defaultDuration));
        assertThat(message.get("hearingDays[0].listingSequence"), is(0));
    }

    private Matcher[] getAllocatedMatchers(final com.jayway.jsonpath.JsonPath hearingIdFilter) {
        return new Matcher[]{withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].hearingDate", hasItem(startDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtRoomId", hasItem(courtRoomId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].hearingDate", hasItem(endDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtCentreId", hasItem(otherCourtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtRoomId", hasItem(otherCourtRoomId.toString()))};
    }

    private Matcher[] getSequenceMatchers(final com.jayway.jsonpath.JsonPath hearingIdFilter) {
        return new Matcher[]{withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].hearingDate", hasItem(startDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].sequence", hasItem(1)),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtRoomId", hasItem(courtRoomId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].hearingDate", hasItem(endDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].sequence", hasItem(2)),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtCentreId", hasItem(otherCourtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtRoomId", hasItem(otherCourtRoomId.toString()))};
    }

    private JsonObjectBuilder populateCorrectedHearingDays() {
        return createObjectBuilder()
                .add("durationMinutes", defaultDuration)
                .add("endTime", hearingStartTime.plusMinutes(defaultDuration).format(formatter))
                .add("hearingDate", startDate.toString())
                .add("sequence", 0)
                .add("startTime", hearingStartTime.format(formatter))
                .add("courtCentreId", courtCentreId.toString())
                .add("courtRoomId", courtRoomId.toString());
    }

    private JsonObject getSequencedHearingDay(final LocalDate startDate, final int i) {
        return createObjectBuilder().add("hearings", createArrayBuilder().add(createObjectBuilder()
                .add("id", hearingId.toString())
                .add("sequenceHearingDays", createArrayBuilder().add(createObjectBuilder()
                        .add("hearingDate", startDate.toString())
                        .add("sequence", i).build()).build())
                .build())).build();
    }

    private JsonNode populateNonDefaultDays(final UUID otherCourtCentreId, final UUID otherCourtRoomId, final JsonObject updateHearingJsonObject, final String courtScheduleId) throws IOException {
        final JsonNode hearingNode = convertToNode(updateHearingJsonObject);
        ((ObjectNode) hearingNode).putArray("nonDefaultDays").add(convertToNode(createObjectBuilder()
                        .add("duration", defaultDuration)
                        .add("startTime", ZonedDateTime.of(startDate, defaultStartTime, UTC).format(formatter))
                        .add("endTime", ZonedDateTime.of(startDate, defaultStartTime.plusMinutes(defaultDuration), UTC).format(formatter))
                        .add("courtCentreId", courtCentreId.toString())
                        .add("roomId", courtRoomId.toString())
                        .add("courtRoomId", 1234)
                        .add("session", "AM")
                        .add("oucode", "B01BLYO")
                        .add("courtScheduleId", courtScheduleId)
                        .build()))
                .add(convertToNode(createObjectBuilder()
                        .add("duration", defaultDuration)
                        .add("startTime", ZonedDateTime.of(endDate, defaultStartTime, UTC).format(formatter))
                        .add("endTime", ZonedDateTime.of(endDate, defaultStartTime.plusMinutes(defaultDuration), UTC).format(formatter))
                        .add("courtCentreId", otherCourtCentreId.toString())
                        .add("roomId", otherCourtRoomId.toString())
                        .add("courtRoomId", 4456)
                        .add("session", "AM")
                        .add("oucode", "B01BH8U")
                        .add("courtScheduleId", courtScheduleId)
                        .build()));
        return hearingNode;
    }

    private JsonNode convertToNode(final JsonObject build) throws IOException {
        return objectMapper.readTree(build.toString());
    }

    private Map<String, String> getPayloadValues(final String action) {
        switch (action) {
            case "listing":
                return new HashMap<String, String>(){{
                    put("hearingId", hearingId.toString());
                    put("caseId", caseId.toString());
                    put("courtCentreId", courtCentreId.toString());
                    put("startDate", startDate.toString());
                    put("hearingStartTime", hearingStartTime.format(formatter));
                    put("estimatedMinutes", String.valueOf(defaultDuration));
                    put("caseUrn", caseUrn);
                }};

            case "allocating":
                return new HashMap<String, String>(){{
                    put("hearingId", hearingId.toString());
                    put("caseId", caseId.toString());
                    put("courtCentreId", courtCentreId.toString());
                    put("courtRoomId", courtRoomId.toString());
                    put("startDate", startDate.toString());
                    put("endDate", endDate.toString());
                }};

            case "updating":
                return new HashMap<String, String>(){{
                    put("hearingId", hearingId.toString());
                    put("caseId", caseId.toString());
                    put("courtCentreId", courtCentreId.toString());
                    put("courtRoomId", courtRoomId.toString());
                    put("startDate", startDate.toString());
                    put("endDate", endDate.toString());
                    put("updatedCourtCentreId", otherCourtCentreId.toString());
                    put("updatedCourtRoomId", otherCourtRoomId.toString());
                }};

            default: return emptyMap();
        }
    }
}
