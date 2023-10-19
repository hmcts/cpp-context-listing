package uk.gov.moj.cpp.listing.event.processor.factory;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static uk.gov.justice.listing.events.Defendants.defendants;
import static uk.gov.justice.listing.events.HearingPartiallyUpdated.hearingPartiallyUpdated;
import static uk.gov.justice.listing.events.Offences.offences;
import static uk.gov.justice.listing.events.ProsecutionCases.prosecutionCases;

import uk.gov.justice.listing.events.HearingPartiallyUpdated;

import java.util.UUID;

import org.junit.Test;

public class PublicHearingPartiallyUpdatedFactoryTest {

    private PublicHearingPartiallyUpdatedFactory publicHearingPartiallyUpdatedFactory = new PublicHearingPartiallyUpdatedFactory();

    @Test
    public void shouldCreate() {
        final UUID caseId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final HearingPartiallyUpdated hearingPartiallyUpdated = hearingPartiallyUpdated()
                .withHearingIdToBeUpdated(hearingId)
                .withProsecutionCases(asList(prosecutionCases()
                        .withCaseId(caseId)
                        .withDefendants(asList(defendants()
                                .withDefendantId(defendantId)
                                .withOffences(asList(offences()
                                        .withOffenceId(offenceId)
                                        .build()))
                                .build()))
                        .build()))
                .build();

        final uk.gov.justice.listing.courts.HearingPartiallyUpdated result = publicHearingPartiallyUpdatedFactory.create(hearingPartiallyUpdated);
        assertThat(result.getHearingIdToBeUpdated(), is(hearingId));
        assertThat(result.getProsecutionCases(), hasSize(1));
        assertThat(result.getProsecutionCases().get(0).getCaseId(), is(caseId));
        assertThat(result.getProsecutionCases().get(0).getDefendants(), hasSize(1));
        assertThat(result.getProsecutionCases().get(0).getDefendants().get(0).getDefendantId(), is(defendantId));

    }

}