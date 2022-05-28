package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;


import com.jayway.restassured.path.json.JsonPath;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.SequenceHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

public class HearingDaysIT extends AbstractIT {

    private static final String LIST_COURT_HEARING_JSON = "list-court-hearing.json";
    private static final String UPDATE_HEARING_FOR_LISTING_JSON = "update-hearing-for-listing.json";
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
    private MessageConsumer messageConsumerClient;


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
        listCourtHearingSteps.verifyUnallocatedHearingFound(hearingId.toString(), unallocatedMatchers);

        //Allocate to different courts
        UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps();
        final JsonObject updateHearingJsonObject = updateHearingSteps.preparePayloadToUpdateHearing(UPDATE_HEARING_FOR_LISTING_JSON, getPayloadValues("allocating"));

        final JsonNode hearingNode = populateNonDefaultDays(otherCourtCentreId, otherCourtRoomId, updateHearingJsonObject);
        updateHearingSteps.updateHearingForListing(objectMapper.treeToValue(hearingNode, JsonObject.class), hearingId);

        //Query for allocated for each court
        final Matcher[] allocatedMatchers = {withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].hearingDate", hasItem(startDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtRoomId", hasItem(courtRoomId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].hearingDate", hasItem(endDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtCentreId", hasItem(otherCourtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtRoomId", hasItem(otherCourtRoomId.toString()))};
        updateHearingSteps.verifyAllocatedHearingFound(hearingId.toString(), courtCentreId, courtRoomId, startDate.toString(), allocatedMatchers);
        updateHearingSteps.verifyAllocatedHearingFound(hearingId.toString(), otherCourtCentreId, otherCourtRoomId, endDate.toString(), allocatedMatchers);

        //Extend end date for allocated hearing
        final LocalDate extendedDate = endDate.plusDays(1);
        final Map<String, String> valueMap = getPayloadValues("updating");
        valueMap.put("endDate",  extendedDate.toString());
        final JsonObject updateAllocatedHearingJsonObject = updateHearingSteps.preparePayloadToUpdateHearing(UPDATE_ALLOCATED_HEARING_FOR_LISTING_JSON, valueMap);

        final JsonNode jsonNode = populateNonDefaultDays(otherCourtCentreId, otherCourtRoomId, updateAllocatedHearingJsonObject);
        updateHearingSteps.updateHearingForListing(objectMapper.treeToValue(jsonNode, JsonObject.class), hearingId);

        //Range Search for each court
        updateHearingSteps.verifyAllocatedHearingFoundByRangeSearch(hearingId.toString(), courtCentreId, startDate.toString(), allocatedMatchers);
        updateHearingSteps.verifyAllocatedHearingFoundByRangeSearch(hearingId.toString(), otherCourtCentreId, endDate.toString(), allocatedMatchers);
        updateHearingSteps.verifyAllocatedHearingFoundByRangeSearch(hearingId.toString(), otherCourtCentreId, extendedDate.toString(), allocatedMatchers);

        //Sequencing command
        SequenceHearingSteps sequenceHearingSteps = new SequenceHearingSteps();
        sequenceHearingSteps.sequenceHearing(getSequencedHearingDay(startDate, 1), hearingId);
        sequenceHearingSteps.sequenceHearing(getSequencedHearingDay(endDate, 2), hearingId);

        final Matcher[] sequenceMatchers = {withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].hearingDate", hasItem(startDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].sequence", hasItem(1)),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtRoomId", hasItem(courtRoomId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].hearingDate", hasItem(endDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].sequence", hasItem(2)),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtCentreId", hasItem(otherCourtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[1].courtRoomId", hasItem(otherCourtRoomId.toString()))};

        sequenceHearingSteps.verifyHearingDaysAreSequencedForHearing(courtCentreId, courtRoomId, startDate.toString(), sequenceMatchers);
        sequenceHearingSteps.verifyHearingDaysAreSequencedForHearing(otherCourtCentreId, otherCourtRoomId, endDate.toString(), sequenceMatchers);

        //Check CourtList getting generated
        CourtListSteps courtListSteps = new CourtListSteps();
        courtListSteps.verifyCourtListGenerated(courtCentreId, PUBLIC, startDate.toString(), endDate.toString());
        courtListSteps.verifyCourtListGenerated(courtCentreId, STANDARD, startDate.toString(), endDate.toString());
        courtListSteps.verifyCourtListGenerated(courtCentreId, ALPHABETICAL, startDate.toString(), endDate.toString());
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
        listCourtHearingSteps.verifyUnallocatedHearingFound(hearingId.toString(), matchers);

        //Trigger command to correct hearing days with court centre
        final String correctHearingDaysWithCourtCentre = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty("listing.command.correct-hearing-day-without-court-centre")));

        final JsonObject payload = createObjectBuilder()
                .add("hearingDays", createArrayBuilder().add(populateCorrectedHearingDays()))
                .add("id", hearingId.toString()).build();

        messageConsumerClient = publicEvents.createConsumer("public.events.listing.hearing-days-without-court-centre-corrected");

        final Response response = restClient.postCommand(correctHearingDaysWithCourtCentre, MEDIA_TYPE_CORRECT_HEARING_DAYS_WITHOUT_COURT_CENTRE,
                payload.toString(), getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));

        final Matcher[] unallocatedMatchers = {withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].hearingDate", hasItem(startDate.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtCentreId", hasItem(courtCentreId.toString())),
                withJsonPath("$.hearings[?(@.id == '" + hearingId + "')].hearingDays[0].courtRoomId", hasItem(courtRoomId.toString()))};
        listCourtHearingSteps.verifyUnallocatedHearingFound(hearingId.toString(), unallocatedMatchers);

        final JsonPath message = QueueUtil.retrieveMessage(messageConsumerClient);
        assertThat(message.get("id"), is(hearingId.toString()));
        assertThat(message.get("hearingDays[0].sittingDay"), is(hearingStartTime.format(formatter)));
        assertThat(message.get("hearingDays[0].courtCentreId"), is(courtCentreId.toString()));
        assertThat(message.get("hearingDays[0].courtRoomId"), is(courtRoomId.toString()));
        assertThat(message.get("hearingDays[0].listedDurationMinutes"), is(defaultDuration));
        assertThat(message.get("hearingDays[0].listingSequence"), is(0));
        messageConsumerClient.close();
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

    private JsonNode populateNonDefaultDays(final UUID otherCourtCentreId, final UUID otherCourtRoomId, final JsonObject updateHearingJsonObject) throws IOException {
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
                .add("courtScheduleId", randomUUID().toString())
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
                        .add("courtScheduleId", randomUUID().toString())
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

            default: return Collections.emptyMap();
        }
    }
}
