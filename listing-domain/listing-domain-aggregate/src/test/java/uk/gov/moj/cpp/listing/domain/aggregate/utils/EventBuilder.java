package uk.gov.moj.cpp.listing.domain.aggregate.utils;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class EventBuilder {


    public static List<Hearing> buildHearings() {
        return buildHearings(1);
    }

    private static List<Hearing> buildHearings(final int total) {
        return IntStream.range(0, total).mapToObj(i -> buildHearing()).collect(toList());
    }

    public static Defendant buildDefendant() {
        final StatementOfOffence statementOfOffence = createStatementOfOffence();
        final Offence offence = createOffence(statementOfOffence);
        return createDefendant(offence);
    }

    private static Hearing buildHearing() {
        return Hearing.hearing()
                .withId(randomUUID())
                .withCourtCentreId(randomUUID())
                .build();
    }


    private static Defendant createDefendant(final Offence offence) {
        return Defendant.defendant()
                .withId(randomUUID())
                .withMasterDefendantId(randomUUID())
                .withCourtProceedingsInitiated(ZonedDateTime.now())
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

    private static Offence createOffence(final StatementOfOffence statementOfOffence) {
        return Offence.offence()
                .withId(randomUUID())
                .withOffenceCode(STRING.next())
                .withStartDate(LocalDate.now().toString())
                .withEndDate(LocalDate.now().toString())
                .withStatementOfOffence(statementOfOffence)
                .build();
    }

}
