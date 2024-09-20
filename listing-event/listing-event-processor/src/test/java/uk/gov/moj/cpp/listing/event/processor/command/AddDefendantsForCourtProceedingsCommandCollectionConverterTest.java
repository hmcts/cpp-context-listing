package uk.gov.moj.cpp.listing.event.processor.command;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.listing.events.Defendant.defendant;
import static uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings.defendantsToBeAddedForCourtProceedings;
import static uk.gov.justice.listing.events.Offence.offence;
import static uk.gov.justice.listing.events.StatementOfOffence.statementOfOffence;

import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import uk.gov.justice.listing.events.ReportingRestriction;

public class AddDefendantsForCourtProceedingsCommandCollectionConverterTest {

    private AddDefendantsForCourtProceedingsCommandCollectionConverter addDefendantsForCourtProceedingsCommandCollectionConverter = new AddDefendantsForCourtProceedingsCommandCollectionConverter();

    private final UUID caseId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID offenceId = randomUUID();
    private final UUID hearingId = randomUUID();
    private final UUID reportingRestrictionId = randomUUID();

    @Test
    public void shouldConvert() {

        final DefendantsToBeAddedForCourtProceedings event = defendantsToBeAddedForCourtProceedings()
                .withHearings(singletonList(hearingId))
                .withCaseId(caseId)
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withOffences(singletonList(offence()
                                .withId(offenceId)
                                .withReportingRestrictions(singletonList(ReportingRestriction.reportingRestriction()
                                        .withId(reportingRestrictionId)
                                        .withLabel("label")
                                        .withOrderedDate(LocalDate.now())
                                        .build()))
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
        assertThat(result.get(0).getDefendants().get(0).getOffences(), hasSize(1));
        assertThat(result.get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions(), hasSize(1));
        assertThat(result.get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions().get(0).getId(), is(reportingRestrictionId));
    }

    @Test
    public void shouldConvertWithOutReportingRestriction() {

        final DefendantsToBeAddedForCourtProceedings event = defendantsToBeAddedForCourtProceedings()
                .withHearings(singletonList(hearingId))
                .withCaseId(caseId)
                .withDefendants(singletonList(defendant()
                        .withId(defendantId)
                        .withOffences(singletonList(offence()
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
        assertThat(result.get(0).getDefendants().get(0).getOffences(), hasSize(1));
        assertThat(result.get(0).getDefendants().get(0).getOffences().get(0).getReportingRestrictions(), nullValue());
    }
}