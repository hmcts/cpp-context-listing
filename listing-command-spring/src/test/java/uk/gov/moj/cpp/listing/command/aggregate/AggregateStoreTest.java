package uk.gov.moj.cpp.listing.command.aggregate;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.listing.events.HearingAddedToCase;
import uk.gov.moj.cpp.listing.command.recorder.EventRecorder;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;
import uk.gov.moj.cpp.listing.persistence.repository.snapshot.AggregateSnapshotJdbcRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class AggregateStoreTest {

    private final AggregateSnapshotJdbcRepository repository = mock(AggregateSnapshotJdbcRepository.class);
    private final EventRecorder recorder = mock(EventRecorder.class);
    private final AggregateStore store = new AggregateStore(repository, recorder);
    private final Metadata command = Metadata.builder().withId(randomUUID()).withName("listing.command.x").build();

    @Test
    void loadCreatesANewAggregateWhenThereIsNoSnapshot() {
        final UUID caseId = randomUUID();
        when(repository.findForUpdate(caseId, Case.class.getName())).thenReturn(Optional.empty());

        assertThat(store.load(caseId, Case.class).getHearingIds()).isEmpty();
    }

    @Test
    void updateLoadsWithALockAppliesTheCommandSavesTheStateAndRecordsTheEvents() {
        final UUID caseId = randomUUID();
        final UUID existingHearing = randomUUID();
        final UUID newHearing = randomUUID();
        final Case existing = new Case();
        existing.addHearing(caseId, existingHearing).toList();
        when(repository.findForUpdate(caseId, Case.class.getName())).thenReturn(Optional.of(AggregateStore.serialise(existing)));

        final List<Object> events = store.update(command, caseId, Case.class, aCase -> aCase.addHearing(caseId, newHearing));

        assertThat(events).singleElement().isInstanceOf(HearingAddedToCase.class);
        final ArgumentCaptor<byte[]> saved = ArgumentCaptor.forClass(byte[].class);
        final InOrder order = inOrder(repository, recorder);
        order.verify(repository).findForUpdate(caseId, Case.class.getName());
        order.verify(repository).save(eq(caseId), eq(Case.class.getName()), saved.capture());
        order.verify(recorder).recordEvents(command, caseId, events);
        assertThat(AggregateStore.deserialise(saved.getValue(), Case.class).getHearingIds()).containsExactly(existingHearing, newHearing);
    }

    @Test
    void updateOfAnAlreadyLoadedAggregateDoesNotReadAgain() {
        final UUID caseId = randomUUID();
        final Case aCase = new Case();

        store.update(command, caseId, aCase, c -> c.addHearing(caseId, randomUUID()));

        verify(repository).save(eq(caseId), eq(Case.class.getName()), any());
        verify(repository, org.mockito.Mockito.never()).findForUpdate(any(), any());
    }
}
