package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.MAGISTRATES;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.listing.events.AddedCasesForHearing;
import uk.gov.justice.listing.events.AllocatedHearingDeleted;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2;
import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.DefendantOffenceIdsV2;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingListedCaseUpdated;
import uk.gov.justice.listing.events.HearingMarkedAsDeleted;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceIds;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2;
import uk.gov.justice.listing.events.RequestedHearingFromStagingHmi;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.listing.events.UpdatedHearingInStagingHmi;
import uk.gov.justice.listing.events.UpdatedHmiFieldsForHearing;
import uk.gov.moj.cpp.listing.domain.CourtApplication;
import uk.gov.moj.cpp.listing.domain.CourtApplicationPartyListingNeeds;
import uk.gov.moj.cpp.listing.domain.CourtCentreDefaults;
import uk.gov.moj.cpp.listing.domain.JudicialRole;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.domain.ListedCase;
import uk.gov.moj.cpp.listing.domain.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.Type;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingAggregateTest {

    @InjectMocks
    private Hearing hearing;

    private final Integer defaultDuration = 30;
    private final LocalTime defaultStartTime = LocalTime.parse("10:00");

    private final UUID hearingId = randomUUID();
    private final Type type = Type.type().withDescription("First Hearing").withId(randomUUID()).build();
    private final int estimateMinutes = 30;
    private final String estimatedDuration = "1 week";
    private final List<ListedCase> listedCases = emptyList();
    private final UUID courtCentreId = randomUUID();
    private final List<JudicialRole> judiciary = emptyList();
    private final UUID courtRoomId = randomUUID();
    private final String listingDirections = null;
    private final JurisdictionType jurisdictionType = MAGISTRATES;
    private final String prosecutorDatesToAvoid = null;
    private final String reportingRestrictionReason = null;
    private final LocalDate endDate = null;
    private final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults().withCourtCentreId(courtCentreId).withDefaultDuration(defaultDuration).withDefaultStartTime(defaultStartTime).build();
    private final List<CourtApplication> courtApplications = emptyList();
    private final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = emptyList();
    private final Integer hearingTypeDuration = 45;
    private final Optional<String> adjournedFromDate = of(now().format(ofPattern("yyyy-MM-dd")));
    private final Optional<LocalDate> weekCommencingStartDate = empty();
    private final Optional<LocalDate> weekCommencingEndDate = empty();
    private final Optional<Integer> weekCommencingDurationInWeeks = empty();
    private final Boolean isSlotsBooked = false;

    private ZonedDateTime startDate;
    private List<NonDefaultDay> nonDefaultDays;

    private final LocalTime preferredStartTime = LocalTime.parse("12:00");
    private final Integer preferredDuration = 45;

    @Test
    public void shouldCalculateHearingDaysWithStartDateAndMultipleNonDefaultDays() {
        startDate = ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(2);
        nonDefaultDays = Stream.of(
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)).withDuration(of(preferredDuration)).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(4)).withDuration(of(defaultDuration)).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(6)).withDuration(of(preferredDuration)).build())
                .collect(Collectors.toList());

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, hearingTypeDuration,
                adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, nonDefaultDays, isSlotsBooked, "", "'", null, of(Boolean.FALSE));

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing hearing = hearingListed.getHearing();

        assertThat(hearing.getEndDate(), is(now().plusDays(6)));
        assertThat(hearing.getHearingDays().size(), is(3));

        assertThat(hearing.getHearingDays().get(0).getStartTime(), is(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)));
        assertThat(hearing.getHearingDays().get(0).getDurationMinutes(), is(preferredDuration));

        assertThat(hearing.getHearingDays().get(1).getStartTime(), is(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(4)));
        assertThat(hearing.getHearingDays().get(1).getDurationMinutes(), is(defaultDuration));

        assertThat(hearing.getHearingDays().get(2).getStartTime(), is(ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(6)));
        assertThat(hearing.getHearingDays().get(2).getDurationMinutes(), is(preferredDuration));

        assertThat(hearing.getNonSittingDays().size(), is(2));

        assertThat(hearing.getNonSittingDays(), hasItems(now().plusDays(3), now().plusDays(5)));
    }

    @Test
    public void shouldNotCalculateHearingDaysWithStartDateNull() {
        startDate = null;
        nonDefaultDays = Stream.of(
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)).withDuration(of(preferredDuration)).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(4)).withDuration(of(defaultDuration)).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(6)).withDuration(of(preferredDuration)).build())
                .collect(Collectors.toList());

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, hearingTypeDuration,
                adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, nonDefaultDays, isSlotsBooked, "", "'", null, of(Boolean.FALSE));

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing hearing = hearingListed.getHearing();

        assertThat(hearing.getEndDate(), is(nullValue()));
        assertThat(hearing.getStartDate(), is(nullValue()));
        assertThat(hearing.getHearingDays(), is(emptyList()));
        assertThat(hearing.getNonSittingDays(), is(emptyList()));
        assertThat(hearing.getNonDefaultDays(), is(emptyList()));
    }

    @Test
    public void shouldSetPossibleDisqualificationOnTheEventWhenTrue() {
        nonDefaultDays = Stream.of(
                        NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)).withDuration(of(preferredDuration)).build(),
                        NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(4)).withDuration(of(defaultDuration)).build(),
                        NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(6)).withDuration(of(preferredDuration)).build())
                .collect(Collectors.toList());

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes,estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, hearingTypeDuration,
                adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, nonDefaultDays, isSlotsBooked, "", "'", null, of(Boolean.TRUE));

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing hearing = hearingListed.getHearing();

        assertThat(hearing.getId(), is(hearingId));
        assertThat(hearing.getIsPossibleDisqualification(), is(Boolean.TRUE));
    }

    @Test
    public void shouldNotSetPossibleDisqualificationOnTheEventWhenFalse() {
        nonDefaultDays = Stream.of(
                        NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)).withDuration(of(preferredDuration)).build(),
                        NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(4)).withDuration(of(defaultDuration)).build(),
                        NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(6)).withDuration(of(preferredDuration)).build())
                .collect(Collectors.toList());

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes,estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, hearingTypeDuration,
                adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, nonDefaultDays, isSlotsBooked, "", "'", null, Optional.empty());

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing hearing = hearingListed.getHearing();

        assertThat(hearing.getId(), is(hearingId));
        assertThat(hearing.getIsPossibleDisqualification(), is(nullValue()));
    }

    @Test
    public void shouldCalculateHearingDaysWithStartDateAndSingleNonDefaultDay() {
        startDate = ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(2);
        nonDefaultDays = Stream.of(
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)).withDuration(of(preferredDuration)).build())
                .collect(Collectors.toList());

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, hearingTypeDuration,
                adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, nonDefaultDays, isSlotsBooked, "", "", null, of(Boolean.FALSE));

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing hearing = hearingListed.getHearing();

        assertThat(hearing.getEndDate(), is(now().plusDays(2)));
        assertThat(hearing.getHearingDays().size(), is(1));

        assertThat(hearing.getHearingDays().get(0).getStartTime(), is(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)));
        assertThat(hearing.getHearingDays().get(0).getDurationMinutes(), is(preferredDuration));

        assertThat(hearing.getNonSittingDays(), is(emptyList()));
    }

    @Test
    public void shouldUpdateDefendantLegalAidStatusForHearingWhenCaseAndDefendantsExistsInProsecutionCaseDefendants() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final String legalAidStatus = "legalAidStatus";

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence().withId(offence1Id).build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence().withId(offence2Id).build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(asList(ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case1Id)
                        .withDefendants(asList(DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant1Id)
                                .withOffenceIds(asList(offence1Id))
                                .build()))
                        .build()))
                .build());

        final Stream<Object> listedHearing = hearing.updateDefendantLegalAidStatusForHearing(hearingId, case1Id, defendant1Id, legalAidStatus);

        final DefendantLegalaidStatusUpdatedForHearing defendantLegalaidStatusUpdatedForHearing = (DefendantLegalaidStatusUpdatedForHearing) listedHearing.findFirst().get();

        assertThat(defendantLegalaidStatusUpdatedForHearing.getCaseId(), is(case1Id));
        assertThat(defendantLegalaidStatusUpdatedForHearing.getDefendantId(), is(defendant1Id));
        assertThat(defendantLegalaidStatusUpdatedForHearing.getHearingId(), is(hearingId));
        assertThat(defendantLegalaidStatusUpdatedForHearing.getLegalAidStatus(), is(legalAidStatus));
    }

    @Test
    public void shouldNotUpdateDefendantLegalAidStatusForHearingWhenCaseAndDefendantsNotExistsInProsecutionCaseDefendants() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final String legalAidStatus = "legalAidStatus";

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence().withId(offence1Id).build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence().withId(offence2Id).build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(asList(ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case1Id)
                        .withDefendants(asList(DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant1Id)
                                .withOffenceIds(asList(offence1Id))
                                .build()))
                        .build()))
                .build());

        final Stream<Object> listedHearing = hearing.updateDefendantLegalAidStatusForHearing(hearingId, case2Id, defendant2Id, legalAidStatus);

        assertThat(listedHearing.findFirst().isPresent(), is(false));
    }


    @Test
    public void shouldNotRaiseNotSittingDaysEventWhenCurrentAndPreviousNonSittingDaysEmptyOrNull() {

        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence().withId(offence1Id).build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence().withId(offence2Id).build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        final Stream<Object> nonSittingDays = hearing.assignNonSittingDays(null, hearingId);

        assertThat(nonSittingDays.collect(Collectors.toList()), is(emptyList()));
    }

    @Test
    public void shouldCreateUnallocatedHearingDeletedButNotFreeBookingSlotsAsNotAllocated() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        final Stream<Object> events = hearing.deleteHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(1));

        final UnallocatedHearingDeleted unallocatedHearingDeleted = (UnallocatedHearingDeleted) eventsList.get(0);

        assertThat(unallocatedHearingDeleted.getHearingId(), is(hearingId));
        assertThat(unallocatedHearingDeleted.getCaseIds().size(), is(2));
        assertThat(unallocatedHearingDeleted.getCaseIds(), hasItems(case1Id, case2Id));
    }

    @Test
    public void shouldMarkHearingAsDeleted() {
        final UUID hearingId = randomUUID();

        hearing.apply(HearingMarkedAsDeleted.hearingMarkedAsDeleted().withHearingIdToDelete(hearingId).build());

        final Stream<Object> events = hearing.markHearingAsDeleted(hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());

        assertThat(eventsList.size(), is(1));

        final HearingMarkedAsDeleted hearingMarkedAsDeleted = (HearingMarkedAsDeleted) eventsList.get(0);

        assertThat(hearingMarkedAsDeleted.getHearingIdToDelete(), is(hearingId));
    }

    @Test
    public void shouldCreateAllocatedHearingDeletedAndFreeBookingSlots() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingId(hearingId)
                .build());

        final Stream<Object> events = hearing.deleteHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(2));

        final AllocatedHearingDeleted unallocatedHearingDeleted = (AllocatedHearingDeleted) eventsList.get(0);

        assertThat(unallocatedHearingDeleted.getHearingId(), is(hearingId));
        assertThat(unallocatedHearingDeleted.getCaseIds().size(), is(2));
        assertThat(unallocatedHearingDeleted.getCaseIds(), hasItems(case1Id, case2Id));

        final AvailableSlotsForHearingFreed availableSlotsForHearingFreed = (AvailableSlotsForHearingFreed) eventsList.get(1);
        assertThat(availableSlotsForHearingFreed.getHearingId(), is(hearingId));
    }

    @Test
    public void shouldCreateAllocatedHearingDeletedWithoutFreeingSlotsOfNonMagistratesJurisdiction() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingId(hearingId)
                .build());

        final Stream<Object> events = hearing.deleteHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(1));

        final AllocatedHearingDeleted unallocatedHearingDeleted = (AllocatedHearingDeleted) eventsList.get(0);

        assertThat(unallocatedHearingDeleted.getHearingId(), is(hearingId));
        assertThat(unallocatedHearingDeleted.getCaseIds().size(), is(2));
        assertThat(unallocatedHearingDeleted.getCaseIds(), hasItems(case1Id, case2Id));
    }

    @Test
    public void shouldDeleteHearingAndNotGenerateOffencesRemovedFromHearingEventWhenNoOffencesbelongtoSeededHearing() {
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence4Id = randomUUID();
        final UUID offence6Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withEndDate(now().plusDays(1))
                        .withStartDate(now())
                        .withEstimatedMinutes(30)
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build())))
                                                .build())))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList<>(asList(
                                                        Offence.offence()
                                                                .withId(offence2Id)
                                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                        .withSeedingHearingId(seedingHearingId)
                                                                        .build())
                                                                .build())))

                                                .build())))
                                        .build())))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                .withHearingId(hearingId)
                .withCourtRoomId(randomUUID())
                .withProsecutionCaseDefendantsOffenceIds(new ArrayList<>(asList(
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case1Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(OffenceIds.offenceIds()
                                                .withId(offence4Id)
                                                .build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(defendant2Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(offence4Id).build(),
                                                OffenceIds.offenceIds().withId(offence6Id).build())))
                                        .build())))
                                .build())))
                .build());

        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());

        assertThat(deleteHearingEventsList.size(), is(2));
        assertThat(deleteHearingEventsList.get(0), not(OffencesRemovedFromHearing.class));

        assertThat(deleteHearingEventsList.get(1), not(OffencesRemovedFromHearing.class));

        final AllocatedHearingDeleted allocatedHearingDeleted = (AllocatedHearingDeleted) deleteHearingEventsList.get(0);
        assertThat(allocatedHearingDeleted.getHearingId(), is(hearingId));
        assertThat(allocatedHearingDeleted.getCaseIds().size(), is(2));
        assertThat(allocatedHearingDeleted.getCaseIds(), hasItems(case1Id, case2Id));

        final AvailableSlotsForHearingFreed availableSlotsForHearingFreed = (AvailableSlotsForHearingFreed) deleteHearingEventsList.get(1);
        assertThat(availableSlotsForHearingFreed.getHearingId(), is(hearingId));
    }

    @Test
    public void shouldUnallocateHearingAndRemoveSeededOffenceIdsWhenHearingContainsOffencesFromOtherSeedHearingAndFreeSlots() {
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        final UUID offence3Id = randomUUID();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withEndDate(now().plusDays(1))
                        .withStartDate(now())
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build())))
                                                .build())))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList<>(asList(
                                                        Offence.offence()
                                                                .withId(offence2Id)
                                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                        .withSeedingHearingId(seedingHearingId)
                                                                        .build())
                                                                .build())))

                                                .build())))
                                        .build())))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                .withHearingId(hearingId)
                .withCourtRoomId(randomUUID())
                .withProsecutionCaseDefendantsOffenceIds(new ArrayList<>(asList(
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case1Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(OffenceIds.offenceIds()
                                                .withId(offence1Id)
                                                .build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(defendant2Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(offence2Id).build(),
                                                OffenceIds.offenceIds().withId(offence3Id).build())))
                                        .build())))
                                .build())))
                .build());

        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId(), is(asList(case1Id)));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(2));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id, offence2Id));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(true));

        final AvailableSlotsForHearingFreed availableSlotsForHearingFreed = (AvailableSlotsForHearingFreed) deleteHearingEventsList.get(1);
        assertThat(availableSlotsForHearingFreed.getHearingId(), is(hearingId));

        hearing.apply(offencesRemovedFromHearing);

        final Stream<Object> allocationStream = hearing.applyAllocationRules(Optional.of(randomUUID()), true, true);

        final HearingAllocatedForListingV2 hearingAllocatedForListing = (HearingAllocatedForListingV2) allocationStream.collect(Collectors.toList()).get(0);

        assertThat(hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds().size(), is(1));
        assertThat(hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds().get(0).getId(), is(case2Id));

        final DefendantOffenceIdsV2 defendantOffenceIdsV2 = hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0);
        assertThat(defendantOffenceIdsV2.getId(), is(defendant2Id));
        assertThat(defendantOffenceIdsV2.getOffenceIds().size(), is(1));
        final OffenceIds offenceIds = defendantOffenceIdsV2.getOffenceIds().get(0);
        assertThat(offenceIds.getId(), is(offence3Id));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(0));

    }

    @Test
    public void shouldCreateOffencesFromExistingAllocatedHearingDeletedEventWhenHearingIsAllocated() {

        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build())))
                                                .build())))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build())))
                                                .build())))
                                        .build()))
                        .build())
                .build());
        ProsecutionCaseDefendantOffenceIdsV2 prosecutionCaseDefendantOffenceIdsV2 = ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                .withId(case1Id)
                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                        .withId(defendant1Id)
                        .withOffenceIds(new ArrayList(asList(OffenceIds.offenceIds()
                                .withId(offence1Id)
                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                        .withSeedingHearingId(randomUUID())
                                        .build())
                                .build())))
                        .build())))
                .build();

        List<ProsecutionCaseDefendantOffenceIdsV2> ids = new ArrayList<>();
        ids.add(prosecutionCaseDefendantOffenceIdsV2);

        hearing.apply(HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(ids).build());

        final Stream<Object> events = hearing.removeOffencesFromExistingHearing(seedingHearingId, hearingId);

        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(1));

        final OffencesRemovedFromExistingAllocatedHearing offencesRemovedFromExistingAllocatedHearing = (OffencesRemovedFromExistingAllocatedHearing) eventsList.get(0);
        assertThat(offencesRemovedFromExistingAllocatedHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromExistingAllocatedHearing.getOffenceIds(), hasItems(offence1Id));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offence2Id));
    }

    @Test
    public void shouldCreateOffencesFromExistingUnallocatedHearingDeletedEventWhenHearingIsUnallocated() {

        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        final Stream<Object> events = hearing.removeOffencesFromExistingHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(1));

        final OffencesRemovedFromExistingUnallocatedHearing offencesRemovedFromExistingUnallocatedHearing = (OffencesRemovedFromExistingUnallocatedHearing) eventsList.get(0);
        assertThat(offencesRemovedFromExistingUnallocatedHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromExistingUnallocatedHearing.getOffenceIds(), hasItems(offence1Id, offence2Id));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().isEmpty(), is(true));

    }

    @Test
    public void shouldUpdateHmiFieldsForHearing() {

        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        final Stream<Object> events = hearing.updateHmiFields(hearingId, "Video", "High", Arrays.asList("RVC", "GSN"));
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(1));

        final UpdatedHmiFieldsForHearing updatedHmiFieldsForHearing = (UpdatedHmiFieldsForHearing) eventsList.get(0);
        assertThat(updatedHmiFieldsForHearing.getHearingId(), is(hearingId));
        assertThat(updatedHmiFieldsForHearing.getBookingType(), is("Video"));
        assertThat(updatedHmiFieldsForHearing.getPriority(), is("High"));
        assertThat(updatedHmiFieldsForHearing.getSpecialRequirements().size(), is(2));
        assertThat(updatedHmiFieldsForHearing.getSpecialRequirements(), hasItems("RVC", "GSN"));

        assertThat(hearing.getCurrentHearingEventState().getBookingType(), is("Video"));
        assertThat(hearing.getCurrentHearingEventState().getPriority(), is("High"));
        assertThat(hearing.getCurrentHearingEventState().getSpecialRequirements().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getSpecialRequirements(), hasItems("RVC", "GSN"));
    }

    @Test
    public void shouldReturnNothingWhenDefendantListIsEmptyForUpdateDefendantCourtProceedingForHearing2() {
        final UUID case1Id = randomUUID();
        final ProsecutionCase prosecutionCase = prosecutionCase().
                withId(case1Id).
                withDefendants(emptyList()).
                build();

        final Stream<Object> listedHearing = hearing.updateDefendantCourtProceedingForHearing(hearingId, prosecutionCase);

        assertThat(listedHearing.count(), is(0L));
    }


    @Test
    public void shouldReturnNothingWhenHearingisDeletedForUpdateDefendantCourtProceedingForHearing2() {

        final UUID case1Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID seedingHearingId = randomUUID();


        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(seedingHearingId)
                .build();

        final ProsecutionCase prosecutionCase = prosecutionCase().
                withId(case1Id).
                withDefendants(asList(uk.gov.justice.core.courts.Defendant.defendant()
                        .withId(case1Defendant1Id)
                        .withOffences(asList(uk.gov.justice.core.courts.Offence.offence()
                                .withId(case1Defendant1Offence1Id)
                                .withSeedingHearing(uk.gov.justice.core.courts.SeedingHearing.seedingHearing()
                                        .withSeedingHearingId(seedingHearingId).build())
                                .build()))
                        .build()))
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build()))
                                        .build()))
                                .build())))
                        .build())
                .build());

        hearing.apply(HearingDeleted.hearingDeleted().withHearingIdToBeDeleted(hearingId).build());
        final Stream<Object> listedHearing = hearing.updateDefendantCourtProceedingForHearing(hearingId, prosecutionCase);

        assertThat(listedHearing.count(), is(0L));


    }

    @Test
    public void shouldBeAbleToMarkHearingAsDeletedIfNotDeleted() {

        final UUID case1Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID seedingHearingId = randomUUID();


        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(seedingHearingId)
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build()))
                                        .build()))
                                .build())))
                        .build())
                .build());

        final Stream<Object> listedHearing = hearing.markHearingAsDeleted(hearingId);

        assertThat(listedHearing.count(), is(1L));


    }

    @Test
    public void shouldNotBeAbleToMarkHearingAsDeletedIfAllreadyDeleted() {

        final UUID case1Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID seedingHearingId = randomUUID();


        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(seedingHearingId)
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build()))
                                        .build()))
                                .build())))
                        .build())
                .build());

        hearing.apply(HearingDeleted.hearingDeleted().withHearingIdToBeDeleted(hearingId).build());

        final Stream<Object> listedHearing = hearing.markHearingAsDeleted(hearingId);

        assertThat(listedHearing.count(), is(0L));


    }

    @Test
    public void shouldBeAbleToDeleteHearingIfNotDeleted() {

        final UUID case1Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID seedingHearingId = randomUUID();


        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(seedingHearingId)
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build()))
                                        .build()))
                                .build())))
                        .build())
                .build());

        final Stream<Object> listedHearing = hearing.deleteUnAllocatedHearing();

        assertThat(listedHearing.count(), is(1L));


    }

    @Test
    public void shouldNotBeAbleToDeleteHearingIfAllReadyDeleted() {

        final UUID case1Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID seedingHearingId = randomUUID();


        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(seedingHearingId)
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build()))
                                        .build()))
                                .build())))
                        .build())
                .build());

        hearing.apply(HearingDeleted.hearingDeleted().withHearingIdToBeDeleted(hearingId).build());

        final Stream<Object> listedHearing = hearing.deleteUnAllocatedHearing();

        assertThat(listedHearing.count(), is(0L));


    }

    @Test
    public void shouldReturnNothingWhenDefendantListIsNullForUpdateDefendantCourtProceedingForHearing2() {
        final UUID case1Id = randomUUID();
        final ProsecutionCase prosecutionCase = prosecutionCase().
                withId(case1Id).
                withDefendants(null).
                build();

        final Stream<Object> listedHearing = hearing.updateDefendantCourtProceedingForHearing(hearingId, prosecutionCase);

        assertThat(listedHearing.count(), is(0L));
    }

    @Test
    public void shouldPopulateSeedingHearingInAllocationWhenSeedingHearingNotExistsInPayloadsOffenceLevel() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEndDate(LocalDate.now().plusDays(2))
                        .withEstimatedMinutes(30)
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        final Stream<Object> allocationStreams = Stream.of(hearing.applyAllocationRules(asList(uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case1Id)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant1Id)
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offence1Id)
                                        .build()))
                                .build()))
                        .build(),
                uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case2Id)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant2Id)
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offence2Id)
                                        .build()))
                                .build()))
                        .build()), true, true), hearing.sendToHmi()).flatMap(i -> i);

        final List<Object> allocationEvents = allocationStreams.collect(Collectors.toList());
        assertThat(allocationEvents.size(), is(2));
        final HearingAllocatedForListingV2 allocatedForListingV2 = (HearingAllocatedForListingV2) allocationEvents.get(0);
        assertThat(allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

        assertThat(allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

        final RequestedHearingFromStagingHmi requestHearingFromStagingHmi = (RequestedHearingFromStagingHmi) allocationEvents.get(1);
        assertThat(requestHearingFromStagingHmi.getHearing().getListedCases().size(), is(2));


        final Stream<Object> updateAllocationStreams = hearing.applyAllocationRules(asList(uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case1Id)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant1Id)
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offence1Id)
                                        .build()))
                                .build()))
                        .build(),
                uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case2Id)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant2Id)
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offence2Id)
                                        .build()))
                                .build()))
                        .build()), false, false);

        final List<Object> updateAllocationEvents = updateAllocationStreams.collect(Collectors.toList());
        assertThat(updateAllocationEvents.size(), is(1));
        final AllocatedHearingUpdatedForListingV2 allocatedUpdatedForListingV2 = (AllocatedHearingUpdatedForListingV2) updateAllocationEvents.get(0);
        assertThat(allocatedUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

        assertThat(allocatedUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantsOffenceIdsWhenCasesAddedToHearing() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant2Id = randomUUID();
        final UUID case2Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID case1Defendant1Offence2Id = randomUUID();
        final UUID case1Defendant2OffenceId = randomUUID();
        final UUID case2Defendant1OffenceId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID relatedSeedingHearingId = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(seedingHearingId)
                .build();
        final SeedingHearing relatedSeedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(relatedSeedingHearingId)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .build();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEndDate(LocalDate.now().plusDays(2))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build()))
                                        .build()))
                                .build())))
                        .build())
                .build());


        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                                .withId(case1Defendant1Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(case1Defendant1Offence2Id)
                                                        .withSeedingHearing(relatedSeedingHearing)
                                                        .build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(case1Defendant2Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(case1Defendant2OffenceId)
                                                        .withSeedingHearing(relatedSeedingHearing)
                                                        .build()))
                                                .build()))
                                .build(),
                        uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case2Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case2Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case2Defendant1OffenceId)
                                                .withSeedingHearing(relatedSeedingHearing)
                                                .build()))
                                        .build()))
                                .build()))
                .build());


        final Stream<Object> streams = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true), hearing.sendToHmi()).flatMap(i -> i);
        final List<Object> events = streams.collect(Collectors.toList());
        assertThat(events.size(), is(2));

        final HearingAllocatedForListingV2 allocatedForListingV2 = (HearingAllocatedForListingV2) events.get(0);
        assertThat(allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().size(), is(2));
        assertThat(allocatedForListingV2.getEstimatedDuration(), is("30 minutes"));

        final ProsecutionCaseDefendantOffenceIdsV2 caseDefendantOffenceIds1 = allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0);
        assertThat(caseDefendantOffenceIds1.getId(), is(case1Id));
        assertThat(caseDefendantOffenceIds1.getDefendants().size(), is((2)));

        final DefendantOffenceIdsV2 defendant1OffenceIds = caseDefendantOffenceIds1.getDefendants().get(0);
        assertThat(defendant1OffenceIds.getId(), is((case1Defendant1Id)));

        final List<OffenceIds> offenceIds = defendant1OffenceIds.getOffenceIds();
        assertThat(offenceIds.size(), is((2)));
        assertThat(offenceIds.get(0).getId(), is((case1Defendant1Offence1Id)));
        assertThat(offenceIds.get(1).getId(), is((case1Defendant1Offence2Id)));

        assertThat(caseDefendantOffenceIds1.getDefendants().get(1).getId(), is((case1Defendant2Id)));
        assertThat(caseDefendantOffenceIds1.getDefendants().get(1).getOffenceIds().size(), is((1)));
        assertThat(caseDefendantOffenceIds1.getDefendants().get(1).getOffenceIds().get(0).getId(), is((case1Defendant2OffenceId)));

        final ProsecutionCaseDefendantOffenceIdsV2 caseDefendantOffenceIds2 = allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(1);
        assertThat(caseDefendantOffenceIds2.getId(), is(case2Id));
        assertThat(caseDefendantOffenceIds2.getDefendants().size(), is((1)));

        final DefendantOffenceIdsV2 defendantCase2OffenceIds = caseDefendantOffenceIds2.getDefendants().get(0);
        assertThat(defendantCase2OffenceIds.getId(), is((case2Defendant1Id)));
        assertThat(defendantCase2OffenceIds.getOffenceIds().size(), is((1)));
        assertThat(defendantCase2OffenceIds.getOffenceIds().get(0).getId(), is((case2Defendant1OffenceId)));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(1).getDefendants().size(), is(1));

        final RequestedHearingFromStagingHmi requestHearingFromStagingHmi = (RequestedHearingFromStagingHmi) events.get(1);

        assertThat(requestHearingFromStagingHmi.getHearing().getListedCases().size(), is(2));
        assertThat(requestHearingFromStagingHmi.getHearing().getListedCases().get(0).getDefendants().size(), is(2));
        assertThat(requestHearingFromStagingHmi.getHearing().getListedCases().get(1).getDefendants().size(), is(1));
    }

    @Test
    public void shouldNotIncludeDefendantWhoseOffencesReRemovedFromHearing() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant2Id = randomUUID();
        final UUID case2Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID case1Defendant1Offence2Id = randomUUID();
        final UUID case1Defendant2OffenceId = randomUUID();
        final UUID case2Defendant1OffenceId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID relatedSeedingHearingId = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(seedingHearingId)
                .build();
        final SeedingHearing relatedSeedingHearing = SeedingHearing.seedingHearing()
                .withSeedingHearingId(relatedSeedingHearingId)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEndDate(LocalDate.now().plusDays(2))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withAllocated(true)
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build()))
                                        .build()))
                                .build())))
                        .build())
                .build());


        hearing.apply(HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                .withHearingId(hearingId)
                .withCourtRoomId(randomUUID())
                .withProsecutionCaseDefendantsOffenceIds(new ArrayList<>(asList(
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case1Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case1Defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(OffenceIds.offenceIds()
                                                .withId(case1Defendant1Offence1Id)
                                                .build(), OffenceIds.offenceIds().withId(case1Defendant1Offence2Id).build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case2Defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(case2Defendant1OffenceId).build())
                                        ))
                                        .build())))
                                .build())))
                .build());


        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(asList(Defendant.defendant()
                                                .withId(case1Defendant1Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(case1Defendant1Offence2Id)
                                                        .withSeedingHearing(relatedSeedingHearing)
                                                        .build()))
                                                .build(),
                                        Defendant.defendant()
                                                .withId(case1Defendant2Id)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(case1Defendant2OffenceId)
                                                        .withSeedingHearing(relatedSeedingHearing)
                                                        .build()))
                                                .build()))
                                .build(),
                        uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case2Id)
                                .withDefendants(asList(Defendant.defendant()
                                        .withId(case2Defendant1Id)
                                        .withOffences(asList(Offence.offence()
                                                .withId(case2Defendant1OffenceId)
                                                .withSeedingHearing(relatedSeedingHearing)
                                                .build()))
                                        .build()))
                                .build()))
                .build());

        hearing.apply(OffencesRemovedFromExistingAllocatedHearing.offencesRemovedFromExistingAllocatedHearing()
                .withHearingId(hearingId)
                .withOffenceIds(asList(case2Defendant1OffenceId))
                .build());


        final Stream<Object> streams = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true), hearing.sendToHmi()).flatMap(i -> i);
        final List<Object> events = streams.collect(Collectors.toList());
        assertThat(events.size(), is(2));

        final AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListingV2 = (AllocatedHearingUpdatedForListingV2) events.get(0);
        assertThat(allocatedHearingUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().size(), is(1));
        assertThat(allocatedHearingUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getId(), is(case1Id));
        assertThat(allocatedHearingUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0).getId(), is(case1Defendant1Id));
        assertThat(allocatedHearingUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0).getOffenceIds().get(0).getId(), is(case1Defendant1Offence1Id));
        assertThat(allocatedHearingUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0).getOffenceIds().get(1).getId(), is(case1Defendant1Offence2Id));
        assertThat(allocatedHearingUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(1).getId(), is(case1Defendant2Id));
        assertThat(allocatedHearingUpdatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(1).getOffenceIds().get(0).getId(), is(case1Defendant2OffenceId));


    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantsOffenceIdsWhenOffenceAddedOrDeleted() {
        final UUID case1Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID case1Defendant1Offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(seedingHearingId)
                .build();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEndDate(LocalDate.now().plusDays(2))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withDefendants(new ArrayList(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build()))
                        .build())
                .build());

        hearing.apply(OffenceAdded.offenceAdded()
                .withHearingId(hearingId)
                .withCaseId(case1Id)
                .withDefendantId(case1Defendant1Id)
                .withOffence(Offence.offence()
                        .withId(case1Defendant1Offence2Id)
                        .withSeedingHearing(seedingHearing)
                        .build())
                .build());

        hearing.apply(OffenceDeleted.offenceDeleted()
                .withHearingId(hearingId)
                .withCaseId(case1Id)
                .withDefendantId(case1Defendant1Id)
                .withOffenceId(case1Defendant1Offence1Id)
                .build());

        final Stream<Object> streams = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true), hearing.sendToHmi()).flatMap(i -> i);
        final List<Object> events = streams.collect(Collectors.toList());
        assertThat(events.size(), is(2));

        final HearingAllocatedForListingV2 allocatedForListingV2 = (HearingAllocatedForListingV2) events.get(0);
        final ProsecutionCaseDefendantOffenceIdsV2 prosecutionCaseDefendantOffenceIdsV2 = allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0);
        assertThat(prosecutionCaseDefendantOffenceIdsV2.getId(), is(case1Id));

        final DefendantOffenceIdsV2 defendantOffenceIdsV2 = prosecutionCaseDefendantOffenceIdsV2.getDefendants().get(0);
        assertThat(defendantOffenceIdsV2.getId(), is(case1Defendant1Id));

        assertThat(defendantOffenceIdsV2.getOffenceIds().size(), is(1));
        assertThat(defendantOffenceIdsV2.getOffenceIds().get(0).getId(), is(case1Defendant1Offence2Id));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(case1Defendant1Offence2Id));

        final RequestedHearingFromStagingHmi requestHearingFromStagingHmi = (RequestedHearingFromStagingHmi) events.get(1);
        assertThat(requestHearingFromStagingHmi.getHearing().getListedCases().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(requestHearingFromStagingHmi.getHearing().getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(case1Defendant1Offence2Id));
    }

    @Test
    public void shouldUpdateProsecutionCaseDefendantsWhenCasesAddedForHearing() {

        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID case3Id = randomUUID();
        final UUID defendant3Id = randomUUID();
        final UUID offence3Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(asList(Offence.offence().withId(offence1Id).build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(asList(Offence.offence().withId(offence2Id).build()))
                                                .build()))
                                        .build())))
                        .build())
                .build());
        final Stream<Object> addedCasesForHearing = hearing.addCasesForHearing(asList(ProsecutionCase.prosecutionCase()
                .withId(case3Id)
                .withDefendants(asList(uk.gov.justice.core.courts.Defendant.defendant()
                        .withId(defendant3Id)
                        .withOffences(asList(uk.gov.justice.core.courts.Offence.offence().withId(offence3Id).build()))
                        .build()))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityReference(STRING.next())
                        .build())
                .build()
        ), asList(offence3Id));

        final AddedCasesForHearing addedCasesForHearing1 = (AddedCasesForHearing) addedCasesForHearing.findFirst().get();

        assertThat(addedCasesForHearing1.getUnAllocatedListedCases().get(0).getId(), is(case3Id));
        assertThat(addedCasesForHearing1.getUnAllocatedListedCases().get(0).getDefendants().get(0).getId(), is(defendant3Id));
        assertThat(addedCasesForHearing1.getUnAllocatedListedCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offence3Id));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(3));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(2).getId(), is(case3Id));
    }

    @Test
    public void shouldRaiseOffenceRemovedEventWhenOffenceIsInAllocatedHearing() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList(asList(Offence.offence().withId(offence1Id).build())))
                                                .build())))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(new ArrayList(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence().withId(offence2Id).build())))
                                                .build())))
                                        .build()))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(asList(ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case1Id)
                        .withDefendants(asList(DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant1Id)
                                .withOffenceIds(asList(offence1Id))
                                .build()))
                        .build()))
                .build());


        final List<OffencesRemovedFromExistingAllocatedHearing> events = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id))
                .map(OffencesRemovedFromExistingAllocatedHearing.class::cast)
                .collect(Collectors.toList());

        assertThat(events.get(0).getHearingId(), is(hearingId));
        assertThat(events.get(0).getOffenceIds().get(0), is(offence1Id));
        assertThat(events.get(0).getOffenceIds().get(1), is(offence2Id));
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();
        final Hearing hearingAggregate = new Hearing();
        final UUID courtCentreId = randomUUID();
        final UUID courtRoomId = randomUUID();
        final ZonedDateTime startDateTime = ZonedDateTime.now();
        hearingAggregate.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type()
                                .withId(randomUUID())
                                .withDescription("type")
                                .build())
                        .withHearingDays(Arrays.asList(uk.gov.justice.listing.events.HearingDay.hearingDay()
                                        .withStartTime(startDateTime.plusDays(5))
                                        .withDurationMinutes(30)
                                        .withCourtRoomId(courtRoomId)
                                        .withCourtCentreId(courtCentreId)
                                        .withSequence(1)
                                        .build(),
                                uk.gov.justice.listing.events.HearingDay.hearingDay()
                                        .withStartTime(startDateTime.plusDays(5))
                                        .withDurationMinutes(30)
                                        .withCourtRoomId(courtRoomId)
                                        .withCourtCentreId(courtCentreId)
                                        .withSequence(1)
                                        .build()))
                        .withJurisdictionType(CROWN)
                        .withHearingLanguage(uk.gov.justice.core.courts.HearingLanguage.ENGLISH)
                        .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(caseId)
                                .withDefendants(Arrays.asList(uk.gov.justice.listing.events.Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(uk.gov.justice.listing.events.Offence.offence()
                                                .withId(offenceId)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .withJurisdictionType(CROWN)
                                                        .build())
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build());

        final Stream<Object> eventStream = hearingAggregate.assignHearingDays(LocalDate.now(), LocalDate.now().plusDays(1), emptyList(), emptyList(), LocalTime.now(), 10, hearingId, new CourtCentre.Builder().build());
        final Object event = eventStream.findFirst().get();
        assertThat(event, notNullValue());
        HearingDaysChangedForHearing hearingDaysChangedForHearing = (HearingDaysChangedForHearing) event;
        assertThat(hearingDaysChangedForHearing.getHearingId(), is(hearingId));
        final List<Object> events2 = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id))
                .collect(Collectors.toList());

        assertThat(events2.isEmpty(), is(true));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(0));

    }


    @Test
    public void shouldRaiseOffenceRemovedEventWhenOffenceIsInUnAllocatedHearing() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList(asList(Offence.offence().withId(offence1Id).build())))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList(asList(Offence.offence().withId(offence2Id).build())))
                                                .build()))
                                        .build()))
                        .build())
                .build());


        final List<OffencesRemovedFromExistingUnallocatedHearing> events = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id))
                .map(OffencesRemovedFromExistingUnallocatedHearing.class::cast)
                .collect(Collectors.toList());

        assertThat(events.get(0).getHearingId(), is(hearingId));
        assertThat(events.get(0).getOffenceIds().get(0), is(offence1Id));
        assertThat(events.get(0).getOffenceIds().get(1), is(offence2Id));

        final List<Object> events2 = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id))
                .collect(Collectors.toList());

        assertThat(events2.isEmpty(), is(true));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().isEmpty(), is(true));
    }

    @Test
    public void shouldAddNewCaseWhenDefendantCourtProceedingsUpdatedV2() {
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withEndDate(now().plusDays(1))
                        .withStartDate(now())
                        .withEstimatedMinutes(30)
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build())))
                                                .build())))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList<>(asList(
                                                        Offence.offence()
                                                                .withId(offence2Id)
                                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                        .withSeedingHearingId(seedingHearingId)
                                                                        .build())
                                                                .build())))

                                                .build())))
                                        .build())))
                        .build())
                .build());
        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(2));
        uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence().build();
        List<uk.gov.justice.core.courts.Offence> offences = new ArrayList<>();
        offences.add(offence);
        List<uk.gov.justice.core.courts.Defendant> defendants = new ArrayList<>();
        uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant().withOffences(offences).build();
        defendants.add(defendant);
        ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier().build();
        ProsecutionCase prosecutionCase = prosecutionCase().withProsecutionCaseIdentifier(prosecutionCaseIdentifier).withDefendants(defendants).build();
        hearing.apply(DefendantCourtProceedingsUpdatedV2.defendantCourtProceedingsUpdatedV2().withHearingId(randomUUID()).withProsecutionCase(prosecutionCase).build());
        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(3));
    }

    @Test
    public void shouldAddNewDefendantWhenDefendantCourtProceedingsUpdatedV2WithSameCaseWithDifferentDefendant() {
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID defendant3Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();


        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withEndDate(now().plusDays(1))
                        .withStartDate(now())
                        .withEstimatedMinutes(30)
                        .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build())))
                                                .build())))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList<>(asList(
                                                        Offence.offence()
                                                                .withId(offence2Id)
                                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                        .withSeedingHearingId(seedingHearingId)
                                                                        .build())
                                                                .build())))

                                                .build())))
                                        .build())))
                        .build())
                .build());
        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(2));
        uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence().withId(offence3Id).build();
        List<uk.gov.justice.core.courts.Offence> offences = new ArrayList<>();
        offences.add(offence);
        List<uk.gov.justice.core.courts.Defendant> defendants = new ArrayList<>();
        uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant().withId(defendant3Id).withOffences(offences).build();
        defendants.add(defendant);
        ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier().build();
        ProsecutionCase prosecutionCase = prosecutionCase().withId(case1Id).withProsecutionCaseIdentifier(prosecutionCaseIdentifier).withDefendants(defendants).build();
        hearing.apply(DefendantCourtProceedingsUpdatedV2.defendantCourtProceedingsUpdatedV2().withHearingId(randomUUID()).withProsecutionCase(prosecutionCase).build());
        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(2));
        uk.gov.justice.listing.events.ListedCase listedCase1 =hearing.getCurrentHearingEventState().getListedCases().stream().filter(lc->lc.getId().equals(case1Id)).findFirst().get();
        assertThat(listedCase1.getDefendants().size(), is(2));
        uk.gov.justice.listing.events.Defendant defendant3  = listedCase1.getDefendants().stream().filter(def -> def.getId().equals(defendant3Id)).findFirst().get();
        assertThat(defendant3.getOffences().size(), is(1));
        assertThat(defendant3.getOffences().get(0).getId(), is(offence3Id));
    }

    @Test
    public void shouldNotReturnHmiEventWhenThereIsNotUpdateEvent() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList(asList(Offence.offence().withId(offence1Id).build())))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList(asList(Offence.offence().withId(offence2Id).build())))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        final List<Object> events = hearing.raiseUpdateHearingInStagingHmi(Stream.empty()).collect(Collectors.toList());

        assertThat(events.isEmpty(), is(true));

    }

    @Test
    public void shouldReturnHmiEventWhenThereIsUpdateEvent() {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offence1Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build())))
                                                .build())))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(new ArrayList<>(asList(Offence.offence()
                                                        .withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build())))
                                                .build())))
                                        .build()))
                        .build())
                .build());
        ProsecutionCaseDefendantOffenceIdsV2 prosecutionCaseDefendantOffenceIdsV2 = ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                .withId(case1Id)
                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                        .withId(defendant1Id)
                        .withOffenceIds(new ArrayList(asList(OffenceIds.offenceIds()
                                .withId(offence1Id)
                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                        .withSeedingHearingId(randomUUID())
                                        .build())
                                .build())))
                        .build())))
                .build();

        List<ProsecutionCaseDefendantOffenceIdsV2> ids = new ArrayList<>();
        ids.add(prosecutionCaseDefendantOffenceIdsV2);

        final Stream<Object> updateEvents = Stream.of(hearing.apply(HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(ids).build()));

        final List<Object> events = hearing.raiseUpdateHearingInStagingHmi(updateEvents).collect(Collectors.toList());

        assertThat(events.isEmpty(), is(false));
        assertThat(events.size(), is(2));
        final UpdatedHearingInStagingHmi updatedHearingInStagingHmi = (UpdatedHearingInStagingHmi) events.get(1);

        assertThat(updatedHearingInStagingHmi.getHearing().getId(), is(hearingId));


    }


    @Test
    public void shouldCreateEventForSplit() {
        final List<uk.gov.justice.listing.events.ListedCase> listedCases = singletonList(uk.gov.justice.listing.events.ListedCase
                .listedCase()
                .withId(randomUUID())
                .withDefendants(singletonList(Defendant.defendant()
                        .withId(randomUUID())
                        .withOffences(singletonList(Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build())).build());

        startDate = ZonedDateTime.now();
        final Stream<Object> listedHearing = hearing.listForSplit(type, listedCases, courtCentreId, "court name", courtRoomId, jurisdictionType, startDate,
                null, null, emptyList());

        final HearingRequestedForListing hearingRequestedForListing = listedHearing.findFirst().map(HearingRequestedForListing.class::cast).get();

        assertThat(hearingRequestedForListing.getListNewHearing().getId(), is(nullValue()));
        assertThat(hearingRequestedForListing.getListNewHearing().getCourtCentre().getId(), is(courtCentreId));
        assertThat(hearingRequestedForListing.getListNewHearing().getCourtCentre().getRoomId(), is(courtRoomId));
        assertThat(hearingRequestedForListing.getListNewHearing().getHearingType().getId(), is(type.getId()));
        assertThat(hearingRequestedForListing.getListNewHearing().getJurisdictionType().toString(), is(jurisdictionType.toString()));
        assertThat(hearingRequestedForListing.getListNewHearing().getEarliestStartDateTime(), is(startDate));
        assertThat(hearingRequestedForListing.getListNewHearing().getListDefendantRequests().get(0).getDefendantId(), is(listedCases.get(0).getDefendants().get(0).getId()));
        assertThat(hearingRequestedForListing.getListNewHearing().getListDefendantRequests().get(0).getProsecutionCaseId(), is(listedCases.get(0).getId()));
        assertThat(hearingRequestedForListing.getListNewHearing().getListDefendantRequests().get(0).getDefendantOffences().get(0), is(listedCases.get(0).getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(hearingRequestedForListing.getListNewHearing().getWeekCommencingDate(), is(nullValue()));
    }

    @Test
    public void shouldCreateEventForWeekCommencingSplit() {
        final List<uk.gov.justice.listing.events.ListedCase> listedCases = singletonList(uk.gov.justice.listing.events.ListedCase
                .listedCase()
                .withId(randomUUID())
                .withDefendants(singletonList(Defendant.defendant()
                        .withId(randomUUID())
                        .withOffences(singletonList(Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build())).build());

        final LocalDate weekCommencingStartDate = LocalDate.now();
        final Stream<Object> listedHearing = hearing.listForSplit(type, listedCases, courtCentreId, "court name", courtRoomId, jurisdictionType, startDate,
                weekCommencingStartDate, 1, emptyList());

        final HearingRequestedForListing hearingRequestedForListing = listedHearing.findFirst().map(HearingRequestedForListing.class::cast).get();

        assertThat(hearingRequestedForListing.getListNewHearing().getId(), is(nullValue()));
        assertThat(hearingRequestedForListing.getListNewHearing().getCourtCentre().getId(), is(courtCentreId));
        assertThat(hearingRequestedForListing.getListNewHearing().getCourtCentre().getRoomId(), is(courtRoomId));
        assertThat(hearingRequestedForListing.getListNewHearing().getHearingType().getId(), is(type.getId()));
        assertThat(hearingRequestedForListing.getListNewHearing().getJurisdictionType().toString(), is(jurisdictionType.toString()));
        assertThat(hearingRequestedForListing.getListNewHearing().getEarliestStartDateTime(), is(nullValue()));
        assertThat(hearingRequestedForListing.getListNewHearing().getListDefendantRequests().get(0).getDefendantId(), is(listedCases.get(0).getDefendants().get(0).getId()));
        assertThat(hearingRequestedForListing.getListNewHearing().getListDefendantRequests().get(0).getProsecutionCaseId(), is(listedCases.get(0).getId()));
        assertThat(hearingRequestedForListing.getListNewHearing().getListDefendantRequests().get(0).getDefendantOffences().get(0), is(listedCases.get(0).getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(hearingRequestedForListing.getListNewHearing().getWeekCommencingDate().getStartDate(), is(weekCommencingStartDate.toString()));
        assertThat(hearingRequestedForListing.getListNewHearing().getWeekCommencingDate().getDuration(), is(1));
    }

    @Test
    public void shouldUpdateCaseIdentifier() {
        final UUID prosecutionAuthorityId = randomUUID();
        final String prosecutionAuthorityCode = "code";

        final UUID case1Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID seedingHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offence1Id)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build())))
                                        .build())))
                                .build()))
                        .build())
                .build());

        Stream<Object> events = hearing.updateCaseIdentifier(prosecutionAuthorityId, prosecutionAuthorityCode, case1Id);
        events = hearing.raiseUpdateHearingInStagingHmi(events);
        final UpdatedHearingInStagingHmi updatedHearingInStagingHmi = (UpdatedHearingInStagingHmi) events.collect(Collectors.toList()).get(1);
        assertThat(updatedHearingInStagingHmi.getHearing().getListedCases().get(0).getCaseIdentifier().getCaseReference(), is("caseReference"));
        assertThat(updatedHearingInStagingHmi.getHearing().getListedCases().get(0).getCaseIdentifier().getAuthorityId(), is(prosecutionAuthorityId));
        assertThat(updatedHearingInStagingHmi.getHearing().getListedCases().get(0).getCaseIdentifier().getAuthorityCode(), is(prosecutionAuthorityCode));

    }


    @Test
    public void shouldTestCurrentHearingEventStateWithAddedMultipleDefendantsInSameCaseWhenCaseSplitAtDefendantLevelAndMergedBack() {

        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(prosecutionCaseId)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build())))
                                        .build())))
                                .build()))
                        .build())
                .build());

        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(prosecutionCaseId)
                                .withDefendants(asList(Defendant.defendant()
                                                .withId(defendantId2)
                                                .withOffences(asList(Offence.offence()
                                                        .withId(offenceId2)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                                        .build()))
                                                .build()))
                                .build()))
                .build());

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst().get().getId(), is(defendantId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId2)).findFirst().get().getId(), is(defendantId2));

    }

    @Test
    public void shouldTestCurrentHearingEventStateWithAddedMultipleOffencesInSameDefendantWhenCaseSplitAtOffenceLevelLevelAndMergedBack() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(prosecutionCaseId)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build())))
                                        .build())))
                                .build()))
                        .build())
                .build());

        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(asList(Offence.offence()
                                        .withId(offenceId2)
                                        .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                        .build()))
                                .build()))
                        .build()))
                .build());

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst().get().getId(), is(defendantId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().stream().filter(o-> o.getId().equals(offenceId)).findFirst().get().getId(), is(offenceId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().stream().filter(o-> o.getId().equals(offenceId2)).findFirst().get().getId(), is(offenceId2));
    }

    @Test
    public void shouldTestCurrentHearingEventStateWithAddedMultipleOffencesInSameDefendantWhenCaseSplitAtOffenceLevelLevelAndMergedBackAndSameOffenceisBeingAdded() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(prosecutionCaseId)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference")
                                        .build())
                                .withDefendants((asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences((asList(Offence.offence()
                                                .withId(offenceId)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build(),Offence.offence()
                                                .withId(offenceId2)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build())))
                                        .build())))
                                .build()))
                        .build())
                .build());

        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(asList(Offence.offence()
                                        .withId(offenceId2)
                                        .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                        .build()))
                                .build()))
                        .build()))
                .build());

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst().get().getId(), is(defendantId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().stream().filter(o-> o.getId().equals(offenceId)).findFirst().get().getId(), is(offenceId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().stream().filter(o-> o.getId().equals(offenceId2)).findFirst().get().getId(), is(offenceId2));
    }

    @Test
    public void shouldTestCurrentHearingEventStateWithAddedMultipleCasesWhenNewCaseIsBeingAdded() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(prosecutionCaseId)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference")
                                        .build())
                                .withDefendants((asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences((asList(Offence.offence()
                                                .withId(offenceId)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build(),Offence.offence()
                                                .withId(offenceId2)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build())))
                                        .build())))
                                        .build()))
                                .build())
                        .build());

        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(prosecutionCaseId1)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId2)
                                .withOffences(asList(Offence.offence()
                                        .withId(offenceId3)
                                        .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                        .build()))
                                .build()))
                        .build()))
                .build());

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(2));
        final uk.gov.justice.listing.events.ListedCase listedCase1 = hearing.getCurrentHearingEventState().getListedCases().stream().filter(pc -> pc.getId().equals(prosecutionCaseId)).findFirst().get();
        assertThat(listedCase1.getDefendants().size(), is(1));
        assertThat(listedCase1.getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst().get().getId(), is(defendantId));
        assertThat(listedCase1.getDefendants().get(0).getOffences().size(), is(2));
        assertThat(listedCase1.getDefendants().get(0).getOffences().stream().filter(o-> o.getId().equals(offenceId)).findFirst().get().getId(), is(offenceId));
        assertThat(listedCase1.getDefendants().get(0).getOffences().stream().filter(o-> o.getId().equals(offenceId2)).findFirst().get().getId(), is(offenceId2));
        final uk.gov.justice.listing.events.ListedCase listedCase2 = hearing.getCurrentHearingEventState().getListedCases().stream().filter(pc -> pc.getId().equals(prosecutionCaseId1)).findFirst().get();
        assertThat(listedCase2.getDefendants().size(), is(1));
        assertThat(listedCase2.getDefendants().stream().filter(d -> d.getId().equals(defendantId2)).findFirst().get().getId(), is(defendantId2));
        assertThat(listedCase2.getDefendants().get(0).getOffences().size(), is(1));
        assertThat(listedCase2.getDefendants().get(0).getOffences().stream().filter(o-> o.getId().equals(offenceId3)).findFirst().get().getId(), is(offenceId3));

    }

    @Test
    public void shouldKeepAllOffencesIntoProsecutionCaseDefendantOffenceIdsWhenHearingExtendedWithNewCase() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID seedingHearingId = randomUUID();

        uk.gov.justice.listing.events.Hearing firstHearing = uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withType(uk.gov.justice.listing.events.Type.type().build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                .withHearingDays(emptyList())
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withCourtRoomId(randomUUID())
                .withAllocated(Boolean.TRUE)
                .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(prosecutionCaseId)
                        .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                .withCaseReference("caseReference")
                                .build())
                        .withDefendants((asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences((asList(Offence.offence()
                                        .withId(offenceId)
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withJurisdictionType(CROWN)
                                                .withSeedingHearingId(seedingHearingId)
                                                .build())
                                        .build(),Offence.offence()
                                        .withId(offenceId2)
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withJurisdictionType(CROWN)
                                                .withSeedingHearingId(seedingHearingId)
                                                .build())
                                        .build())))
                                .build())))
                        .build()))
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(firstHearing)
                .build());

        uk.gov.justice.listing.events.ListedCase newCase = uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(prosecutionCaseId1)
                .withMarkers(singletonList(Marker.marker().build()))
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId2)
                        .withOffences(asList(Offence.offence()
                                .withId(offenceId3)
                                .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                .build()))
                        .build()))
                .build();

        uk.gov.justice.listing.events.Hearing extendedHearing = uk.gov.justice.listing.events.Hearing.hearing()
                .withValuesFrom(firstHearing)
                .withListedCases(asList(firstHearing.getListedCases().get(0), newCase))
                .build();

        hearing.apply(HearingListedCaseUpdated.hearingListedCaseUpdated()
                .withHearing(extendedHearing)
                .withUnAllocatedListedCases(asList(newCase))
                .build());

        AllocatedHearingExtendedForListingV2 allocatedHearingExtendedForListingV2s = hearing.applyAllocationRulesForExtendedHearing(extendedHearing, false, false).findFirst().map(AllocatedHearingExtendedForListingV2.class::cast).orElse(null);

        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().size(), is(2));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(0).getId(), is(prosecutionCaseId));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().size(), is(1));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0).getId(), is(defendantId));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0).getOffenceIds().size(), is(2));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0).getOffenceIds().get(0), is(offenceId));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0).getOffenceIds().get(1), is(offenceId2));

        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(1).getId(), is(prosecutionCaseId1));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().size(), is(1));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().get(0).getId(), is(defendantId2));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().get(0).getOffenceIds().size(), is(1));
        assertThat(allocatedHearingExtendedForListingV2s.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().get(0).getOffenceIds().get(0), is(offenceId3));



    }

    @Test
    public void shouldRaiseHearingAllocationEventWhenHearingIsAllocatedAfterAddingCaseIntoAnUnallocatedHearingFromAnotherUnAllocatedHearing() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID seedingHearingId = randomUUID();

        uk.gov.justice.listing.events.Hearing firstHearing = uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withType(uk.gov.justice.listing.events.Type.type().build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                .withHearingDays(emptyList())
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withCourtRoomId(randomUUID())
                .withAllocated(Boolean.FALSE)
                .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(prosecutionCaseId)
                        .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                .withCaseReference("caseReference")
                                .build())
                        .withDefendants((asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences((asList(Offence.offence()
                                        .withId(offenceId)
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withJurisdictionType(CROWN)
                                                .withSeedingHearingId(seedingHearingId)
                                                .build())
                                        .build(), Offence.offence()
                                        .withId(offenceId2)
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withJurisdictionType(CROWN)
                                                .withSeedingHearingId(seedingHearingId)
                                                .build())
                                        .build())))
                                .build())))
                        .build()))
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(firstHearing)
                .build());

        uk.gov.justice.listing.events.ListedCase newCase = uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(prosecutionCaseId1)
                .withMarkers(singletonList(Marker.marker().build()))
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId2)
                        .withOffences(asList(Offence.offence()
                                .withId(offenceId3)
                                .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                .build()))
                        .build()))
                .build();

        uk.gov.justice.listing.events.Hearing extendedHearing = uk.gov.justice.listing.events.Hearing.hearing()
                .withValuesFrom(firstHearing)
                .withListedCases(asList(firstHearing.getListedCases().get(0), newCase))
                .build();

        hearing.apply(HearingListedCaseUpdated.hearingListedCaseUpdated()
                .withHearing(extendedHearing)
                .withUnAllocatedListedCases(asList(newCase))
                .build());

        final Stream<Object> listingAllocationStreams = hearing.applyAllocationRules(asList(uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(prosecutionCaseId)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendantId)
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offenceId)
                                        .build()))
                                .build()))
                        .build(),
                uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(prosecutionCaseId1)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendantId2)
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offenceId2)
                                        .build(), uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offenceId3)
                                        .build()))
                                .build()))
                        .build()), true, true);

        List<Object> listingAllocationEvents = listingAllocationStreams.collect(Collectors.toList());

        assertThat(listingAllocationEvents.size(), is(1));
        final HearingAllocatedForListingV2 hearingAllocatedForListingV2 = (HearingAllocatedForListingV2) listingAllocationEvents.get(0);
        assertThat(hearingAllocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

        assertThat(hearingAllocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0)
                .getOffenceIds().get(0).getId(), is(offenceId));

        assertThat(hearingAllocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));


    }

    @Test
    public void testOffencesRemovedFromExistingOffencesFromUnAllocatedHearingAfterCasesAddedToHearing() {

        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(prosecutionCaseId)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build())))
                                        .build())))
                                .build()))
                        .build())
                .build());

        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId2)
                                .withOffences(asList(Offence.offence()
                                        .withId(offenceId2)
                                        .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                        .build()))
                                .build()))
                        .build()))
                .build());

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst().get().getId(), is(defendantId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId2)).findFirst().get().getId(), is(defendantId2));

        hearing.apply(OffencesRemovedFromExistingUnallocatedHearing.offencesRemovedFromExistingUnallocatedHearing().withHearingId(hearingId).withOffenceIds(asList(offenceId2)).build());

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst().get().getId(), is(defendantId));


    }

    @Test
    public void shouldRaiseHearingAllocationEventWhenHearingIsAllocatedAfterAddingCaseIntoAnUnallocatedHearingFromAnotherUnAllocatedHearingRepeatedly() {

        final UUID defendantId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID prosecutionCaseId1 = randomUUID();
        final UUID prosecutionCaseId2 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID defendantId3 = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID offenceId4 = randomUUID();
        final UUID seedingHearingId = randomUUID();

        uk.gov.justice.listing.events.Hearing firstHearing = uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withType(uk.gov.justice.listing.events.Type.type().build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                .withHearingDays(emptyList())
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withCourtRoomId(randomUUID())
                .withAllocated(Boolean.FALSE)
                .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(prosecutionCaseId)
                        .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                .withCaseReference("caseReference")
                                .build())
                        .withDefendants((asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences((asList(Offence.offence()
                                        .withId(offenceId)
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withJurisdictionType(CROWN)
                                                .withSeedingHearingId(seedingHearingId)
                                                .build())
                                        .build(), Offence.offence()
                                        .withId(offenceId2)
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withJurisdictionType(CROWN)
                                                .withSeedingHearingId(seedingHearingId)
                                                .build())
                                        .build())))
                                .build())))
                        .build()))
                .build();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(firstHearing)
                .build());

        uk.gov.justice.listing.events.ListedCase newCase1 = uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(prosecutionCaseId1)
                .withMarkers(singletonList(Marker.marker().build()))
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId2)
                        .withOffences(asList(Offence.offence()
                                .withId(offenceId3)
                                .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                .build()))
                        .build()))
                .build();

        uk.gov.justice.listing.events.Hearing extendedHearing1 = uk.gov.justice.listing.events.Hearing.hearing()
                .withValuesFrom(firstHearing)
                .withListedCases(asList(firstHearing.getListedCases().get(0), newCase1))
                .build();

        hearing.apply(HearingListedCaseUpdated.hearingListedCaseUpdated()
                .withHearing(extendedHearing1)
                .withUnAllocatedListedCases(asList(newCase1))
                .build());

        uk.gov.justice.listing.events.ListedCase newCase2 = uk.gov.justice.listing.events.ListedCase.listedCase()
                .withId(prosecutionCaseId2)
                .withMarkers(singletonList(Marker.marker().build()))
                .withDefendants(asList(Defendant.defendant()
                        .withId(defendantId3)
                        .withOffences(asList(Offence.offence()
                                .withId(offenceId4)
                                .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                .build()))
                        .build()))
                .build();
        uk.gov.justice.listing.events.Hearing extendedHearing2 = uk.gov.justice.listing.events.Hearing.hearing()
                .withValuesFrom(firstHearing)
                .withListedCases(asList(firstHearing.getListedCases().get(0), newCase1, newCase2))
                .build();

        hearing.apply(HearingListedCaseUpdated.hearingListedCaseUpdated()
                .withHearing(extendedHearing2)
                .withUnAllocatedListedCases(asList(newCase2))
                .build());

        final Stream<Object> listingAllocationStreams = hearing.applyAllocationRules(asList(uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(prosecutionCaseId)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendantId)
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offenceId)
                                        .build()))
                                .build()))
                        .build(),
                uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(prosecutionCaseId1)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendantId2)
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offenceId2)
                                        .build(), uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offenceId3)
                                        .build()))
                                .build()))
                        .build(),
                uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(prosecutionCaseId2)
                        .withDefendants(asList(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendantId2)
                                .withOffences(asList( uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offenceId4)
                                        .build()))
                                .build()))
                        .build()), true, true);

        List<Object> listingAllocationEvents = listingAllocationStreams.collect(Collectors.toList());

        assertThat(listingAllocationEvents.size(), is(1));
        final HearingAllocatedForListingV2 hearingAllocatedForListingV2 = (HearingAllocatedForListingV2) listingAllocationEvents.get(0);
        assertThat(hearingAllocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

        assertThat(hearingAllocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0)
                .getOffenceIds().get(0).getId(), is(offenceId));

        assertThat(hearingAllocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

        assertThat(hearingAllocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(2).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

    }

    @Test
    public void testOffencesRemovedFromExistingOffencesFromAllocatedHearingAfterCasesAddedToHearing() {

        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID seedingHearingId = randomUUID();
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(prosecutionCaseId)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(defendantId)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(offenceId)
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build())))
                                        .build())))
                                .build()))
                        .build())
                .build());

        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(prosecutionCaseId)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId2)
                                .withOffences(asList(Offence.offence()
                                        .withId(offenceId2)
                                        .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
                                        .build()))
                                .build()))
                        .build()))
                .build());

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId)).findFirst().get().getId(), is(defendantId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId2)).findFirst().get().getId(), is(defendantId2));

        hearing.apply(OffencesRemovedFromExistingAllocatedHearing.offencesRemovedFromExistingAllocatedHearing().withHearingId(hearingId).withOffenceIds(asList(offenceId)).build());

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getId(), is(prosecutionCaseId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().stream().filter(d -> d.getId().equals(defendantId2)).findFirst().get().getId(), is(defendantId2));


    }


    private <T> List<T> asList(T... a) {
        return new ArrayList<>(java.util.Arrays.asList(a));
    }
}
