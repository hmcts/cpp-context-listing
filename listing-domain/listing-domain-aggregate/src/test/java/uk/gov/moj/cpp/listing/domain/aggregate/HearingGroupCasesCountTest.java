package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2;
import uk.gov.justice.listing.events.CaseRemovedFromGroupCases;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.Type;
import uk.gov.moj.cpp.listing.domain.CaseIdentifier;
import uk.gov.moj.cpp.listing.domain.ListedCase;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * CAD-947: the aggregate stores the group size (numberOfGroupCases) that progression sends when a
 * case is removed from a group, and emits it on allocation and on allocated-hearing updates.
 */
class HearingGroupCasesCountTest {

    private final UUID hearingId = randomUUID();
    private final UUID groupId = randomUUID();
    private final UUID masterCaseId = randomUUID();
    private final UUID memberCase1Id = randomUUID();
    private final UUID memberCase2Id = randomUUID();

    private Hearing hearing;

    @BeforeEach
    void setUp() {
        hearing = new Hearing();
    }

    @Test
    void shouldUseCountFromCommandWhenPresent() {
        listGroupHearing(3);

        final CaseRemovedFromGroupCases event = removeMember(memberCase1Id, 2);

        assertThat(event.getNumberOfGroupCases(), is(2));
    }

    @Test
    void shouldUseCountFromCommandWhenMasterRemoved() {
        listGroupHearing(3);

        final CaseRemovedFromGroupCases event = removeCase(masterCaseId, memberCase1Id, 2);

        assertThat(event.getNumberOfGroupCases(), is(2));
    }

    @Test
    void shouldRaiseEventWithoutCountWhenCommandHasNone() {
        listGroupHearing(3);

        final CaseRemovedFromGroupCases event = removeMember(memberCase1Id, null);

        assertThat(event.getNumberOfGroupCases(), is(nullValue()));
    }

    @Test
    void shouldLeaveCountUnchangedWhenRemovalHasNoCount() {
        listGroupHearing(3);
        removeMember(memberCase1Id, null);

        assertThat(allocate().getNumberOfGroupCases(), is(3));
    }

    @Test
    void shouldStoreCountFromEventWhenStateWasNull() {
        listGroupHearing(null);
        removeMember(memberCase1Id, 2);

        assertThat(allocate().getNumberOfGroupCases(), is(2));
    }

    @Test
    void shouldEmitListedCountOnAllocationWhenNoRemoval() {
        listGroupHearing(3);

        assertThat(allocate().getNumberOfGroupCases(), is(3));
    }

    @Test
    void shouldEmitCurrentCountOnAllocationAfterRemoval() {
        listGroupHearing(3);
        removeMember(memberCase1Id, 2);

        assertThat(allocate().getNumberOfGroupCases(), is(2));
    }

    @Test
    void shouldEmitListedCountOnAllocatedHearingUpdate() {
        listGroupHearing(3);
        allocate();

        assertThat(updateAllocation().getNumberOfGroupCases(), is(3));
    }

    @Test
    void shouldEmitCurrentCountOnAllocatedHearingUpdateAfterRemoval() {
        listGroupHearing(3);
        allocate();
        removeMember(memberCase1Id, 2);

        assertThat(updateAllocation().getNumberOfGroupCases(), is(2));
    }

    @Test
    void shouldEmitNullCountOnAllocatedHearingUpdateWhenNeverSet() {
        listGroupHearing(null);
        allocate();

        assertThat(updateAllocation().getNumberOfGroupCases(), is(nullValue()));
    }

    private AllocatedHearingUpdatedForListingV2 updateAllocation() {
        final List<Object> events = hearing.applyAllocationRules(emptyList(), empty(), true, true, true)
                .collect(Collectors.toList());

        final Object updated = events.stream()
                .filter(AllocatedHearingUpdatedForListingV2.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("AllocatedHearingUpdatedForListingV2 not raised: " + events));
        return (AllocatedHearingUpdatedForListingV2) updated;
    }

    private CaseRemovedFromGroupCases removeMember(final UUID removedCaseId, final Integer numberOfGroupCases) {
        return removeCase(removedCaseId, null, numberOfGroupCases);
    }

    private CaseRemovedFromGroupCases removeCase(final UUID removedCaseId, final UUID newGroupMasterId, final Integer numberOfGroupCases) {
        final List<Object> events = hearing.removeCaseFromGroupCases(hearingId, groupId,
                domainListedCase(removedCaseId, false, false),
                newGroupMasterId == null ? null : domainListedCase(newGroupMasterId, true, true),
                numberOfGroupCases).collect(Collectors.toList());

        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(CaseRemovedFromGroupCases.class));
        return (CaseRemovedFromGroupCases) events.get(0);
    }

    private HearingAllocatedForListingV2 allocate() {
        final List<Object> events = hearing.applyAllocationRules(emptyList(), empty(), true, true, true)
                .collect(Collectors.toList());

        final Object allocated = events.stream()
                .filter(HearingAllocatedForListingV2.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new AssertionError("HearingAllocatedForListingV2 not raised: " + events));
        return (HearingAllocatedForListingV2) allocated;
    }

    private void listGroupHearing(final Integer numberOfGroupCases) {
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(Type.type().withId(randomUUID()).withDescription("Trial").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(JurisdictionType.CROWN)
                        .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
                        .withCourtRoomId(randomUUID())
                        .withCourtCentreId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEndDate(LocalDate.now().plusDays(2))
                        .withEstimatedMinutes(30)
                        .withIsGroupProceedings(true)
                        .withNumberOfGroupCases(numberOfGroupCases)
                        .withListedCases(asList(
                                eventListedCase(masterCaseId, true, true),
                                eventListedCase(memberCase1Id, true, false),
                                eventListedCase(memberCase2Id, true, false)))
                        .build())
                .build());
    }

    private uk.gov.justice.listing.events.ListedCase eventListedCase(final UUID caseId, final boolean isGroupMember, final boolean isGroupMaster) {
        return uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(caseId)
                .withIsCivil(true)
                .withGroupId(groupId)
                .withIsGroupMember(isGroupMember)
                .withIsGroupMaster(isGroupMaster)
                .withDefendants(asList(Defendant.defendant()
                        .withId(randomUUID())
                        .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                        .build()))
                .build();
    }

    private ListedCase domainListedCase(final UUID caseId, final boolean isGroupMember, final boolean isGroupMaster) {
        return ListedCase.listedCase()
                .withId(caseId)
                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                        .withAuthorityId(randomUUID())
                        .withAuthorityCode("TEST")
                        .withCaseReference("TESTREF")
                        .build())
                .withDefendants(emptyList())
                .withIsCivil(of(true))
                .withGroupId(of(groupId))
                .withIsGroupMember(of(isGroupMember))
                .withIsGroupMaster(of(isGroupMaster))
                .build();
    }
}
