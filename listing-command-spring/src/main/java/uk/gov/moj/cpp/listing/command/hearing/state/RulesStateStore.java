package uk.gov.moj.cpp.listing.command.hearing.state;

import uk.gov.moj.cpp.listing.command.hearing.rules.ApplicationRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.ApplicationRulesState;
import uk.gov.moj.cpp.listing.command.hearing.rules.CaseRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.CaseRulesState;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRulesState;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository.Kind;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Loads and keeps the state of the Case, Application and SeedHearing rules (MBD refactor P2.3.3,
 * ADR 015): one JSON document per former aggregate id in the changeset 034 tables, mapped by field
 * ({@link HearingRulesStateCodec}). None of these aggregates had any viewstore state.
 *
 * <p>Each load first takes the instance's lock for the rest of the transaction (P2.4,
 * {@code RulesStateJdbcRepository#lock}), so concurrent commands on one case, application or
 * seeding hearing run one after the other (no lost update of the state document).
 */
public class RulesStateStore {

    private final RulesStateJdbcRepository repository;
    private final HearingRulesStateCodec codec = new HearingRulesStateCodec();
    private final Clock clock;

    public RulesStateStore(final RulesStateJdbcRepository repository, final Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public CaseRules loadCase(final UUID caseId) {
        final CaseRules rules = new CaseRules(caseId);
        repository.lock(Kind.CASE, caseId);
        repository.find(Kind.CASE, caseId).map(json -> codec.readDocument(json, CaseDocument.class).toState()).ifPresent(rules::restore);
        return rules;
    }

    public void saveCase(final CaseRules rules) {
        final CaseRulesState state = rules.exportState();
        if (state != null) {
            repository.save(Kind.CASE, state.caseId(), codec.writeDocument(CaseDocument.of(state)), ZonedDateTime.now(clock));
        }
    }

    public ApplicationRules loadApplication(final UUID applicationId) {
        final ApplicationRules rules = new ApplicationRules(applicationId);
        repository.lock(Kind.APPLICATION, applicationId);
        repository.find(Kind.APPLICATION, applicationId).map(json -> codec.readDocument(json, ApplicationDocument.class).toState()).ifPresent(rules::restore);
        return rules;
    }

    public void saveApplication(final ApplicationRules rules) {
        final ApplicationRulesState state = rules.exportState();
        if (state != null) {
            repository.save(Kind.APPLICATION, state.applicationId(), codec.writeDocument(ApplicationDocument.of(state)), ZonedDateTime.now(clock));
        }
    }

    public SeedHearingRules loadSeedHearing(final UUID seedingHearingId) {
        final SeedHearingRules rules = new SeedHearingRules(seedingHearingId);
        repository.lock(Kind.SEED_HEARING, seedingHearingId);
        repository.find(Kind.SEED_HEARING, seedingHearingId).map(json -> codec.readDocument(json, SeedHearingDocument.class).toState()).ifPresent(rules::restore);
        return rules;
    }

    public void saveSeedHearing(final SeedHearingRules rules) {
        final SeedHearingRulesState state = rules.exportState();
        if (state != null) {
            repository.save(Kind.SEED_HEARING, state.streamId(), codec.writeDocument(SeedHearingDocument.of(state)), ZonedDateTime.now(clock));
        }
    }

    // The stored documents: the state's fields by name (as HearingRulesStateCodec.Document).

    @SuppressWarnings({"java:S1104", "PMD.BeanMembersShouldSerialize"})
    static final class CaseDocument {
        UUID caseId;
        List<UUID> hearingIds;
        Map<UUID, uk.gov.justice.listing.events.Defendant> currentDefendants;
        List<uk.gov.justice.listing.events.Defendant> defendantsToBeUpdated;

        static CaseDocument of(final CaseRulesState state) {
            final CaseDocument d = new CaseDocument();
            d.caseId = state.caseId();
            d.hearingIds = state.hearingIds();
            d.currentDefendants = state.currentDefendants();
            d.defendantsToBeUpdated = state.defendantsToBeUpdated();
            return d;
        }

        CaseRulesState toState() {
            return new CaseRulesState(caseId, hearingIds, currentDefendants, defendantsToBeUpdated);
        }
    }

    @SuppressWarnings({"java:S1104", "PMD.BeanMembersShouldSerialize"})
    static final class ApplicationDocument {
        UUID applicationId;
        List<UUID> hearingIds;

        static ApplicationDocument of(final ApplicationRulesState state) {
            final ApplicationDocument d = new ApplicationDocument();
            d.applicationId = state.applicationId();
            d.hearingIds = state.hearingIds();
            return d;
        }

        ApplicationRulesState toState() {
            return new ApplicationRulesState(applicationId, hearingIds);
        }
    }

    @SuppressWarnings({"java:S1104", "PMD.BeanMembersShouldSerialize"})
    static final class SeedHearingDocument {
        UUID streamId;
        List<DayEntry> seededHearingIdsForHearingDay;
        List<DayEntry> existingHearingIdsForHearingDay;
        List<UUID> previousHearingIds;
        List<UUID> currentHearingIds;
        UUID seedingHearingId;

        static SeedHearingDocument of(final SeedHearingRulesState state) {
            final SeedHearingDocument d = new SeedHearingDocument();
            d.streamId = state.streamId();
            d.seededHearingIdsForHearingDay = DayEntry.of(state.seededHearingIdsForHearingDay());
            d.existingHearingIdsForHearingDay = DayEntry.of(state.existingHearingIdsForHearingDay());
            d.previousHearingIds = state.previousHearingIds();
            d.currentHearingIds = state.currentHearingIds();
            d.seedingHearingId = state.seedingHearingId();
            return d;
        }

        SeedHearingRulesState toState() {
            return new SeedHearingRulesState(streamId, DayEntry.toState(seededHearingIdsForHearingDay), DayEntry.toState(existingHearingIdsForHearingDay),
                    previousHearingIds, currentHearingIds, seedingHearingId);
        }
    }

    /** A sitting day (may be null) and its hearing ids. */
    @SuppressWarnings({"java:S1104", "PMD.BeanMembersShouldSerialize"})
    static final class DayEntry {
        String hearingDay;
        List<UUID> hearingIds;

        static List<DayEntry> of(final List<SeedHearingRulesState.HearingDayEntry> entries) {
            return entries == null ? null : entries.stream().map(e -> {
                final DayEntry d = new DayEntry();
                d.hearingDay = e.hearingDay();
                d.hearingIds = e.hearingIds();
                return d;
            }).toList();
        }

        static List<SeedHearingRulesState.HearingDayEntry> toState(final List<DayEntry> entries) {
            return entries == null ? null : entries.stream().map(e -> new SeedHearingRulesState.HearingDayEntry(e.hearingDay, e.hearingIds)).toList();
        }
    }
}
