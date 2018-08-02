package uk.gov.moj.cpp.listing.domain.aggregate.utils;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.listing.events.BailStatus;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.StatementOfOffence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class EventBuilder {

    private static final int ONE_HOUR_ESTIMATE = 60;

    public static List<Hearing> buildHearings() {
        return buildHearings(1);
    }

    public static List<Hearing> buildHearings(final int total) {
        return IntStream.range(0, total).mapToObj(i -> buildHearing()).collect(toList());
    }

    public static Defendant buildDefendant() {
        StatementOfOffence statementOfOffence = createStatementOfOffence();
        Offence offence = createOffence(statementOfOffence);
        return createDefendant(offence);
    }

    private static Hearing buildHearing() {
        return Hearing.hearing()
                .withId(randomUUID())
                .withCaseId(randomUUID())
                .withType(STRING.next())
                .withCourtCentreId(randomUUID())
                .withStartDate(LocalDate.now().toString())
                .withEstimateMinutes(ONE_HOUR_ESTIMATE)
                .withDefendants(singletonList(buildDefendant()))
                .build();
    }


    private static Defendant createDefendant(final Offence offence) {
        return Defendant.defendant()
                .withId(randomUUID())
                .withPersonId(randomUUID())
                .withFirstName(STRING.next())
                .withLastName(STRING.next())
                .withDateOfBirth(LocalDate.now().toString())
                .withBailStatus(BailStatus.CONDITIONAL)
                .withCustodyTimeLimit(of(LocalDate.now().toString()))
                .withDefenceOrganisation(STRING.next())
                .withOffences(singletonList(offence))
                .build();
    }

    private static StatementOfOffence createStatementOfOffence() {
        return StatementOfOffence.statementOfOffence()
                .withTitle(STRING.next())
                .withLegislation(STRING.next())
                .build();
    }

    private static Offence createOffence(final StatementOfOffence statementOfOffence) {
        return Offence.offence()
                .withId(randomUUID())
                .withOffenceCode(STRING.next())
                .withStartDate(LocalDate.now().toString())
                .withEndDate(Optional.ofNullable(LocalDate.now().toString()))
                .withStatementOfOffence(statementOfOffence)
                .build();
    }

}
