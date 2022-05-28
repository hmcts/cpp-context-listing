package uk.gov.moj.cpp.listing.domain.aggregate;


import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.NextHearingRequested;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class SeedHearingAggregateTest {

    @InjectMocks
    private SeedHearingAggregate seedHearingAggregate;

    @Test
    public void shouldRaiseNextHearingRequested() {
        final UUID hearing1Id = randomUUID();
        final UUID hearing2Id = randomUUID();
        final List<HearingListingNeeds> hearingListingNeedsList = Arrays.asList(buildHearingListingNeeds(hearing1Id), buildHearingListingNeeds(hearing2Id));
        final String adjournedFromDate = "2021-01-27";
        final String hearingDay = "2021-01-26";
        final List<UUID> shadowListedOffences = Arrays.asList(randomUUID());
        final List<CourtCentreDefaults> courtCentreDefaults = Arrays.asList(CourtCentreDefaults.courtCentreDefaults()
                .withCourtCentreId(randomUUID())
                .build());

        final Stream<Object> stream = seedHearingAggregate.requestNextHearings(hearingListingNeedsList, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);
        Stream<Object> streams = seedHearingAggregate.apply(stream);


        List<Object> nextHearingRequestedList = streams.collect(Collectors.toList());

        assertThat(nextHearingRequestedList.size(), is(2));
        NextHearingRequested nextHearing1Requested = (NextHearingRequested) nextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(hearing1Id));
        NextHearingRequested nextHearing2Requested = (NextHearingRequested) nextHearingRequestedList.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(hearing2Id));
    }

    @Test
    public void shouldRaiseDeleteNextHearingRequested() {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final List<HearingListingNeeds> firstHearingListingNeedsList = Arrays.asList(buildHearingListingNeeds(hearingId1), buildHearingListingNeeds(hearingId2));
        final String adjournedFromDate = "2021-01-01";
        final String hearingDay = "2021-01-26";
        final List<UUID> shadowListedOffences = Arrays.asList(randomUUID());
        final List<CourtCentreDefaults> courtCentreDefaults = Arrays.asList(CourtCentreDefaults.courtCentreDefaults()
                .withCourtCentreId(randomUUID())
                .build());

        final Stream<Object> stream = seedHearingAggregate.requestNextHearings(firstHearingListingNeedsList, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);
        Stream<Object> streams = seedHearingAggregate.apply(stream);

        List<Object> nextHearingRequestedList = streams.collect(Collectors.toList());

        assertThat(nextHearingRequestedList.size(), is(2));
        NextHearingRequested nextHearing1Requested = (NextHearingRequested) nextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(hearingId1));
        NextHearingRequested nextHearing2Requested = (NextHearingRequested) nextHearingRequestedList.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(hearingId2));

        final Stream<Object> stream2 = seedHearingAggregate.deleteNextHearings(seedingHearingId, hearingDay);
        Stream<Object> streams2 = seedHearingAggregate.apply(stream2);

        List<Object> deleteNextHearingRequestedList = streams2.collect(Collectors.toList());

        assertThat(deleteNextHearingRequestedList.size(), is(2));

        DeleteNextHearingRequested hearingDeleted1 = (DeleteNextHearingRequested) deleteNextHearingRequestedList.get(0);
        DeleteNextHearingRequested hearingDeleted2 = (DeleteNextHearingRequested) deleteNextHearingRequestedList.get(1);
        assertThat(Arrays.asList(hearingDeleted1.getHearingId(), hearingDeleted2.getHearingId()).containsAll(Arrays.asList(hearingId1, hearingId2)), is(true));

    }

    @Test
    public void shouldRaiseExistingIdWhenUpdateExistingHearingRequested() {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final String hearingDay = "2021-01-26";

        final List<ProsecutionCase> prosecutionCases = Arrays.asList(buildProsecutionCase(prosecutionCaseId1), buildProsecutionCase(prosecutionCaseId2));
        final List<UUID> shadowListedOffences = Arrays.asList(randomUUID());

        final Stream<Object> stream = seedHearingAggregate.requestUpdateExistingHearing(seedingHearingId, hearingId, hearingDay, prosecutionCases, shadowListedOffences);
        Stream<Object> streams = seedHearingAggregate.apply(stream);

        List<Object> existingHearingIds = streams.collect(Collectors.toList());

        assertThat(existingHearingIds.size(), is(1));
        UpdateExistingHearingRequested updateExistingHearingRequested = (UpdateExistingHearingRequested) existingHearingIds.get(0);
        assertThat(updateExistingHearingRequested.getSeedingHearingId(), is(seedingHearingId));
        assertThat(updateExistingHearingRequested.getHearingId(), is(hearingId));
        assertThat(updateExistingHearingRequested.getProsecutionCases().size(), is(2));
        assertThat(updateExistingHearingRequested.getShadowListedOffences().size(), is(1));
    }

    private HearingListingNeeds buildHearingListingNeeds(final UUID hearingId) {
        return HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withJudiciary(Arrays.asList(JudicialRole.judicialRole()
                        .withJudicialId(randomUUID())
                        .withIsBenchChairman(null)
                        .withIsDeputy(null)
                        .withJudicialRoleType(JudicialRoleType.judicialRoleType().withJudicialRoleTypeId(randomUUID()).build())
                        .build()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(emptyList())
                .build();
    }

    private ProsecutionCase buildProsecutionCase(final UUID prosecutionCaseId) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .build();
    }
}

