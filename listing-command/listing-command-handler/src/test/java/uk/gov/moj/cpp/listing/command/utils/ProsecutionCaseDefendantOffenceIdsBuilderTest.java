package uk.gov.moj.cpp.listing.command.utils;


import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import uk.gov.justice.listing.courts.Defendants;
import uk.gov.justice.listing.courts.Offences;
import uk.gov.justice.listing.courts.ProsecutionCases;
import uk.gov.moj.cpp.listing.domain.ProsecutionCaseDefendantOffenceIds;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class ProsecutionCaseDefendantOffenceIdsBuilderTest {

    @Test
    public void shouldBuildFromProsecutionCases() {

        final UUID offenceId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID caseId = randomUUID();

        final List<ProsecutionCases> prosecutionCases = asList(ProsecutionCases.prosecutionCases()
                .withCaseId(caseId)
                .withDefendants(asList(Defendants.defendants()
                        .withDefendantId(defendantId)
                        .withOffences(asList(Offences.offences()
                                .withOffenceId(offenceId)
                                .build()))
                        .build()))
                .build());

        ProsecutionCaseDefendantOffenceIdsBuilder prosecutionCaseDefendantOffenceIdsBuilder = new ProsecutionCaseDefendantOffenceIdsBuilder();
        List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds = prosecutionCaseDefendantOffenceIdsBuilder.buildFromProsecutionCases(prosecutionCases);

        assertThat(caseId, is(prosecutionCaseDefendantOffenceIds.get(0).getId()));
        assertThat(defendantId, is(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().get(0).getId()));
        assertThat(offenceId, is(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(prosecutionCaseDefendantOffenceIds.size(),equalTo(1));
        assertThat(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().size(),equalTo(1));
        assertThat(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().get(0).getOffences().size(),equalTo(1));
    }

    @Test
    public void shouldBuildFromProsecutionCasesWithMultipleCases() {

        final UUID case1Defendant1OffenceId1 = randomUUID();
        final UUID case1Defendant1OffenceId2 = randomUUID();
        final UUID case1DefendantId1 = randomUUID();
        final UUID caseId1 = randomUUID();

        final UUID case2DefendantId1Offence1 = randomUUID();
        final UUID case2DefendantId1 = randomUUID();
        final UUID case2DefendantId2 = randomUUID();
        final UUID case2DefendantId2Offence1 = randomUUID();
        final UUID case2DefendantId2Offence2 = randomUUID();
        final UUID caseId_2 = randomUUID();

        final List<ProsecutionCases> prosecutionCases = asList(ProsecutionCases.prosecutionCases()
                .withCaseId(caseId1)
                .withDefendants(asList(Defendants.defendants()
                        .withDefendantId(case1DefendantId1)
                        .withOffences(asList(Offences.offences()
                                .withOffenceId(case1Defendant1OffenceId1)
                                .build(),
                                Offences.offences()
                                .withOffenceId(case1Defendant1OffenceId2)
                                .build()))
                        .build()))
                .build(),
                ProsecutionCases.prosecutionCases()
                        .withCaseId(caseId_2)
                        .withDefendants(asList(Defendants.defendants()
                                .withDefendantId(case2DefendantId1)
                                .withOffences(asList(Offences.offences()
                                        .withOffenceId(case2DefendantId1Offence1)
                                        .build()))
                                .build(),
                                Defendants.defendants()
                                        .withDefendantId(case2DefendantId2)
                                        .withOffences(asList(Offences.offences()
                                                .withOffenceId(case2DefendantId2Offence1)
                                                .build(),
                                                Offences.offences()
                                                .withOffenceId(case2DefendantId2Offence2)
                                                .build()))
                                        .build()
                                ))
                        .build());

        ProsecutionCaseDefendantOffenceIdsBuilder prosecutionCaseDefendantOffenceIdsBuilder = new ProsecutionCaseDefendantOffenceIdsBuilder();
        List<ProsecutionCaseDefendantOffenceIds> prosecutionCaseDefendantOffenceIds = prosecutionCaseDefendantOffenceIdsBuilder.buildFromProsecutionCases(prosecutionCases);

        assertThat(caseId1, is(prosecutionCaseDefendantOffenceIds.get(0).getId()));
        assertThat(case1DefendantId1, is(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().get(0).getId()));
        assertThat(case1Defendant1OffenceId1, is(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(case1Defendant1OffenceId2, is(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().get(0).getOffences().get(1).getId()));

        assertThat(caseId_2, is(prosecutionCaseDefendantOffenceIds.get(1).getId()));
        assertThat(case2DefendantId1, is(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().get(0).getId()));
        assertThat(case2DefendantId1Offence1, is(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(case2DefendantId2, is(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().get(1).getId()));
        assertThat(case2DefendantId1Offence1, is(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().get(0).getOffences().get(0).getId()));
        assertThat(case2DefendantId2Offence1, is(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().get(1).getOffences().get(0).getId()));
        assertThat(case2DefendantId2Offence2, is(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().get(1).getOffences().get(1).getId()));

        assertThat(prosecutionCaseDefendantOffenceIds.size(),equalTo(2));
        assertThat(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().size(),equalTo(1));
        assertThat(prosecutionCaseDefendantOffenceIds.get(0).getDefendants().get(0).getOffences().size(),equalTo(2));

        assertThat(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().size(),equalTo(2));
        assertThat(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().get(0).getOffences().size(),equalTo(1));
        assertThat(prosecutionCaseDefendantOffenceIds.get(1).getDefendants().get(1).getOffences().size(),equalTo(2));

    }
}
