package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetHearingIdsWithBody;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.it.util.HearingHelper.pollForHearingByIdWithJmsDelay;
import static uk.gov.moj.cpp.listing.steps.RemoveOffencesFromHearingSteps.PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.courtSchedulerCallCountForHearing;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.RemoveOffencesFromHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * SPRDT-1364: listing subscribes to the event progression publishes after a split (SPRDT-1362) so the
 * original hearing loses the moved offences. The event carries the same {hearingId, offenceIds}
 * contract as the hearing-context sibling covered by {@link RemoveOffencesFromHearingIT}, and is
 * routed to the same listing.command.remove-selected-offences-from-existing-hearing.
 */
class RemoveOffencesFromHearingByProgressionIT extends AbstractIT {

    private static final String COURT_SCHEDULE_ID = "8e837de0-743a-4a2c-9db3-b2e678c48729";

    @Test
    void shouldRemoveOffencesFromAllocatedHearingWhenProgressionPublishesSplitRemoval() {
        final HearingsData hearings = hearingsDataWithAllocationDataAndJudiciary();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearings);
        stubGetProvisionalBookedSlotsSingleCourtScheduleCountBased();
        stubListHearingInCourtSessions(hearings.getHearingData().get(0).getId().toString(),
                COURT_SCHEDULE_ID, hearings.getHearingData().get(0).getHearingStartTime());
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(ALLOCATED);

        final String hearingId = hearings.getHearingData().get(0).getId().toString();
        final List<String> offencesToRemove = firstOffenceOfFirstDefendant(hearings);
        final int initialOffenceCount = offenceCountOfFirstDefendant(hearings);

