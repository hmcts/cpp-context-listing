package uk.gov.moj.cpp.listing.common.aggregate.backfill;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.justice.listing.events.HearingAddedToCase;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.common.aggregate.AggregateStore;
import uk.gov.moj.cpp.listing.common.aggregate.backfill.StreamReplayer.StoredEvent;
import uk.gov.moj.cpp.listing.domain.aggregate.Case;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class StreamReplayerTest {

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final StreamReplayer replayer = new StreamReplayer(objectMapper);

    @Test
    void rebuildsACaseFromItsStreamInPositionOrder() throws Exception {
        final UUID caseId = randomUUID();
        final UUID firstHearing = randomUUID();
        final UUID secondHearing = randomUUID();

        // deliberately out of order: the replay sorts by position_in_stream
        final Optional<Aggregate> rebuilt = replayer.replay(List.of(
                event(2, "listing.events.hearing-added-to-case", new HearingAddedToCase(caseId, secondHearing)),
                event(1, "listing.events.hearing-added-to-case", new HearingAddedToCase(caseId, firstHearing)),
                new StoredEvent(3, "listing.events.some-event-no-aggregate-applies", "{}")));

        assertThat(rebuilt.orElseThrow(), instanceOf(Case.class));
        assertThat(((Case) rebuilt.get()).getHearingIds(), containsInAnyOrder(firstHearing, secondHearing));

        // and the result survives the snapshot round trip
        final Case restored = AggregateStore.deserialise(AggregateStore.serialise(rebuilt.get()), Case.class);
        assertThat(restored.getHearingIds(), containsInAnyOrder(firstHearing, secondHearing));
    }

    @Test
    void aStreamWithNoAppliedEventsHasNoAggregate() {
        final Optional<Aggregate> rebuilt = replayer.replay(List.of(
                new StoredEvent(1, "listing.events.not-applied-by-any-aggregate", "{}")));

        assertThat(rebuilt.isPresent(), is(false));
    }

    @Test
    void rejectsAStreamMixingEventsOfTwoAggregates() throws Exception {
        final UUID caseId = randomUUID();
        final List<StoredEvent> mixed = List.of(
                event(1, "listing.events.hearing-added-to-case", new HearingAddedToCase(caseId, randomUUID())),
                new StoredEvent(2, "listing.event.publish-court-list-requested", "{}"));

        assertThrows(IllegalStateException.class, () -> replayer.replay(mixed));
    }

    private StoredEvent event(final long position, final String name, final Object pojo) throws Exception {
        return new StoredEvent(position, name, objectMapper.writeValueAsString(pojo));
    }
}
