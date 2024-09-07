package uk.gov.moj.cpp.listing.it;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.buildPublishCourtListCommandPayload;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubUpdateAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;
import static uk.gov.moj.cpp.listing.utils.SystemIdMapperStub.stubIdMapperReturningExistingAssociation;
import static uk.gov.moj.cpp.platform.test.feature.toggle.FeatureStubber.stubFeaturesFor;

import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.PublishCourtListSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

public class ExhibitScenarioIT extends AbstractIT {

    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";

    private static final UUID DEFAULT_COURT_ROOM_ID = null;
    private static final String DEFAULT_COURT_CENTRE_NAME = STRING.next();
    private static final String EXPORT_SUCCESSFUL = "EXPORT_SUCCESSFUL";

    private static final ViewStoreCleaner viewStoreCleaner = new ViewStoreCleaner();


    @Before
    public void cleanTables() {
        viewStoreCleaner.cleanViewStoreTables();
    }

    /**
     * Scenarios 1:Case 1 with 1 offence and one defendent.
     * Create Case in MG court
     * Add case notes as 10:30 on 28/05/2023
     * Adjournment case in NORWICH crown court on 28/05/2023 for fixed date.
     * go to publish and download hearing list
     * select Courthouse,Fixed date and Filter list by date
     * click on apply button
     * click on draft list/Final button
     * Verify the data in XML file.
     */
    @Test
    public void testAdjournHearingListedForSpecificDate() throws Exception {
        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDateWithParameters(1, courtCentreId, courtRoomUUID, "DISTRICT_JUDGE");
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        }

