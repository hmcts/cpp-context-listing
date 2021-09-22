package uk.gov.moj.cpp.listing.command.utils;


import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.listing.courts.DefendantsToRemove;
import uk.gov.justice.listing.courts.OffencesToRemove;
import uk.gov.justice.listing.courts.ProsecutionCasesToRemove;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class ProsecutionCasesBuilderTest {

    @Test
    public void shouldBuildFromProsecutionCases() {

        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final List<ProsecutionCasesToRemove> prosecutionCasesToRemove = asList(ProsecutionCasesToRemove.prosecutionCasesToRemove()
                .withCaseId(caseId)
                .withDefendantsToRemove(asList(DefendantsToRemove.defendantsToRemove()
                        .withDefendantId(defendantId)
                        .withOffencesToRemove(asList(OffencesToRemove.offencesToRemove()
                                .withOffenceId(offenceId)
                                .build()))
                        .build()))
                .build());

        ProsecutionCasesBuilder prosecutionCasesBuilder = new ProsecutionCasesBuilder();
        List<uk.gov.justice.listing.events.ProsecutionCases> prosecutionCases = prosecutionCasesBuilder.buildEventProsecutionCasesToRemove(prosecutionCasesToRemove);

        assertThat(caseId, is(prosecutionCases.get(0).getCaseId()));
        assertThat(defendantId, is(prosecutionCases.get(0).getDefendants().get(0).getDefendantId()));
        assertThat(offenceId, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getOffenceId()));
        assertThat(prosecutionCases.size(), equalTo(1));
        assertThat(prosecutionCases.get(0).getDefendants().size(), equalTo(1));
        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().size(), equalTo(1));
    }

    @Test
    public void shouldBuildFromProsecutionCasesWithMultipleCases() {

        final UUID offenceId = randomUUID();
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID defendantId2 = randomUUID();
        final UUID caseId = randomUUID();
        final UUID caseId2 = randomUUID();

        final List<ProsecutionCasesToRemove> prosecutionCasesToRemove = asList(ProsecutionCasesToRemove.prosecutionCasesToRemove()
                        .withCaseId(caseId)
                        .withDefendantsToRemove(asList(DefendantsToRemove.defendantsToRemove()
                                .withDefendantId(defendantId)
                                .withOffencesToRemove(asList(OffencesToRemove.offencesToRemove()
                                        .withOffenceId(offenceId)
                                        .build()))
                                .build()))
                        .build(),
                ProsecutionCasesToRemove.prosecutionCasesToRemove()
                        .withCaseId(caseId2)
                        .withDefendantsToRemove(asList(DefendantsToRemove.defendantsToRemove()
                                .withDefendantId(defendantId2)
                                .withOffencesToRemove(asList(OffencesToRemove.offencesToRemove()
                                                .withOffenceId(offenceId2)
                                                .build(),
                                        OffencesToRemove.offencesToRemove()
                                                .withOffenceId(offenceId3)
                                                .build()))
                                .build()))
                        .build());

        ProsecutionCasesBuilder prosecutionCasesBuilder = new ProsecutionCasesBuilder();
        List<uk.gov.justice.listing.events.ProsecutionCases> prosecutionCases = prosecutionCasesBuilder.buildEventProsecutionCasesToRemove(prosecutionCasesToRemove);

        assertThat(prosecutionCases.size(), is(2));
        assertThat(caseId, is(prosecutionCases.get(0).getCaseId()));
        assertThat(caseId2, is(prosecutionCases.get(1).getCaseId()));

        assertThat(prosecutionCases.get(0).getDefendants().size(), equalTo(1));
        assertThat(prosecutionCases.get(1).getDefendants().size(), equalTo(1));
        assertThat(defendantId, is(prosecutionCases.get(0).getDefendants().get(0).getDefendantId()));
        assertThat(defendantId2, is(prosecutionCases.get(1).getDefendants().get(0).getDefendantId()));

        assertThat(prosecutionCases.get(0).getDefendants().get(0).getOffences().size(), equalTo(1));
        assertThat(prosecutionCases.get(1).getDefendants().get(0).getOffences().size(), equalTo(2));
        assertThat(offenceId, is(prosecutionCases.get(0).getDefendants().get(0).getOffences().get(0).getOffenceId()));
        assertThat(offenceId2, is(prosecutionCases.get(1).getDefendants().get(0).getOffences().get(0).getOffenceId()));
        assertThat(offenceId3, is(prosecutionCases.get(1).getDefendants().get(0).getOffences().get(1).getOffenceId()));


    }
}
