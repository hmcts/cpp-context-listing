package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.it.SearchAvailableHearingIT.MATCHED_DEFENDANTS;
import static uk.gov.moj.cpp.listing.it.util.HearingHelper.pollForHearingById;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate_CivilCase;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithShadowListedOffences;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.*;
import static uk.gov.moj.cpp.listing.utils.ProgressionServiceStub.stubProgressionServiceCivilCase;
import static uk.gov.moj.cpp.listing.utils.ProgressionServiceStub.stubProgressionServiceCivilCaseSummons;

import com.google.common.collect.ImmutableMap;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")
public class ListCourtHearingIT extends AbstractIT {

    static final boolean UNALLOCATED = false;
    static final boolean ALLOCATED = true;

    @Test
    public void shouldListHearingWithUnallocatedData() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsData());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleCountBasedSlot() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1));
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleCountBasedSlot_CivilCase() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate_CivilCase(1));
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        stubProgressionServiceCivilCase();
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        io.restassured.path.json.JsonPath jsonPath = listCourtHearingSteps.getHearingConfirmedPublicEventPayload();
        assertThat(jsonPath.get("sendNotificationToParties"), is(true));
        assertThat(jsonPath.get("confirmedHearing.prosecutionCases"), hasSize(2));
        assertThat(jsonPath.get("confirmedHearing.prosecutionCases[0].isCivil"), is(true));
        assertThat(jsonPath.get("confirmedHearing.prosecutionCases[1].isCivil"), is(true));
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleCountBasedSlot_CivilSummons() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate_CivilCase(1));
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        stubProgressionServiceCivilCaseSummons();
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        io.restassured.path.json.JsonPath jsonPath = listCourtHearingSteps.getHearingConfirmedPublicEventPayload();
        assertThat(jsonPath.get("sendNotificationToParties"), is(false));
        assertThat(jsonPath.get("confirmedHearing.prosecutionCases"), hasSize(2));
        assertThat(jsonPath.get("confirmedHearing.prosecutionCases[0].isCivil"), is(true));
        assertThat(jsonPath.get("confirmedHearing.prosecutionCases[1].isCivil"), is(true));
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleCountBasedSlotHmiEnabled() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1));
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test
    public void shouldListHearingWithPossibleDisqualification() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithPossibleDisqualification());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingWithPossibleDisqualificationFromAPI();
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleCountBasedSlots() {

        stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1));
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleCountBasedSlotsHmiEnabled() {

        stubGetProvisionalBookedSlotsMultipleCourtSchedulesCountBased();

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1));
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleDurationBasedSlot() {

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1));
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test
    public void shouldListHearingWithAdjournedDateSingleDurationBasedSlotHmiEnabled() {

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1));
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test

    public void shouldListHearingWithAdjournedDateMultipleDurationBasedSlotsWinterTime() {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1);

        final String courtRoomId1 = hearingsData.getHearingData().get(0).getCourtRoomId().toString();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<String, String>() {{
            put("2020-02-11", courtRoomId1);
            put("2020-02-12", courtRoomId1);
            put("2020-02-13", "33b7d399-8379-437c-980d-af9487b1198c");
        }};
        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(courtRoomSchedules, courtCentreId);
        stubListHearingInCourtSessionsForProvisionalBooking(hearingsData.getHearingData().get(0).getId().toString(), "2020-02-11");

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);

        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedWithHearingDays(ALLOCATED, courtRoomSchedules.keySet().toArray(String[]::new), courtRoomSchedules.values().toArray(String[]::new));
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleDurationBasedSlotsSummerTime() {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1);

        final String courtRoomId1 = hearingsData.getHearingData().get(0).getCourtRoomId().toString();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<String, String>() {{
            put("2020-05-21", courtRoomId1);
            put("2020-05-22", "33b7d399-8379-437c-980d-af9487b1198c");
            put("2020-05-23", courtRoomId1);
        }};
        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(courtRoomSchedules, courtCentreId);
        stubListHearingInCourtSessionsForProvisionalBooking(hearingsData.getHearingData().get(0).getId().toString(), "2020-05-21");

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedWithHearingDays(ALLOCATED, courtRoomSchedules.keySet().toArray(String[]::new), courtRoomSchedules.values().toArray(String[]::new));
    }

    @Test
    public void shouldListHearingWithAdjournedDateMultipleDurationBasedSlotsSummerTimeHmiEnabled() {

        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(1);

        final String courtRoomId1 = hearingsData.getHearingData().get(0).getCourtRoomId().toString();
        final String courtCentreId = hearingsData.getHearingData().get(0).getCourtCentreId().toString();
        final Map<String, String> courtRoomSchedules = new LinkedHashMap<String, String>() {{
            put("2020-05-21", courtRoomId1);
            put("2020-05-22", "33b7d399-8379-437c-980d-af9487b1198c");
            put("2020-05-23", courtRoomId1);
        }};
        stubGetProvisionalBookedSlotsMultipleCourtScheduleDurationBased(courtRoomSchedules, courtCentreId);
        stubListHearingInCourtSessionsForProvisionalBooking(hearingsData.getHearingData().get(0).getId().toString(), "2020-05-21");

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedWithHearingDays(ALLOCATED, courtRoomSchedules.keySet().toArray(String[]::new), courtRoomSchedules.values().toArray(String[]::new));
    }

    @Test
    public void shouldListHearingWithAllocatedData() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciary());
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
    }

    @Test
    public void shouldListHearingAsUnallocatedAndSendDummyCourtroomToHmi() {
        final UUID hearingId = randomUUID();
        final String caseUrn = RandomGenerator.STRING.next();
        final UUID masterDefendantId = randomUUID();
        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(hearingId, null, caseUrn, masterDefendantId, MATCHED_DEFENDANTS, MAGISTRATES.name(), MAGISTRATES.name(),
                null, null);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithUnAllocationDataAndJudiciary(caseAndDefendantData));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
    }

    @Test
    public void shouldListHearingWithAllocatedDataHmiEnabled() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        verifyJudiciaryAssignedToAllocatedHearingFromAPI(hearingsData);
    }

    @Test
    public void shouldListHearingWithUnallocatedDataForStandaloneApplication() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataStandaloneApplication());
        listCourtHearingSteps.whenCaseIsSubmittedForListingStandaloneApplication();
        listCourtHearingSteps.verifyHearingListedFromAPIForStandaloneApplication(UNALLOCATED);
    }

    @Test
    public void shouldListHearingWithLegalEntity() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithLegalEntity());
        listCourtHearingSteps.whenCaseIsSubmittedForListingWithLegalEntity();
        listCourtHearingSteps.verifyHearingListedWithLegalEntity(UNALLOCATED);
    }

    @Test
    public void shouldListHearingByIdWhenItExists() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.singleHearingData());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyExistingHearingById();
    }

    @Test
    public void shouldListHearingByIdWhenItDoesntExist() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.singleHearingData());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyNonExistentHearingById();
    }

    @Test
    public void shouldListHearingByIdWhenIdIsInvalid() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.singleHearingData());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingByIdWithInvalidId();
    }

    @Test
    public void shouldListHearingWithShadowListedFlag() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithShadowListedOffences());
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();

        verifyShadowListingFlagAndReportingRestrictions(listCourtHearingSteps);
    }

    @Test
    public void shouldListHearingWithShadowListedFlagHmiEnabled() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithShadowListedOffences());
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        verifyShadowListingFlagAndReportingRestrictions(listCourtHearingSteps);
    }

    @Test
    public void shouldExtendHearingWithShadowListedFlag() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithShadowListedOffences());
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        listCourtHearingSteps.whenProgressionHearingExtended();
        verifyShadowListingFlagAndReportingRestrictions(listCourtHearingSteps);
    }

    @Test
    public void shouldExtendHearingWithShadowListedFlagHmiEnabled() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithShadowListedOffences());
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        listCourtHearingSteps.whenProgressionHearingExtended();
        verifyShadowListingFlagAndReportingRestrictions(listCourtHearingSteps);
    }

    @Test
    public void shouldRetrieveCasesByDefendantAndHearingDateForAllocatedHearing() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsDataWithAllocationDataAndJudiciary());
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased(LocalDate.now(), ImmutableMap.of("courtRoomId", listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId().toString()));
        stubListHearingInCourtSessions(listCourtHearingSteps.getHearingsData().getHearingData().get(0).getId().toString(),
                "8e837de0-743a-4a2c-9db3-b2e678c48729",
                ZonedDateTime.now(ZoneId.of("Europe/London")).withHour(10).withMinute(0).withSecond(0).withNano(0));
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        listCourtHearingSteps.verifyQueryAPIFindCaseByPersonDefendantAndHearingDate();
    }

    @Test
    public void shouldRetrieveCasesByDefendantAndHearingDateForUnAllocatedHearing() {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(HearingsData.hearingsDataWithLegalEntity());
        listCourtHearingSteps.whenCaseIsSubmittedForListingWithLegalEntity();
        listCourtHearingSteps.verifyHearingListedWithLegalEntity(UNALLOCATED);
        listCourtHearingSteps.verifyQueryAPIFindCaseByOrganisationDefendantAndHearingDate();
    }

    private void verifyShadowListingFlagAndReportingRestrictions(final ListCourtHearingSteps listCourtHearingSteps) {
        final HearingData hearingData = listCourtHearingSteps.getHearingsData().getHearingData().get(0);
        pollForHearing(hearingData.getCourtCentreId().toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings[0].listedCases[0].shadowListed",

                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[1].shadowListed",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].shadowListed",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[1].shadowListed",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[2].shadowListed",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[1].offences[0].shadowListed",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[1].offences[1].shadowListed",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[1].offences[2].shadowListed",
                        equalTo(true)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].reportingRestrictions[0].label",
                        equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictionDataList().get(0).getLabel())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].reportingRestrictions[0].orderedDate",
                        equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictionDataList().get(0).getOrderedDate().get().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].reportingRestrictions[0].judicialResultId",
                        equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictionDataList().get(0).getJudicialResultId().get().toString()))
        });
    }

    private void verifyJudiciaryAssignedToAllocatedHearingFromAPI(final HearingsData hearingsData) {

        List<Matcher> matchers = Lists.newArrayList();
        final List<JudicialRoleData> judicialRoleDataList = hearingsData.getHearingData().get(0).getJudiciary();
        assertThat(judicialRoleDataList.size(), is(greaterThan(0)));
        IntStream.range(0, judicialRoleDataList.size())
                .forEach(judiciaryIndex -> {
                    final String baseJudiciaryPath = String.format("$.judiciary[%d]", judiciaryIndex);
                    matchers.add(withJsonPath(baseJudiciaryPath + ".judicialId", is(judicialRoleDataList.get(judiciaryIndex).getJudicialId().toString())));
                    matchers.add(withJsonPath(baseJudiciaryPath + ".judicialRoleType.judiciaryType", is(judicialRoleDataList.get(judiciaryIndex).getJudicialRoleType().getJudiciaryType())));
                });

        pollForHearingById(getLoggedInUser(), hearingsData.getHearingData().get(0).getId(), allOf(matchers.toArray(new Matcher[0])));
    }
}
