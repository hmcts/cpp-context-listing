package uk.gov.moj.cpp.listing.command.hearing.state;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.HearingAddedToCase;
import uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.moj.cpp.listing.command.hearing.flow.HearingFlow;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.rules.ApplicationRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.CaseRules;
import uk.gov.moj.cpp.listing.command.hearing.rules.SeedHearingRules;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository.Kind;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.StaticListableBeanFactory;

/**
 * The Case, Application and SeedHearing rules state (changeset 034, ADR 015 P2.3.3): what is kept
 * is what the JEE aggregate would have rebuilt from its stream, and the flow keeps it only when the
 * rules raised something.
 */
class RulesStateStoreTest {

    private final Map<String, String> rows = new HashMap<>();
    private final RulesStateJdbcRepository repository = inMemoryRepository(rows);
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC);
    private final RulesStateStore store = new RulesStateStore(repository, clock);
    private final Metadata command = Metadata.builder().withId(randomUUID()).withName("listing.command.x").build();

    @Test
    void theCaseHearingsAndThePendingDefendantUpdateRoundTrip() {
        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final CaseRules rules = store.loadCase(caseId);
        rules.updateDefendant(caseId, Defendant.defendant().withId(defendantId).withFirstName(of("Ann")).withLastName(of("Smith")).build()).toList();
        store.saveCase(rules);

        final CaseRules reloaded = store.loadCase(caseId);
        assertThat(reloaded.getHearingIds()).isEmpty();

        // The pending ("later") update is replayed to the first hearing, merged with the current defendant.
        final UUID hearingId = randomUUID();
        final List<Object> events = reloaded.addHearing(caseId, hearingId).toList();
        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isEqualTo(new HearingAddedToCase(caseId, hearingId));
        final DefendantsToBeUpdated replayed = (DefendantsToBeUpdated) events.get(1);
        assertThat(replayed.getHearings()).containsExactly(hearingId);
        assertThat(replayed.getDefendants().get(0).getId()).isEqualTo(defendantId);
        assertThat(replayed.getDefendants().get(0).getFirstName()).isEqualTo("Ann");

        store.saveCase(reloaded);
        final CaseRules again = store.loadCase(caseId);
        assertThat(again.getHearingIds()).containsExactly(hearingId);
        // the pending update was consumed: a second hearing gets no replay
        assertThat(again.addHearing(caseId, randomUUID()).toList()).hasSize(1);
    }

    @Test
    void aDuplicateHearingIsRemovedFromTheCaseAtOnce() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final CaseRules rules = store.loadCase(caseId);
        rules.addHearing(caseId, hearingId).toList();

        rules.markHearingAsDuplicate(hearingId, caseId).toList();

        assertThat(rules.getHearingIds()).isEmpty();
    }

    @Test
    void theApplicationHearingsKeepTheirOrderAndDuplicates() {
        final UUID applicationId = randomUUID();
        final UUID first = randomUUID();
        final UUID second = randomUUID();
        final ApplicationRules rules = store.loadApplication(applicationId);
        rules.addToHearing(applicationId, first).toList();
        rules.addToHearing(applicationId, second).toList();
        rules.addToHearing(applicationId, first).toList();
        store.saveApplication(rules);

        assertThat(store.loadApplication(applicationId).getHearingIds()).containsExactly(first, second, first);
    }

    @Test
    void theSeedStateRoundTripsIncludingANullSittingDay() {
        final UUID seedingHearingId = randomUUID();
        final UUID existingHearingId = randomUUID();
        final SeedHearingRules rules = store.loadSeedHearing(seedingHearingId);
        rules.requestUpdateExistingHearing(seedingHearingId, existingHearingId, null, List.of(), List.of()).toList();
        store.saveSeedHearing(rules);

        final SeedHearingRules reloaded = store.loadSeedHearing(seedingHearingId);
        final List<Object> events = reloaded.deleteNextHearings(seedingHearingId, null).toList();

        assertThat(events).containsExactly(RemoveOffencesFromExistingHearingRequested.removeOffencesFromExistingHearingRequested()
                .withHearingId(existingHearingId).withSeedingHearingId(seedingHearingId).build());
    }

    /**
     * The JEE bug kept for parity (SeedHearingAggregate:300): the existing hearing is not removed
     * from its sitting day, so deleting the next hearings again asks again to remove its offences.
     */
    @Test
    void anExistingHearingStaysUnderItsSittingDayAfterItsOffencesAreRemoved() {
        final UUID seedingHearingId = randomUUID();
        final UUID existingHearingId = randomUUID();
        final String day = LocalDate.of(2026, 11, 19).toString();
        final SeedHearingRules rules = store.loadSeedHearing(seedingHearingId);
        rules.apply(UpdateExistingHearingRequested.updateExistingHearingRequested().withHearingId(existingHearingId).withHearingDay(day)
                .withSeedingHearingId(seedingHearingId).build());

        assertThat(rules.deleteNextHearings(seedingHearingId, day).toList()).hasSize(1);
        store.saveSeedHearing(rules);

        assertThat(store.loadSeedHearing(seedingHearingId).deleteNextHearings(seedingHearingId, day).toList()).hasSize(1);
    }

    @Test
    void aSeededNextHearingIsDeletedOnlyOnce() {
        final UUID seedingHearingId = randomUUID();
        final String day = "2026-11-19";
        final SeedHearingRules rules = store.loadSeedHearing(seedingHearingId);
        rules.apply(uk.gov.justice.listing.events.NextHearingRequested.nextHearingRequested().withHearingDay(day)
                .withHearing(uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds().withId(randomUUID()).build()).build());
        final List<Object> first = rules.deleteNextHearings(seedingHearingId, day).toList();
        store.saveSeedHearing(rules);

        assertThat(first).hasSize(1).first().isInstanceOf(DeleteNextHearingRequested.class);
        assertThat(store.loadSeedHearing(seedingHearingId).deleteNextHearings(seedingHearingId, day).toList()).isEmpty();
    }

    @Test
    void theFlowKeepsTheStateOnlyWhenTheRulesRaisedSomething() {
        final RulesStateStore spied = org.mockito.Mockito.spy(store);
        final HearingFlow flow = new HearingFlow(null, spied, emptyListeners(), emptyProcessors(), new ProcessorQueue(), clock);
        final UUID caseId = randomUUID();

        flow.updateCase(command, caseId, rules -> rules.updateDefendantOffences(
                new uk.gov.moj.cpp.listing.domain.CaseOffences(caseId, randomUUID(), List.of())));
        verify(spied, never()).saveCase(any());

        flow.updateCase(command, caseId, rules -> rules.addHearing(caseId, randomUUID()));
        verify(spied).saveCase(any());
        assertThat(rows).containsKey("CASE:" + caseId);
    }

    @Test
    void anUnknownIdLoadsTheEmptyRules() {
        final RulesStateJdbcRepository none = mock(RulesStateJdbcRepository.class);
        when(none.find(any(), any())).thenReturn(empty());
        final RulesStateStore emptyStore = new RulesStateStore(none, clock);

        assertThat(emptyStore.loadCase(randomUUID()).getHearingIds()).isEmpty();
        assertThat(emptyStore.loadApplication(randomUUID()).getHearingIds()).isEmpty();
        final UUID seedId = randomUUID();
        assertThat(emptyStore.loadSeedHearing(seedId).exportState().streamId()).isEqualTo(seedId);
        final ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
        emptyStore.saveCase(emptyStore.loadCase(seedId));
        verify(none).save(eq(Kind.CASE), eq(seedId), json.capture(), any());
        assertThat(json.getValue()).contains("hearingIds");
    }

    private static RulesStateJdbcRepository inMemoryRepository(final Map<String, String> rows) {
        final RulesStateJdbcRepository repository = mock(RulesStateJdbcRepository.class);
        when(repository.find(any(), any())).thenAnswer(i -> Optional.ofNullable(rows.get(i.getArgument(0) + ":" + i.getArgument(1))));
        org.mockito.Mockito.doAnswer(i -> rows.put(i.getArgument(0) + ":" + i.getArgument(1), i.getArgument(2)))
                .when(repository).save(any(), any(), any(), any());
        return repository;
    }

    private static org.springframework.beans.factory.ObjectProvider<ListenerPort> emptyListeners() {
        final StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("l", (ListenerPort) listeners -> { });
        return factory.getBeanProvider(ListenerPort.class);
    }

    private static org.springframework.beans.factory.ObjectProvider<ProcessorPort> emptyProcessors() {
        final StaticListableBeanFactory factory = new StaticListableBeanFactory();
        factory.addBean("p", (ProcessorPort) processors -> { });
        return factory.getBeanProvider(ProcessorPort.class);
    }

    @SuppressWarnings("unused")
    private static Stream<Object> none() {
        return Stream.empty();
    }
}
