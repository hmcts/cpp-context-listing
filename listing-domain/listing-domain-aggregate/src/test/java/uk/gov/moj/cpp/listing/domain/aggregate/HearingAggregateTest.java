package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.time.LocalDate.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.MAGISTRATES;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing;
import uk.gov.justice.listing.events.DefendantOffenceIds;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingLanguage;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIds;
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
    private final Boolean isCountBasedSlotSelected = false;
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

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, hearingTypeDuration,
                adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, nonDefaultDays, isCountBasedSlotSelected, isSlotsBooked);

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing hearing = hearingListed.getHearing();

        assertThat(hearing.getEndDate(), is(of(now().plusDays(6))));
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

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, hearingTypeDuration,
                adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, nonDefaultDays, isCountBasedSlotSelected, isSlotsBooked);

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing hearing = hearingListed.getHearing();

        assertThat(hearing.getEndDate(), is(empty()));
        assertThat(hearing.getStartDate(), is(empty()));
        assertThat(hearing.getHearingDays(), is(emptyList()));
        assertThat(hearing.getNonSittingDays(), is(emptyList()));
        assertThat(hearing.getNonDefaultDays(), is(emptyList()));
    }


    @Test
    public void shouldCalculateHearingDaysWithStartDateAndSingleNonDefaultDay() {
        startDate = ZonedDateTime.of(now(), defaultStartTime, UTC).plusDays(2);
        nonDefaultDays = Stream.of(
                NonDefaultDay.nonDefaultDay().withStartTime(ZonedDateTime.of(now(), preferredStartTime, UTC).plusDays(2)).withDuration(of(preferredDuration)).build())
                .collect(Collectors.toList());

        final Stream<Object> listedHearing = hearing.list(hearingId, type, estimateMinutes, listedCases, courtCentreId, judiciary, courtRoomId, listingDirections, jurisdictionType, prosecutorDatesToAvoid,
                reportingRestrictionReason, startDate, endDate, courtCentreDefaults, courtApplications, courtApplicationPartyListingNeeds, hearingTypeDuration,
                adjournedFromDate, weekCommencingStartDate, weekCommencingEndDate, weekCommencingDurationInWeeks, nonDefaultDays, isCountBasedSlotSelected, isSlotsBooked);

        final HearingListed hearingListed = (HearingListed) listedHearing.findFirst().get();
        final uk.gov.justice.listing.events.Hearing hearing = hearingListed.getHearing();

        assertThat(hearing.getEndDate(), is(of(now().plusDays(2))));
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
                        .withJurisdictionType(uk.gov.justice.listing.events.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(Arrays.asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(Arrays.asList(Offence.offence().withId(offence1Id).build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(Arrays.asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(Arrays.asList(Offence.offence().withId(offence2Id).build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(Arrays.asList(ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case1Id)
                        .withDefendants(Arrays.asList(DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant1Id)
                                .withOffenceIds(Arrays.asList(offence1Id))
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
                        .withJurisdictionType(uk.gov.justice.listing.events.JurisdictionType.MAGISTRATES)
                        .withHearingDays(emptyList())
                        .withListedCases(Arrays.asList(uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case1Id)
                                        .withDefendants(Arrays.asList(Defendant.defendant()
                                                .withId(defendant1Id)
                                                .withOffences(Arrays.asList(Offence.offence().withId(offence1Id).build()))
                                                .build()))
                                        .build(),
                                uk.gov.justice.listing.events.ListedCase.listedCase()
                                        .withId(case2Id)
                                        .withDefendants(Arrays.asList(Defendant.defendant()
                                                .withId(defendant2Id)
                                                .withOffences(Arrays.asList(Offence.offence().withId(offence2Id).build()))
                                                .build()))
                                        .build()))
                        .build())
                .build());

        hearing.apply(HearingAllocatedForListing.hearingAllocatedForListing()
                .withHearingId(hearingId)
                .withProsecutionCaseDefendantsOffenceIds(Arrays.asList(ProsecutionCaseDefendantOffenceIds.prosecutionCaseDefendantOffenceIds()
                        .withId(case1Id)
                        .withDefendants(Arrays.asList(DefendantOffenceIds.defendantOffenceIds()
                                .withId(defendant1Id)
                                .withOffenceIds(Arrays.asList(offence1Id))
                                .build()))
                        .build()))
                .build());

        final Stream<Object> listedHearing = hearing.updateDefendantLegalAidStatusForHearing(hearingId, case2Id, defendant2Id, legalAidStatus);

        assertThat(listedHearing.findFirst().isPresent(), is(false));
    }

}
