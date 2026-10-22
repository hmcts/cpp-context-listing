package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.Type;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A group hearing listed unallocated and later allocated by a command that does not carry
 * isGroupProceedings (e.g. update-hearings-for-listing) must keep the flag on the allocation event.
 */
class HearingGroupProceedingsAllocationTest {

    private final UUID hearingId = randomUUID();
    private final UUID groupId = randomUUID();

    private Hearing hearing;

    @BeforeEach
    void setUp() {
        hearing = new Hearing();
    }

    @Test
    void shouldEmitListedGroupFlagWhenAllocationCommandHasNone() {
        listHearing(TRUE, 3);

        final HearingAllocatedForListingV2 allocated = allocated(hearing.applyAllocationRules(emptyList(), empty(), true, true, null));

        assertThat(allocated.getIsGroupProceedings(), is(TRUE));
        assertThat(allocated.getNumberOfGroupCases(), is(3));
    }

    @Test
    void shouldEmitListedGroupFlagWhenAllocatedWithoutGroupArgument() {
        listHearing(TRUE, 3);

        final HearingAllocatedForListingV2 allocated = allocated(hearing.applyAllocationRules(emptyList(), false, false));

        assertThat(allocated.getIsGroupProceedings(), is(TRUE));
    }

    @Test
    void shouldPreferGroupFlagFromAllocationCommand() {
        listHearing(TRUE, 3);

        final HearingAllocatedForListingV2 allocated = allocated(hearing.applyAllocationRules(emptyList(), empty(), true, true, FALSE));

        assertThat(allocated.getIsGroupProceedings(), is(FALSE));
    }

    @Test
    void shouldLeaveGroupFlagNullForNonGroupHearing() {
        listHearing(null, null);

        final HearingAllocatedForListingV2 allocated = allocated(hearing.applyAllocationRules(emptyList(), empty(), true, true, null));

        assertThat(allocated.getIsGroupProceedings(), is(nullValue()));
    }

    @Test
    void shouldKeepGroupFlagFromEarlierAllocationEventWhenReallocated() {
        listHearing(null, 3);
        hearing.apply(HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                .withHearingId(hearingId)
                .withIsGroupProceedings(TRUE)
                .build());
        hearing.apply(HearingUnallocatedForListing.hearingUnallocatedForListing()
                .withHearingId(hearingId)
                .build());

        final HearingAllocatedForListingV2 allocated = allocated(hearing.applyAllocationRules(emptyList(), empty(), true, true, null));

        assertThat(allocated.getIsGroupProceedings(), is(TRUE));
    }

    private HearingAllocatedForListingV2 allocated(final Stream<Object> stream) {
        final List<Object> events = stream.collect(Collectors.toList());
        return events.stream()
                .filter(HearingAllocatedForListingV2.class::isInstance)
                .map(HearingAllocatedForListingV2.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("HearingAllocatedForListingV2 not raised: " + events));
    }

    private void listHearing(final Boolean isGroupProceedings, final Integer numberOfGroupCases) {
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(Type.type().withId(randomUUID()).withDescription("First hearing").build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(JurisdictionType.MAGISTRATES)
                        .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
                        .withCourtRoomId(randomUUID())
                        .withCourtCentreId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEndDate(LocalDate.now().plusDays(2))
                        .withEstimatedMinutes(30)
                        .withIsGroupProceedings(isGroupProceedings)
                        .withNumberOfGroupCases(numberOfGroupCases)
                        .withListedCases(asList(listedCase(true), listedCase(false)))
                        .build())
                .build());
    }

    private ListedCase listedCase(final boolean isGroupMaster) {
        return ListedCase.listedCase()
                .withId(randomUUID())
                .withIsCivil(true)
                .withGroupId(groupId)
                .withIsGroupMember(true)
                .withIsGroupMaster(isGroupMaster)
                .withDefendants(asList(Defendant.defendant()
                        .withId(randomUUID())
                        .withOffences(asList(Offence.offence().withId(randomUUID()).build()))
                        .build()))
                .build();
    }
}
