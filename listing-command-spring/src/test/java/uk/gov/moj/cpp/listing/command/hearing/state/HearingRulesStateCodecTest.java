package uk.gov.moj.cpp.listing.command.hearing.state;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.SeedingHearing;
import uk.gov.justice.listing.events.Type;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRulesState;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/** Every field of the command-side hearing state survives the {@code hearing_rules_state} JSON (ADR 015, P2.3.2). */
class HearingRulesStateCodecTest {

    private final HearingRulesStateCodec codec = new HearingRulesStateCodec();

    @Test
    void theStateOfAListedHearingRoundTrips() {
        final HearingRules rules = listedHearing();
        final HearingRulesState state = rules.exportState();

        final String json = codec.toJson(state);
        final HearingRulesState read = codec.fromJson(json);

        assertThat(codec.toJson(read)).isEqualTo(json);
        assertThat(read.hearingId()).isEqualTo(state.hearingId());
        assertThat(read.estimatedMinutes()).isEqualTo(20);
        assertThat(read.type()).isEqualTo(state.type());
        assertThat(read.judiciary()).hasSize(1);
        assertThat(read.judiciary().get(0).getIsDeputy()).isEqualTo(state.judiciary().get(0).getIsDeputy());
        assertThat(read.nonDefaultDays()).hasSize(1);
        assertThat(read.nonDefaultDays().get(0).getCourtScheduleId()).isEqualTo(state.nonDefaultDays().get(0).getCourtScheduleId());
        assertThat(read.hearingDays()).hasSize(2);
        assertThat(read.prosecutionCaseDefendantOffenceIds()).hasSize(1);
        assertThat(read.prosecutionCaseDefendantOffenceIds().get(0).getDefendants().get(0).getOffences().get(0).getSeedingHearing())
                .isEqualTo(state.prosecutionCaseDefendantOffenceIds().get(0).getDefendants().get(0).getOffences().get(0).getSeedingHearing());
        assertThat(read.prosecutionCaseDefendants()).isEqualTo(state.prosecutionCaseDefendants());
        assertThat(read.applicationOffenceIds()).isEqualTo(state.applicationOffenceIds());
        assertThat(read.confirmedCourtApplicationIds()).isEqualTo(state.confirmedCourtApplicationIds());
        assertThat(read.updateSlot()).isTrue();
        assertThat(read.hasAdjournmentDate()).isTrue();
    }

    @Test
    void theTombstoneRoundTrips() {
        final UUID hearingId = randomUUID();
        final HearingRulesState tombstone = new HearingRulesState(hearingId, true, false, null, null, null, null, null, null, null, null, null,
                null, false, false, List.of());

        final HearingRulesState read = codec.fromJson(codec.toJson(tombstone));

        assertThat(read.hearingId()).isEqualTo(hearingId);
        assertThat(read.duplicate()).isTrue();
        assertThat(read.deleted()).isFalse();
    }

    @Test
    void restoringTheStateGivesTheSameState() {
        final HearingRules rules = listedHearing();
        final HearingRulesState state = codec.fromJson(codec.toJson(rules.exportState()));

        final HearingRules restored = new HearingRules();
        restored.restore(rules.getCurrentHearingEventState(), new HearingRules.ViewstoreExtras(false, null, false, null), state);

        assertThat(codec.toJson(restored.exportState())).isEqualTo(codec.toJson(rules.exportState()));
        assertThat(restored.getCurrentHearingEventState()).isEqualTo(rules.getCurrentHearingEventState());
    }

    @Test
    void theRestoredListsCanBeChangedByTheRules() {
        final HearingRules rules = listedHearing();
        final HearingRulesState state = codec.fromJson(codec.toJson(rules.exportState()));

        state.prosecutionCaseDefendantOffenceIds().get(0).getDefendants().get(0).getOffences().clear();
        state.prosecutionCaseDefendants().values().iterator().next().clear();
        state.hearingDays().clear();
    }

    static HearingRules listedHearing() {
        final UUID hearingId = randomUUID();
        final UUID courtCentreId = randomUUID();
        final LocalDate startDate = LocalDate.now().plusDays(10);
        final UUID offenceId = randomUUID();
        final Hearing hearing = Hearing.hearing()
                .withId(hearingId)
                .withType(Type.type().withId(randomUUID()).withDescription("Trial").withWelshDescription("Treial").build())
                .withAllocated(false)
                .withAdjournedFromDate("2026-01-01")
                .withStartDate(startDate)
                .withEndDate(startDate.plusDays(1))
                .withEstimatedMinutes(1)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(randomUUID())
                .withHearingLanguage(HearingLanguage.ENGLISH)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withJudiciary(new ArrayList<>(List.of(JudicialRole.judicialRole()
                        .withJudicialId(randomUUID())
                        .withIsDeputy(true)
                        .withJudicialRoleType(JudicialRoleType.judicialRoleType().withJudiciaryType("Judge").withJudicialRoleTypeId(randomUUID()).build())
                        .build())))
                .withListedCases(new ArrayList<>(List.of(ListedCase.listedCase()
                        .withId(randomUUID())
                        .withDefendants(new ArrayList<>(List.of(Defendant.defendant()
                                .withId(randomUUID())
                                .withOffences(new ArrayList<>(List.of(Offence.offence()
                                        .withId(offenceId)
                                        .withSeedingHearing(SeedingHearing.seedingHearing().withSeedingHearingId(randomUUID())
                                                .withSittingDay("2026-01-01").withJurisdictionType(JurisdictionType.MAGISTRATES).build())
                                        .build())))
                                .build())))
                        .build())))
                .withCourtApplications(new ArrayList<>(List.of(CourtApplication.courtApplication()
                        .withId(randomUUID())
                        .withOffences(new ArrayList<>(List.of(Offence.offence().withId(randomUUID()).build())))
                        .build())))
                .withHearingDays(new ArrayList<>(List.of(day(startDate, courtCentreId, 0), day(startDate.plusDays(1), courtCentreId, 1))))
                .withNonDefaultDays(new ArrayList<>())
                .withNonSittingDays(new ArrayList<>())
                .build();
        final HearingRules rules = new HearingRules();
        rules.apply(Stream.of(
                HearingListed.hearingListed().withHearing(hearing).build(),
                NonDefaultDaysAssignedToHearing.nonDefaultDaysAssignedToHearing()
                        .withHearingId(hearingId)
                        .withNonDefaultDays(List.of(NonDefaultDay.nonDefaultDay()
                                .withCourtScheduleId(randomUUID().toString())
                                .withStartTime(startDate.atTime(10, 0).atZone(ZoneOffset.UTC))
                                .withDuration(60)
                                .build()))
                        .build()));
        return rules;
    }

    private static HearingDay day(final LocalDate date, final UUID courtCentreId, final int sequence) {
        final ZonedDateTime start = date.atTime(10, 0).atZone(ZoneOffset.UTC);
        return HearingDay.hearingDay()
                .withHearingDate(date)
                .withStartTime(start)
                .withEndTime(start.plusHours(1))
                .withDurationMinutes(60)
                .withSequence(sequence)
                .withCourtCentreId(courtCentreId)
                .withCourtScheduleId(randomUUID())
                .withIsCancelled(false)
                .withIsDraft(false)
                .build();
    }
}
