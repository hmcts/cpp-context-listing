package uk.gov.moj.cpp.listing.it;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.RestrictCourtListSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import org.junit.Test;

@SuppressWarnings({"squid:UnusedPrivateMethod", "squid:S1607"})
public class RestrictListFromCourtIT extends AbstractIT {

    @Test
    public void shouldRestrictListingCaseFromCourtForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(false);
        }

        try (final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData)) {
            restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getRestrictListingFromCourtData(hearingsData));
            restrictCourtListSteps.verifyRestrictCourtListInActiveMQ();
            restrictCourtListSteps.verifyCaseOrDefendantOrOffenceListingRestrictedInHearing(true, true, false);

        }
    }

    @Test
    public void shouldUnRestrictDefendantsAndOffencesFromListingCaseForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(false);
        }

        try (final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData)) {
            restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getDefendantsAndOffencesDataToBeUnrestricted(hearingsData));
            restrictCourtListSteps.verifyRestrictCourtListInActiveMQ();
            restrictCourtListSteps.verifyCaseOrDefendantOrOffenceListingRestrictedInHearing(false, false, false);

        }
    }

    @Test
    public void shouldRestrictDefendantsAndOffencesFromListingCaseForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(false);
        }

        try (final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData)) {
            restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getDefendantsAndOffencesDataToBeRestricted(hearingsData));
            restrictCourtListSteps.verifyRestrictCourtListInActiveMQ();
            restrictCourtListSteps.verifyCaseOrDefendantOrOffenceListingRestrictedInHearing(false, true, true);

        }
    }

    @Test
    public void shouldRestrictCourtApplicationFromCourtForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        try (final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData)) {
            restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getCourtApplicationDataToBeRestricted(hearingsData));
            restrictCourtListSteps.verifyRestrictCourtListInActiveMQ();
            restrictCourtListSteps.verifyCourtApplicationorApplicantorRespondentListingRestrictedInHearing(true, true, false, false);

        }
    }

    @Test
    public void shouldRestrictCourtApplicationTypeFromCourtForHearingId() {
        HearingsData hearingsData = HearingsData.hearingsData();
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
            listCourtHearingSteps.verifyHearingListedInActiveMQ();
            listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);
        }

        try (final RestrictCourtListSteps restrictCourtListSteps = new RestrictCourtListSteps(hearingsData)) {
            restrictCourtListSteps.whenRestrictingCaseOrStandaloneApplicationForCourtListing(restrictCourtListSteps.getCourtApplicationTypeToBeRestricted(hearingsData));
            restrictCourtListSteps.verifyRestrictCourtListInActiveMQ();
            restrictCourtListSteps.verifyCourtApplicationorApplicantorRespondentListingRestrictedInHearing(false, false, false, true);

        }
    }
}
