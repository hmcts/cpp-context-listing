package uk.gov.moj.cpp.listing.domain.aggregate;


import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;

import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JudicialRoleType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.SeedingHearing;
import uk.gov.justice.listing.events.CreateNextHearing;
import uk.gov.justice.listing.events.CreateNextHearingRequested;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.NextHearingReplaced;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.events.NextHearingRequested;
import uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested;
import uk.gov.justice.listing.events.UnscheduledNextHearingRequested;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
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
    public void shouldRaiseDeleteNextHearingRequestedForDeletePreviousHearingsAndCreateNextHearing() {
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final UUID hearingId3 = randomUUID();
        final List<HearingListingNeeds> firstHearingListingNeedsList = Arrays.asList(buildHearingListingNeeds(hearingId1), buildHearingListingNeeds(hearingId2));
        final String adjournedFromDate = "2021-01-01";
        final String hearingDay = "2021-01-26";
        final List<UUID> shadowListedOffences = Arrays.asList(randomUUID());
        final List<CourtCentreDefaults> courtCentreDefaults = Arrays.asList(CourtCentreDefaults.courtCentreDefaults()
                .withCourtCentreId(randomUUID())
                .build());
        final CreateNextHearing createNextHearing = CreateNextHearing.createNextHearing()
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .withSittingDay(hearingDay)
                        .withJurisdictionType(CROWN)
                        .build())
                .withHearing(Hearing.hearing()
                        .withId(hearingId3)
                        .build())
                .build();

        final Stream<Object> stream = seedHearingAggregate.requestNextHearings(firstHearingListingNeedsList, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);
        Stream<Object> streams = seedHearingAggregate.apply(stream);

        List<Object> nextHearingRequestedList = streams.collect(Collectors.toList());

        assertThat(nextHearingRequestedList.size(), is(2));
        NextHearingRequested nextHearing1Requested = (NextHearingRequested) nextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(hearingId1));
        NextHearingRequested nextHearing2Requested = (NextHearingRequested) nextHearingRequestedList.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(hearingId2));

        final Stream<Object> stream2 = seedHearingAggregate.deletePreviousHearingsAndCreateNextHearing(seedingHearingId, hearingDay,createNextHearing);
        Stream<Object> streams2 = seedHearingAggregate.apply(stream2);

        List<Object> deleteNextHearingRequestedAndCreateNextHearingRequestedList = streams2.collect(Collectors.toList());

        assertThat(deleteNextHearingRequestedAndCreateNextHearingRequestedList.size(), is(3));

        DeleteNextHearingRequested hearingDeleted1 = (DeleteNextHearingRequested) deleteNextHearingRequestedAndCreateNextHearingRequestedList.get(0);
        DeleteNextHearingRequested hearingDeleted2 = (DeleteNextHearingRequested) deleteNextHearingRequestedAndCreateNextHearingRequestedList.get(1);
        assertThat(Arrays.asList(hearingDeleted1.getHearingId(), hearingDeleted2.getHearingId()).containsAll(Arrays.asList(hearingId1, hearingId2)), is(true));
        CreateNextHearingRequested createNextHearingRequested = (CreateNextHearingRequested)deleteNextHearingRequestedAndCreateNextHearingRequestedList.get(2);
        assertThat(createNextHearingRequested.getCreateNextHearing().getHearing().getId(), is(hearingId3));

    }

    @Test
    public void shouldRaiseRemoveOffencesFromExistingHearingRequestedForDeletePreviousHearingsAndCreateNextHearing() {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final String hearingDay = "2021-01-26";

        final List<ProsecutionCase> prosecutionCases = Arrays.asList(buildProsecutionCase(prosecutionCaseId1), buildProsecutionCase(prosecutionCaseId2));
        final List<UUID> shadowListedOffences = Arrays.asList(randomUUID());

        final Stream<Object> stream = seedHearingAggregate.requestUpdateExistingHearing(seedingHearingId, hearingId, hearingDay, prosecutionCases, shadowListedOffences);
        seedHearingAggregate.apply(stream);

        final CreateNextHearing createNextHearing = CreateNextHearing.createNextHearing()
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .withSittingDay(hearingDay)
                        .withJurisdictionType(CROWN)
                        .build())
                .withHearing(Hearing.hearing()
                        .withId(hearingId)
                        .build())
                .build();
        final Stream<Object> stream2 = seedHearingAggregate.deletePreviousHearingsAndCreateNextHearing(seedingHearingId, hearingDay,createNextHearing);
        Stream<Object> streams2 = seedHearingAggregate.apply(stream2);

        List<Object> removeOffenceFromExistingHearingRequestedAndCreateNextHearingRequestedList = streams2.collect(Collectors.toList());

        assertThat(removeOffenceFromExistingHearingRequestedAndCreateNextHearingRequestedList.size(), is(2));
        RemoveOffencesFromExistingHearingRequested removeOffencesFromExistingHearingRequested = (RemoveOffencesFromExistingHearingRequested) removeOffenceFromExistingHearingRequestedAndCreateNextHearingRequestedList.get(0);
        assertThat(Arrays.asList(removeOffencesFromExistingHearingRequested.getHearingId()).containsAll(Arrays.asList(hearingId)), is(true));
        CreateNextHearingRequested createNextHearingRequested = (CreateNextHearingRequested)removeOffenceFromExistingHearingRequestedAndCreateNextHearingRequestedList.get(1);
        assertThat(createNextHearingRequested.getCreateNextHearing().getHearing().getId(), is(hearingId));

    }

    @Test
    public void shouldNextHearingReplacedWhenSeedingHearingAmendedAfterDeletePreviousHearingsAndCreateNextHearing(){
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

        List<Object> nextHearingRequestedList = stream.collect(Collectors.toList());

        assertThat(nextHearingRequestedList.size(), is(2));
        NextHearingRequested nextHearing1Requested = (NextHearingRequested) nextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(hearingId1));
        NextHearingRequested nextHearing2Requested = (NextHearingRequested) nextHearingRequestedList.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(hearingId2));

        //SeedingHearing Amended so delete 2 next hearings
        final CreateNextHearing createNextHearing = CreateNextHearing.createNextHearing()
                .withSeedingHearing(SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .withSittingDay(hearingDay)
                        .withJurisdictionType(CROWN)
                        .build())
                .withHearing(Hearing.hearing()
                        .withId(hearingId1)
                        .build())
                .build();

        final Stream<Object> stream2 = seedHearingAggregate.deletePreviousHearingsAndCreateNextHearing(seedingHearingId, hearingDay, createNextHearing);

        List<Object> deleteNextHearingRequestedAndCreateNextHearingRequestedList = stream2.collect(Collectors.toList());

        assertThat(deleteNextHearingRequestedAndCreateNextHearingRequestedList.size(), is(3));

        DeleteNextHearingRequested hearingDeleted1 = (DeleteNextHearingRequested) deleteNextHearingRequestedAndCreateNextHearingRequestedList.get(0);
        DeleteNextHearingRequested hearingDeleted2 = (DeleteNextHearingRequested) deleteNextHearingRequestedAndCreateNextHearingRequestedList.get(1);
        assertThat(Arrays.asList(hearingDeleted1.getHearingId(), hearingDeleted2.getHearingId()).containsAll(Arrays.asList(hearingId1, hearingId2)), is(true));

        seedHearingAggregate.apply(hearingDeleted1);
        seedHearingAggregate.apply(hearingDeleted2);

        // and create 2 new Next hearings
        final UUID newHearingId1 = randomUUID();
        final UUID newHearingId2 = randomUUID();
        final List<HearingListingNeeds> secondHearingListingNeedsList = singletonList(buildHearingListingNeeds(newHearingId1));
        final Stream<Object> stream3 = seedHearingAggregate.requestNextHearings(secondHearingListingNeedsList, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);

        List<Object> newNextHearingRequestedList = stream3.collect(Collectors.toList());
        assertThat(newNextHearingRequestedList.size(), is(2));
        nextHearing1Requested = (NextHearingRequested) newNextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(newHearingId1));

        // replaced events to old hearings to new hearings
        final NextHearingReplaced nextHearing1Replaced = (NextHearingReplaced) newNextHearingRequestedList.get(1);
        assertThat(nextHearing1Replaced.getNewHearingId(), is(newHearingId1));
        assertThat(nextHearing1Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing1Replaced.getOldHearingIds(), hasItems(hearingId1, hearingId2));

        final List<HearingListingNeeds> secondHearingListingNeedsList2 = singletonList(buildHearingListingNeeds(newHearingId2));
        final Stream<Object> stream31 = seedHearingAggregate.requestNextHearings(secondHearingListingNeedsList2, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);

        List<Object> newNextHearingRequestedList1 = stream31.collect(Collectors.toList());
        assertThat(newNextHearingRequestedList1.size(), is(2));
        nextHearing1Requested = (NextHearingRequested) newNextHearingRequestedList1.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(newHearingId2));

        // replaced events to old hearings to new hearings
        final NextHearingReplaced nextHearing1Replaced2 = (NextHearingReplaced) newNextHearingRequestedList1.get(1);
        assertThat(nextHearing1Replaced2.getNewHearingId(), is(newHearingId2));
        assertThat(nextHearing1Replaced2.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing1Replaced2.getOldHearingIds(), hasItems(hearingId1, hearingId2));

    }




    @Test
    public void shouldRaiseExistingIdWhenUpdateExistingHearingRequested() {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final String hearingDay = "2021-01-26";

        final List<ProsecutionCase> prosecutionCases = Arrays.asList(buildProsecutionCase(prosecutionCaseId1), buildProsecutionCase(prosecutionCaseId2));
        final List<UUID> shadowListedOffences = List.of(randomUUID());

        final Stream<Object> stream = seedHearingAggregate.requestUpdateExistingHearing(seedingHearingId, hearingId, hearingDay, prosecutionCases, shadowListedOffences);
        Stream<Object> streams = seedHearingAggregate.apply(stream);

        List<Object> existingHearingIds = streams.toList();

        assertThat(existingHearingIds.size(), is(1));
        UpdateExistingHearingRequested updateExistingHearingRequested = (UpdateExistingHearingRequested) existingHearingIds.get(0);
        assertThat(updateExistingHearingRequested.getSeedingHearingId(), is(seedingHearingId));
        assertThat(updateExistingHearingRequested.getHearingId(), is(hearingId));
        assertThat(updateExistingHearingRequested.getProsecutionCases().size(), is(2));
        assertThat(updateExistingHearingRequested.getShadowListedOffences().size(), is(1));
    }

    @Test
    public void shouldNextHearingReplacedWhenSeedingHearingAmended(){
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

        List<Object> nextHearingRequestedList = stream.collect(Collectors.toList());

        assertThat(nextHearingRequestedList.size(), is(2));
        NextHearingRequested nextHearing1Requested = (NextHearingRequested) nextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(hearingId1));
        NextHearingRequested nextHearing2Requested = (NextHearingRequested) nextHearingRequestedList.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(hearingId2));

        //SeedingHearing Amended so delete 2 next hearings
        final Stream<Object> stream2 = seedHearingAggregate.deletePreviousHearingsAndCreateNextHearing(seedingHearingId, hearingDay, CreateNextHearing.createNextHearing().build());

        List<Object> deleteNextHearingRequestedList = stream2.collect(Collectors.toList());

        assertThat(deleteNextHearingRequestedList.size(), is(3));

        DeleteNextHearingRequested hearingDeleted1 = (DeleteNextHearingRequested) deleteNextHearingRequestedList.get(0);
        DeleteNextHearingRequested hearingDeleted2 = (DeleteNextHearingRequested) deleteNextHearingRequestedList.get(1);
        assertThat(Arrays.asList(hearingDeleted1.getHearingId(), hearingDeleted2.getHearingId()).containsAll(Arrays.asList(hearingId1, hearingId2)), is(true));

        // and create 2 new Next hearings
        final UUID newHearingId1 = randomUUID();
        final UUID newHearingId2 = randomUUID();
        final List<HearingListingNeeds> secondHearingListingNeedsList = singletonList(buildHearingListingNeeds(newHearingId1));
        final Stream<Object> stream3 = seedHearingAggregate.requestNextHearings(secondHearingListingNeedsList, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);

        List<Object> newNextHearingRequestedList = stream3.collect(Collectors.toList());
        assertThat(newNextHearingRequestedList.size(), is(2));
        nextHearing1Requested = (NextHearingRequested) newNextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(newHearingId1));

        // replaced events to old hearings to new hearings
        final NextHearingReplaced nextHearing1Replaced = (NextHearingReplaced) newNextHearingRequestedList.get(1);
        assertThat(nextHearing1Replaced.getNewHearingId(), is(newHearingId1));
        assertThat(nextHearing1Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing1Replaced.getOldHearingIds(), hasItems(hearingId1, hearingId2));

        final List<HearingListingNeeds> secondHearingListingNeedsList2 = singletonList(buildHearingListingNeeds(newHearingId2));
        final Stream<Object> stream31 = seedHearingAggregate.requestNextHearings(secondHearingListingNeedsList2, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);

        List<Object> newNextHearingRequestedList1 = stream31.collect(Collectors.toList());
        assertThat(newNextHearingRequestedList1.size(), is(2));
        nextHearing1Requested = (NextHearingRequested) newNextHearingRequestedList1.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(newHearingId2));

        // replaced events to old hearings to new hearings
        final NextHearingReplaced nextHearing1Replaced2 = (NextHearingReplaced) newNextHearingRequestedList1.get(1);
        assertThat(nextHearing1Replaced2.getNewHearingId(), is(newHearingId2));
        assertThat(nextHearing1Replaced2.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing1Replaced2.getOldHearingIds(), hasItems(hearingId1, hearingId2));


        //SeedingHearing Amended again so delete 2 next hearings
        final Stream<Object> stream4 = seedHearingAggregate.deleteNextHearings(seedingHearingId, hearingDay);

        List<Object> deleteNextHearingRequestedList2 = stream4.collect(Collectors.toList());

        assertThat(deleteNextHearingRequestedList2.size(), is(2));

        DeleteNextHearingRequested hearingDeleted3 = (DeleteNextHearingRequested) deleteNextHearingRequestedList2.get(0);
        DeleteNextHearingRequested hearingDeleted4 = (DeleteNextHearingRequested) deleteNextHearingRequestedList2.get(1);
        assertThat(Arrays.asList(hearingDeleted3.getHearingId(), hearingDeleted4.getHearingId()).containsAll(Arrays.asList(newHearingId1, newHearingId2)), is(true));

        // and create 2 new Next hearings
        final UUID newHearingId3 = randomUUID();
        final UUID newHearingId4 = randomUUID();
        final List<HearingListingNeeds> thirdHearingListingNeedsList = Arrays.asList(buildHearingListingNeeds(newHearingId3), buildHearingListingNeeds(newHearingId4));
        final Stream<Object> stream5 = seedHearingAggregate.requestNextHearings(thirdHearingListingNeedsList, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);

        List<Object> newNextHearingRequestedList2 = stream5.collect(Collectors.toList());
        assertThat(newNextHearingRequestedList2.size(), is(4));
        nextHearing1Requested = (NextHearingRequested) newNextHearingRequestedList2.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(newHearingId3));
        nextHearing2Requested = (NextHearingRequested) newNextHearingRequestedList2.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(newHearingId4));

        // replaced events to old hearings to new hearings
        final NextHearingReplaced nextHearing3Replaced = (NextHearingReplaced) newNextHearingRequestedList2.get(2);
        assertThat(nextHearing3Replaced.getNewHearingId(), is(newHearingId3));
        assertThat(nextHearing3Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing3Replaced.getOldHearingIds().size(), is(2));
        assertThat(nextHearing3Replaced.getOldHearingIds(), hasItems(newHearingId1, newHearingId2));
        final NextHearingReplaced nextHearing4Replaced = (NextHearingReplaced) newNextHearingRequestedList2.get(3);
        assertThat(nextHearing4Replaced.getNewHearingId(), is(newHearingId4));
        assertThat(nextHearing4Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing4Replaced.getOldHearingIds().size(), is(2));
        assertThat(nextHearing4Replaced.getOldHearingIds(), hasItems(newHearingId1, newHearingId2));
    }

    @Test
    public void shouldUnscheduledNextHearingReplacedWhenSeedingHearingAmended(){
        final UUID seedingHearingId = randomUUID();
        final UUID hearingId1 = randomUUID();
        final UUID hearingId2 = randomUUID();
        final List<HearingUnscheduledListingNeeds> firstHearingListingNeedsList = Arrays.asList(buildHearingUnscheduledListingNeeds(hearingId1), buildHearingUnscheduledListingNeeds(hearingId2));
        final String adjournedFromDate = "2021-01-01";
        final String hearingDay = "2021-01-26";
        final List<UUID> shadowListedOffences = Arrays.asList(randomUUID());
        final List<CourtCentreDefaults> courtCentreDefaults = Arrays.asList(CourtCentreDefaults.courtCentreDefaults()
                .withCourtCentreId(randomUUID())
                .build());

        final Stream<Object> stream = seedHearingAggregate.requestNextUnscheduledHearings(firstHearingListingNeedsList, hearingDay, courtCentreDefaults);

        List<Object> nextHearingRequestedList = stream.collect(Collectors.toList());

        assertThat(nextHearingRequestedList.size(), is(2));
        UnscheduledNextHearingRequested nextHearing1Requested = (UnscheduledNextHearingRequested) nextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(hearingId1));
        UnscheduledNextHearingRequested nextHearing2Requested = (UnscheduledNextHearingRequested) nextHearingRequestedList.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(hearingId2));

        //SeedingHearing Amended so delete 2 next hearings
        final Stream<Object> stream2 = seedHearingAggregate.deletePreviousHearingsAndCreateNextHearing(seedingHearingId, hearingDay, CreateNextHearing.createNextHearing().build());

        List<Object> deleteNextHearingRequestedList = stream2.collect(Collectors.toList());

        assertThat(deleteNextHearingRequestedList.size(), is(3));

        DeleteNextHearingRequested hearingDeleted1 = (DeleteNextHearingRequested) deleteNextHearingRequestedList.get(0);
        DeleteNextHearingRequested hearingDeleted2 = (DeleteNextHearingRequested) deleteNextHearingRequestedList.get(1);
        assertThat(Arrays.asList(hearingDeleted1.getHearingId(), hearingDeleted2.getHearingId()).containsAll(Arrays.asList(hearingId1, hearingId2)), is(true));

        // and create 2 new Next hearings
        final UUID newHearingId1 = randomUUID();
        final UUID newHearingId2 = randomUUID();
        final List<HearingUnscheduledListingNeeds> secondHearingListingNeedsList = Arrays.asList(buildHearingUnscheduledListingNeeds(newHearingId1), buildHearingUnscheduledListingNeeds(newHearingId2));
        final Stream<Object> stream3 = seedHearingAggregate.requestNextUnscheduledHearings(secondHearingListingNeedsList, hearingDay, courtCentreDefaults);

        List<Object> newNextHearingRequestedList = stream3.collect(Collectors.toList());
        assertThat(newNextHearingRequestedList.size(), is(4));
        nextHearing1Requested = (UnscheduledNextHearingRequested) newNextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(newHearingId1));
        nextHearing2Requested = (UnscheduledNextHearingRequested) newNextHearingRequestedList.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(newHearingId2));

        // replaced events to old hearings to new hearings
        final NextHearingReplaced nextHearing1Replaced = (NextHearingReplaced) newNextHearingRequestedList.get(2);
        assertThat(nextHearing1Replaced.getNewHearingId(), is(newHearingId1));
        assertThat(nextHearing1Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing1Replaced.getOldHearingIds(), hasItems(hearingId1, hearingId2));
        final NextHearingReplaced nextHearing2Replaced = (NextHearingReplaced) newNextHearingRequestedList.get(3);
        assertThat(nextHearing2Replaced.getNewHearingId(), is(newHearingId2));
        assertThat(nextHearing2Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing2Replaced.getOldHearingIds(), hasItems(hearingId1, hearingId2));

        //SeedingHearing Amended again so delete 2 next hearings
        final Stream<Object> stream4 = seedHearingAggregate.deleteNextHearings(seedingHearingId, hearingDay);

        List<Object> deleteNextHearingRequestedList2 = stream4.collect(Collectors.toList());

        assertThat(deleteNextHearingRequestedList2.size(), is(2));

        DeleteNextHearingRequested hearingDeleted3 = (DeleteNextHearingRequested) deleteNextHearingRequestedList2.get(0);
        DeleteNextHearingRequested hearingDeleted4 = (DeleteNextHearingRequested) deleteNextHearingRequestedList2.get(1);
        assertThat(Arrays.asList(hearingDeleted3.getHearingId(), hearingDeleted4.getHearingId()).containsAll(Arrays.asList(newHearingId1, newHearingId2)), is(true));

        // and create 2 new Next hearings
        final UUID newHearingId3 = randomUUID();
        final UUID newHearingId4 = randomUUID();
        final List<HearingUnscheduledListingNeeds> thirdHearingListingNeedsList = Arrays.asList(buildHearingUnscheduledListingNeeds(newHearingId3), buildHearingUnscheduledListingNeeds(newHearingId4));
        final Stream<Object> stream5 = seedHearingAggregate.requestNextUnscheduledHearings(thirdHearingListingNeedsList, hearingDay, courtCentreDefaults);

        List<Object> newNextHearingRequestedList2 = stream5.collect(Collectors.toList());
        assertThat(newNextHearingRequestedList2.size(), is(4));
        nextHearing1Requested = (UnscheduledNextHearingRequested) newNextHearingRequestedList2.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(newHearingId3));
        nextHearing2Requested = (UnscheduledNextHearingRequested) newNextHearingRequestedList2.get(1);
        assertThat(nextHearing2Requested.getHearing().getId(), is(newHearingId4));

        // replaced events to old hearings to new hearings
        final NextHearingReplaced nextHearing3Replaced = (NextHearingReplaced) newNextHearingRequestedList2.get(2);
        assertThat(nextHearing3Replaced.getNewHearingId(), is(newHearingId3));
        assertThat(nextHearing3Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing3Replaced.getOldHearingIds().size(), is(2));
        assertThat(nextHearing3Replaced.getOldHearingIds(), hasItems(newHearingId1, newHearingId2));
        final NextHearingReplaced nextHearing4Replaced = (NextHearingReplaced) newNextHearingRequestedList2.get(3);
        assertThat(nextHearing4Replaced.getNewHearingId(), is(newHearingId4));
        assertThat(nextHearing4Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing4Replaced.getOldHearingIds().size(), is(2));
        assertThat(nextHearing4Replaced.getOldHearingIds(), hasItems(newHearingId1, newHearingId2));
    }

    @Test
    public void shouldNextHearingReplacedWhenExistingHearingAmended(){
        final UUID seedingHearingId = randomUUID();
        final UUID existingHearingId = randomUUID();
        final HearingListingNeeds firstHearingListingNeeds =buildHearingListingNeeds(existingHearingId);
        final String adjournedFromDate = "2021-01-01";
        final String hearingDay = "2021-01-26";
        final List<UUID> shadowListedOffences = Arrays.asList(randomUUID());
        final List<CourtCentreDefaults> courtCentreDefaults = Arrays.asList(CourtCentreDefaults.courtCentreDefaults()
                .withCourtCentreId(randomUUID())
                .build());

        final Stream<Object> stream = seedHearingAggregate.requestUpdateExistingHearing(seedingHearingId, existingHearingId, hearingDay, firstHearingListingNeeds.getProsecutionCases(), shadowListedOffences);

        List<Object> nextHearingRequestedList = stream.collect(Collectors.toList());

        assertThat(nextHearingRequestedList.size(), is(1));
        UpdateExistingHearingRequested updateExistingHearingRequested = (UpdateExistingHearingRequested) nextHearingRequestedList.get(0);
        assertThat(updateExistingHearingRequested.getHearingId(), is(existingHearingId));

        //SeedingHearing Amended so delete offences
        final Stream<Object> stream2 = seedHearingAggregate.deletePreviousHearingsAndCreateNextHearing(seedingHearingId, hearingDay, CreateNextHearing.createNextHearing().build());

        List<Object> deleteNextHearingRequestedList = stream2.collect(Collectors.toList());

        assertThat(deleteNextHearingRequestedList.size(), is(2));

        RemoveOffencesFromExistingHearingRequested removeOffencesFromExistingHearingRequested = (RemoveOffencesFromExistingHearingRequested) deleteNextHearingRequestedList.get(0);
        assertThat(removeOffencesFromExistingHearingRequested.getHearingId(), is(existingHearingId));

        // and create a new Next hearings
        final UUID newHearingId1 = randomUUID();
        final List<HearingListingNeeds> secondHearingListingNeedsList = singletonList(buildHearingListingNeeds(newHearingId1));
        final Stream<Object> stream3 = seedHearingAggregate.requestNextHearings(secondHearingListingNeedsList, hearingDay, courtCentreDefaults, of(adjournedFromDate), shadowListedOffences);

        List<Object> newNextHearingRequestedList = stream3.collect(Collectors.toList());
        assertThat(newNextHearingRequestedList.size(), is(2));
        NextHearingRequested nextHearing1Requested = (NextHearingRequested) newNextHearingRequestedList.get(0);
        assertThat(nextHearing1Requested.getHearing().getId(), is(newHearingId1));

        // replaced events to old existing hearings to new hearings
        final NextHearingReplaced nextHearing1Replaced = (NextHearingReplaced) newNextHearingRequestedList.get(1);
        assertThat(nextHearing1Replaced.getNewHearingId(), is(newHearingId1));
        assertThat(nextHearing1Replaced.getSeedingHearingId(), is(seedingHearingId));
        assertThat(nextHearing1Replaced.getOldHearingIds(), hasItems(existingHearingId));
    }

    private HearingListingNeeds buildHearingListingNeeds(final UUID hearingId) {
        return HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withJudiciary(Collections.singletonList(JudicialRole.judicialRole()
                        .withJudicialId(randomUUID())
                        .withIsBenchChairman(null)
                        .withIsDeputy(null)
                        .withJudicialRoleType(JudicialRoleType.judicialRoleType().withJudicialRoleTypeId(randomUUID()).build())
                        .build()))
                .withJurisdictionType(JurisdictionType.CROWN)
                .withProsecutionCases(emptyList())
                .build();
    }

    private HearingUnscheduledListingNeeds buildHearingUnscheduledListingNeeds(final UUID hearingId){
        return HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                .withId(hearingId)
                .withProsecutionCases(emptyList())
                .build();
    }

    private ProsecutionCase buildProsecutionCase(final UUID prosecutionCaseId) {
        return ProsecutionCase.prosecutionCase()
                .withId(prosecutionCaseId)
                .build();
    }
}

