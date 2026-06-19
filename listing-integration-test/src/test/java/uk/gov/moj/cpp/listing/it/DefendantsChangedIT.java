package uk.gov.moj.cpp.listing.it;


import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData.partialDefendantUpdateWithoutIsYouth;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData.updatedDefendantData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData.updatedDefendantDataWithIsYouth;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateDefendantSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class DefendantsChangedIT extends AbstractIT {

    @Test
    void shouldUpdateDefendantsFollowingPublicDefendantsChangedEventFromProgression() {
        HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        UpdatedDefendantData updatedDefendantData = updatedDefendantData(defendantData);

        final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData);
        updateDefendantSteps.whenPublicEventProgressionCaseDefendantsUpdatedIsPublished();
        updateDefendantSteps.verifyHearingListedFromAPIWithJmsDelay(false);
    }

    @Test
    void shouldUpdateDefendantsFollowingPublicDefendantsChangedEventFromProgressionHmiEnabled() {
        String courtScheduleId = "8e837de0-743a-4a2c-9db3-b2e678c48729";
        HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        final ZonedDateTime hearingStartTime = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getHearingStartTime();
        final LocalDate hearingDate = hearingStartTime.toLocalDate();
        final UUID courtroomId = listCourtHearingSteps.getHearingsData().getHearingData().get(0).getCourtRoomId();
        final UUID bookingId = randomUUID();
        final UUID courtCentreId = getRandomCourtCenterId();

        Map<String, String> stubParams = new HashMap<>();
        stubParams.put("SESSION_DATE", hearingDate.toString());
        stubParams.put("COURT_CENTRE_ID", courtCentreId.toString());
        stubParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        stubParams.put("COURT_ROOM_ID", courtroomId.toString());
        stubParams.put("BOOKING_ID", bookingId.toString());
        stubParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(stubParams);        stubListHearingInCourtSessions(hearingsData.getHearingData().get(0).getId().toString(),
                courtScheduleId, hearingsData.getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        HearingData hearingData = hearingsData.getHearingData().get(0);
        UpdatedDefendantData updatedDefendantData = updatedDefendantData(defendantData);

        final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData);
        updateDefendantSteps.whenPublicEventProgressionCaseDefendantsUpdatedIsPublished();
        updateDefendantSteps.verifyHearingListedFromAPIWithJmsDelay(true);
    }

    /**
     * {@code public.progression.case-defendant-changed} is processed into {@code listing.command.update-defendants-for-hearing}.
     * Listed defendant starts as an adult ({@code isYouth} false); after the update payload marks them as youth, the view reflects {@code isYouth} true.
     */
    @Test
    void shouldSetDefendantAsYouthAfterUpdateDefendantsForHearingWhenInitiallyAdult() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAdultDefendants();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        listCourtHearingSteps.verifyFirstListedDefendantYouthStatusWithJmsDelay(UNALLOCATED, false);

        final DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        final UpdatedDefendantData updatedDefendantData = updatedDefendantData(defendantData);

        final UpdateDefendantSteps updateDefendantSteps = new UpdateDefendantSteps(caseId, hearingData, updatedDefendantData);
        updateDefendantSteps.whenPublicEventProgressionCaseDefendantsUpdatedIsPublished();
        updateDefendantSteps.verifyHearingListedFromAPIWithJmsDelay(false, true);
    }

    /**
     * Covers {@code Case.updateDefendant} merge and {@code NewDomainToEventConverter.updateEventDefendant} retention:
     * once {@code isYouth} is true, a later {@code public.progression.case-defendant-changed} without {@code isYouth} must not clear it.
     */
    @Test
    void shouldRetainIsYouthWhenSecondCaseDefendantChangedEventOmitsIsYouthFlag() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAdultDefendants();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        listCourtHearingSteps.verifyFirstListedDefendantYouthStatusWithJmsDelay(UNALLOCATED, false);

        final DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final UpdatedDefendantData youthUpdate = updatedDefendantDataWithIsYouth(defendantData, Boolean.TRUE);
        final UpdateDefendantSteps youthFlagSet = new UpdateDefendantSteps(caseId, hearingData, youthUpdate);
        youthFlagSet.whenPublicEventProgressionCaseDefendantChangedIsPublished(youthUpdate);
        youthFlagSet.verifyListedDefendantIsYouthWithJmsDelay(UNALLOCATED, true);

        final UpdatedDefendantData partialUpdate = partialDefendantUpdateWithoutIsYouth(
                defendantData, "YouthRetainedFirst", "YouthRetainedLast");
        final UpdateDefendantSteps partialUpdateSteps = new UpdateDefendantSteps(caseId, hearingData, partialUpdate);
        partialUpdateSteps.whenPublicEventProgressionCaseDefendantChangedIsPublished(partialUpdate);
        partialUpdateSteps.verifyDefendantDetailsUpdatedWithJmsDelay(UNALLOCATED);
        partialUpdateSteps.verifyListedDefendantIsYouthWithJmsDelay(UNALLOCATED, true);
    }

    /**
     * Youth flag may arrive in the second {@code public.progression.case-defendant-changed} after a partial update without {@code isYouth}.
     */
    @Test
    void shouldSetIsYouthWhenYouthFlagArrivesInSecondCaseDefendantChangedEvent() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAdultDefendants();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        listCourtHearingSteps.verifyFirstListedDefendantYouthStatusWithJmsDelay(UNALLOCATED, false);

        final DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final UpdatedDefendantData partialUpdate = partialDefendantUpdateWithoutIsYouth(
                defendantData, "BeforeYouthFirst", "BeforeYouthLast");
        final UpdateDefendantSteps partialUpdateSteps = new UpdateDefendantSteps(caseId, hearingData, partialUpdate);
        partialUpdateSteps.whenPublicEventProgressionCaseDefendantChangedIsPublished(partialUpdate);
        // Partial payload omits isYouth; verify name change only (API may omit isYouth when unset).
        partialUpdateSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final UpdatedDefendantData youthUpdate = updatedDefendantDataWithIsYouth(defendantData, Boolean.TRUE);
        final UpdateDefendantSteps youthUpdateSteps = new UpdateDefendantSteps(caseId, hearingData, youthUpdate);
        youthUpdateSteps.whenPublicEventProgressionCaseDefendantChangedIsPublished(youthUpdate);
        youthUpdateSteps.verifyListedDefendantIsYouthWithJmsDelay(UNALLOCATED, true);
    }

    /**
     * Explicit {@code isYouth: false} in a later event must not downgrade a defendant who was already marked as youth.
     */
    @Test
    void shouldRetainIsYouthWhenSecondCaseDefendantChangedEventExplicitlySetsAdult() {
        final HearingsData hearingsData = HearingsData.hearingsDataWithAdultDefendants();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final UpdatedDefendantData youthUpdate = updatedDefendantDataWithIsYouth(defendantData, Boolean.TRUE);
        final UpdateDefendantSteps youthFlagSet = new UpdateDefendantSteps(caseId, hearingData, youthUpdate);
        youthFlagSet.whenPublicEventProgressionCaseDefendantChangedIsPublished(youthUpdate);
        youthFlagSet.verifyListedDefendantIsYouthWithJmsDelay(UNALLOCATED, true);

        final UpdatedDefendantData adultExplicitUpdate = updatedDefendantDataWithIsYouth(defendantData, Boolean.FALSE);
        final UpdateDefendantSteps adultUpdateSteps = new UpdateDefendantSteps(caseId, hearingData, adultExplicitUpdate);
        adultUpdateSteps.whenPublicEventProgressionCaseDefendantChangedIsPublished(adultExplicitUpdate);
        adultUpdateSteps.verifyListedDefendantIsYouthWithJmsDelay(UNALLOCATED, true);
    }

    /**
     * Hearing listed with a young defendant; a subsequent partial {@code case-defendant-changed} without {@code isYouth} retains youth.
     */
    @Test
    void shouldRetainIsYouthWhenListedAsYouthAndSubsequentEventOmitsIsYouthFlag() {
        final HearingsData hearingsData = HearingsData.singleHearingDataForYoungDefendants();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        listCourtHearingSteps.verifyFirstListedDefendantYouthStatusWithJmsDelay(UNALLOCATED, true);

        final DefendantData defendantData = hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0);
        final UUID caseId = hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId();
        final HearingData hearingData = hearingsData.getHearingData().get(0);

        final UpdatedDefendantData partialUpdate = partialDefendantUpdateWithoutIsYouth(
                defendantData, "YoungListedFirst", "YoungListedLast");
        final UpdateDefendantSteps partialUpdateSteps = new UpdateDefendantSteps(caseId, hearingData, partialUpdate);
        partialUpdateSteps.whenPublicEventProgressionCaseDefendantChangedIsPublished(partialUpdate);
        partialUpdateSteps.verifyDefendantDetailsUpdatedWithJmsDelay(UNALLOCATED);
        partialUpdateSteps.verifyListedDefendantIsYouthWithJmsDelay(UNALLOCATED, true);
    }
}
