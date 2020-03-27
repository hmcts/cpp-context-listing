package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.buildPublishCourtListCommandPayload;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetAllCrownCourtCentres;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;
import static uk.gov.moj.cpp.listing.utils.SystemIdMapperStub.stubIdMapperReturningExistingAssociation;

import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.steps.PublishCourtListSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.Ignore;
import org.junit.Test;

public class PublishCourtListIT extends AbstractIT {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final UUID DEFAULT_COURT_ROOM_ID = null;

    private static final String LISTING_COMMAND_PUBLISH_LISTS_FOR_ALL_CROWN_COURTS = "listing.command.publish-court-lists-for-crown-courts";
    private static final String MEDIA_TYPE_LISTING_COMMAND_PUBLISH_LISTS_FOR_ALL_CROWN_COURTS = "application/vnd.listing.command.publish-court-lists-for-crown-courts+json";

    @Test
    public void shouldPublishCourtListWithNoHearings() throws Exception {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtMappings(new CourtCentreData(UUID.randomUUID(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID));
        stubOrganisationUnit(courtCentreId);
        stubIdMapperReturningExistingAssociation(courtListId);

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(null, publishCourtListCommandPayload);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus("COURT_LIST_REQUESTED");
        publishCourtListSteps.waitForPublishedCourtListStored(courtCentreId, publishCourtListType, startDate);
        publishCourtListSteps.waitForCompletedExport(courtCentreId, publishCourtListType, startDate);
        publishCourtListSteps.verifySentPublishedCourtListHasNoHearings();
    }

    @Ignore("To be fixed in SCSL-469")
    @Test
    public void shouldPublishCourtListWithHearings() throws Exception {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreId);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(courtRoomId);

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus("COURT_LIST_REQUESTED");
        publishCourtListSteps.waitForPublishedCourtListStored(courtCentreId, publishCourtListType, startDate);
        publishCourtListSteps.waitForCompletedExport(courtCentreId, publishCourtListType, startDate);
        publishCourtListSteps.verifySentPublishedCourtListHearingData();
    }

    @Test
    public void publishFinalCourtListsForAllCrownCourts() {

        final UUID courtCentreIdOne = randomUUID();
        final UUID courtCentreIdTwo = randomUUID();
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        stubGetAllCrownCourtCentres(courtCentreIdOne, courtCentreIdTwo);
        final JsonObject commandAsJson = createObjectBuilder().build();
        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreIdOne)
                .combine(loadHearingDataWithJudiciary(courtCentreIdTwo));
        final JsonObject publishCourtListCommandPayloadUsingFirstCourtCentreArbitrarily = buildPublishCourtListCommandPayload(courtCentreIdOne, publishCourtListType, startDate);
        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayloadUsingFirstCourtCentreArbitrarily);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        final LocalDate expectedPublishDate = DateAndTimeUtils.getNextWorkingDay(LocalDate.now());

        sendPublishFinalCourtListsForAllCrownCourtsCommand(commandAsJson);

        publishCourtListSteps.verifyThatWeSuccessfullyRequestedAFinalListPublication(courtCentreIdOne, expectedPublishDate);
        publishCourtListSteps.verifyThatWeSuccessfullyRequestedAFinalListPublication(courtCentreIdTwo, expectedPublishDate);

    }

    private void sendPublishFinalCourtListsForAllCrownCourtsCommand(final JsonObject commandAsJson) {

        final String rawUrl = String.format("%s/%s", getBaseUri(), readConfig().getProperty(LISTING_COMMAND_PUBLISH_LISTS_FOR_ALL_CROWN_COURTS));

        final Response response = restClient
                .postCommand(
                        rawUrl,
                        MEDIA_TYPE_LISTING_COMMAND_PUBLISH_LISTS_FOR_ALL_CROWN_COURTS,
                        commandAsJson.toString(),
                        getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

}
