package uk.gov.moj.cpp.listing.it;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;

import uk.gov.moj.cpp.listing.steps.PublishCourtListSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;

public class PublishCourtListIT extends AbstractIT {

    @Test
    public void shouldPublishCourtListWithNoHearings() throws Exception {

        UUID courtCentreId = randomUUID();
        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(courtCentreId);

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
