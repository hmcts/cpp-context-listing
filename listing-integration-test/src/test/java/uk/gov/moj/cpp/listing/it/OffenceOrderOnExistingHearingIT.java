package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.CourtListIT.STANDARD;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.CivilOffenceData;
import uk.gov.moj.cpp.listing.steps.CourtListSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.ListNextHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.Response;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

/**
 * Offences are printed on the standard court list in the order they are stored on the hearing,
 * while Case at a glance and Manage hearing sort them by count / orderIndex.
 *
 * <p>Real-life trigger: offences 1 and 3 are adjourned to a new hearing, then offence 2 is
 * resulted into that same hearing as an existing hearing ({@code existingHearingId} prompt).
 * Progression raises {@code listing.update-related-hearing}, listing emits
 * {@code cases-added-to-hearing}, and the listener appends offence 2 after offence 3 — so the
 * court list reads 1, 3, 2 while the UI reads 1, 2, 3.
 */
class OffenceOrderOnExistingHearingIT extends AbstractIT {

    private static final String MEDIA_TYPE_SEARCH_HEARING_JSON = "application/vnd.listing.search.hearing+json";
    private static final String MEDIA_TYPE_SEARCH_COURT_LIST_PAYLOAD = "application/vnd.listing.search.court.list.payload+json";

    private final OffenceData offence1 = offence("Offence one", 1);
    private final OffenceData offence2 = offence("Offence two", 2);
    private final OffenceData offence3 = offence("Offence three", 3);
    private final OffenceData offence4 = offence("Offence four", 4);

    @Test
    void shouldStoreOffencesInOrderIndexOrderWhenAnOffenceIsResultedIntoAnExistingHearing() {
        final HearingsData existingHearings = existingHearingWithOffences(offence1, offence3);
        final UUID existingHearingId = existingHearings.getHearingData().get(0).getId();

        resultOffencesIntoExistingHearing(existingHearings, offence2);

        final List<String> storedOffenceIds = pollForStoredOffenceIdsOnceArrived(existingHearingId, existingHearings, offence2);
        assertThat(storedOffenceIds, contains(idOf(offence1), idOf(offence2), idOf(offence3)));
    }

    @Test
    void shouldPrintOffencesOnTheStandardCourtListInOrderIndexOrderWhenAnOffenceIsResultedIntoAnExistingHearing() {
        final HearingsData existingHearings = existingHearingWithOffences(offence1, offence3);

        resultOffencesIntoExistingHearing(existingHearings, offence2);
        pollForStoredOffenceIdsOnceArrived(existingHearings.getHearingData().get(0).getId(), existingHearings, offence2);

        final UpdatedHearingData allocation = allocate(existingHearings);

        // the allocated hearing sits on more than one day, so it is printed more than once — every
        // printing must list the offences in orderIndex order
        final List<List<String>> printedOffenceIdsPerAppearance = pollForStandardCourtListOffenceIdsOnceArrived(allocation, existingHearings, offence2);
        assertThat(printedOffenceIdsPerAppearance, is(not(empty())));
        assertThat(printedOffenceIdsPerAppearance, everyItem(contains(idOf(offence1), idOf(offence2), idOf(offence3))));
    }

    /**
     * Production incident: offences already on the hearing are resulted into it again from
     * another hearing, arriving in progression's drifted order. Offence 4 is new and rides along
     * only so the test knows when the event has been processed.
     */
    @Test
    void shouldKeepOrderIndexOrderWhenOffencesAlreadyOnTheHearingAreResultedIntoItAgainInADifferentOrder() {
        final HearingsData existingHearings = existingHearingWithOffences(offence1, offence2, offence3);
        final UUID existingHearingId = existingHearings.getHearingData().get(0).getId();

        resultOffencesIntoExistingHearing(existingHearings, offence3, offence4, offence1, offence2);

        final List<String> storedOffenceIds = pollForStoredOffenceIdsOnceArrived(existingHearingId, existingHearings, offence4);
        assertThat(storedOffenceIds, contains(idOf(offence1), idOf(offence2), idOf(offence3), idOf(offence4)));
    }

    /**
     * The hearing that will later have offences resulted into it, listed with the given offences
     * on its first defendant, in the given order.
     */
    private HearingsData existingHearingWithOffences(final OffenceData... offencesToList) {
        final HearingsData existingHearings = hearingsData();
        final List<OffenceData> offences = firstDefendant(existingHearings).getOffences();
        offences.clear();
        offences.addAll(List.of(offencesToList));

        final ListCourtHearingSteps steps = new ListCourtHearingSteps(existingHearings);
        steps.whenCaseIsSubmittedForListing();
        steps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        return existingHearings;
    }

    /**
     * Mirrors progression's {@code listing.update-related-hearing} for a result carrying an
     * existingHearingId: same case and defendant, the given offences in the given order.
     */
    private void resultOffencesIntoExistingHearing(final HearingsData existingHearings, final OffenceData... resultedOffences) {
        final HearingsData seedingHearings = hearingsData();
        final ListCourtHearingSteps seedingSteps = new ListCourtHearingSteps(seedingHearings);
        seedingSteps.whenCaseIsSubmittedForListing();
        seedingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final HearingsData relatedHearing = hearingsData();
        final List<ListedCaseData> relatedCases = relatedHearing.getHearingData().get(0).getListedCases();
        relatedCases.clear();
        relatedCases.add(caseWithOffences(existingHearings, resultedOffences));

        new ListNextHearingSteps(seedingHearings.getHearingData().get(0))
                .whenUpdateRelatedHearingSubmittedForListing(existingHearings.getHearingData().get(0).getId(), relatedHearing);
    }

