package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.listing.events.Defendant.defendant;
import static uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings.defendantsToBeAddedForCourtProceedings;
import static uk.gov.justice.listing.events.Offence.offence;
import static uk.gov.justice.listing.events.StatementOfOffence.statementOfOffence;

import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;

import java.util.List;
import java.util.UUID;

import org.junit.Test;

public class AddDefendantsForCourtProceedingsCommandCollectionConverterTest {

    private AddDefendantsForCourtProceedingsCommandCollectionConverter addDefendantsForCourtProceedingsCommandCollectionConverter = new AddDefendantsForCourtProceedingsCommandCollectionConverter();

    @Test
    public void shouldConvert() {

        final UUID caseId = randomUUID();
        final UUID defendantId = randomUUID();
        final UUID offenceId = randomUUID();
        final UUID hearingId = randomUUID();

        final DefendantsToBeAddedForCourtProceedings event = defendantsToBeAddedForCourtProceedings()
                .withHearings(asList(hearingId))
                .withCaseId(caseId)
                .withDefendants(asList(defendant()
                        .withId(defendantId)
                        .withOffences(asList(offence()
                                .withId(offenceId)
                                .withStatementOfOffence(statementOfOffence()
                                        .withTitle("title")
                                        .withLegislation("legislation")
                                        .build())
                                .build()))
                        .build()))
                .build();


        final List<AddDefendantsForCourtProceedingsCommand> result = addDefendantsForCourtProceedingsCommandCollectionConverter.convert(event);

        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCaseId(), is(caseId));
        assertThat(result.get(0).getDefendants(), hasSize(1));
        assertThat(result.get(0).getHearingId(), is(hearingId));

    }
}