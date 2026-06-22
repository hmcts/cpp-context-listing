package uk.gov.moj.cpp.listing.domain.aggregate;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.DefendantsToBeUpdatedLater;
import uk.gov.justice.listing.events.HearingAddedToCase;
import uk.gov.moj.cpp.listing.domain.Defendant;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CaseAggregateTest {

    @Test
    public void shouldAddSameHearing() {
        Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();

        final Stream<Object> addedStreams = caseAggregate.addHearing(caseId, hearingId);
        assertThat(addedStreams.count(), is(1L));
        assertThat(caseAggregate.getHearingIds().size(), is(1));
    }


    @Test
    void shouldAddUpdateDefendantsLater() {
        Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();

        final List<Object> events = caseAggregate.updateDefendant(caseId, Defendant.defendant().withId(UUID.randomUUID()).build())
                .toList();
        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(DefendantsToBeUpdatedLater.class));
    }

    @Test
    void shouldAddUpdateDefendant() {
        Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();

        caseAggregate.addHearing(caseId, hearingId);
        final List<Object> events = caseAggregate.updateDefendant(caseId, Defendant.defendant().withId(UUID.randomUUID()).build())
                .toList();
        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(DefendantsToBeUpdated.class));
    }

    @Test
    public void shouldNotAddAlreadyAddedHearing() {
        Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        caseAggregate.apply(HearingAddedToCase.hearingAddedToCase().withHearingId(hearingId).withCaseId(caseId).build());

        final Stream<Object> addedStreams = caseAggregate.addHearing(caseId, hearingId);
        assertThat(addedStreams.count(), is(1L));
        assertThat(caseAggregate.getHearingIds().size(), is(1));
    }

    @Test
    public void shouldAddHearing() {
        Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        caseAggregate.apply(HearingAddedToCase.hearingAddedToCase().withHearingId(UUID.randomUUID()).withCaseId(UUID.randomUUID()).build());
        caseAggregate.apply(HearingAddedToCase.hearingAddedToCase().withHearingId(UUID.randomUUID()).withCaseId(UUID.randomUUID()).build());
        caseAggregate.apply(HearingAddedToCase.hearingAddedToCase().withHearingId(UUID.randomUUID()).withCaseId(UUID.randomUUID()).build());

        final Stream<Object> addedStreams = caseAggregate.addHearing(caseId, hearingId);

        assertThat(addedStreams.count(), is(1L));
        assertThat(caseAggregate.getHearingIds().size(), is(4));
    }

}
