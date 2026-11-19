package uk.gov.moj.cpp.listing.command.hearing.state;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.command.hearing.rules.HearingRules;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.HearingRulesStateJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.HearingRulesStateJdbcRepository.StoredHearingRulesState;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.hearing.RulesStateJdbcRepository.Kind;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * P2.4 (ADR 3, ADR 013 open point): every load of a hearing / case / application / seeding hearing
 * takes the instance's lock before it reads anything, so commands and inbound events on the same
 * instance serialise; and a hearing listing never listed can be tombstoned (keyed by the stream id).
 */
class HearingStateStoreTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC);
    private final HearingRepository hearingRepository = mock(HearingRepository.class);
    private final HearingRulesStateJdbcRepository stateRepository = mock(HearingRulesStateJdbcRepository.class);
    private final HearingStateStore store = new HearingStateStore(hearingRepository, stateRepository, clock);

    @Test
    void theHearingIsLockedBeforeItsRowOrStateIsRead() {
        final UUID hearingId = randomUUID();
        when(stateRepository.find(hearingId)).thenReturn(Optional.empty());

        store.load(hearingId);

        final InOrder order = inOrder(stateRepository, hearingRepository);
        order.verify(stateRepository).lock(hearingId);
        order.verify(hearingRepository).findBy(hearingId);
        order.verify(stateRepository).find(hearingId);
    }

    @Test
    void caseApplicationAndSeedHearingAreLockedBeforeTheirStateIsRead() {
        final RulesStateJdbcRepository repository = mock(RulesStateJdbcRepository.class);
        when(repository.find(any(), any())).thenReturn(Optional.empty());
        final RulesStateStore rulesStore = new RulesStateStore(repository, clock);
        final UUID caseId = randomUUID();
        final UUID applicationId = randomUUID();
        final UUID seedingHearingId = randomUUID();

        rulesStore.loadCase(caseId);
        rulesStore.loadApplication(applicationId);
        rulesStore.loadSeedHearing(seedingHearingId);

        final InOrder order = inOrder(repository);
        order.verify(repository).lock(Kind.CASE, caseId);
        order.verify(repository).find(Kind.CASE, caseId);
        order.verify(repository).lock(Kind.APPLICATION, applicationId);
        order.verify(repository).find(Kind.APPLICATION, applicationId);
        order.verify(repository).lock(Kind.SEED_HEARING, seedingHearingId);
        order.verify(repository).find(Kind.SEED_HEARING, seedingHearingId);
    }

    @Test
    void aHearingNeverListedThatIsMarkedAsDuplicateIsKeptUnderTheStreamId() {
        final UUID hearingId = randomUUID();
        when(stateRepository.find(hearingId)).thenReturn(Optional.empty());
        final HearingRules rules = store.load(hearingId);

        assertThat(rules.markHearingAsDuplicate(hearingId, List.of()).toList()).hasSize(1);
        store.save(hearingId, rules);

        final ArgumentCaptor<StoredHearingRulesState> saved = ArgumentCaptor.forClass(StoredHearingRulesState.class);
        verify(stateRepository).save(saved.capture(), any());
        assertThat(saved.getValue().hearingId()).isEqualTo(hearingId);
        assertThat(saved.getValue().duplicate()).isTrue();
    }
}