        final PublishCourtListType publishCourtListType = PublishCourtListType.DRAFT;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

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
        publishCourtListSteps.verifySentPublishedCourtListHearingDataForDraft(true, "RestrictionApplied");
    }

    /**
     * Scenarios 2:Two cases on same hearing on Same offence and Same defendant with Related hearing in Specific case.
     * <p>
     * Create Case 1 with one offence and 1 defendant  in MG court .
     * Add case notes as 10:30 on 28/05/2023.
     * Result cases with CCII on Northampton crown court.
     * Create Case 2 with same offence and same defendant  in MG court.
     * Result with CCII on Northampton crown court.
     * Select Fixed date in find hearing date screen.
     * Check the specific case check box is checked in related hearing and search hearing 2 for case 1.
     * Share the results.
     * click on draft list/Final button
     * Verify the data in XML file.
     */

    @Test
    public void testTwoCasesWithLinkedHearingProducesFinalListOnAFixedDate() throws Exception {
        final ImmutableMap<String, Boolean> features = of("amendReshare", true);
        stubFeaturesFor("listing", features);
        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;

        final HearingsData hearingsData = HearingsData.singleHearingDataSingleCaseWithSingleOffence(courtCentreId, courtRoomUUID, "DISTRICT_JUDGE", "Norwich Crown court", 1);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        final HearingsData anotherHearing = HearingsData.singleHearingDataSingleCaseWithSingleOffence(courtCentreId, courtRoomUUID, "DISTRICT_JUDGE", "Norwich Crown court", 1);
        //Copy defendant data and offence details
        DefendantData defendantData = anotherHearing.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        defendantData.copyDefendantData(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0));
        //Now submit the case and make sure its unallocated
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(anotherHearing)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        //Link hearing
        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(anotherHearing.getHearingData().get(0))) {
            listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(anotherHearing.getHearingData().get(0).getId(), hearingsData);
            listNextHearingSteps.verifyUpdateRelatedHearingRequestedInActiveMQ(anotherHearing.getHearingData().get(0).getId());
        }

        final PublishCourtListType publishCourtListType = PublishCourtListType.FINAL;
        final LocalDate startDate = LocalDate.now();
        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.createMessageConsumer();
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "false");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingDataForDraft(true, "RestrictionApplied");
    }

    /**
     * scenario:3 Two cases on same hearing on different offence and different defendant with Related hearing Specific case.
     * <p>
     * Create Case 1 with one offence and 1 defendant  in MG court
     * Add case notes as 10:30 on 28/05/2023.
     * Result cases with CCII on Leeds crown court.
     * Create Case 2 with other offence and other. defendant  in MG court.
     * Result with CCII on Leeds crown court.
     * Select Fixed date in find hearing date screen.
     * Check the specific case check box is checked in related hearing and  search hearing 2 for case 1.
     * Share the results.
     * click on draft list/Final button.
     * Verify the data in XML file.
     */
    @Test
    public void testRelatedHearingSpecificCasesForFixedDate() throws Exception {
        final ImmutableMap<String, Boolean> features = of("amendReshare", true);
        stubFeaturesFor("listing", features);
        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;

        final HearingsData hearingsData = HearingsData.singleHearingDataSingleCaseWithSingleOffence(courtCentreId, courtRoomUUID, "DISTRICT_JUDGE", "Norwich Crown court", 1);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        final HearingsData anotherHearing = HearingsData.singleHearingDataSingleCaseWithSingleOffence(courtCentreId, courtRoomUUID, "DISTRICT_JUDGE", "Norwich Crown court", 1);

        //Now submit the case and make sure its unallocated
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(anotherHearing)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        //Link hearing
        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(anotherHearing.getHearingData().get(0))) {
            listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(anotherHearing.getHearingData().get(0).getId(), hearingsData);
            listNextHearingSteps.verifyUpdateRelatedHearingRequestedInActiveMQ(anotherHearing.getHearingData().get(0).getId());
        }

        final PublishCourtListType publishCourtListType = PublishCourtListType.FINAL;
        final LocalDate startDate = LocalDate.now();
        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.createMessageConsumer();
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "false");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingDataForDraft(true, "RestrictionApplied");
    }

    /**
     * Scenarios 5: Related hearing on  Matched defendants and linked cases with 2 defendant and 2 offence on same two cases
     * <p>
     * Create Case 1 with Two offence and Two defendant  in MG court .
     * Add case notes as 10:30 on 28/05/2023.
     * Result cases with A(Adj) on Leeds crown court.
     * Create Case 2  same offence  and 1 defendant matching in MG court.
     * Result with A(Adj) on Leeds crown court.
     * Select Fixed date in find hearing date screen.
     * Check the Matched defendants and linked cases check box is checked in related hearing and search hearing 2 for case 1.
     * Share the results.
     * click on draft list/Final button
     * Verify the data in XML file
     */

    @Test
    public void testRelatedHearingSpecificCasesForFixedDateWithTwoDefendantsAndTwoOffences() throws Exception {
        final ImmutableMap<String, Boolean> features = of("amendReshare", true);
        stubFeaturesFor("listing", features);
        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;

        final HearingsData hearingsData = HearingsData.singleHearingDataSingleCaseWithTwoDefendantAndTwoOffence(courtCentreId, courtRoomUUID, "DISTRICT_JUDGE", "Norwich Crown court", 1);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        final HearingsData anotherHearing = HearingsData.singleHearingDataSingleCaseWithTwoDefendantAndTwoOffence(courtCentreId, courtRoomUUID, "DISTRICT_JUDGE", "Norwich Crown court", 1);

        //Now submit the case and make sure its unallocated
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(anotherHearing)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }

        //Link hearing
        try (final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(anotherHearing.getHearingData().get(0))) {
            listNextHearingSteps.whenUpdateRelatedHearingSubmittedForListing(anotherHearing.getHearingData().get(0).getId(), hearingsData);
            listNextHearingSteps.verifyUpdateRelatedHearingRequestedInActiveMQ(anotherHearing.getHearingData().get(0).getId());
        }

        final PublishCourtListType publishCourtListType = PublishCourtListType.FINAL;
        final LocalDate startDate = LocalDate.now();
        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData, publishCourtListCommandPayload);
        publishCourtListSteps.createMessageConsumer();
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "false");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingDataForDraft(true, "RestrictionApplied");
    }

    /**
     * Scenarios 6: Week commencing for period of one week.
     * <p>
     * Create Case 1 and Case 2 with one offence and 1 defendant in MG court
     * Adjournment 2 cases to Nottingham CC court for week commencing of 21 Aug 2023.
     * Go to Publish and download list.
     * Select Courthouse as  Crown Court.
     * Select Week commencing Filter list by date as 21 Aug 2023 Week commencing .click on apply button.
     * Click on Share warn/Firm list.
     * Verify the data in XML file.
     */
    @Test
    public void testWeekendCommencingWithTwoCases() throws Exception {

        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;

        final HearingsData hearingsData1 = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 1, courtCentreId, courtRoomUUID, "DISTRICT_JUDGE");
        hearingsData1.getHearingData().get(0).setName("Nottingham crown court");
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData1)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
        }

        final HearingsData hearingsData2 = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 1, courtCentreId, courtRoomUUID, "DISTRICT_JUDGE");
        hearingsData2.getHearingData().get(0).setName("Nottingham crown court");
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData2)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
        }

        final PublishCourtListType publishCourtListType = PublishCourtListType.WARN;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData2.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData2.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData2, publishCourtListCommandPayload);
        publishCourtListSteps.createMessageConsumer();
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "true");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingDataForWarn();
    }

    /**
     * Scenarios 7: Week commencing for period of  two week.
     * <p>
     * Create Case 1 and Case 2 with one offence and 1 defendant in MG court.
     * Adjournment  case 1 and case 2 to Nottingham CC court for week commencing of 21 Aug 2023.
     * Go to unallocated hearing screen.
     * Go to Unallocated hearing screen.
     * Select court,Prosecutor,Hearing type,Jurisdiction and Possible disqual. click on filter.
     * Select case 1 and Case 2.Click on allocate.
     * Allocate case 1 and Case 2 to crown court.
     * Select Week commencing as 21-Aug-2023 and For a period of Two week option for case1 and case 2
     * Select Week commencing for period of  two week for week start date as 21-Aug-2023.click on save.
     * Select Week commencing Filter list by date as 21 Aug 2023 Week commencing .click on apply button.
     * Click on Share warn/Firm list.
     * Verify the data in XML file.
     * Select Week commencing Filter list by date as 28 Aug 2023 Week commencing .click on apply button.
     * Click on Share warn/Firm list.
     * Verify the data in XML file.
     *
     * @throws Exception
     */

    @Test
    public void testTwoWeekendCommencingWithTwoCases() throws Exception {

        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;

        final HearingsData hearingsData1 = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 2, courtCentreId, courtRoomUUID, "DISTRICT_JUDGE");
        hearingsData1.getHearingData().get(0).setName("Nottingham crown court");
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData1)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
        }

        final HearingsData hearingsData2 = HearingsData.hearingsDataForWeekCommencing(LocalDate.now(), 2, courtCentreId, courtRoomUUID, "DISTRICT_JUDGE");
        hearingsData2.getHearingData().get(0).setName("Nottingham crown court");
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData2)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
        }

        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData2.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData2.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData2, publishCourtListCommandPayload);
        publishCourtListSteps.createMessageConsumer();
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "true");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingDataForFirm();
    }

    /**
     * Scenario where mandatory sitting tag was not present in some cases
     * @throws Exception
     */
    @Test
    public void testWeekendCommencingWhereSittingIsNull() throws Exception {

        stubUpdateAvailableHearingSlotsService();
        final UUID courtCentreId = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
        final UUID courtRoomUUID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");
        final UUID courtListId = randomUUID();
        final int courtRoomId = 231;

        final HearingsData hearingsData1 = HearingsData.singleHearingDataSingleCaseWithSingleOffence(courtCentreId, courtRoomUUID, "DISTRICT_JUDGE", "Norwich Crown court", 1);
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData1)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
        }

        final PublishCourtListType publishCourtListType = PublishCourtListType.FIRM;
        final LocalDate startDate = LocalDate.now();

        final JsonObject publishCourtListCommandPayload = buildPublishCourtListCommandPayload(
                courtCentreId,
                publishCourtListType,
                startDate);

        stubGetReferenceDataCourtCentreById(courtCentreId);

        stubIdMapperReturningExistingAssociation(courtListId);
        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, DEFAULT_COURT_ROOM_ID, DEFAULT_COURT_CENTRE_NAME));
        stubGetReferenceDataCpCourtRooms(hearingsData1.getHearingData().get(0).getCourtRoomId(), courtRoomId);
        stubGetReferenceDataXhibitCourtRoomMappings(hearingsData1.getHearingData().get(0).getCourtRoomId());

        final PublishCourtListSteps publishCourtListSteps = new PublishCourtListSteps(hearingsData1, publishCourtListCommandPayload);
        publishCourtListSteps.createMessageConsumer();
        publishCourtListSteps.acceptCourtListXmlFiles();
        publishCourtListSteps.sendPublishCourtListCommand();
        publishCourtListSteps.verifyCourtListPublishStatus(EXPORT_SUCCESSFUL, "true");
        TimeUnit.SECONDS.sleep(20);
        publishCourtListSteps.verifySentPublishedCourtListHearingDataFirmWithSittingTagPresent();
    }

    /**
     * Scenarios 20:Verify the Public Court List,Standard court list,Ushers list and Alphabetical list should download with data
     *
     * Create case in Luton Magistrates' Court' Court for selected hearing date
     * Got Publish and Download Hearing List
     * Select Exeter Magistrates' Court
     * Select selected hearing date
     * Click on" Public Court List"
     * Public court list should download.
     * Click on "Standard court list"
     * Standard court list should download.
     * Click on "Ushers list"
     * Ushers list should download.
     * Click on "Alphabetical list"
     * Alphabetical list ist should download.
     */

    @Test
    public void testUshersCourtList() {
        HearingsData firstHearing = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearing)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }
        UpdatedHearingData updatedHearingDataForAllocation = UpdatedHearingData.updatedHearingDataForAllocation(firstHearing.getHearingData().get(0).getId());

        try (final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(firstHearing, updatedHearingDataForAllocation)) {
            updateHearingSteps.whenHearingIsUpdatedForListing();
            updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPI();
            updateHearingSteps.verifyPublicHearingChangesSaved();
        }
        CourtListSteps courtListSteps = new CourtListSteps(updatedHearingDataForAllocation);
        courtListSteps.verifyCourtListRequestedAndIsCorrect(CourtListType.USHERS_MAGISTRATE.name());
    }


}
