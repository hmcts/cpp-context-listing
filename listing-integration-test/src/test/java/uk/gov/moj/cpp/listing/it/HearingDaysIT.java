package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.SequenceHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.it.util.ItClock;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Filter;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
public class HearingDaysIT extends AbstractIT {

    private static final String LIST_COURT_HEARING_JSON = "list-court-hearing.json";
    private static final String UPDATE_HEARING_FOR_LISTING_JSON = "update-hearing-for-listing.json";
    private static final String UPDATE_HEARING_FOR_LISTING_SPLIT_JSON = "update-hearing-for-listing-split.json";
    private static final String UPDATE_ALLOCATED_HEARING_FOR_LISTING_JSON = "update-allocated-hearing-for-listing.json";
    private static final String MEDIA_TYPE_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE = "application/vnd.listing.correct-hearing-days-without-court-centre+json";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
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
    private final LocalTime defaultStartTime = LocalTime.parse("10:00");
    private final int defaultDuration = 20;
    private UUID otherCourtCentreId;
    private UUID otherCourtRoomId;


    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Test
    void testHearingDaysWithCourtCentreForSplit() throws IOException {
        stubGetAvailableHearingSlots();

        startDate = ItClock.today();
        endDate = ItClock.today().plusDays(1);
        hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);
        hearingId = randomUUID();
        caseId = randomUUID();
        courtCentreId = getRandomCourtCenterId();
        courtRoomId = getRandomCourtRoomId();
        caseUrn = "TVL16116BT1UU";
        otherCourtCentreId = getRandomCourtCenterId(asList(courtCentreId));
        otherCourtRoomId = randomUUID();

        final HearingsData hearingsData = HearingsData.singleHearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData) ;
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        final UUID courtCentreId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtCentreId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);

        stubListHearingInCourtSessions(hearingsData.getHearingData().get(0).getId().toString(),
                courtScheduleId, hearingsData.getHearingData().get(0).getHearingStartTime());
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
                        hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(1).getOffenceId().toString(), courtScheduleId));


        final JsonNode jsonNode = populateNonDefaultDays(otherCourtCentreId, otherCourtRoomId, updateHearingJsonObjectSplit, courtScheduleId);
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
    void testHearingDaysCorrectedWithCourtCentre() throws IOException {
        startDate = ItClock.today();
        hearingId = randomUUID();
        caseId = randomUUID();
        courtCentreId = getRandomCourtCenterId();
        courtRoomId = getRandomCourtRoomId();
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
//                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString())),
//                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtRoomId", hasItem(courtRoomId.toString())),
//                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].hearingDate", hasItem(endDate.toString())),
//                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtCentreId", hasItem(otherCourtCentreId.toString())),
//                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtRoomId", hasItem(otherCourtRoomId.toString()))
                };
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
