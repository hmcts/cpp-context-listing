package uk.gov.moj.cpp.listing.command.handler;

import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class EventBuilder {


    public static List<Hearing> buildHearings() {
        return buildHearings(1);
    }

    public static List<Hearing> buildHearings(final int total) {
        return IntStream.range(0, total).mapToObj(i -> buildHearing()).collect(toList());
    }

    public static Defendant buildDefendant() {
        StatementOfOffence statementOfOffence = createStatementOfOffence();
        Offence offence = createOffence(randomUUID(), statementOfOffence);
        return createDefendant(offence, randomUUID());
    }

    public static List<Defendant> buildDefendants(UUID defendantId, UUID offenceId) {
        StatementOfOffence statementOfOffence = createStatementOfOffence();
        Offence offence = createOffence(offenceId, statementOfOffence);
        return singletonList(createDefendant(offence, defendantId));
    }

    public static Hearing buildHearing() {
        return Hearing.hearing()
                .withId(randomUUID())
                .withCourtCentreId(randomUUID())
                .build();
    }


    private static Defendant createDefendant(final Offence offence, final UUID defendantId) {
        return Defendant.defendant()
                .withId(defendantId)
                .withBailStatus(new BailStatus.Builder().withId(fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).withCode("P").withDescription("Conditional Bail with Pre-Release conditions").build())
                .withCustodyTimeLimit(LocalDate.now().toString())
                .withOffences(singletonList(offence))
                .build();
    }

    private static StatementOfOffence createStatementOfOffence() {
        return StatementOfOffence.statementOfOffence()
                .withTitle(STRING.next())
                .withWelshTitle(STRING.next())
                .withLegislation(STRING.next())
                .build();
    }

    private static Offence createOffence(final UUID offenceId, final StatementOfOffence statementOfOffence) {
        return Offence.offence()
                .withId(offenceId)
                .withOffenceCode(STRING.next())
                .withStartDate(LocalDate.now().toString())
                .withEndDate(LocalDate.now().toString())
                .withStatementOfOffence(statementOfOffence)
                .build();
    }

}
