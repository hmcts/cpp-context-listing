package uk.gov.moj.cpp.listing.it;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtRoomMappings;

import uk.gov.moj.cpp.listing.steps.PublishCourtListSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;

public class PublishCourtListIT extends AbstractIT {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final UUID DEFAULT_COURT_ROOM_ID = null;

    @Test
    public void shouldPublishCourtListWithNoHearings() throws Exception {

        UUID courtCentreId = randomUUID();
        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(courtCentreId);

        stubGetReferenceDataCourtRoomMappings(new CourtCentreData(UUID.randomUUID(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID));

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(null, publishCourtListCommandPayload);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus("EXPORT_SUCCESSFUL");
        publishCourtListSteps.verifySentPublishedCourtListHasNoHearings();
    }

    @Test
    public void shouldPublishCourtListWithHearings() throws Exception {

        UUID courtCentreId = randomUUID();
        final JsonObject publishCourtListJsonObject = buildPublishCourtListCommandPayload(courtCentreId);

        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreId);

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListJsonObject);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus("EXPORT_SUCCESSFUL");
        publishCourtListSteps.verifySentPublishedCourtListHearingData();
    }

    private JsonObject buildPublishCourtListCommandPayload(final UUID courtCentreId) {
        return createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("startDate", now().toString())
                .add("endDate", now().plusDays(2).toString())
                .add("publishCourtListType", "FIRM")
                .build();
    }

}
