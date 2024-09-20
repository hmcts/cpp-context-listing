package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;

import uk.gov.moj.cpp.listing.steps.HearingAsMarkedSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.UUID;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class HearingAsMarkedIT extends AbstractIT {

    @Test
    public void shouldHearingAsMarked() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        HearingData hearingData = hearingsData.getHearingData().get(0);
        final HearingAsMarkedSteps hearingAsMarkedSteps = new HearingAsMarkedSteps(hearingData);
        hearingAsMarkedSteps.whenHearingMarkedAsDuplicatePublicEventIsPublished();
        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicatePublicEventInActiveMQ();
        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicateInActiveMQ();
        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicateForCaseInActiveMQ();
        hearingAsMarkedSteps.verifyDeletedFromHearingViewStore();
    }

    @Test
    public void shouldRemoveUnallocatedHearingMarkedAsDuplicate() {
        final HearingsData hearingsData = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        listCourtHearingSteps.verifyPrivateEventRequestedHearingFromStagingHmiNotInActiveMQ();

        HearingData hearingData = hearingsData.getHearingData().get(0);
        final HearingAsMarkedSteps hearingAsMarkedSteps = new HearingAsMarkedSteps(hearingData);
        hearingAsMarkedSteps.whenUnallocatedHearingMarkedAsDuplicateCommandIsSent();
        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicateInActiveMQ();
        hearingAsMarkedSteps.verifyDeletedFromHearingViewStore();
        hearingAsMarkedSteps.verifyPrivateEventDeletedHearingInStagingHmiNotInActiveMQ();
    }

    @Disabled("will be handled with DD-34779")
    @Test
    public void shouldHearingDeletedForHmi() {
        final UUID courtCentreId = randomUUID();
        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, CROWN_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListingHmiEnabled();
        listCourtHearingSteps.verifyHearingListedInActiveMQ();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);
        listCourtHearingSteps.verifyPrivateEventRequestedHearingFromStagingHmiInActiveMQ();

        HearingData hearingData = hearingsData.getHearingData().get(0);
        final HearingAsMarkedSteps hearingAsMarkedSteps = new HearingAsMarkedSteps(hearingData);
        hearingAsMarkedSteps.whenHearingMarkedAsDuplicatePublicEventIsPublished();
        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicatePublicEventInActiveMQ();
        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicateInActiveMQ();
        hearingAsMarkedSteps.verifyHearingMarkedAsDuplicateForCaseInActiveMQ();
        hearingAsMarkedSteps.verifyDeletedFromHearingViewStore();
        hearingAsMarkedSteps.verifyHmiPublicEventForDeleteHearing();
        hearingAsMarkedSteps.verifyPrivateEventDeletedHearingInStagingHmiInActiveMQ();
    }

}