    private UpdatedHearingData allocate(final HearingsData hearings) {
        final UpdatedHearingData allocation = updatedHearingDataForAllocation(hearings.getHearingData().get(0).getId());
        stubListHearingInCourtSessionsWithMultipleSchedules(
                hearings.getHearingData().get(0).getId().toString(),
                allocation.getNonDefaultDays().get(0).getCourtScheduleId().orElseThrow(),
                allocation.getNonDefaultDays().get(1).getCourtScheduleId().orElseThrow(),
                ZonedDateTime.parse(allocation.getNonDefaultDays().get(0).getStartTime()),
                allocation.getNonDefaultDays().get(0).getDuration().orElse(20));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(hearings, allocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();
        updateHearingSteps.verifyPublicEventHearingChangesSaved();
        return allocation;
    }

    /**
     * Polls only until the given offence is present, then reads the order once — a wrong order
     * fails fast with the actual sequence instead of timing out.
     */
    private List<String> pollForStoredOffenceIdsOnceArrived(final UUID hearingId, final HearingsData existingHearings, final OffenceData arrivedOffence) {
        final String offenceIdsPath = format("$.listedCases[?(@.id==''{0}'')].defendants[?(@.id==''{1}'')].offences[*].id",
                firstCase(existingHearings).getCaseId(), firstDefendant(existingHearings).getDefendantId());
        final String url = String.format("%s/%s", getBaseUri(), format(readConfig().getProperty("listing.search.hearing"), hearingId.toString()));

        final ResponseData response = pollWithDefaults(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK),
                        payload().isJson(withJsonPath(offenceIdsPath, hasItem(idOf(arrivedOffence)))));

        return JsonPath.read(response.getPayload(), offenceIdsPath);
    }

    /**
     * One list of offence ids per appearance of the hearing on the standard court list.
     */
    private List<List<String>> pollForStandardCourtListOffenceIdsOnceArrived(final UpdatedHearingData allocation, final HearingsData existingHearings, final OffenceData arrivedOffence) {
        // registers the reference-data and document stubs the court list query needs
        new CourtListSteps(allocation);

        final String hearingsPath = format("$.hearingDates[*].courtRooms[*].timeslots[*].hearings[?(@.caseId==''{0}'')]",
                firstCase(existingHearings).getCaseId());
        final String offenceIdsPath = hearingsPath + format(".defendants[?(@.id==''{0}'')].offences[*].id",
                firstDefendant(existingHearings).getDefendantId());
        final String defendantOffenceIdsPath = format("$.defendants[?(@.id==''{0}'')].offences[*].id",
                firstDefendant(existingHearings).getDefendantId());
        final String url = String.format("%s/%s", getBaseUri(), format(readConfig().getProperty("listing.search.court.list.payload-court-room-id"),
                allocation.getCourtCentreId(), allocation.getStartDate(), STANDARD, allocation.getStartDate(), allocation.getCourtRoomId()));

        final ResponseData response = pollWithDefaults(requestParams(url, MEDIA_TYPE_SEARCH_COURT_LIST_PAYLOAD).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK),
                        payload().isJson(withJsonPath(offenceIdsPath, hasItem(idOf(arrivedOffence)))));

        final List<Object> printedHearings = JsonPath.read(response.getPayload(), hearingsPath);
        final List<List<String>> offenceIdsPerAppearance = new ArrayList<>();
        printedHearings.forEach(printedHearing -> offenceIdsPerAppearance.add(JsonPath.read(printedHearing, defendantOffenceIdsPath)));
        return offenceIdsPerAppearance;
    }

    private static ListedCaseData caseWithOffences(final HearingsData existingHearings, final OffenceData... offences) {
        final ListedCaseData existingCase = firstCase(existingHearings);
        final DefendantData existingDefendant = firstDefendant(existingHearings);
        final DefendantData sameDefendantWithGivenOffences = new DefendantData(existingDefendant.getDefendantId(), existingDefendant.getFirstName(),
                existingDefendant.getLastName(), existingDefendant.getDateOfBirth(), existingDefendant.getCustodyTimeLimit(),
                existingDefendant.getBailStatus(), existingDefendant.getDefenceOrganisation(), new ArrayList<>(List.of(offences)),
                existingDefendant.getLegalEntityDefendant(), existingDefendant.getRestrictFromCourtList(), existingDefendant.getIsYouth(),
                existingDefendant.getProceedingsConcluded(), existingDefendant.getMasterDefendantId(),
                existingDefendant.getCourtProceedingsInitiated(), existingDefendant.getListingReason());
        return new ListedCaseData(existingCase.getCaseId(), existingCase.getAuthorityId(), existingCase.getAuthorityCode(),
                existingCase.getCaseReference(), new ArrayList<>(List.of(sameDefendantWithGivenOffences)),
                existingCase.getRestrictFromCourtList(), existingCase.getEjected(), existingCase.getCaseMarkers(), existingCase.getCaseStatus(),
                existingCase.getGroupId(), existingCase.getCivil(), existingCase.getGroupMember(), existingCase.getGroupMaster());
    }

    private static ListedCaseData firstCase(final HearingsData hearings) {
        return hearings.getHearingData().get(0).getListedCases().get(0);
    }

    private static DefendantData firstDefendant(final HearingsData hearings) {
        return firstCase(hearings).getDefendants().get(0);
    }

    private static OffenceData offence(final String title, final int orderIndex) {
        return new OffenceData(randomUUID(), "TW01046", ItClock.today(), ItClock.today(), title, title, "wording",
                0, orderIndex, "Contrary to section 1", randomUUID(), Optional.empty(), Optional.empty(), ItClock.today(),
                Optional.of(Boolean.FALSE), null, null, new CivilOffenceData(false));
    }

    private static String idOf(final OffenceData offence) {
        return offence.getOffenceId().toString();
    }
}
