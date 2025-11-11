package uk.gov.moj.cpp.listing.scenario;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import uk.gov.moj.cpp.listing.it.AbstractIT;

import javax.json.JsonObject;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.*;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetProsecutorPoliceFlag;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;

class TestSpiScenario extends AbstractIT {

    private static final String LIST_SPI_ALLOCATED_HEARING_JSON = "test-data/list-court-hearing/spi_allocated.json";
    private static final String LIST_SPI_UNALLOCATED_HEARING_JSON = "test-data/list-court-hearing/spi_unallocated.json";
    private static final String LIST_SPI_TWO_DEFENDANTS_UNALLOCATED_JSON = "test-data/list-court-hearing/spi_two_defendants_unallocated.json";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private UUID hearingId;
    private UUID caseId;
    private UUID courtCentreId;
    private UUID courtRoomId;
    private UUID hearingTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private ZonedDateTime hearingStartTime;
    private String caseUrn;
    private final LocalTime defaultStartTime = LocalTime.parse("10:00");
    private final int defaultDuration = 20;
    private UUID otherCourtCentreId;
    private UUID otherCourtRoomId;
    private final UUID prosecutionCaseId = randomUUID();
    private final String jurisdictionType = "MAGISTRATES";


    @Test
    void testHearingDaysWithCourtCentreScenario() throws IOException {
        stubGetAvailableHearingSlots();
        startDate = LocalDate.now();
        endDate = LocalDate.now().plusDays(1);
        hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);
        hearingId = randomUUID();
        caseId = randomUUID();
        courtCentreId = randomUUID();
        hearingTypeId = randomUUID();

        stubGetReferenceDataCourtCentreById(courtCentreId);
        courtRoomId = randomUUID();
        caseUrn = "TVL16116BT1UU";

        otherCourtCentreId = randomUUID();
        otherCourtRoomId = randomUUID();

        ListCourtHearingStepsSpi listCourtHearingSteps = new ListCourtHearingStepsSpi();
        Map<String, String> payload = getPayloadValues("listing");
        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps.preparePayloadToListCourtHearing(LIST_SPI_ALLOCATED_HEARING_JSON, payload);
        stubGetProsecutorPoliceFlag(UUID.fromString("764bff92-a135-34cb-b858-8bb6b4b66301"));
        stubSearchBookHearingSlots(hearingId.toString(), courtCentreId.toString(), startDate.toString(), hearingStartTime);
        listCourtHearingSteps.listCourtHearing(listCourtHearingJsonObject.getJsonArray("hearings").getJsonObject(0));

        listCourtHearingSteps.verifyAllocatedHearingFound(payload);


    }

    @Test
    void shouldListHearingWithUnallocatedData() throws IOException {
        startDate = LocalDate.now();
        endDate = LocalDate.now().plusDays(1);
        hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);
        hearingId = randomUUID();
        caseId = randomUUID();
        courtCentreId = randomUUID();
        hearingTypeId = randomUUID();

        stubGetReferenceDataCourtCentreById(courtCentreId);
        courtRoomId = randomUUID();
        caseUrn = "TVL16116BT1UU";

        otherCourtCentreId = randomUUID();
        otherCourtRoomId = randomUUID();

        ListCourtHearingStepsSpi listCourtHearingSteps = new ListCourtHearingStepsSpi();
        Map<String, String> payload = getPayloadValues("listing");
        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps.preparePayloadToListCourtHearing(LIST_SPI_UNALLOCATED_HEARING_JSON, payload);

        listCourtHearingSteps.listCourtHearing(listCourtHearingJsonObject.getJsonArray("hearings").getJsonObject(0));

        listCourtHearingSteps.verifyUnallocatedHearingFound(payload);
    }

    @Test
    void shouldListHearingWithTwoDefendantsUnallocated() throws IOException {
        stubGetAvailableHearingSlots();
        startDate = LocalDate.now();
        endDate = LocalDate.now().plusDays(1);
        hearingStartTime = ZonedDateTime.of(startDate, defaultStartTime, UTC);
        hearingId = randomUUID();
        caseId = randomUUID();
        courtCentreId = randomUUID();
        hearingTypeId = randomUUID();

        stubGetReferenceDataCourtCentreById(courtCentreId);
        courtRoomId = randomUUID();
        caseUrn = "TVL16116BT1UU";

        otherCourtCentreId = randomUUID();
        otherCourtRoomId = randomUUID();

        ListCourtHearingStepsSpi listCourtHearingSteps = new ListCourtHearingStepsSpi();
        Map<String, String> payload = getPayloadValues("listing");
        final JsonObject listCourtHearingJsonObject = listCourtHearingSteps.preparePayloadToListCourtHearing(LIST_SPI_TWO_DEFENDANTS_UNALLOCATED_JSON, payload);

        listCourtHearingSteps.listCourtHearing(listCourtHearingJsonObject.getJsonArray("hearings").getJsonObject(0));
        listCourtHearingSteps.verifyUnallocatedTwoDefendantsHearingFound(payload);
    }


    private Map<String, String> getPayloadValues(final String action) {
        switch (action) {
            case "listing":
                return new HashMap<>() {{
                    put("hearingId", hearingId.toString());
                    put("caseId", caseId.toString());
                    put("courtCentreId", courtCentreId.toString());
                    put("courtRoomId", courtCentreId.toString());
                    put("jurisdictionType", jurisdictionType);
                    put("hearingTypeId", hearingTypeId.toString());
                    put("startDate", startDate.toString());
                    put("hearingStartTime", hearingStartTime.format(formatter));
                    put("estimatedMinutes", String.valueOf(defaultDuration));
                    put("prosecutionCaseId", String.valueOf(prosecutionCaseId));
                    put("listedStartDateTime", hearingStartTime.format(formatter));

                    put("caseUrn", caseUrn);
                }};

            case "allocating":
                return new HashMap<>() {{
                    put("hearingId", hearingId.toString());
                    put("caseId", caseId.toString());
                    put("courtCentreId", courtCentreId.toString());
                    put("courtRoomId", courtRoomId.toString());
                    put("startDate", startDate.toString());
                    put("endDate", endDate.toString());
                }};

            case "updating":
                return new HashMap<>() {{
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