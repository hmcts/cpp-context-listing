package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.buildPublishCourtListCommandPayload;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingData;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;
import static uk.gov.moj.cpp.listing.utils.AzureScheduleServiceStub.stubGetAvailableHearingSlots;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetAllCrownCourtCentres;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;
import static uk.gov.moj.cpp.listing.utils.SystemIdMapperStub.stubIdMapperReturningExistingAssociation;

import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.PublishCourtListSteps;
import uk.gov.moj.cpp.listing.steps.RestrictCourtListSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class PublishCourtListIT extends AbstractIT {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final UUID DEFAULT_COURT_ROOM_ID = null;
    private static final String DEFAULT_COURT_CENTRE_NAME = STRING.next();
    private static final String EXPORT_SUCCESSFUL = "EXPORT_SUCCESSFUL";

    private static final String LISTING_COMMAND_PUBLISH_LISTS_FOR_ALL_CROWN_COURTS = "listing.command.publish-court-lists-for-crown-courts";
    private static final String MEDIA_TYPE_LISTING_COMMAND_PUBLISH_LISTS_FOR_ALL_CROWN_COURTS = "application/vnd.listing.command.publish-court-lists-for-crown-courts+json";

    private static final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();

    @Before
    public void cleanTables() {
        viewStoreCleaner.cleanViewStoreTables();
    }

    @Ignore("flaky test, failing in pipeline, but passing locally")
    @Test
    public void shouldPublishCourtListWithNoHearings() throws Exception {

        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID hearingTypeId = fromString("52edf232-3c09-4c74-a6ad-737985c2e662");
        final UUID courtListId = randomUUID();
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataXhibitCourtRoomMappings(randomUUID());
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataHearingTypes(hearingTypeId);
        stubIdMapperReturningExistingAssociation(courtListId);

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(null, publishCourtListCommandPayload);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus("COURT_LIST_REQUESTED", "true");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHasNoHearings();
    }

    @Test
    public void shouldPublishCourtListWithHearings() {
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "true");
    }

    @Test
    public void publishFinalCourtListsForAllCrownCourts() {

        final UUID courtCentreIdOne = randomUUID();
        final UUID courtCentreIdTwo = randomUUID();
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();
        stubGetAllCrownCourtCentres(courtCentreIdOne, courtCentreIdTwo);
        stubGetReferenceDataCourtCentreById(courtCentreIdOne);
        stubGetReferenceDataCourtCentreById(courtCentreIdTwo);
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


    @Test
    public void shouldPublishCourtListWithHearingsWithPublicListNoteForFirmPublishType() throws Exception {
        stubGetAvailableHearingSlots();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForPublicListNote(hearingsData.getHearingData().get(0), true, "publicListNote");

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
            updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
        }


        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "true");
        publishCourtListSteps.verifySentPublishedCourtListHearingData(true, "RestrictionApplied, publicListNote");
    }


    @Test
    public void shouldPublishCourtListWithHearingsWithOutPublicListNoteForFirmPublishType() throws Exception {
        stubGetAvailableHearingSlots();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForPublicListNote(hearingsData.getHearingData().get(0), true, null);

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingWithUpdatedNoPublicListNoteWhenQueryingFromAPI();
        }


        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "true");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingData(true, "RestrictionApplied");
    }


    @Test
    public void shouldPublishCourtListWithHearingsWithVideoLinkForDraftPublishTypeAndPublishPublicMessage() throws Exception {
        stubGetAvailableHearingSlots();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.DRAFT;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForPublicListNote(hearingsData.getHearingData().get(0), true, "videoLinkDetails");

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
            updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
        }


        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.createMessageConsumer();
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "false");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingDataForDraft(true, "RestrictionApplied, videoLinkDetails");
    }

    @Test
    public void shouldPublishCourtListWithHearingsWithPublicListNoteForFinalPublishType() throws Exception {
        stubGetAvailableHearingSlots();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FINAL;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForPublicListNote(hearingsData.getHearingData().get(0), true, "publicListNote");

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingUpdatedResultsWithPublicListNoteInAllocationInMQ();
            updateHearingSteps.verifyHearingWithUpdatedPublicListNoteWhenQueryingFromAPI();
        }


        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.createMessageConsumer();
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "false");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingDataForDraft(true, "RestrictionApplied, publicListNote");
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


    @Test
    public void shouldPublishCourtListWithHearingsWithOutListNoteWhenRRNotPresentForFirmPublishType() throws Exception {
        stubGetAvailableHearingSlots();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        final HearingsData hearingsData = loadHearingData(courtCentreId, courtRoomUUID);

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForPublicListNote(hearingsData.getHearingData().get(0), true, null);

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingWithUpdatedNoPublicListNoteWhenQueryingFromAPI();
        }

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "true");
        publishCourtListSteps.verifySentPublishedCourtListHearingData(true, "");
    }


    @Test
    public void shouldRestrictListingCaseFromCourtForHearingId() throws Exception {
        stubGetAvailableHearingSlots();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        HearingsData hearingsData = HearingsData.hearingsDataWithRestriction(courtCentreId,courtRoomUUID,"DISTRICT_JUDGE");
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedAndListed();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(true);
        }

        final UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForPublicListNote(hearingsData.getHearingData().get(0), true, null);

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearingsData, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListingWithPublicListNote();
            updateHearingSteps.verifyHearingWithUpdatedNoPublicListNoteWhenQueryingFromAPI();
        }

        try (final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData)) {
            restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getRestrictListingFromCourtData(hearingsData));
            restrictCourtListSteps.verifyRestrictCourtListInActiveMQ();
            restrictCourtListSteps.verifyListingRestrictedInHearing(true, true, false);

        }

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.verifyHearingListedFromAPI(true);
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "true");
        publishCourtListSteps.verifySentRestrictedPublishedCourtListHearingData();

    }

}