        pollForHearingByIdWithJmsDelay(USER_ID_VALUE, UUID.fromString(hearingId),
                withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(initialOffenceCount)));

        // Listing an ALLOCATED crown hearing consults courtscheduler, so the baseline must be > 0.
        // Asserting that first is what stops the guard below degenerating into 0 == 0, which would
        // pass even if the counter matched nothing at all.
        final int courtSchedulerCallsBeforeRemoval = courtSchedulerCallCountForHearing(hearingId);
        assertThat("the SPRDT-1227 guard can only prove anything if it can see courtscheduler calls "
                        + "for this hearing in the first place",
                courtSchedulerCallsBeforeRemoval, greaterThan(0));

        final RemoveOffencesFromHearingSteps steps = new RemoveOffencesFromHearingSteps();
        steps.whenProgressionRaisedOffencesRemovedPublicEvent(hearingId, offencesToRemove);
        steps.verifyPublicListingOffencesRemovedFromAllocatedHearing();

        pollForHearingByIdWithJmsDelay(USER_ID_VALUE, UUID.fromString(hearingId),
                withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(initialOffenceCount - 1)));

        // SPRDT-1227 regression guard: the hearing keeps its allocation and courtscheduler is not
        // re-consulted for it.
        assertThat(courtSchedulerCallCountForHearing(hearingId), is(courtSchedulerCallsBeforeRemoval));
        // Still allocated. The room is the one court-schedule enrichment assigned, not the one the
        // listing request asked for, so assert it is still set rather than which room it is.
        pollForHearingByIdWithJmsDelay(USER_ID_VALUE, UUID.fromString(hearingId),
                withJsonPath("$.hearingDays[0].courtRoomId", notNullValue()));

        // AC-4 second clause: the allocated range-search (businessType + allocated + ouCode) must
        // still return this hearing after the removal. Court-scheduler supplies only the hearing
        // IDs; listing then enriches each one from its own viewstore, so a hearing that had been
        // dropped from the allocated view would come back missing here even though the stub still
        // offered its id.
        final LocalDate hearingDate = hearings.getHearingData().get(0).getHearingStartTime().toLocalDate();
        stubGetHearingIdsWithBody("{ \"results\": 1, \"pageCount\": 1, \"hearingIds\": ["
                + "{\"hearingId\":\"" + hearingId + "\",\"courtScheduleId\":\"" + COURT_SCHEDULE_ID + "\","
                + "\"hearingDate\":\"" + hearingDate + "\",\"hearingDayCount\":1,\"hearingDayPosition\":1}"
                + "] }");

        final String rangeSearchUrl = String.format("%s/listing-query-api/query/api/rest/listing/hearings/range-search"
                        + "?allocated=true&jurisdictionType=MAGISTRATES&businessType=REM&ouCode=B04KO00"
                        + "&sessionStartDate=%s&sessionEndDate=%s&pageSize=10&pageNumber=1",
                getBaseUri(), hearingDate, hearingDate);
        final RequestParams rangeSearchParams = requestParams(rangeSearchUrl, "application/vnd.listing.search.hearings+json")
                .withHeader(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue())
                .build();

        pollWithDefaults(rangeSearchParams).until(status().is(OK), payload().isJson(allOf(
                withJsonPath("$.hearings[*].id", hasItem(hearingId)),
                withJsonPath("$.hearings[0].allocated", is(true)))));
    }

    @Test
    void shouldRemoveOffencesFromUnallocatedHearingWhenProgressionPublishesSplitRemoval() {
        final HearingsData hearings = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final String hearingId = hearings.getHearingData().get(0).getId().toString();
        final List<String> offencesToRemove = firstOffenceOfFirstDefendant(hearings);
        final int initialOffenceCount = offenceCountOfFirstDefendant(hearings);
        // Unlike the allocated case, listing an UNALLOCATED hearing never consults courtscheduler, so
        // this baseline is legitimately 0 and the assertion below reads "still zero". The counter
        // itself is proven live by the allocated test, which asserts a non-zero baseline.
        final int courtSchedulerCallsBeforeRemoval = courtSchedulerCallCountForHearing(hearingId);

        final RemoveOffencesFromHearingSteps steps =
                new RemoveOffencesFromHearingSteps(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING);
        steps.whenProgressionRaisedOffencesRemovedPublicEvent(hearingId, offencesToRemove);
        steps.verifyPublicListingOffencesRemovedFromUnallocatedHearing();

        pollForHearingByIdWithJmsDelay(USER_ID_VALUE, UUID.fromString(hearingId),
                withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(initialOffenceCount - 1)));

        assertThat(courtSchedulerCallCountForHearing(hearingId), is(courtSchedulerCallsBeforeRemoval));
    }

    /**
     * The aggregate short-circuits when none of the offences are still on the hearing, so replaying
     * the event emits nothing rather than a second removal.
     */
    @Test
    void shouldEmitNothingWhenTheSameProgressionEventIsReplayed() {
        final HearingsData hearings = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final String hearingId = hearings.getHearingData().get(0).getId().toString();
        final List<String> offencesToRemove = firstOffenceOfFirstDefendant(hearings);
        final int initialOffenceCount = offenceCountOfFirstDefendant(hearings);

        final RemoveOffencesFromHearingSteps steps =
                new RemoveOffencesFromHearingSteps(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING);
        steps.whenProgressionRaisedOffencesRemovedPublicEvent(hearingId, offencesToRemove);
        steps.verifyPublicListingOffencesRemovedFromUnallocatedHearing();

        pollForHearingByIdWithJmsDelay(USER_ID_VALUE, UUID.fromString(hearingId),
                withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(initialOffenceCount - 1)));

        steps.whenProgressionRaisedOffencesRemovedPublicEvent(hearingId, offencesToRemove);
        steps.verifyNoFurtherPublicListingOffencesRemoved();

        pollForHearingByIdWithJmsDelay(USER_ID_VALUE, UUID.fromString(hearingId),
                withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(initialOffenceCount - 1)));
    }

    @Test
    void shouldEmitNothingWhenTheHearingHasNoneOfTheOffences() {
        final HearingsData hearings = hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearings);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        final String hearingId = hearings.getHearingData().get(0).getId().toString();
        final int initialOffenceCount = offenceCountOfFirstDefendant(hearings);

        final RemoveOffencesFromHearingSteps steps =
                new RemoveOffencesFromHearingSteps(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING);
        steps.whenProgressionRaisedOffencesRemovedPublicEvent(hearingId, List.of(UUID.randomUUID().toString()));
        steps.verifyNoFurtherPublicListingOffencesRemoved();

        pollForHearingByIdWithJmsDelay(USER_ID_VALUE, UUID.fromString(hearingId),
                withJsonPath("$.listedCases[0].defendants[0].offences.length()", equalTo(initialOffenceCount)));
    }

    private static List<String> firstOffenceOfFirstDefendant(final HearingsData hearings) {
        return hearings.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().stream()
                .limit(1)
                .map(offenceData -> offenceData.getOffenceId().toString())
                .collect(Collectors.toList());
    }

    // Derived from the generated data rather than hard-coded, so a change to the test-data factory's
    // offence count surfaces as a real failure instead of a misleading one.
    private static int offenceCountOfFirstDefendant(final HearingsData hearings) {
        return hearings.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().size();
    }
}
