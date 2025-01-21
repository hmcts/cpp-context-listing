package uk.gov.moj.cpp.listing.it;

import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.notHmiEnabledHearingsData;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.CROWN_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubDeleteAvailableHearingSlotsService;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.verifyDeleteAvailableHearingSlotsStubCommandInvoked;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListUnscheduledCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.VacatingTrialSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings({"squid:S1607"})
public class VacateHearingIT extends AbstractIT {

    private static final String CONTEXT_NAME = "listing";
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();

    @BeforeEach
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanEventStoreTables(CONTEXT_NAME);
        databaseCleaner.cleanStreamBufferTable(CONTEXT_NAME);
        databaseCleaner.cleanStreamStatusTable(CONTEXT_NAME);
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "hearing");
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "listing_notes");
    }

    public static Stream<Arguments> provideJurisdictionTypes() {
        return Stream.of(
                Arguments.of(CROWN_JURISDICTION),
                Arguments.of(MAGISTRATES_JURISDICTION)
        );
    }

    @Test
    public void shouldVacateMagistrateCourtHearingAndHearingSlotsFreed_WhenInitiatedFromHearing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final String hearingId = hearingsData.getHearingData().get(0).getId().toString();
        stubDeleteAvailableHearingSlotsService(hearingId);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublished();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        verifyDeleteAvailableHearingSlotsStubCommandInvoked(hearingId);
    }

    @Test
    public void shouldVacateMagistrateCourtHearingAndHearingSlotsFreed_WhenInitiatedFromListing() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final String hearingId = hearingsData.getHearingData().get(0).getId().toString();
        stubDeleteAvailableHearingSlotsService(hearingId);
        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacatedFromWithinListing();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
        verifyDeleteAvailableHearingSlotsStubCommandInvoked(hearingId);
    }

    @Test
    public void shouldNotVacateMagistrateCourtHearingAndNotAttemptToFreeHearingSlotsWhenReasonIdIsEmpty() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublishedWithEmptyVacatedTrialReasonId();
        vacatingTrialSteps.verifyVacatedTrialWithEmptyReasonIdWhenQueryingFromAPI();
    }

    @Test
    public void shouldVacateCrownCourtHearingAndNotAttemptToFreeHearingSlots() {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(CROWN_JURISDICTION);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(ALLOCATED);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenPublicEventHearingTrialVacatedIsPublished();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
    }

    @ParameterizedTest
    @MethodSource("provideJurisdictionTypes")
    public void shouldVacateUnallocatedHearingAndNotAttemptToFreeHearingSlots(final String jurisdictionType) {
        final HearingsData hearingsData = hearingsData(jurisdictionType);
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacatedFromWithinListing();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
    }

    @ParameterizedTest
    @MethodSource("provideJurisdictionTypes")
    public void shouldVacateUnscheduledHearingAndNotAttemptToFreeHearingSlots(final String jurisdictionType) {
        final HearingsData hearingsData = notHmiEnabledHearingsData(jurisdictionType);
        final ListUnscheduledCourtHearingSteps listCourtHearingSteps = new ListUnscheduledCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForUnscheduledListing();
        listCourtHearingSteps.verifyHearingUnscheduledListedFromAPI();

        final VacatingTrialSteps vacatingTrialSteps = new VacatingTrialSteps(hearingsData);
        vacatingTrialSteps.whenHearingIsVacatedFromWithinListing();
        vacatingTrialSteps.verifyVacatedTrialWhenQueryingFromAPI();
    }

}
