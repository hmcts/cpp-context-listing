package uk.gov.moj.cpp.listing.command.aggregate;

import static org.assertj.core.api.Assertions.assertThat;

import uk.gov.justice.listing.events.HearingAddedToCase;
import uk.gov.moj.cpp.listing.domain.aggregate.Application;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;
import uk.gov.moj.cpp.listing.domain.aggregate.Hearing;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * The Spring build reads the aggregate snapshots the Phase 1 JEE app wrote (ADR 011/014).
 *
 * <p>{@code phase1-snapshots/*.ser} are {@code aggregate_snapshot.state} values copied on 2026-11-19
 * from a local cpp-developers-docker viewstore after a Phase 1 IT run (test data only: random
 * strings and ids from the IT builders, no case data). They were written by the JEE
 * {@code AggregateStore} (the Maven build of the same aggregate sources) and are read here with the
 * Gradle build's classes: same classes, packages and serialVersionUIDs, or this test fails.
 */
class AggregateSnapshotCompatibilityTest {

    @Test
    void readsAHearingSnapshotWrittenByThePhase1App() throws IOException {
        final Hearing hearing = AggregateStore.deserialise(snapshot("Hearing"), Hearing.class);

        assertThat(hearing.getCurrentHearingEventState()).isNotNull();
        assertThat(hearing.getCurrentHearingEventState().getId()).isNotNull();
        assertThat(hearing.getProsecutionCaseDefendantOffenceIds()).isNotEmpty();
        assertThat(roundTrip(hearing, Hearing.class).getCurrentHearingEventState().getId())
                .isEqualTo(hearing.getCurrentHearingEventState().getId());
    }

    @Test
    void readsACaseSnapshotWrittenByThePhase1AppAndKeepsApplyingCommands() throws IOException {
        final Case aCase = AggregateStore.deserialise(snapshot("Case"), Case.class);
        assertThat(aCase.getHearingIds()).hasSize(2);

        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final List<Object> events = aCase.addHearing(caseId, hearingId).toList();

        assertThat(events).first().isInstanceOf(HearingAddedToCase.class);
        assertThat(aCase.getHearingIds()).hasSize(3).contains(hearingId);
        assertThat(roundTrip(aCase, Case.class).getHearingIds()).containsExactlyElementsOf(aCase.getHearingIds());
    }

    @Test
    void readsAnApplicationSnapshotWrittenByThePhase1App() throws IOException {
        final Application application = AggregateStore.deserialise(snapshot("Application"), Application.class);

        assertThat(roundTrip(application, Application.class)).isNotNull();
    }

    private static <A extends uk.gov.justice.domain.aggregate.Aggregate> A roundTrip(final A aggregate, final Class<A> type) {
        return AggregateStore.deserialise(AggregateStore.serialise(aggregate), type);
    }

    private static byte[] snapshot(final String type) throws IOException {
        try (InputStream in = AggregateSnapshotCompatibilityTest.class.getResourceAsStream("/phase1-snapshots/" + type + ".ser")) {
            assertThat(in).as(type + ".ser").isNotNull();
            return in.readAllBytes();
        }
    }
}
