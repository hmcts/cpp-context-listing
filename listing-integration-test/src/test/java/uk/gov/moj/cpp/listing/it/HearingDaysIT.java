package uk.gov.moj.cpp.listing.it;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
public class HearingDaysIT extends AbstractIT {

    private static final String UPDATE_HEARING_FOR_LISTING_SPLIT_JSON = "update-hearing-for-listing-split.json";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private UUID courtCentreId;
    private UUID courtRoomId;
    private LocalDate startDate;
    private LocalDate endDate;
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
        courtCentreId = getRandomCourtCenterId();
        courtRoomId = getRandomCourtRoomId();
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

        // Anchor the split day span on working days. The split derives new hearing days from the base
        // hearing date; built with raw plusDays(1)/plusDays(2) the span straddles a weekend whenever the
        // suite runs Wed-Sat. Courts do not sit at weekends, so no hearing-requested-for-listing is emitted
        // for the weekend day and verifyHearingRequestedForListingEvent(2) times out. plusWorkingDays keeps
        // the span on Mon-Fri and is identical to plusDays on a weekday run with no weekend in range.
        final LocalDate splitStartDate = ItClock.plusWorkingDays(hearingData.getHearingStartDate(), 1);
        final LocalDate splitEndDate = ItClock.plusWorkingDays(splitStartDate, 2);

        final JsonObject updateHearingJsonObjectSplit =
                updateHearingStepsSplit.preparePayloadToUpdateHearing(UPDATE_HEARING_FOR_LISTING_SPLIT_JSON,
                getSplitPayloadValues(hearingData.getId().toString(), hearingData.getListedCases().get(0).getCaseId().toString(),
                        hearingData.getCourtCentreId().toString(), hearingData.getCourtRoomId().toString(),
                        splitStartDate.toString(), splitEndDate.toString(),
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


}
