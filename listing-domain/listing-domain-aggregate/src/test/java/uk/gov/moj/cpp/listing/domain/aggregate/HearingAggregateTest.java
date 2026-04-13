package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.ApplicationEjected;
import uk.gov.justice.listing.events.AvailableSlotsForHearingFreed;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CaseIdentifierUpdated;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtRoomRemovedFromHearing;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.DefendantOffenceIdsV2;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingListedCaseUpdated;
import uk.gov.justice.listing.events.HearingMarkedAsDeleted;
import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.listing.events.HearingRequestedForListing;
import uk.gov.justice.listing.events.HearingResultStatusUpdated;
import uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved;
import uk.gov.justice.listing.events.JudiciaryChangedForHearingsStatus;
import uk.gov.justice.listing.events.Marker;
import uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.OffenceAdded;
import uk.gov.justice.listing.events.OffenceDeleted;
import uk.gov.justice.listing.events.OffenceIds;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.SequencesResetOnHearingDays;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class HearingAggregateTest {

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
    private LocalDate endDate = null;
    private final CourtCentreDefaults courtCentreDefaults = CourtCentreDefaults.courtCentreDefaults().withCourtCentreId(courtCentreId).withDefaultDuration(defaultDuration).withDefaultStartTime(defaultStartTime).build();
    private final List<CourtApplication> courtApplications = emptyList();
    private final List<CourtApplicationPartyListingNeeds> courtApplicationPartyListingNeeds = emptyList();
    private final Optional<String> adjournedFromDate = of(now().format(ofPattern("yyyy-MM-dd")));
    private final Optional<LocalDate> weekCommencingStartDate = empty();
    private final Optional<LocalDate> weekCommencingEndDate = empty();
    private final Optional<Integer> weekCommencingDurationInWeeks = empty();
    private final Boolean isSlotsBooked = false;

    private ZonedDateTime startDate;
    private List<NonDefaultDay> nonDefaultDays;
    private List<LocalDate> nonSittingDays;
    private List<uk.gov.moj.cpp.listing.domain.HearingDay> hearingDays;

    private final LocalTime preferredStartTime = LocalTime.parse("12:00");
    private final Integer preferredDuration = 45;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    private static final Logger LOGGER = Logger.getLogger(HearingAggregateTest.class.getName());





    @Test
    public void shouldCalculateHearingDaysWithStartDateAndMultipleNonDefaultDays() {
        startDate = ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(2);
        nonDefaultDays = Stream.of(
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)).withDuration(of(preferredDuration)).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(4)).withDuration(of(defaultDuration)).build(),
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(6)).withDuration(of(preferredDuration)).build())
                .collect(Collectors.toList());

        hearingDays = nonDefaultDays.stream()
                .map(nd -> uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                        .withStartTime(nd.getStartTime())
                        .withDurationMinutes(nd.getDuration().orElse(defaultDuration))
                        .build())
                .collect(Collectors.toList());

        endDate = now().plusDays(6);

        nonSittingDays = Stream.of(LocalDate.now().plusDays(3), LocalDate.now().plusDays(5)).collect(Collectors.toList());
        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingDays, nonDefaultDays, nonSittingDays, isSlotsBooked,
                "", "'", null, of(Boolean.FALSE),of(false),empty());

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
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingDays, nonDefaultDays, nonSittingDays, isSlotsBooked,
                "", "'", null, of(Boolean.FALSE), of(false), empty());

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

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingDays, nonDefaultDays, nonSittingDays, isSlotsBooked,
                "", "'", null, of(Boolean.TRUE), of(false), empty());

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

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingDays, nonDefaultDays, nonSittingDays, isSlotsBooked, "", "'", null, Optional.empty(), of(false), empty());

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

        hearingDays = nonDefaultDays.stream()
                .map(nd -> uk.gov.moj.cpp.listing.domain.HearingDay.hearingDay()
                        .withStartTime(nd.getStartTime())
                        .withDurationMinutes(nd.getDuration().orElse(defaultDuration))
                        .build())
                .collect(Collectors.toList());

        nonSittingDays = new ArrayList<>();

        endDate = now().plusDays(2);

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, estimatedDuration, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, hearingDays, nonDefaultDays, nonSittingDays, isSlotsBooked, "", "", null, of(Boolean.FALSE), of(false), empty());

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
    public void shouldNotRaiseAnyEventsAsHearingIsDelted() {
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

        hearing.apply(HearingDeleted.hearingDeleted().withHearingIdToBeDeleted(hearingId)
                .build());

        final Stream<Object> events = hearing.deleteHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(0));

    }

    @Test
    public void shouldNotRaiseAnyEventsAsHearingIsMarkedAsDuplicated() {
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

        hearing.apply(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate().withHearingId(hearingId)
                .build());

        final Stream<Object> events = hearing.deleteHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(0));

    }


    @Test
    public void shouldNotRaiseAnyEventsAsHearingIsResulted() {
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

        hearing.apply(HearingResultStatusUpdated.hearingResultStatusUpdated().withHearingId(hearingId)
                .build());

        final Stream<Object> events = hearing.deleteHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(0));

    }

    @Test
    void shouldCreateAllocatedHearingDeletedAndFreeSlotsForCrownJurisdiction() {
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
        assertThat(eventsList.size(), is(2));

        final AllocatedHearingDeleted unallocatedHearingDeleted = (AllocatedHearingDeleted) eventsList.get(0);

        assertThat(unallocatedHearingDeleted.getHearingId(), is(hearingId));
        assertThat(unallocatedHearingDeleted.getCaseIds().size(), is(2));
        assertThat(unallocatedHearingDeleted.getCaseIds(), hasItems(case1Id, case2Id));
        assertTrue(eventsList.get(1) instanceof AvailableSlotsForHearingFreed);
    }

    @Test
    public void shouldNotUnallocateHearingAndRemoveSeededOffenceIdsWhenAllocatedHearingContainsOffencesFromOtherSeedHearingAndFreeSlots() {
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
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
                        .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
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
                                                                        .withSeedingHearingId(seedingHearingId2)
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
                                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                        .withSeedingHearingId(seedingHearingId)
                                                        .build())
                                                .build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(defendant2Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(offence2Id)
                                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                                .withSeedingHearingId(seedingHearingId2)
                                                                .build()).build(),
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
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(1));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(false));

        final AvailableSlotsForHearingFreed availableSlotsForHearingFreed = (AvailableSlotsForHearingFreed) deleteHearingEventsList.get(1);
        assertThat(availableSlotsForHearingFreed.getHearingId(), is(hearingId));

        hearing.apply(offencesRemovedFromHearing);

        final Stream<Object> allocationStream = hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(), empty(), null);

        final AllocatedHearingUpdatedForListingV2 hearingAllocatedForListing = (AllocatedHearingUpdatedForListingV2) allocationStream.collect(Collectors.toList()).get(0);

        assertThat(hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds().size(), is(1));
        assertThat(hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds().get(0).getId(), is(case2Id));

        final DefendantOffenceIdsV2 defendantOffenceIdsV2 = hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0);
        assertThat(defendantOffenceIdsV2.getId(), is(defendant2Id));
        assertThat(defendantOffenceIdsV2.getOffenceIds().size(), is(2));
        assertThat(defendantOffenceIdsV2.getOffenceIds().stream().map(OffenceIds::getId).toList(), hasItems(offence2Id, offence3Id));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));

    }

    @Test
    public void shouldRemoveUnAllocatedNextHearingWhenSeedingHearingAmendedAndNextHearingHasNotAnotherSeedingOffences() {
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                        .withHearing(prepareHearing(hearingId, false,
                                Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)),Map.of(offence2Id, Optional.of(seedingHearingId)) ))))) )
                .build());

        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(1));

        final UnallocatedHearingDeleted unallocatedHearingDeleted = (UnallocatedHearingDeleted) deleteHearingEventsList.get(0);

        assertThat(unallocatedHearingDeleted.getHearingId(), is(hearingId));
        assertThat(unallocatedHearingDeleted.getCaseIds().size(), is(1));
        assertThat(unallocatedHearingDeleted.getCaseIds(), hasItems(case1Id));
    }

    @Test
    public void shouldRemoveAllocatedNextHearingWhenSeedingHearingAmendedAndNextHearingHasNotAnotherSeedingOffences() {
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)),Map.of(offence2Id, Optional.of(seedingHearingId)) ))))) )
                .build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final AllocatedHearingDeleted allocatedHearingDeleted = (AllocatedHearingDeleted) deleteHearingEventsList.get(0);

        assertThat(allocatedHearingDeleted.getHearingId(), is(hearingId));
        assertThat(allocatedHearingDeleted.getCaseIds().size(), is(1));
        assertThat(allocatedHearingDeleted.getCaseIds(), hasItems(case1Id));
    }

    @Test
    public void shouldRemoveOffencesFromNextHearingWhenSeedingHearingAmendedAndNextHearingHasMultipleSeedingOffencesWithTheSameCase() {
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.of(seedingHearingId2))))))))
                .build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(1));
        assertThat(offencesRemovedFromHearing.getSeededOffences().get(0), is(offence1Id));
        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offence2Id));
    }

    @Test
    public void shouldNotRemoveAllocatedNextHearingWhenSeedingHearingAmendedAndNextHearingHasMultipleSeedingOffencesAndNotSeededOffences() {
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();
        final UUID offence4Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.empty())))),
                        case2Id, asList(Map.of(defendant2Id, asList(Map.of(offence3Id, Optional.of(seedingHearingId2)), Map.of(offence4Id, Optional.empty())))))))
                .build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId().isEmpty(), is(true));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(1));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(false));
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
        assertThat(offencesRemovedFromExistingAllocatedHearing.getIsResultFlow(), is(true));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offence2Id));
    }

    @Test
    public void shouldRemoveNewOffenceWhenNextHearingIsExistingAllocatedHearing() {

        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID newOffenceId = randomUUID();

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

        List<ProsecutionCaseDefendantOffenceIdsV2> ids = new ArrayList<>();
        ids.add(ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
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
                .build());
        ids.add(ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                .withId(case2Id)
                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                        .withId(defendant2Id)
                        .withOffenceIds(new ArrayList(asList(OffenceIds.offenceIds()
                                .withId(offence2Id)
                                .build())))
                        .build())))
                .build());

        hearing.apply(HearingAllocatedForListingV2.hearingAllocatedForListingV2()
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(ids).build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case2Id).withDefendantId(defendant2Id).withOffence(Offence.offence()
                        .withId(newOffenceId)
                        .build()).build());

        final Stream<Object> events = hearing.removeOffencesFromExistingHearing(seedingHearingId, hearingId);

        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(1));

        final OffencesRemovedFromExistingAllocatedHearing offencesRemovedFromExistingAllocatedHearing = (OffencesRemovedFromExistingAllocatedHearing) eventsList.get(0);
        assertThat(offencesRemovedFromExistingAllocatedHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromExistingAllocatedHearing.getOffenceIds(), hasItems(offence2Id, newOffenceId));
        assertThat(offencesRemovedFromExistingAllocatedHearing.getIsResultFlow(), is(true));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(offence1Id));
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
    public void shouldRemoveNewOffenceWhenNextHearingIsUnallocated() {

        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID newOffenceId = randomUUID();

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

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case2Id).withDefendantId(defendant2Id).withOffence(Offence.offence()
                        .withId(newOffenceId)
                        .build()).build());

        final Stream<Object> events = hearing.removeOffencesFromExistingHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(1));

        final OffencesRemovedFromExistingUnallocatedHearing offencesRemovedFromExistingUnallocatedHearing = (OffencesRemovedFromExistingUnallocatedHearing) eventsList.get(0);
        assertThat(offencesRemovedFromExistingUnallocatedHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromExistingUnallocatedHearing.getOffenceIds(), hasItems(newOffenceId, offence2Id));
        assertThat(offencesRemovedFromExistingUnallocatedHearing.getIsResultFlow(), is(true));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getId(), is(case1Id));
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
    public void shouldNotBeAbleToEjectApplicationIfHearingIsNotCreated() {

        final UUID applicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final String removalReason = "removal reason";

        final Stream<Object> listedHearing = hearing.ejectApplication(hearingId, applicationId, removalReason);

        assertThat(listedHearing.count(), is(0L));

    }

    @Test
    public void shouldBeAbleToEjectApplicationIfHearingIsListed() {

        final UUID applicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final String removalReason = "removal reason";


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
                        .withCourtApplications(new ArrayList<>(asList(uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                .build())))
                        .build())
                .build()
        );


        final Stream<Object> listedHearing = hearing.ejectApplication(hearingId, applicationId, removalReason);

        assertThat(listedHearing.count(), is(1L));

    }

    @Test
    void shouldBeAbleToEjectApplicationAndAvailableSlotsForHearingFreed_IfHearingIsListedAndAllEjectedTrue() {

        final UUID applicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final String removalReason = "removal reason";

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withCourtApplications(new ArrayList<>(asList(uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                .withIsEjected(true)
                                .build())))
                        .build())
                .build()
        );


        var listedHearing = hearing.ejectApplication(hearingId, applicationId, removalReason).toList();

        assertThat(listedHearing, hasSize(2));

        var availableSlotsForHearingFreed = (AvailableSlotsForHearingFreed)listedHearing.get(0);
        var applicationEjected = (ApplicationEjected)listedHearing.get(1);

        assertThat(availableSlotsForHearingFreed.getHearingId(), is(hearingId));
        assertThat(applicationEjected.getHearingId(), is(hearingId));
    }

    @Test
    void shouldBeAbleToEjectHearingAndAvailableSlotsForHearingFreed_IfHearingIsListedAndAllEjectedTrue() {

        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final String removalReason = "removal reason";

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
                        .withAllocated(true)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(
                                asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(caseId)
                                        .withDefendants(emptyList())
                                        .withIsEjected(true)
                                        .build()
                                )
                        ))
                        .build())
                .build()
        );


        var listedHearing = hearing.ejectCase(hearingId, caseId, removalReason).toList();

        assertThat(listedHearing, hasSize(2));

        var availableSlotsForHearingFreed = (AvailableSlotsForHearingFreed)listedHearing.get(0);
        var caseEjected = (CaseEjected)listedHearing.get(1);

        assertThat(availableSlotsForHearingFreed.getHearingId(), is(hearingId));
        assertThat(caseEjected.getHearingId(), is(hearingId));
    }

    @Test
    void shouldBeAbleToEjectApplicationAndAvailableSlotsForHearingFreedForCrown() {

        final UUID applicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final String removalReason = "removal reason";

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(CROWN)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withCourtApplications(new ArrayList<>(asList(uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                .withIsEjected(true)
                                .build())))
                        .build())
                .build()
        );


        var listedHearing = hearing.ejectApplication(hearingId, applicationId, removalReason).toList();

        assertThat(listedHearing, hasSize(2));

        var availableSlotsForHearingFreed = (AvailableSlotsForHearingFreed)listedHearing.get(0);
        var applicationEjected = (ApplicationEjected)listedHearing.get(1);

        assertThat(availableSlotsForHearingFreed.getHearingId(), is(hearingId));
        assertThat(applicationEjected.getHearingId(), is(hearingId));
    }

    @Test
    void shouldBeAbleToEjectCaseAndAvailableSlotsForHearingFreedForCrown() {

        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final String removalReason = "removal reason";

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(CROWN)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(
                                asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(caseId)
                                        .withDefendants(emptyList())
                                        .withIsEjected(true)
                                        .build()
                                )
                        ))
                        .build())
                .build()
        );

        var listedHearing = hearing.ejectCase(hearingId, caseId, removalReason).toList();

        assertThat(listedHearing, hasSize(2));

        var availableSlotsForHearingFreed = (AvailableSlotsForHearingFreed) listedHearing.get(0);
        var caseEjected = (CaseEjected) listedHearing.get(1);

        assertThat(availableSlotsForHearingFreed.getHearingId(), is(hearingId));
        assertThat(caseEjected.getHearingId(), is(hearingId));
    }

    @Test
    void shouldBeAbleToEjectApplicationAndNoSlotsForHearingFreedForHearingThatHasAlreadyStarted() {

        final UUID applicationId = randomUUID();
        final UUID hearingId = randomUUID();
        final String removalReason = "removal reason";

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now())
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withCourtApplications(new ArrayList<>(asList(uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                .withId(applicationId)
                                .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                .withIsEjected(true)
                                .build())))
                        .build())
                .build()
        );


        var listedHearing = hearing.ejectApplication(hearingId, applicationId, removalReason).toList();

        assertThat(listedHearing, hasSize(1));

        var applicationEjected = (ApplicationEjected)listedHearing.get(0);

        assertThat(applicationEjected.getHearingId(), is(hearingId));
    }

    @Test
    void shouldNotBeAbleToMarkHearingAsDeletedIfAllreadyDeleted() {

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
    void shouldNotBeAbleToMarkHearingAsDeletedIfHearingResulted() {

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

        hearing.apply(HearingResultStatusUpdated.hearingResultStatusUpdated().withHearingId(hearingId).build());

        final Stream<Object> listedHearing = hearing.markHearingAsDeleted(hearingId);

        assertThat(listedHearing.count(), is(0L));


    }

    @Test
    void shouldBeAbleToDeleteHearingIfNotDeleted() {

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
    void shouldNotBeAbleToDeleteHearingIfAllReadyDeleted() {

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
    void shouldReturnNothingWhenDefendantListIsNullForUpdateDefendantCourtProceedingForHearing2() {
        final UUID case1Id = randomUUID();
        final ProsecutionCase prosecutionCase = prosecutionCase().
                withId(case1Id).
                withDefendants(null).
                build();

        final Stream<Object> listedHearing = hearing.updateDefendantCourtProceedingForHearing(hearingId, prosecutionCase);

        assertThat(listedHearing.count(), is(0L));
    }

    @Test
    void shouldPopulateSeedingHearingInAllocationWhenSeedingHearingNotExistsInPayloadsOffenceLevel() {
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
                        .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
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
                        .build()), true, true)).flatMap(i -> i);

        final List<Object> allocationEvents = allocationStreams.toList();
        assertThat(allocationEvents.size(), is(1));
        final HearingAllocatedForListingV2 allocatedForListingV2 = (HearingAllocatedForListingV2) allocationEvents.get(0);
        assertThat(allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

        assertThat(allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(1).getDefendants().get(0)
                .getOffenceIds().get(0).getSeedingHearing().getSeedingHearingId(), is(seedingHearingId));

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
    void shouldUpdateProsecutionCaseDefendantsOffenceIdsWhenCasesAddedToHearing() {
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
                        .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
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


        final Stream<Object> streams = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(),empty(), null)).flatMap(i -> i);
        final List<Object> events = streams.toList();
        assertThat(events.size(), is(1));

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
    }

    @Test
    void shouldNotIncludeDefendantWhoseOffencesReRemovedFromHearing() {
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
                        .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
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

        final Stream<Object> streams = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(),empty(), null)).flatMap(i -> i);
        final List<Object> events = streams.toList();
        assertThat(events.size(), is(1));

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
    void shouldUpdateProsecutionCaseDefendantsOffenceIdsWhenOffenceAddedOrDeleted() {
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
                        .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
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

        final Stream<Object> streams = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(),empty(), null)).flatMap(i -> i);
        final List<Object> events = streams.toList();
        assertThat(events.size(), is(1));

        final HearingAllocatedForListingV2 allocatedForListingV2 = (HearingAllocatedForListingV2) events.get(0);
        final ProsecutionCaseDefendantOffenceIdsV2 prosecutionCaseDefendantOffenceIdsV2 = allocatedForListingV2.getProsecutionCaseDefendantsOffenceIds().get(0);
        assertThat(prosecutionCaseDefendantOffenceIdsV2.getId(), is(case1Id));

        final DefendantOffenceIdsV2 defendantOffenceIdsV2 = prosecutionCaseDefendantOffenceIdsV2.getDefendants().get(0);
        assertThat(defendantOffenceIdsV2.getId(), is(case1Defendant1Id));

        assertThat(defendantOffenceIdsV2.getOffenceIds().size(), is(1));
        assertThat(defendantOffenceIdsV2.getOffenceIds().get(0).getId(), is(case1Defendant1Offence2Id));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().size(), is(1));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getId(), is(case1Defendant1Offence2Id));
    }

    @Test
    void shouldUpdateProsecutionCaseDefendantsWhenCasesAddedForHearing() {

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
    void shouldRaiseOffenceRemovedEventWhenOffenceIsInAllocatedHearing() {
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


        final List<OffencesRemovedFromExistingAllocatedHearing> events = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id), Hearing.SOURCE_LISTING, false)
                .map(OffencesRemovedFromExistingAllocatedHearing.class::cast)
                .collect(Collectors.toList());

        assertThat(events.get(0).getHearingId(), is(hearingId));
        assertThat(events.get(0).getIsResultFlow(), is(false));
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
        final List<Object> events2 = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id), Hearing.SOURCE_LISTING, false)
                .collect(Collectors.toList());

        assertThat(events2.isEmpty(), is(true));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(0));

    }

    @Test
    void shouldRaiseOffenceRemovedEventWhenOffenceIsInAllocatedHearingWhenSourceIsListing() {
        shouldRaiseOffenceRemovedEventWhenOffenceIsInAllocatedHearing(Hearing.SOURCE_LISTING);
    }

    @Test
    void shouldRaiseOffenceRemovedEventWhenOffenceIsInAllocatedHearingWhenSourceIsHearing() {
        shouldRaiseOffenceRemovedEventWhenOffenceIsInAllocatedHearing(Hearing.SOURCE_HEARING);
    }


    @Test
    void shouldRaiseOffenceRemovedEventWhenOffenceIsInUnAllocatedHearing() {
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


        final List<OffencesRemovedFromExistingUnallocatedHearing> events = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id), Hearing.SOURCE_LISTING, false)
                .map(OffencesRemovedFromExistingUnallocatedHearing.class::cast)
                .collect(Collectors.toList());

        assertThat(events.get(0).getHearingId(), is(hearingId));
        assertThat(events.get(0).getOffenceIds().get(0), is(offence1Id));
        assertThat(events.get(0).getOffenceIds().get(1), is(offence2Id));

        final List<Object> events2 = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id), Hearing.SOURCE_LISTING, false)
                .collect(Collectors.toList());

        assertThat(events2.isEmpty(), is(true));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().isEmpty(), is(true));
    }

    @Test
    void shouldAddNewCaseWhenDefendantCourtProceedingsUpdatedV2() {
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
    void shouldAddNewDefendantWhenDefendantCourtProceedingsUpdatedV2WithSameCaseWithDifferentDefendant() {
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
        uk.gov.justice.listing.events.ListedCase listedCase1 = hearing.getCurrentHearingEventState().getListedCases().stream().filter(lc -> lc.getId().equals(case1Id)).findFirst().get();
        assertThat(listedCase1.getDefendants().size(), is(2));
        uk.gov.justice.listing.events.Defendant defendant3 = listedCase1.getDefendants().stream().filter(def -> def.getId().equals(defendant3Id)).findFirst().get();
        assertThat(defendant3.getOffences().size(), is(1));
        assertThat(defendant3.getOffences().get(0).getId(), is(offence3Id));
    }

    @Test
    void shouldAddNewOffenceWhenDefendantCourtProceedingsUpdatedV2WithSameCaseWithSameDefendantButDifferentOffence() {
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
        uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant().withId(defendant1Id).withOffences(offences).build();
        defendants.add(defendant);
        ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier().build();
        ProsecutionCase prosecutionCase = prosecutionCase().withId(case1Id).withProsecutionCaseIdentifier(prosecutionCaseIdentifier).withDefendants(defendants).build();
        hearing.apply(DefendantCourtProceedingsUpdatedV2.defendantCourtProceedingsUpdatedV2().withHearingId(randomUUID()).withProsecutionCase(prosecutionCase).build());
        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(2));
        uk.gov.justice.listing.events.ListedCase listedCase1 = hearing.getCurrentHearingEventState().getListedCases().stream().filter(lc -> lc.getId().equals(case1Id)).findFirst().get();
        assertThat(listedCase1.getDefendants().size(), is(1));
        uk.gov.justice.listing.events.Defendant defendant1 = listedCase1.getDefendants().stream().filter(def -> def.getId().equals(defendant1Id)).findFirst().get();
        assertThat(defendant1.getOffences().size(), is(2));
        assertTrue(defendant1.getOffences().stream().anyMatch(o -> o.getId().equals(offence1Id)));
        assertTrue(defendant1.getOffences().stream().anyMatch(o -> o.getId().equals(offence3Id)));
    }

    @Test
    void shouldAddNothingWhenDefendantCourtProceedingsUpdatedV2WithSameCaseWithSameDefendantAmndSameOffence() {
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
        uk.gov.justice.core.courts.Offence offence = uk.gov.justice.core.courts.Offence.offence().withId(offence1Id).build();
        List<uk.gov.justice.core.courts.Offence> offences = new ArrayList<>();
        offences.add(offence);
        List<uk.gov.justice.core.courts.Defendant> defendants = new ArrayList<>();
        uk.gov.justice.core.courts.Defendant defendant = uk.gov.justice.core.courts.Defendant.defendant().withId(defendant1Id).withOffences(offences).build();
        defendants.add(defendant);
        ProsecutionCaseIdentifier prosecutionCaseIdentifier = prosecutionCaseIdentifier().build();
        ProsecutionCase prosecutionCase = prosecutionCase().withId(case1Id).withProsecutionCaseIdentifier(prosecutionCaseIdentifier).withDefendants(defendants).build();
        hearing.apply(DefendantCourtProceedingsUpdatedV2.defendantCourtProceedingsUpdatedV2().withHearingId(randomUUID()).withProsecutionCase(prosecutionCase).build());
        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(2));
        uk.gov.justice.listing.events.ListedCase listedCase1 = hearing.getCurrentHearingEventState().getListedCases().stream().filter(lc -> lc.getId().equals(case1Id)).findFirst().get();
        assertThat(listedCase1.getDefendants().size(), is(1));
        uk.gov.justice.listing.events.Defendant defendant1 = listedCase1.getDefendants().stream().filter(def -> def.getId().equals(defendant1Id)).findFirst().get();
        assertThat(defendant1.getOffences().size(), is(1));
        assertTrue(defendant1.getOffences().stream().anyMatch(o -> o.getId().equals(offence1Id)));
    }

    @Test
    void shouldCreateEventForSplit() {
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

        final NonDefaultDay nonDefaultDay = NonDefaultDay.nonDefaultDay()
                .withDuration(Optional.of(1))
                .withStartTime(ZonedDateTime.now())
                .withCourtCentreId(Optional.of("courtCentreId"))
                .withCourtRoomId(Optional.of(1))
                .withCourtScheduleId(Optional.of("courtScheduleId"))
                .withOucode(Optional.of("oucode"))
                .withSession(Optional.of("PM"))
                .withRoomId(Optional.of("roomId"))
                .build();
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);

        final Stream<Object> listedHearing = hearing.listForSplit(type, listedCases, courtCentreId, "court name", courtRoomId, jurisdictionType, startDate,
                null, null, emptyList(), nonDefaultDays);

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
        assertThat(hearingRequestedForListing.getListNewHearing().getNonDefaultDays().size(), is(1));
    }

    @Test
    void shouldCreateEventForWeekCommencingSplit() {
        final List<uk.gov.justice.listing.events.ListedCase> listedCases = singletonList(uk.gov.justice.listing.events.ListedCase
                .listedCase()
                .withId(randomUUID())
                .withDefendants(singletonList(Defendant.defendant()
                        .withId(randomUUID())
                        .withOffences(singletonList(Offence.offence()
                                .withId(randomUUID())
                                .build()))
                        .build())).build());

        final NonDefaultDay nonDefaultDay = NonDefaultDay.nonDefaultDay()
                .withDuration(Optional.of(1))
                .withStartTime(ZonedDateTime.now())
                .withCourtCentreId(Optional.of("courtCentreId"))
                .withCourtRoomId(Optional.of(1))
                .withCourtScheduleId(Optional.of("courtScheduleId"))
                .withOucode(Optional.of("oucode"))
                .withSession(Optional.of("PM"))
                .withRoomId(Optional.of("roomId"))
                .build();
        final List<NonDefaultDay> nonDefaultDays = new ArrayList<>();
        nonDefaultDays.add(nonDefaultDay);

        final LocalDate weekCommencingStartDate = LocalDate.now();
        final Stream<Object> listedHearing = hearing.listForSplit(type, listedCases, courtCentreId, "court name", courtRoomId, jurisdictionType, startDate,
                weekCommencingStartDate, 1, emptyList(), nonDefaultDays);

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
        assertThat(hearingRequestedForListing.getListNewHearing().getNonDefaultDays().size(), is(1));
    }

    @Test
    void shouldUpdateCaseIdentifier() {
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
        CaseIdentifierUpdated caseIdentifierUpdated = (CaseIdentifierUpdated) events.toList().get(0);
        assertThat(caseIdentifierUpdated.getProsecutionCaseId(), is(case1Id));
        assertThat(caseIdentifierUpdated.getProsecutionAuthorityId(), is(prosecutionAuthorityId));
        assertThat(caseIdentifierUpdated.getProsecutionAuthorityCode(), is(prosecutionAuthorityCode));

    }


    @Test
    void shouldTestCurrentHearingEventStateWithAddedMultipleDefendantsInSameCaseWhenCaseSplitAtDefendantLevelAndMergedBack() {

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
    void shouldTestCurrentHearingEventStateWithAddedMultipleOffencesInSameDefendantWhenCaseSplitAtOffenceLevelLevelAndMergedBack() {

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
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offenceId)).findFirst().get().getId(), is(offenceId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offenceId2)).findFirst().get().getId(), is(offenceId2));
    }

    @Test
    void shouldTestCurrentHearingEventStateWithAddedMultipleOffencesInSameDefendantWhenCaseSplitAtOffenceLevelLevelAndMergedBackAndSameOffenceisBeingAdded() {

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
                                                .build(), Offence.offence()
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
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offenceId)).findFirst().get().getId(), is(offenceId));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offenceId2)).findFirst().get().getId(), is(offenceId2));
    }

    @Test
    void shouldTestCurrentHearingEventStateWithAddedMultipleCasesWhenNewCaseIsBeingAdded() {

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
                                                .build(), Offence.offence()
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
        assertThat(listedCase1.getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offenceId)).findFirst().get().getId(), is(offenceId));
        assertThat(listedCase1.getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offenceId2)).findFirst().get().getId(), is(offenceId2));
        final uk.gov.justice.listing.events.ListedCase listedCase2 = hearing.getCurrentHearingEventState().getListedCases().stream().filter(pc -> pc.getId().equals(prosecutionCaseId1)).findFirst().get();
        assertThat(listedCase2.getDefendants().size(), is(1));
        assertThat(listedCase2.getDefendants().stream().filter(d -> d.getId().equals(defendantId2)).findFirst().get().getId(), is(defendantId2));
        assertThat(listedCase2.getDefendants().get(0).getOffences().size(), is(1));
        assertThat(listedCase2.getDefendants().get(0).getOffences().stream().filter(o -> o.getId().equals(offenceId3)).findFirst().get().getId(), is(offenceId3));

    }

    @Test
    void shouldKeepAllOffencesIntoProsecutionCaseDefendantOffenceIdsWhenHearingExtendedWithNewCase() {

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
                .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
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
    void shouldRaiseHearingAllocationEventWhenHearingIsAllocatedAfterAddingCaseIntoAnUnallocatedHearingFromAnotherUnAllocatedHearing() {

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
                .withHearingDays(singletonList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
                .withStartDate(LocalDate.now())
                .withEndDate(LocalDate.now())
                .withCourtCentreId(randomUUID())
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
        hearing.assignCourtRoom(randomUUID(), hearingId, Optional.empty());

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

        List<Object> listingAllocationEvents = listingAllocationStreams.toList();

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
    void testOffencesRemovedFromExistingOffencesFromUnAllocatedHearingAfterCasesAddedToHearing() {

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
    void shouldRaiseHearingAllocationEventWhenHearingIsAllocatedAfterAddingCaseIntoAnUnallocatedHearingFromAnotherUnAllocatedHearingRepeatedly() {

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
                .withHearingDays(singletonList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
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
                                .withSeedingHearing(SeedingHearing.seedingHearing()
                                        .withSeedingHearingId(seedingHearingId).withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN).build())
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

        final Stream<Object> listingAllocationStreams = hearing
                .applyAllocationRules(asList(uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
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
                                .withOffences(asList(uk.gov.moj.cpp.listing.domain.OffenceIds.offenceIds()
                                        .withId(offenceId4)
                                        .build()))
                                .build()))
                        .build()), true, true);

        List<Object> listingAllocationEvents = listingAllocationStreams.toList();

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
    void testOffencesRemovedFromExistingOffencesFromAllocatedHearingAfterCasesAddedToHearing() {

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

    @Test
    void shouldNotRaiseHearingVacatedEventAsBoxWorkHearingDoesNotExistInListingContext() {

        final UUID vacatingTrialReasonId = randomUUID();
        final Stream<Object> events = hearing.hearingVacateTrial(Optional.of(vacatingTrialReasonId));
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(0));

    }

    @Test
    void shouldRaiseUnallocatedDeleteHearingEvent() {
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
                                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                                                                .withSeedingHearingId(seedingHearingId)
                                                                .build())
                                                        .build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        hearing.apply(CasesAddedToHearing.casesAddedToHearing()
                .withHearingId(hearingId)
                .withUnAllocatedListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(case2Id)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendant2Id)
                                .withOffences(asList(Offence.offence()
                                        .withId(offence2Id)
                                        .withSeedingHearing(SeedingHearing.seedingHearing()
                                                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                                                .withSeedingHearingId(seedingHearingId)
                                                .build())
                                        .build()))
                                .build()))
                        .build()))
                .build());

        final Stream<Object> events = hearing.deleteHearing(seedingHearingId, hearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());
        assertThat(eventsList.size(), is(1));

        final UnallocatedHearingDeleted unallocatedHearingDeleted = (UnallocatedHearingDeleted) eventsList.get(0);

        assertThat(unallocatedHearingDeleted.getHearingId(), is(hearingId));
    }

    @Test
    void shouldNotAddOffenceWhenAllOffencesOfDefendantRemoved()  {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID case3Id = randomUUID();
        final UUID case4Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant2Id = randomUUID();
        final UUID case1Defendant3Id = randomUUID();
        final UUID case2Defendant1Id = randomUUID();
        final UUID case3Defendant1Id = randomUUID();
        final UUID case4Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID case1Defendant2Offence1Id = randomUUID();
        final UUID case1Defendant3Offence1Id = randomUUID();
        final UUID case2Defendant1Offence1Id = randomUUID();
        final UUID case2Defendant1Offence2Id = randomUUID();
        final UUID case3Defendant1Offence1Id = randomUUID();
        final UUID case3Defendant1Offence2Id = randomUUID();
        final UUID case4Defendant1Offence1Id = randomUUID();
        final UUID case4Defendant1Offence2Id = randomUUID();
        final UUID case1Defendant2Offence2Id = randomUUID();
        final UUID case1Defendant3Offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID case1Defendant2Offence3Id = randomUUID();

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
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference1")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build(),Defendant.defendant()
                                        .withId(case1Defendant2Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case1Defendant2Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build(),Defendant.defendant()
                                        .withId(case1Defendant3Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case1Defendant3Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build(),uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case2Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference2")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case2Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case2Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build(),Offence.offence()
                                                .withId(case2Defendant1Offence2Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build(),uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case3Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference3")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case2Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case3Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build(),Offence.offence()
                                                .withId(case3Defendant1Offence2Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build(),uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case4Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference4")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case2Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case4Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build(),Offence.offence()
                                                .withId(case4Defendant1Offence2Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build()))
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
                                                .build())))
                                        .build(),DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case1Defendant2Id)
                                        .withOffenceIds(new ArrayList<>(asList(OffenceIds.offenceIds()
                                                .withId(case1Defendant2Offence1Id)
                                                .build())))
                                        .build(),DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case1Defendant3Id)
                                        .withOffenceIds(new ArrayList<>(asList(OffenceIds.offenceIds()
                                                .withId(case1Defendant3Offence1Id)
                                                .build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case2Defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(case2Defendant1Offence1Id).build(),
                                                OffenceIds.offenceIds().withId(case2Defendant1Offence2Id).build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case3Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case3Defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(case3Defendant1Offence1Id).build(),
                                                OffenceIds.offenceIds().withId(case3Defendant1Offence2Id).build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case4Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case4Defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(case4Defendant1Offence1Id).build(),
                                                OffenceIds.offenceIds().withId(case4Defendant1Offence2Id).build())))
                                        .build())))
                                .build())))
                .build());
        hearing.apply(OffenceAdded.offenceAdded()
                .withHearingId(hearingId)
                .withCaseId(case1Id)
                .withDefendantId(case1Defendant2Id)
                .withOffence(Offence.offence()
                        .withId(case1Defendant2Offence2Id)
                        .withSeedingHearing(seedingHearing)
                        .build())
                .build());

        hearing.apply(OffenceAdded.offenceAdded()
                .withHearingId(hearingId)
                .withCaseId(case1Id)
                .withDefendantId(case1Defendant3Id)
                .withOffence(Offence.offence()
                        .withId(case1Defendant3Offence2Id)
                        .withSeedingHearing(seedingHearing)
                        .build())
                .build());

        hearing.apply(OffencesRemovedFromExistingAllocatedHearing.offencesRemovedFromExistingAllocatedHearing()
                .withHearingId(hearingId)
                .withOffenceIds(asList(case1Defendant2Offence1Id,case1Defendant2Offence2Id))
                .build());

        final Stream<Object> events = hearing.addOffences(case1Id, case1Defendant2Id, asList(uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(case1Defendant2Offence3Id)
                .withSeedingHearing(Optional.of(uk.gov.moj.cpp.listing.domain.SeedingHearing.seedingHearing()
                        .withSeedingHearingId(seedingHearingId)
                        .build()))
                .build()));

        final List<Object> eventsList = events.collect(Collectors.toList());

        assertThat(eventsList.size(), is(0));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(4));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(1).getOffences().size(), is(2));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().get(0).getDefendants().get(1).getOffences().get(0).getId(), is(case1Defendant3Offence1Id));

    }

    @Test
    public void shouldNotDefendantWhenAllDefendantOfCaseAreRemoved()  {
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID case3Id = randomUUID();
        final UUID case4Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant2Id = randomUUID();
        final UUID case1Defendant3Id = randomUUID();
        final UUID case2Defendant1Id = randomUUID();
        final UUID case3Defendant1Id = randomUUID();
        final UUID case4Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID case1Defendant2Offence1Id = randomUUID();
        final UUID case1Defendant3Offence1Id = randomUUID();
        final UUID case2Defendant1Offence1Id = randomUUID();
        final UUID case2Defendant1Offence2Id = randomUUID();
        final UUID case3Defendant1Offence1Id = randomUUID();
        final UUID case3Defendant1Offence2Id = randomUUID();
        final UUID case4Defendant1Offence1Id = randomUUID();
        final UUID case4Defendant1Offence2Id = randomUUID();
        final UUID case1Defendant2Offence2Id = randomUUID();
        final UUID case1Defendant3Offence2Id = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID case1Defendant2Offence3Id = randomUUID();

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
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case1Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference1")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build(),Defendant.defendant()
                                        .withId(case1Defendant2Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case1Defendant2Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build(),Defendant.defendant()
                                        .withId(case1Defendant3Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case1Defendant3Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build(),uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case2Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference2")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case2Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case2Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build(),Offence.offence()
                                                .withId(case2Defendant1Offence2Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build(),uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case3Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference3")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case2Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case3Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build(),Offence.offence()
                                                .withId(case3Defendant1Offence2Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build(),uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(case4Id)
                                .withCaseIdentifier(CaseIdentifier.caseIdentifier()
                                        .withCaseReference("caseReference4")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case2Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case4Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build(),Offence.offence()
                                                .withId(case4Defendant1Offence2Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build()))
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
                                                .build())))
                                        .build(),DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case1Defendant2Id)
                                        .withOffenceIds(new ArrayList<>(asList(OffenceIds.offenceIds()
                                                .withId(case1Defendant2Offence1Id)
                                                .build())))
                                        .build(),DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case1Defendant3Id)
                                        .withOffenceIds(new ArrayList<>(asList(OffenceIds.offenceIds()
                                                .withId(case1Defendant3Offence1Id)
                                                .build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case2Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case2Defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(case2Defendant1Offence1Id).build(),
                                                OffenceIds.offenceIds().withId(case2Defendant1Offence2Id).build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case3Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case3Defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(case3Defendant1Offence1Id).build(),
                                                OffenceIds.offenceIds().withId(case3Defendant1Offence2Id).build())))
                                        .build())))
                                .build(),
                        ProsecutionCaseDefendantOffenceIdsV2.prosecutionCaseDefendantOffenceIdsV2()
                                .withId(case4Id)
                                .withDefendants(new ArrayList<>(asList(DefendantOffenceIdsV2.defendantOffenceIdsV2()
                                        .withId(case4Defendant1Id)
                                        .withOffenceIds(new ArrayList<>(asList(
                                                OffenceIds.offenceIds().withId(case4Defendant1Offence1Id).build(),
                                                OffenceIds.offenceIds().withId(case4Defendant1Offence2Id).build())))
                                        .build())))
                                .build())))
                .build());
        hearing.apply(OffenceAdded.offenceAdded()
                .withHearingId(hearingId)
                .withCaseId(case1Id)
                .withDefendantId(case1Defendant2Id)
                .withOffence(Offence.offence()
                        .withId(case1Defendant2Offence2Id)
                        .withSeedingHearing(seedingHearing)
                        .build())
                .build());

        hearing.apply(OffenceAdded.offenceAdded()
                .withHearingId(hearingId)
                .withCaseId(case1Id)
                .withDefendantId(case1Defendant3Id)
                .withOffence(Offence.offence()
                        .withId(case1Defendant3Offence2Id)
                        .withSeedingHearing(seedingHearing)
                        .build())
                .build());

        hearing.apply(OffencesRemovedFromExistingAllocatedHearing.offencesRemovedFromExistingAllocatedHearing()
                .withHearingId(hearingId)
                .withOffenceIds(asList(case3Defendant1Offence1Id,case3Defendant1Offence2Id))
                .build());

        final Stream<Object> events = hearing.addDefendantsForCourtProceedings(case3Id,asList(uk.gov.moj.cpp.listing.domain.Defendant.defendant()
                .withId(randomUUID())
                .withOffences(asList(uk.gov.moj.cpp.listing.domain.Offence.offence()
                        .withId(randomUUID())
                        .withSeedingHearing(Optional.of(uk.gov.moj.cpp.listing.domain.SeedingHearing.seedingHearing()
                                .withSeedingHearingId(seedingHearingId)
                                .build()))
                        .build()))
                .build()));

        final List<Object> eventsList = events.collect(Collectors.toList());

        assertThat(eventsList.size(), is(0));
        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(3));

    }




    private <T> List<T> asList(T... a) {
        return new ArrayList<>(java.util.Arrays.asList(a));
    }

    private void shouldRaiseOffenceRemovedEventWhenOffenceIsInAllocatedHearing(final String source) {
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


        final List<OffencesRemovedFromExistingAllocatedHearing> events = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id), source, false)
                .map(OffencesRemovedFromExistingAllocatedHearing.class::cast)
                .collect(Collectors.toList());

        assertThat(events.get(0).getHearingId(), is(hearingId));
        assertThat(events.get(0).getOffenceIds().get(0), is(offence1Id));
        assertThat(events.get(0).getOffenceIds().get(1), is(offence2Id));
        assertThat(events.get(0).getSourceContext(), is(source));
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
        final List<Object> events2 = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id), source, false)
                .collect(Collectors.toList());

        assertThat(events2.isEmpty(), is(true));

        assertThat(hearing.getCurrentHearingEventState().getListedCases().size(), is(0));

    }

    @Test
    public void shouldNotRaiseOffenceRemovedEventWhenOffenceIsInAllocatedHearing() {
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

        hearing.apply(HearingDeleted.hearingDeleted().withHearingIdToBeDeleted(hearingId).build());


        final Stream<Object> events = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id), null, false);
        assertThat(events.count(), is(0L));
    }

    @Test
    public void shouldNotRaiseOffenceRemovedEventWhenHearingDeleted() {
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

        hearing.apply(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate().withHearingId(hearingId).build());


        final Stream<Object> events = hearing.removeSelectedOffencesFromExistingHearing(hearingId, asList(offence1Id, offence2Id), null, null);
        assertThat(events.count(), is(0L));
    }

    @Test
    public void shouldAddDefendantsForCourtProceedings() {
        // Arrange
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId1 = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        // Create offences
        uk.gov.moj.cpp.listing.domain.Offence offence1 = uk.gov.moj.cpp.listing.domain.Offence.offence().withId(offenceId1)
                .withStatementOfOffence(uk.gov.moj.cpp.listing.domain.StatementOfOffence.statementOfOffence().withLegislation(Optional.empty()).build()).build();
        uk.gov.moj.cpp.listing.domain.Offence offence2 = uk.gov.moj.cpp.listing.domain.Offence.offence().withId(offenceId2)
                .withStatementOfOffence(uk.gov.moj.cpp.listing.domain.StatementOfOffence.statementOfOffence().withLegislation(Optional.empty()).build()).build();

        // Create defendants
        uk.gov.moj.cpp.listing.domain.Defendant defendant1 = uk.gov.moj.cpp.listing.domain.Defendant.defendant().withId(defendantId1).withOffences(singletonList(offence1)).build();
        uk.gov.moj.cpp.listing.domain.Defendant defendant2 = uk.gov.moj.cpp.listing.domain.Defendant.defendant().withId(defendantId2).withOffences(singletonList(offence2)).build();


        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(caseId)
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(defendantId1)
                                        .withOffences(new ArrayList(asList(Offence.offence().withId(offenceId1)
                                                .withStatementOfOffence(StatementOfOffence.statementOfOffence().withLegislation("").build()).build())))
                                        .build())))
                                .build()))
                        .build())
                .build());

        // Act
        List<NewDefendantAddedForCourtProceedings> events = hearing.addDefendantsForCourtProceedings(caseId, asList(defendant1, defendant2)).map(o -> (NewDefendantAddedForCourtProceedings)o).toList();

        // Assert
        assertThat(events.size(), is(1));
        assertThat(events.get(0).getHearingId(), is(hearingId));
        assertThat(events.get(0).getDefendant().getId(), is(defendantId2));
    }

    private uk.gov.justice.listing.events.Hearing prepareHearing(final UUID hearingId, final Boolean allocated,  final Map<UUID,List<Map<UUID, List<Map<UUID, Optional<UUID>>>>>> cases){
        return uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withType(uk.gov.justice.listing.events.Type.type().build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(CROWN)
                .withHearingDays(emptyList())
                .withCourtRoomId(randomUUID())
                .withEndDate(now().plusDays(1))
                .withStartDate(now())
                .withEstimatedMinutes(30)
                .withEstimatedDuration("30 minutes")
                .withAllocated(allocated)
                .withListedCases(cases.entrySet().stream().map(pcase -> uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(pcase.getKey())
                        .withDefendants(pcase.getValue().stream().flatMap(v -> v.entrySet().stream())
                                .map(defendant -> Defendant.defendant()
                                .withId(defendant.getKey())
                                .withOffences(defendant.getValue().stream().flatMap(v -> v.entrySet().stream())
                                        .map(offence -> Offence.offence()
                                                .withId(offence.getKey())
                                                .withSeedingHearing(offence.getValue().isEmpty() ? null : SeedingHearing.seedingHearing()
                                                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                                                        .withSeedingHearingId(offence.getValue().get())
                                                        .build())
                                                .build())
                                        .collect(Collectors.toList()))
                                        .build()).collect(Collectors.toList()))
                        .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Test
    void shouldChangeJudiciaryStatusForHearingsStatus(){
        final List<Object> eventStreams = hearing.judiciaryChangedForHearingsStatus().toList();
        assertThat(eventStreams, hasSize(1));
        assertThat(((JudiciaryChangedForHearingsStatus)eventStreams.get(0)).getStatus(), is("Success"));
    }

    @Test
    public void shouldSetFieldWhenOffenceAddedToCase(){
        final UUID case1Id = randomUUID();
        final UUID case1Defendant1Id = randomUUID();
        final UUID case1Defendant1Offence1Id = randomUUID();
        final UUID case1Defendant1Offence2Id = randomUUID();
        final SeedingHearing seedingHearing = SeedingHearing.seedingHearing()
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                .withSeedingHearingId(randomUUID())
                .build();

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
                                        .withCaseReference("caseReference1")
                                        .build())
                                .withDefendants(new ArrayList<>(asList(Defendant.defendant()
                                        .withId(case1Defendant1Id)
                                        .withOffences(new ArrayList<>(asList(Offence.offence()
                                                .withId(case1Defendant1Offence1Id)
                                                .withSeedingHearing(seedingHearing)
                                                .build())))
                                        .build())))
                                .build()))
                        .build()).build());


        hearing.addOffences(case1Id, case1Defendant1Id, asList(uk.gov.moj.cpp.listing.domain.Offence.offence()
                .withId(case1Defendant1Offence2Id)
                .withStatementOfOffence(uk.gov.moj.cpp.listing.domain.StatementOfOffence.statementOfOffence().build())
                .build()));

        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().stream()
                .filter( o-> o.getId().equals(case1Id))
                .map(uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds::getDefendants)
                .flatMap(Collection::stream).filter(def -> def.getId().equals(case1Defendant1Id))
                .map(uk.gov.moj.cpp.listing.domain.DefendantOffenceIds::getOffences)
                .flatMap(Collection::stream)
                .filter(off -> Objects.nonNull(off.getIsNewOffence()))
                .anyMatch(uk.gov.moj.cpp.listing.domain.OffenceIds::getIsNewOffence), is(true));
    }

    @Test
    public void shouldNotRemoveAllocatedNextHearingWhenSeedingHearingAmendedAndNextHearingWasExtendedWithAnotherCase() {
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence3Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId))))))))
                .build());

        hearing.addCasesToHearing(asList(ProsecutionCase.prosecutionCase()
                .withId(case2Id)
                .withDefendants(asList(uk.gov.justice.core.courts.Defendant.defendant()
                        .withId(defendant2Id)
                        .withOffences(asList(uk.gov.justice.core.courts.Offence.offence().withId(offence3Id)
                                .build()))
                        .build()))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityReference(STRING.next())
                        .build())
                .build()
        ), null, empty());

        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId(), is(asList(case1Id)));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(1));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(false));
    }

    @Test
    public void shouldNotRemoveAllocatedNextHearingWhenSeedingHearingAmendedAndNextHearingWasExtendedWithTheSameCase() {
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.empty())))))))
                .build());

        hearing.addCasesToHearing(asList(ProsecutionCase.prosecutionCase()
                .withId(case1Id)
                .withDefendants(asList(uk.gov.justice.core.courts.Defendant.defendant()
                        .withId(defendant2Id)
                        .withOffences(asList(uk.gov.justice.core.courts.Offence.offence().withId(offence3Id)
                                .build()))
                        .build()))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityReference(STRING.next())
                        .build())
                .build()
        ), null, empty());

        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId().isEmpty(), is(true));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(1));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(false));
    }

    @Test
    public void shouldRemoveOnlySeedingOffencesWhenHearingExtendedAndResultedByMultipleHearing() {
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID case3Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID defendant3Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence3Id = randomUUID();
        final UUID offence5Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId))))),
                                case2Id, asList(Map.of(defendant2Id, asList(Map.of(offence3Id, Optional.of(seedingHearingId2))))))))
                .build());

        hearing.addCasesToHearing(asList(ProsecutionCase.prosecutionCase()
                .withId(case3Id)
                .withDefendants(asList(uk.gov.justice.core.courts.Defendant.defendant()
                        .withId(defendant3Id)
                        .withOffences(asList(uk.gov.justice.core.courts.Offence.offence().withId(offence5Id)
                                .build()))
                        .build()))
                .withProsecutionCaseIdentifier(prosecutionCaseIdentifier()
                        .withProsecutionAuthorityCode(STRING.next())
                        .withProsecutionAuthorityId(randomUUID())
                        .withProsecutionAuthorityReference(STRING.next())
                        .build())
                .build()
        ), null, empty());

        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId(), is(asList(case1Id)));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(1));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(false));

        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().size(), is(2));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().stream().map(uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds::getId).toList(), hasItem(case2Id));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().stream().map(uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds::getId).toList(), hasItem(case3Id));

    }

    @Test
    public void shouldRemoveOnlySeedingOffencesWhenHearingExtendedWithTheSameCaseAndResultedByMultipleHearing() {
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();
        final UUID offence4Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.empty())))),
                                case2Id, asList(Map.of(defendant2Id, asList(Map.of(offence3Id, Optional.of(seedingHearingId2)), Map.of(offence4Id, Optional.empty())))))))
                .build());

        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId().isEmpty(), is(true));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(1));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(false));
    }

    @Test
    public void shouldDeleteNextHearingWhenNextHearingHasSeedingHearingsAndNewOffenceAdded(){
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId))))))))
                .build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case1Id).withDefendantId(defendant1Id).withOffence(Offence.offence()
                        .withId(offence2Id)
                        .build()).build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final AllocatedHearingDeleted offencesRemovedFromHearing = (AllocatedHearingDeleted) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIds().size(), is(1));
        assertThat(offencesRemovedFromHearing.getCaseIds(), hasItems(case1Id));
    }

    @Test
    public void shouldDeleteOffencesWhenNextHearingHasMultipleSeedingHearingsOneCaseAndNewOffenceAdded(){
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offenceNewId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.of(seedingHearingId2))))))))
                .build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case1Id).withDefendantId(defendant1Id).withOffence(Offence.offence()
                        .withId(offenceNewId)
                        .build()).build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(1));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id));
        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().get(0).getDefendants().size(), is(1));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().get(0).getDefendants().get(0).getOffences().size(), is(2));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().get(0).getDefendants().get(0).getOffences().stream().map(uk.gov.moj.cpp.listing.domain.OffenceIds::getId).toList(), hasItems(offence2Id, offence2Id ));
    }

    @Test
    public void shouldDeleteOffencesWhenNextHearingHasMultipleSeedingHearingsOneCaseMultipleDefendantsAndNewOffenceAdded(){
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();
        final UUID offence4Id = randomUUID();
        final UUID offenceNewId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.of(seedingHearingId2))),
                                defendant2Id, asList(Map.of(offence3Id, Optional.of(seedingHearingId)), Map.of(offence4Id, Optional.of(seedingHearingId2))))))))
                .build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case1Id).withDefendantId(defendant1Id).withOffence(Offence.offence()
                        .withId(offenceNewId)
                        .build()).build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(2));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence3Id));
        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().get(0).getDefendants().size(), is(2));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().get(0).getDefendants().stream().filter(def -> def.getId().equals(defendant1Id)).findFirst().orElseThrow().getOffences().size(), is(2));
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds().get(0).getDefendants().stream().filter(def -> def.getId().equals(defendant2Id)).findFirst().orElseThrow().getOffences().size(), is(1));
    }

    @Test
    public void shouldDeleteNextHearingWhenNextHearingHasSeedingHearingsMultipleCasesAndNewOffenceAdded(){
        final UUID seedingHearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offence3Id = randomUUID();
        final UUID offence4Id = randomUUID();
        final UUID offenceNewId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.of(seedingHearingId))))),
                                case2Id, asList(Map.of(defendant2Id, asList(Map.of(offence3Id, Optional.of(seedingHearingId)), Map.of(offence4Id, Optional.of(seedingHearingId))))))))
                .build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case1Id).withDefendantId(defendant1Id).withOffence(Offence.offence()
                        .withId(offenceNewId)
                        .build()).build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final AllocatedHearingDeleted offencesRemovedFromHearing = (AllocatedHearingDeleted) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIds().size(), is(2));
        assertThat(offencesRemovedFromHearing.getCaseIds(), hasItems(case1Id, case2Id));
    }

    @Test
    public void shouldRemoveSeedingOffencesANdNewOffenceWhenNextHearingHasMultipleSeedingHearingsCaseAndNewOffenceAdded(){
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offenceNewId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.of(seedingHearingId))))),
                                case2Id, asList(Map.of(defendant2Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId2)), Map.of(offence2Id, Optional.of(seedingHearingId2))))))))
                .build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case1Id).withDefendantId(defendant1Id).withOffence(Offence.offence()
                        .withId(offenceNewId)
                        .build()).build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case2Id).withDefendantId(defendant2Id).withOffence(Offence.offence()
                        .withId(randomUUID())
                        .build()).build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId(), hasItems(case1Id));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(3));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id, offence1Id, offenceNewId));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(false));
    }

    @Test
    public void shouldRemoveSeedingOffencesAndNewOffenceWhenNextHearingHasSeedingHearingAndExtendedByHearingAndNewOffenceAdded(){
        final UUID seedingHearingId = randomUUID();
        final UUID seedingHearingId2 = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();
        final UUID defendant1Id = randomUUID();
        final UUID defendant2Id = randomUUID();
        final UUID offence1Id = randomUUID();
        final UUID offence2Id = randomUUID();
        final UUID offenceNewId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(prepareHearing(hearingId, true,
                        Map.of( case1Id, asList(Map.of(defendant1Id, asList(Map.of(offence1Id, Optional.of(seedingHearingId)), Map.of(offence2Id, Optional.of(seedingHearingId))))),
                                case2Id, asList(Map.of(defendant2Id, asList(Map.of(offence1Id, Optional.empty()), Map.of(offence2Id, Optional.empty())))))))
                .build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case1Id).withDefendantId(defendant1Id).withOffence(Offence.offence()
                        .withId(offenceNewId)
                        .build()).build());

        hearing.apply(OffenceAdded.offenceAdded().withHearingId(hearingId)
                .withCaseId(case2Id).withDefendantId(defendant2Id).withOffence(Offence.offence()
                        .withId(randomUUID())
                        .build()).build());


        final Stream<Object> deleteHearingStream = hearing.deleteHearing(seedingHearingId, hearingId);

        final List<Object> deleteHearingEventsList = deleteHearingStream.collect(Collectors.toList());
        assertThat(deleteHearingEventsList.size(), is(2));

        final OffencesRemovedFromHearing offencesRemovedFromHearing = (OffencesRemovedFromHearing) deleteHearingEventsList.get(0);

        assertThat(offencesRemovedFromHearing.getSeedingHearingId(), is(seedingHearingId));
        assertThat(offencesRemovedFromHearing.getHearingId(), is(hearingId));
        assertThat(offencesRemovedFromHearing.getCaseIdsSeededByOnlySeedingHearingId(), hasItems(case1Id));
        assertThat(offencesRemovedFromHearing.getSeededOffences().size(), is(3));
        assertThat(offencesRemovedFromHearing.getSeededOffences(), hasItems(offence1Id, offence2Id, offenceNewId));
        assertThat(offencesRemovedFromHearing.getUnallocated(), is(false));
    }

    // Unit tests for magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected method

    @Test
    void shouldReturnFalse_WhenCurrentHearingEventStateIsNull() {
        final UUID ejectedItemId = randomUUID();

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(false));
    }

    @Test
    void shouldReturnTrue_WhenJurisdictionTypeIsCrownAndOtherConditionsMatch() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(CROWN)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(true));
    }

    @Test
    void shouldReturnFalse_WhenStartDateIsInThePast() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().minusDays(1)) // Past date
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(false));
    }

    @Test
    void shouldReturnFalse_WhenStartDateIsNull() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(null) // Null start date should be allowed
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(false));
    }

    @Test
    void shouldReturnFalse_WhenHearingIsNotAllocated() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(false) // Not allocated
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(false));
    }

    @Test
    void shouldReturnTrue_WhenNoCasesAndNoApplications() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(null) // No cases
                        .withCourtApplications(null) // No applications
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(true));
    }

    @Test
    void shouldReturnTrue_WhenAllCasesAreEjected() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(emptyList())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(emptyList())
                                        .withIsEjected(true) // Ejected
                                        .build()
                        )))
                        .withCourtApplications(null)
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(true));
    }

    @Test
    void shouldReturnFalse_WhenSomeCasesAreNotEjected() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(emptyList())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(emptyList())
                                        .withIsEjected(false) // Not ejected
                                        .build()
                        )))
                        .withCourtApplications(null)
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(false));
    }

    @Test
    void shouldReturnTrue_WhenAllApplicationsAreEjected() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID application1Id = randomUUID();
        final UUID application2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(null)
                        .withCourtApplications(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application1Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application2Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build()
                        )))
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(true));
    }

    @Test
    void shouldReturnTrue_WhenAllApplicationsAreEjectedAndStartDateIsToday() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID application1Id = randomUUID();
        final UUID application2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now())
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(null)
                        .withCourtApplications(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application1Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application2Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build()
                        )))
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(true));
    }

    @Test
    void shouldReturnFalse_WhenHearingResulted() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID application1Id = randomUUID();
        final UUID application2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now())
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(null)
                        .withCourtApplications(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application1Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application2Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build()
                        )))
                        .build())
                .build()
        );

        hearing.apply(HearingResultStatusUpdated.
                hearingResultStatusUpdated().
                withHearingId(hearingId).build());


        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(false));
    }

    @Test
    void shouldReturnFalse_WhenSomeApplicationsAreNotEjected() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID application1Id = randomUUID();
        final UUID application2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(null)
                        .withCourtApplications(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application1Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application2Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(false) // Not ejected
                                        .build()
                        )))
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(false));
    }

    @Test
    void shouldReturnTrue_WhenEjectedItemIsExcludedFromCaseCheck() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID case2Id = ejectedItemId; // This is the ejected item

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(emptyList())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(emptyList())
                                        .withIsEjected(false) // Not ejected, but this is the ejected item so should be excluded
                                        .build()
                        )))
                        .withCourtApplications(null)
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(true));
    }

    @Test
    void shouldReturnTrue_WhenEjectedItemIsExcludedFromApplicationCheck() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID application1Id = randomUUID();
        final UUID application2Id = ejectedItemId; // This is the ejected item

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(null)
                        .withCourtApplications(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application1Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application2Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(false) // Not ejected, but this is the ejected item so should be excluded
                                        .build()
                        )))
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(true));
    }

    @Test
    void shouldReturnTrue_WhenLinkedApplicationIsExcludedFromCheck() {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID application1Id = randomUUID();
        final UUID application2Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(null)
                        .withCourtApplications(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application1Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(true) // Ejected
                                        .build(),
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application2Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(false) // Not ejected, but linked to ejected case so should be excluded
                                        .withLinkedCaseIds(asList(ejectedItemId)) // Linked to the ejected item
                                        .build()
                        )))
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, is(true));
    }

    @Test
    void shouldReturnTrue_WhenAllConditionsAreMet() {
        validateMagistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(true, true, true);
    }

    @Test
    void shouldReturnFalse_WhenCasesAreEjectedButApplicationsAreNot() {
        validateMagistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(true, false, false);
    }

    @Test
    void shouldReturnFalse_WhenApplicationsAreEjectedButCasesAreNot() {
        validateMagistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(false, true, false);
    }

    private void validateMagistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(final boolean isCaseEjected, final boolean isApplicationEjected, final boolean expectedResult) {
        final UUID ejectedItemId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID case1Id = randomUUID();
        final UUID application1Id = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(randomUUID())
                        .withAllocated(true)
                        .withStartDate(LocalDate.now().plusDays(1))
                        .withEstimatedMinutes(30)
                        .withEstimatedDuration("30 minutes")
                        .withListedCases(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(emptyList())
                                        .withIsEjected(isCaseEjected) // Ejected
                                        .build()
                        )))
                        .withCourtApplications(new ArrayList<>(asList(
                                uk.gov.justice.listing.events.CourtApplication.courtApplication()
                                        .withId(application1Id)
                                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                                        .withIsEjected(isApplicationEjected) // Ejected
                                        .build()
                        )))
                        .build())
                .build()
        );

        boolean result = hearing.magistrateHearingIsInTheFutureAndAllCaseAndApplicationAreEjected(ejectedItemId);

        assertThat(result, CoreMatchers.is(expectedResult));
    }

    @Test
    void shouldRemoveCourtRoomRaiseThreeEvent_WhenAllConditionMeet() {
        final UUID courtRoomIdAssigned = randomUUID();
        final LocalDate futureStartDate = now().plusDays(5);

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withCourtRoomId(courtRoomIdAssigned)
                        .withStartDate(futureStartDate)
                        .withHearingDays(emptyList())
                        .withListedCases(emptyList())
                        .build())
                .build());

        final Stream<Object> result = hearing.removeCourtRoom(hearingId);
        final List<Object> eventsList = result.collect(Collectors.toList());

        assertThat(eventsList.size(), is(3));

        final HearingUnallocatedCourtroomRemoved eventHearingUnallocatedCourtroomRemoved = (HearingUnallocatedCourtroomRemoved) eventsList.get(0);
        final CourtRoomRemovedFromHearing eventCourtRoomRemovedFromHearing = (CourtRoomRemovedFromHearing) eventsList.get(1);
        final SequencesResetOnHearingDays eventSequencesResetOnHearingDays = (SequencesResetOnHearingDays) eventsList.get(2);

        assertThat(eventHearingUnallocatedCourtroomRemoved.getHearingId(), is(hearingId));
        assertThat(eventSequencesResetOnHearingDays.getHearingId(), is(hearingId));
        assertThat(eventCourtRoomRemovedFromHearing.getHearingId(), is(hearingId));
    }

    @Test
    void shouldNotRaiseHearingUnallocatedCourtroomRemoved_WhenHearingIsDuplicate() {
        final UUID courtRoomIdAssigned = randomUUID();
        final LocalDate futureStartDate = now().plusDays(5);

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withCourtRoomId(courtRoomIdAssigned)
                        .withStartDate(futureStartDate)
                        .withHearingDays(emptyList())
                        .withListedCases(emptyList())
                        .build())
                .build());

        hearing.apply(HearingMarkedAsDuplicate.hearingMarkedAsDuplicate().withHearingId(hearingId).build());

        final Stream<Object> result = hearing.removeCourtRoom(hearingId);
        final List<Object> eventsList = result.collect(Collectors.toList());

        assertThat(eventsList.size(), is(0));
    }

    @Test
    void shouldNotRaiseHearingUnallocatedCourtroomRemoved_WhenHearingIsDeleted() {
        final UUID courtRoomIdAssigned = randomUUID();
        final LocalDate futureStartDate = now().plusDays(5);

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withCourtRoomId(courtRoomIdAssigned)
                        .withStartDate(futureStartDate)
                        .withHearingDays(emptyList())
                        .withListedCases(emptyList())
                        .build())
                .build());

        hearing.apply(HearingDeleted.hearingDeleted().withHearingIdToBeDeleted(hearingId).build());

        final Stream<Object> result = hearing.removeCourtRoom(hearingId);
        final List<Object> eventsList = result.collect(Collectors.toList());

        assertThat(eventsList.size(), is(0));
    }

    @Test
    void shouldNotRaiseHearingUnallocatedCourtroomRemoved_WhenNoCourtRoomAssigned() {
        final LocalDate futureStartDate = now().plusDays(5);

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withStartDate(futureStartDate)
                        .withHearingDays(emptyList())
                        .withListedCases(emptyList())
                        .build())
                .build());

        final Stream<Object> result = hearing.removeCourtRoom(hearingId);
        final List<Object> eventsList = result.collect(Collectors.toList());

        assertThat(eventsList.size(), is(0));
    }

    @Test
    void shouldRemoveCourtRoomRaiseTwoEvent_WhenJurisdictionTypeIsNotCrown() {
        final UUID courtRoomIdAssigned = randomUUID();
        final LocalDate futureStartDate = now().plusDays(5);

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withCourtRoomId(courtRoomIdAssigned)
                        .withStartDate(futureStartDate)
                        .withHearingDays(emptyList())
                        .withListedCases(emptyList())
                        .build())
                .build());

        final Stream<Object> result = hearing.removeCourtRoom(hearingId);
        final List<Object> eventsList = result.collect(Collectors.toList());

        assertThat(eventsList.size(), is(2));
        final CourtRoomRemovedFromHearing eventCourtRoomRemovedFromHearing = (CourtRoomRemovedFromHearing) eventsList.get(0);
        final SequencesResetOnHearingDays eventSequencesResetOnHearingDays = (SequencesResetOnHearingDays) eventsList.get(1);
        assertThat(eventSequencesResetOnHearingDays.getHearingId(), is(hearingId));
        assertThat(eventCourtRoomRemovedFromHearing.getHearingId(), is(hearingId));
    }

    @Test
    void shouldRemoveCourtRoomRaiseTwoEvent_WhenHearingHasResulted() {
        final UUID courtRoomIdAssigned = randomUUID();
        final LocalDate futureStartDate = now().plusDays(5);

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withCourtRoomId(courtRoomIdAssigned)
                        .withStartDate(futureStartDate)
                        .withHearingDays(emptyList())
                        .withListedCases(emptyList())
                        .build())
                .build());

        hearing.apply(HearingResultStatusUpdated.hearingResultStatusUpdated().withHearingId(hearingId).build());

        final Stream<Object> result = hearing.removeCourtRoom(hearingId);
        final List<Object> eventsList = result.collect(Collectors.toList());

        assertThat(eventsList.size(), is(2));
        final CourtRoomRemovedFromHearing eventCourtRoomRemovedFromHearing = (CourtRoomRemovedFromHearing) eventsList.get(0);
        final SequencesResetOnHearingDays eventSequencesResetOnHearingDays = (SequencesResetOnHearingDays) eventsList.get(1);
        assertThat(eventSequencesResetOnHearingDays.getHearingId(), is(hearingId));
        assertThat(eventCourtRoomRemovedFromHearing.getHearingId(), is(hearingId));
    }

    @Test
    void shouldRemoveCourtRoomRaiseTwoEvent_WhenStartDateIsInThePast() {
        final UUID courtRoomIdAssigned = randomUUID();
        final LocalDate pastStartDate = now().minusDays(5);

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withCourtRoomId(courtRoomIdAssigned)
                        .withStartDate(pastStartDate)
                        .withHearingDays(emptyList())
                        .withListedCases(emptyList())
                        .build())
                .build());

        final Stream<Object> result = hearing.removeCourtRoom(hearingId);
        final List<Object> eventsList = result.collect(Collectors.toList());

        assertThat(eventsList.size(), is(2));
        final CourtRoomRemovedFromHearing eventCourtRoomRemovedFromHearing = (CourtRoomRemovedFromHearing) eventsList.get(0);
        final SequencesResetOnHearingDays eventSequencesResetOnHearingDays = (SequencesResetOnHearingDays) eventsList.get(1);
        assertThat(eventSequencesResetOnHearingDays.getHearingId(), is(hearingId));
        assertThat(eventCourtRoomRemovedFromHearing.getHearingId(), is(hearingId));

    }

    @Test
    void shouldRemoveCourtRoomRaiseThreeEvent_WhenStartDateIsToday() {
        final UUID courtRoomIdAssigned = randomUUID();
        final LocalDate todayStartDate = now();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(hearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withCourtRoomId(courtRoomIdAssigned)
                        .withStartDate(todayStartDate)
                        .withHearingDays(emptyList())
                        .withListedCases(emptyList())
                        .build())
                .build());

        final Stream<Object> result = hearing.removeCourtRoom(hearingId);
        final List<Object> eventsList = result.collect(Collectors.toList());

        assertThat(eventsList.size(), is(3));

        final HearingUnallocatedCourtroomRemoved eventHearingUnallocatedCourtroomRemoved = (HearingUnallocatedCourtroomRemoved) eventsList.get(0);
        final CourtRoomRemovedFromHearing eventCourtRoomRemovedFromHearing = (CourtRoomRemovedFromHearing) eventsList.get(1);
        final SequencesResetOnHearingDays eventSequencesResetOnHearingDays = (SequencesResetOnHearingDays) eventsList.get(2);

        assertThat(eventHearingUnallocatedCourtroomRemoved.getHearingId(), is(hearingId));
        assertThat(eventSequencesResetOnHearingDays.getHearingId(), is(hearingId));
        assertThat(eventCourtRoomRemovedFromHearing.getHearingId(), is(hearingId));
    }

    @Test
    void shouldAddCourtApplicationIdToConfirmedListWhenCourtApplicationAddedForHearing() {
        final UUID applicationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        applyHearingListedWithMinimalCase(caseId, defendantId, offenceId);
        hearing.onCourtApplicationAddedForHearing(createCourtApplicationAddedForHearingEvent(applicationId));

        final HearingAllocatedForListingV2 allocatedEvent = getAllocatedHearingEvent();

        assertThat(allocatedEvent, is(notNullValue()));
        assertThat(allocatedEvent.getCourtApplicationIds(), is(notNullValue()));
        assertThat(allocatedEvent.getCourtApplicationIds(), hasSize(1));
        assertThat(allocatedEvent.getCourtApplicationIds(), hasItem(applicationId));
    }

    @Test
    void shouldNotAddCourtApplicationIdWhenApplicationIsNull() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        applyHearingListedWithMinimalCase(caseId, defendantId, offenceId);
        hearing.onCourtApplicationAddedForHearing(createCourtApplicationAddedForHearingEvent(null));

        final HearingAllocatedForListingV2 allocatedEvent = getAllocatedHearingEvent();

        assertThat(allocatedEvent, is(notNullValue()));
        assertThat(allocatedEvent.getCourtApplicationIds(), is(nullValue()));
    }

    @Test
    void shouldNotAddDuplicateCourtApplicationIdWhenAlreadyInConfirmedList() {
        final UUID applicationId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        applyHearingListedWithCourtApplicationsAndCase(asList(applicationId), caseId, defendantId, offenceId);
        hearing.onCourtApplicationAddedForHearing(createCourtApplicationAddedForHearingEvent(applicationId));

        final HearingAllocatedForListingV2 allocatedEvent = getAllocatedHearingEvent();

        assertThat(allocatedEvent, is(notNullValue()));
        assertThat(allocatedEvent.getCourtApplicationIds(), is(notNullValue()));
        assertThat(allocatedEvent.getCourtApplicationIds(), hasSize(1));
        assertThat(allocatedEvent.getCourtApplicationIds(), hasItem(applicationId));
    }

    @Test
    void shouldAddMultipleCourtApplicationIdsToConfirmedList() {
        final UUID applicationId1 = randomUUID();
        final UUID applicationId2 = randomUUID();
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();

        applyHearingListedWithMinimalCase(caseId, defendantId, offenceId);
        hearing.onCourtApplicationAddedForHearing(createCourtApplicationAddedForHearingEvent(applicationId1));
        hearing.onCourtApplicationAddedForHearing(createCourtApplicationAddedForHearingEvent(applicationId2));

        final HearingAllocatedForListingV2 allocatedEvent = getAllocatedHearingEvent();

        assertThat(allocatedEvent, is(notNullValue()));
        assertThat(allocatedEvent.getCourtApplicationIds(), is(notNullValue()));
        assertThat(allocatedEvent.getCourtApplicationIds(), hasSize(2));
        assertThat(allocatedEvent.getCourtApplicationIds(), hasItems(applicationId1, applicationId2));
    }

    private void applyBasicHearingListed() {
        hearing.apply(createBasicHearingListedEvent());
    }

    private void applyHearingListedWithMinimalCase(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        hearing.apply(createHearingListedEventWithCase(caseId, defendantId, offenceId));
    }

    private void applyHearingListedWithCourtApplicationsAndCase(final List<UUID> applicationIds, final UUID caseId, final UUID defendantId, final UUID offenceId) {
        hearing.apply(createHearingListedEventWithCourtApplicationsAndCase(applicationIds, caseId, defendantId, offenceId));
    }

    private void applyHearingListedWithCourtApplications(final List<UUID> applicationIds) {
        hearing.apply(createHearingListedEventWithCourtApplications(applicationIds));
    }

    private HearingListed createBasicHearingListedEvent() {
        return HearingListed.hearingListed()
                .withHearing(createBasicHearingEvent())
                .build();
    }

    private HearingListed createHearingListedEventWithCase(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return HearingListed.hearingListed()
                .withHearing(createHearingEventWithCase(caseId, defendantId, offenceId))
                .build();
    }

    private HearingListed createHearingListedEventWithCourtApplicationsAndCase(final List<UUID> applicationIds, final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return HearingListed.hearingListed()
                .withHearing(createHearingEventWithCourtApplicationsAndCase(applicationIds, caseId, defendantId, offenceId))
                .build();
    }

    private HearingListed createHearingListedEventWithCourtApplications(final List<UUID> applicationIds) {
        return HearingListed.hearingListed()
                .withHearing(createHearingEventWithCourtApplications(applicationIds))
                .build();
    }

    private uk.gov.justice.listing.events.Hearing createBasicHearingEvent() {
        return createHearingEventWithCourtApplications(emptyList());
    }

    private uk.gov.justice.listing.events.Hearing createHearingEventWithCase(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        return uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withType(uk.gov.justice.listing.events.Type.type().build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
                .withCourtRoomId(randomUUID())
                .withStartDate(LocalDate.now().plusDays(1))
                .withEndDate(LocalDate.now().plusDays(2))
                .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(caseId)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(asList(Offence.offence()
                                        .withId(offenceId)
                                        .build()))
                                .build()))
                        .build())))
                .build();
    }

    private uk.gov.justice.listing.events.Hearing createHearingEventWithCourtApplicationsAndCase(final List<UUID> applicationIds, final UUID caseId, final UUID defendantId, final UUID offenceId) {
        final List<uk.gov.justice.listing.events.CourtApplication> courtApplications = new ArrayList<>(applicationIds.stream()
                .map(id -> uk.gov.justice.listing.events.CourtApplication.courtApplication()
                        .withId(id)
                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                        .build())
                .collect(Collectors.toList()));

        return uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withType(uk.gov.justice.listing.events.Type.type().build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                .withHearingDays(asList(HearingDay.hearingDay().withCourtScheduleId(randomUUID()).build()))
                .withCourtRoomId(randomUUID())
                .withStartDate(LocalDate.now().plusDays(1))
                .withEndDate(LocalDate.now().plusDays(2))
                .withListedCases(new ArrayList<>(asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                        .withId(caseId)
                        .withDefendants(asList(Defendant.defendant()
                                .withId(defendantId)
                                .withOffences(asList(Offence.offence()
                                        .withId(offenceId)
                                        .build()))
                                .build()))
                        .build())))
                .withCourtApplications(courtApplications)
                .build();
    }

    private uk.gov.justice.listing.events.Hearing createHearingEventWithCourtApplications(final List<UUID> applicationIds) {
        if (applicationIds.isEmpty()) {
            return uk.gov.justice.listing.events.Hearing.hearing()
                    .withId(hearingId)
                    .withType(uk.gov.justice.listing.events.Type.type().build())
                    .withHearingLanguage(HearingLanguage.ENGLISH)
                    .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                    .withHearingDays(emptyList())
                    .withListedCases(emptyList())
                    .build();
        }

        final List<uk.gov.justice.listing.events.CourtApplication> courtApplications = new ArrayList<>(applicationIds.stream()
                .map(id -> uk.gov.justice.listing.events.CourtApplication.courtApplication()
                        .withId(id)
                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                        .build())
                .collect(Collectors.toList()));

        return uk.gov.justice.listing.events.Hearing.hearing()
                .withId(hearingId)
                .withType(uk.gov.justice.listing.events.Type.type().build())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                .withHearingDays(emptyList())
                .withListedCases(emptyList())
                .withCourtApplications(courtApplications)
                .build();
    }

    private CourtApplicationAddedForHearing createCourtApplicationAddedForHearingEvent(final UUID applicationId) {
        final uk.gov.justice.listing.events.CourtApplication courtApplication = applicationId != null
                ? uk.gov.justice.listing.events.CourtApplication.courtApplication()
                        .withId(applicationId)
                        .withApplicant(ApplicantRespondent.applicantRespondent().build())
                        .build()
                : null;

        return CourtApplicationAddedForHearing.courtApplicationAddedForHearing()
                .withHearingId(hearingId)
                .withCourtApplication(courtApplication)
                .build();
    }

    private HearingAllocatedForListingV2 getAllocatedHearingEvent() {
        final Stream<Object> allocationEvents = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(), empty(), null)).flatMap(i -> i);
        final List<Object> events = allocationEvents.collect(Collectors.toList());
        if (events.isEmpty()) {
            return null;
        }
        return events.stream()
                .filter(HearingAllocatedForListingV2.class::isInstance)
                .map(HearingAllocatedForListingV2.class::cast)
                .findFirst()
                .orElse(null);
    }

    // Unit tests for setHearingResultStatus method

    @Test
    void shouldReturnHearingResultStatusUpdatedEvent_WhenCurrentHearingEventStateIsNotNull() {
        // Given
        final UUID testHearingId = randomUUID();

        // Initialize hearing with HearingListed event to set currentHearingEventState
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(testHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .build())
                .build());

        // When
        final Stream<Object> events = hearing.setHearingResultStatus(testHearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());

        // Then
        assertThat(eventsList.size(), is(1));
        assertThat(eventsList.get(0), CoreMatchers.instanceOf(HearingResultStatusUpdated.class));

        final HearingResultStatusUpdated event = (HearingResultStatusUpdated) eventsList.get(0);
        assertThat(event.getHearingId(), is(testHearingId));
    }

    @Test
    void shouldReturnEmptyStream_WhenCurrentHearingEventStateIsNull() {
        // Given
        final UUID testHearingId = randomUUID();
        // Hearing aggregate is not initialized, so currentHearingEventState is null

        // When
        final Stream<Object> events = hearing.setHearingResultStatus(testHearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());

        // Then
        assertThat(eventsList.size(), is(0));
    }

    @Test
    void shouldSetResultedFlagToTrue_WhenHearingResultStatusUpdatedEventIsApplied() {
        // Given
        final UUID testHearingId = randomUUID();

        // Initialize hearing with HearingListed event
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(testHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .build())
                .build());

        // When - set hearing result status
        final Stream<Object> events = hearing.setHearingResultStatus(testHearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());

        // Apply the event to verify resulted flag is set
        eventsList.forEach(event -> hearing.apply(event));

        // Then - verify that resulted flag prevents further operations
        // Attempting to delete a resulted hearing should return empty stream
        final Stream<Object> deleteEvents = hearing.deleteHearing(randomUUID(), testHearingId);
        final List<Object> deleteEventsList = deleteEvents.collect(Collectors.toList());
        assertThat(deleteEventsList.size(), is(0));
    }

    @Test
    void shouldReturnHearingResultStatusUpdatedEvent_WithCorrectHearingId() {
        // Given
        final UUID testHearingId = randomUUID();
        final UUID differentHearingId = randomUUID();

        // Initialize hearing with HearingListed event
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(testHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .build())
                .build());

        // When
        final Stream<Object> events = hearing.setHearingResultStatus(testHearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());

        // Then
        assertThat(eventsList.size(), is(1));
        final HearingResultStatusUpdated event = (HearingResultStatusUpdated) eventsList.get(0);
        assertThat(event.getHearingId(), is(testHearingId));
    }

    @Test
    void shouldReturnEmptyStream_WhenHearingIsDeleted() {
        // Given
        final UUID testHearingId = randomUUID();

        // Initialize hearing
        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(testHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .build())
                .build());

        // Delete the hearing (this sets currentHearingEventState to null)
        hearing.apply(HearingDeleted.hearingDeleted()
                .withHearingIdToBeDeleted(testHearingId)
                .build());

        // When
        final Stream<Object> events = hearing.setHearingResultStatus(testHearingId);
        final List<Object> eventsList = events.collect(Collectors.toList());

        // Then
        assertThat(eventsList.size(), is(0));
    }

    // ─── CROWN allocation with courtScheduleIds and isDraft tests ────────

    @Test
    void shouldAllocateCrownHearingWhenAllHearingDaysHaveCourtScheduleIdsAndNoneAreDraft() {
        final UUID crownHearingId = randomUUID();
        final UUID crownCourtRoomId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(crownHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(Arrays.asList(
                                HearingDay.hearingDay()
                                        .withCourtScheduleId(randomUUID())
                                        .withHearingDate(LocalDate.now().plusDays(5))
                                        .withIsDraft(false)
                                        .build()))
                        .withCourtRoomId(crownCourtRoomId)
                        .withStartDate(LocalDate.now().plusDays(5))
                        .withEndDate(LocalDate.now().plusDays(5))
                        .withEstimatedMinutes(240)
                        .withEstimatedDuration("240 minutes")
                        .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(randomUUID())
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build());

        final Stream<Object> allocationStream = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(), empty(), null)).flatMap(i -> i);
        final List<Object> allocationEvents = allocationStream.toList();

        assertThat(allocationEvents.size(), is(1));
        assertTrue(allocationEvents.get(0) instanceof HearingAllocatedForListingV2);
    }

    @Test
    void shouldNotAllocateCrownHearingWhenAnyHearingDayIsDraft() {
        final UUID crownHearingId = randomUUID();
        final UUID crownCourtRoomId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(crownHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(Arrays.asList(
                                HearingDay.hearingDay()
                                        .withCourtScheduleId(randomUUID())
                                        .withHearingDate(LocalDate.now().plusDays(5))
                                        .withIsDraft(true)  // draft session
                                        .build()))
                        .withCourtRoomId(crownCourtRoomId)
                        .withStartDate(LocalDate.now().plusDays(5))
                        .withEndDate(LocalDate.now().plusDays(5))
                        .withEstimatedMinutes(240)
                        .withEstimatedDuration("240 minutes")
                        .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(randomUUID())
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build());

        final Stream<Object> allocationStream = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(), empty(), null)).flatMap(i -> i);
        final List<Object> allocationEvents = allocationStream.toList();

        // Should NOT allocate because isDraft=true
        assertThat(allocationEvents.size(), is(0));
    }

    @Test
    void shouldNotAllocateCrownHearingWhenMultiDayAndOneHearingDayIsDraft() {
        final UUID crownHearingId = randomUUID();
        final UUID crownCourtRoomId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(crownHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(Arrays.asList(
                                HearingDay.hearingDay()
                                        .withCourtScheduleId(randomUUID())
                                        .withHearingDate(LocalDate.now().plusDays(5))
                                        .withIsDraft(false)
                                        .build(),
                                HearingDay.hearingDay()
                                        .withCourtScheduleId(randomUUID())
                                        .withHearingDate(LocalDate.now().plusDays(6))
                                        .withIsDraft(true)  // one day is draft
                                        .build()))
                        .withCourtRoomId(crownCourtRoomId)
                        .withStartDate(LocalDate.now().plusDays(5))
                        .withEndDate(LocalDate.now().plusDays(6))
                        .withEstimatedMinutes(720)
                        .withEstimatedDuration("720 minutes")
                        .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(randomUUID())
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build());

        final Stream<Object> allocationStream = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(), empty(), null)).flatMap(i -> i);
        final List<Object> allocationEvents = allocationStream.toList();

        // Should NOT allocate because one hearingDay has isDraft=true
        assertThat(allocationEvents.size(), is(0));
    }

    @Test
    void shouldNotAllocateCrownHearingWhenHearingDaysMissCourtScheduleIds() {
        final UUID crownHearingId = randomUUID();
        final UUID crownCourtRoomId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(crownHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(Arrays.asList(
                                HearingDay.hearingDay()
                                        .withHearingDate(LocalDate.now().plusDays(5))
                                        .withIsDraft(false)
                                        .build()))  // no courtScheduleId — must NOT allocate
                        .withCourtRoomId(crownCourtRoomId)
                        .withStartDate(LocalDate.now().plusDays(5))
                        .withEndDate(LocalDate.now().plusDays(5))
                        .withEstimatedMinutes(240)
                        .withEstimatedDuration("240 minutes")
                        .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(randomUUID())
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build());

        final Stream<Object> allocationStream = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(), empty(), null)).flatMap(i -> i);
        final List<Object> allocationEvents = allocationStream.toList();

        // Crown hearings without courtScheduleIds must NOT allocate
        assertThat(allocationEvents.size(), is(0));
    }

    @Test
    void shouldNotAllocateCrownHearingWhenHearingDaysEmpty() {
        final UUID crownHearingId = randomUUID();
        final UUID crownCourtRoomId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(crownHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(emptyList())
                        .withCourtRoomId(crownCourtRoomId)
                        .withStartDate(LocalDate.now().plusDays(5))
                        .withEndDate(LocalDate.now().plusDays(5))
                        .withEstimatedMinutes(240)
                        .withEstimatedDuration("240 minutes")
                        .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                .withId(randomUUID())
                                .withDefendants(Arrays.asList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withOffences(Arrays.asList(Offence.offence()
                                                .withId(randomUUID())
                                                .build()))
                                        .build()))
                                .build()))
                        .build())
                .build());

        final Stream<Object> allocationStream = Stream.of(hearing.applyAllocationRules(of(randomUUID()), true, true, emptyList(), empty(), null)).flatMap(i -> i);
        final List<Object> allocationEvents = allocationStream.toList();

        // Crown hearings with empty hearingDays must NOT allocate — no courtScheduleIds
        assertThat(allocationEvents.size(), is(0));
    }

    // ─── CROWN vacate-trial slot payback tests ───────────────────────────

    @Test
    void shouldEmitAvailableSlotsForHearingFreedWhenVacatingCrownTrialWithCourtScheduleIds() {
        final UUID crownHearingId = randomUUID();
        final UUID vacatingTrialReasonId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(crownHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(Arrays.asList(
                                HearingDay.hearingDay()
                                        .withCourtScheduleId(randomUUID())
                                        .withHearingDate(LocalDate.now().plusDays(5))
                                        .withIsDraft(false)
                                        .build()))
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(5))
                        .withEndDate(LocalDate.now().plusDays(5))
                        .withEstimatedMinutes(240)
                        .withEstimatedDuration("240 minutes")
                        .build())
                .build());

        final Stream<Object> events = hearing.hearingVacateTrial(Optional.of(vacatingTrialReasonId));
        final List<Object> eventsList = events.toList();

        // Should emit both HearingTrialVacated + AvailableSlotsForHearingFreed
        assertThat(eventsList.size(), is(2));
        assertTrue(eventsList.stream().anyMatch(e -> e instanceof uk.gov.justice.listing.events.HearingTrialVacated));
        assertTrue(eventsList.stream().anyMatch(AvailableSlotsForHearingFreed.class::isInstance));
    }

    @Test
    void shouldEmitAvailableSlotsForHearingFreedWhenVacatingCrownTrial() {
        final UUID crownHearingId = randomUUID();
        final UUID vacatingTrialReasonId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(crownHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(Arrays.asList(
                                HearingDay.hearingDay()
                                        .withHearingDate(LocalDate.now().plusDays(5))
                                        .build()))  // no courtScheduleId
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(5))
                        .withEstimatedMinutes(240)
                        .build())
                .build());

        final Stream<Object> events = hearing.hearingVacateTrial(Optional.of(vacatingTrialReasonId));
        final List<Object> eventsList = events.toList();

        // Should emit both HearingTrialVacated + AvailableSlotsForHearingFreed for Crown jurisdiction
        assertThat(eventsList.size(), is(2));
        assertTrue(eventsList.stream().anyMatch(e -> e instanceof uk.gov.justice.listing.events.HearingTrialVacated));
        assertTrue(eventsList.stream().anyMatch(AvailableSlotsForHearingFreed.class::isInstance));
    }

    @Test
    void shouldNotEmitAvailableSlotsForHearingFreedWhenVacatingCrownTrialWithoutReason() {
        final UUID crownHearingId = randomUUID();

        hearing.apply(HearingListed.hearingListed()
                .withHearing(uk.gov.justice.listing.events.Hearing.hearing()
                        .withId(crownHearingId)
                        .withType(uk.gov.justice.listing.events.Type.type().build())
                        .withHearingLanguage(HearingLanguage.ENGLISH)
                        .withJurisdictionType(uk.gov.justice.core.courts.JurisdictionType.CROWN)
                        .withHearingDays(Arrays.asList(
                                HearingDay.hearingDay()
                                        .withCourtScheduleId(randomUUID())
                                        .withHearingDate(LocalDate.now().plusDays(5))
                                        .build()))
                        .withCourtRoomId(randomUUID())
                        .withStartDate(LocalDate.now().plusDays(5))
                        .withEstimatedMinutes(240)
                        .build())
                .build());

        final Stream<Object> events = hearing.hearingVacateTrial(Optional.empty());  // no reason
        final List<Object> eventsList = events.toList();

        // Should only emit HearingTrialVacated (no slot freed because vacatingTrialReasonId is empty)
        assertThat(eventsList.size(), is(1));
        assertTrue(eventsList.get(0) instanceof uk.gov.justice.listing.events.HearingTrialVacated);
    }
}
