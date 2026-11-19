package uk.gov.moj.cpp.listing.common.aggregate;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.event.PublishCourtListRequested;
import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.outbox.EventRecorder;
import uk.gov.moj.cpp.listing.domain.aggregate.Application;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;
import uk.gov.moj.cpp.listing.domain.aggregate.PublishCourtListRequestAggregate;
import uk.gov.moj.cpp.listing.domain.aggregate.SeedHearingAggregate;
import uk.gov.moj.cpp.listing.persistence.repository.snapshot.AggregateSnapshotJdbcRepository;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateStoreTest {

    @Mock
    private AggregateSnapshotJdbcRepository aggregateSnapshotJdbcRepository;

    @Mock
    private EventRecorder eventRecorder;

    @InjectMocks
    private AggregateStore aggregateStore;

    /** Every migrated aggregate type must survive a snapshot round trip in its initial state. */
    @ParameterizedTest
    @ValueSource(classes = {Hearing.class, Case.class, Application.class, PublishCourtListRequestAggregate.class, SeedHearingAggregate.class})
    void newAggregatesRoundTrip(final Class<? extends Aggregate> type) throws Exception {
        final Aggregate aggregate = type.getDeclaredConstructor().newInstance();

        final Aggregate copy = AggregateStore.deserialise(AggregateStore.serialise(aggregate), type);

        assertThat(copy, instanceOf(type));
    }

    @Test
    void updateRunsTheCommandOnTheSnapshotSavesTheNewStateThenRecordsTheEvents() throws Exception {
        final UUID requestId = randomUUID();
        final JsonEnvelope command = mock(JsonEnvelope.class);
        when(aggregateSnapshotJdbcRepository.findForUpdate(requestId, PublishCourtListRequestAggregate.class.getName()))
                .thenReturn(Optional.empty());

        final List<Object> events = aggregateStore.update(command, requestId, PublishCourtListRequestAggregate.class,
                aggregate -> aggregate.recordCourtListRequested(requestId, randomUUID(), LocalDate.of(2026, 11, 23),
                        LocalDate.of(2026, 11, 27), PublishCourtListType.FIRM, ZonedDateTime.now(), true));

        assertThat(events, hasSize(1));
        assertThat(events.get(0), instanceOf(PublishCourtListRequested.class));

        final ArgumentCaptor<byte[]> saved = ArgumentCaptor.forClass(byte[].class);
        final InOrder inOrder = inOrder(aggregateSnapshotJdbcRepository, eventRecorder);
        inOrder.verify(aggregateSnapshotJdbcRepository).save(eq(requestId), eq(PublishCourtListRequestAggregate.class.getName()), saved.capture());
        inOrder.verify(eventRecorder).record(command, requestId, events);

        // state changed by the command (weekCommencing is set outside apply()) is in the snapshot
        final PublishCourtListRequestAggregate restored = AggregateStore.deserialise(saved.getValue(), PublishCourtListRequestAggregate.class);
        assertThat(weekCommencing(restored), is(true));
    }

    @Test
    void loadUsesTheSavedSnapshot() throws Exception {
        final UUID requestId = randomUUID();
        final PublishCourtListRequestAggregate original = new PublishCourtListRequestAggregate();
        original.recordCourtListRequested(requestId, randomUUID(), LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 27),
                PublishCourtListType.FIRM, ZonedDateTime.now(), true).forEach(event -> { });
        when(aggregateSnapshotJdbcRepository.findForUpdate(requestId, PublishCourtListRequestAggregate.class.getName()))
                .thenReturn(Optional.of(AggregateStore.serialise(original)));

        final PublishCourtListRequestAggregate loaded = aggregateStore.load(requestId, PublishCourtListRequestAggregate.class);

        assertThat(loaded, notNullValue());
        assertThat(weekCommencing(loaded), is(true));
    }

    private static boolean weekCommencing(final PublishCourtListRequestAggregate aggregate) throws Exception {
        final Field field = PublishCourtListRequestAggregate.class.getDeclaredField("weekCommencing");
        field.setAccessible(true);
        return field.getBoolean(aggregate);
    }
}
