package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void shouldMergeDefendantFieldsWhenUpdatingExistingDefendantWithHearing() {
        final Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final UUID masterDefendantId = UUID.randomUUID();

        caseAggregate.addHearing(caseId, hearingId);

        caseAggregate.updateDefendant(caseId, Defendant.defendant()
                .withId(defendantId)
                .withFirstName(of("John"))
                .withLastName(of("Doe"))
                .withIsYouth(of(true))
                .withMasterDefendantId(of(masterDefendantId))
                .build());

        final List<Object> events = caseAggregate.updateDefendant(caseId, Defendant.defendant()
                .withId(defendantId)
                .withLastName(of("Smith"))
                .build()).toList();

        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(DefendantsToBeUpdated.class));

        final uk.gov.justice.listing.events.Defendant mergedDefendant =
                ((DefendantsToBeUpdated) events.get(0)).getDefendants().get(0);

        assertEquals(defendantId, mergedDefendant.getId());
        assertEquals("John", mergedDefendant.getFirstName());
        assertEquals("Smith", mergedDefendant.getLastName());
        assertTrue(Boolean.TRUE.equals(mergedDefendant.getIsYouth()));
        assertEquals(masterDefendantId, mergedDefendant.getMasterDefendantId());
        assertEquals(List.of(hearingId), ((DefendantsToBeUpdated) events.get(0)).getHearings());
    }

    @Test
    void shouldMergeDefendantFieldsWhenUpdatingExistingDefendantWithoutHearing() {
        final Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        caseAggregate.updateDefendant(caseId, Defendant.defendant()
                .withId(defendantId)
                .withFirstName(of("Jane"))
                .withLastName(of("Doe"))
                .withIsYouth(of(true))
                .build());

        final List<Object> events = caseAggregate.updateDefendant(caseId, Defendant.defendant()
                .withId(defendantId)
                .withLastName(of("Smith"))
                .build()).toList();

        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(DefendantsToBeUpdatedLater.class));

        final uk.gov.justice.listing.events.Defendant mergedDefendant =
                ((DefendantsToBeUpdatedLater) events.get(0)).getDefendants().get(0);

        assertEquals("Jane", mergedDefendant.getFirstName());
        assertEquals("Smith", mergedDefendant.getLastName());
        assertTrue(Boolean.TRUE.equals(mergedDefendant.getIsYouth()));
    }

    @Test
    void shouldRetainIsYouthWhenPartialUpdateDoesNotProvideIsYouth() {
        final Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        caseAggregate.addHearing(caseId, hearingId);

        caseAggregate.updateDefendant(caseId, Defendant.defendant()
                .withId(defendantId)
                .withFirstName(of("Youth"))
                .withIsYouth(of(true))
                .build());

        final uk.gov.justice.listing.events.Defendant mergedDefendant = extractDefendantFromUpdate(
                caseAggregate.updateDefendant(caseId, Defendant.defendant()
                        .withId(defendantId)
                        .withFirstName(of("Updated"))
                        .build()));

        assertEquals("Updated", mergedDefendant.getFirstName());
        assertTrue(Boolean.TRUE.equals(mergedDefendant.getIsYouth()));
    }

    @Test
    void shouldReplayMergedDefendantWhenHearingAddedAfterDeferredUpdate() {
        final Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        caseAggregate.updateDefendant(caseId, Defendant.defendant()
                .withId(defendantId)
                .withFirstName(of("Deferred"))
                .withLastName(of("Original"))
                .build());

        caseAggregate.updateDefendant(caseId, Defendant.defendant()
                .withId(defendantId)
                .withLastName(of("Merged"))
                .build());

        final List<Object> events = caseAggregate.addHearing(caseId, hearingId).toList();

        assertThat(events.size(), is(2));
        assertThat(events.get(0), instanceOf(HearingAddedToCase.class));
        assertThat(events.get(1), instanceOf(DefendantsToBeUpdated.class));

        final uk.gov.justice.listing.events.Defendant replayedDefendant =
                ((DefendantsToBeUpdated) events.get(1)).getDefendants().get(0);

        assertEquals("Deferred", replayedDefendant.getFirstName());
        assertEquals("Merged", replayedDefendant.getLastName());
    }

    @Test
    void shouldUseFullDefendantOnFirstUpdateWithoutMerge() {
        final Case caseAggregate = new Case();
        final UUID caseId = UUID.randomUUID();
        final UUID hearingId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();

        caseAggregate.addHearing(caseId, hearingId);

        final uk.gov.justice.listing.events.Defendant eventDefendant = extractDefendantFromUpdate(
                caseAggregate.updateDefendant(caseId, Defendant.defendant()
                        .withId(defendantId)
                        .withFirstName(of("First"))
                        .withLastName(of("Update"))
                        .build()));

        assertEquals(defendantId, eventDefendant.getId());
        assertEquals("First", eventDefendant.getFirstName());
        assertEquals("Update", eventDefendant.getLastName());
    }

    private static uk.gov.justice.listing.events.Defendant extractDefendantFromUpdate(final Stream<Object> eventStream) {
        final Object event = eventStream.toList().get(0);
        if (event instanceof DefendantsToBeUpdated defendantsToBeUpdated) {
            return defendantsToBeUpdated.getDefendants().get(0);
        }
        if (event instanceof DefendantsToBeUpdatedLater defendantsToBeUpdatedLater) {
            return defendantsToBeUpdatedLater.getDefendants().get(0);
        }
        throw new AssertionError("Unexpected event type: " + event.getClass());
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
