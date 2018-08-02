package uk.gov.moj.cpp.listing.domain.aggregate.utils;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.listing.events.BailStatus;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.domain.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class DomainBuilder {

    private static final boolean UNALLOCATED = false;
    private static final int ONE_HOUR_ESTIMATE = 60;

    private DomainBuilder() {}

    public static List<Hearing> buildHearings() {
        return buildHearings(1);
    }

    public static List<Hearing> buildHearings(int total) {
        return IntStream.range(0, total).mapToObj(i -> buildHearing()).collect(toList());
    }

    public static CaseSimpleOffences buildCaseSimpleOffences() {
        return CaseSimpleOffences.createCaseSimpleOffencesBuilder()
                .setCaseId(randomUUID())
                .setOffences(Collections.singletonList(createSimpleOffence()))
                .build();
    }

    public static CaseOffences buildCaseOffences() {
        StatementOfOffence statementOfOffence = createStatementOfOffence();
        return CaseOffences.createCaseOffencesBuilder()
                .setCaseId(randomUUID())
                .setOffences(Collections.singletonList(createOffence(statementOfOffence)))
                .build();
    }

    public static Defendant buildDefendant() {
        StatementOfOffence statementOfOffence = createStatementOfOffence();
        Offence offence = createOffence(statementOfOffence);
        return createDefendant(offence);
    }

    private static Hearing buildHearing() {
        return new Hearing(randomUUID().toString(), randomUUID().toString(), randomUUID().toString(),
                RandomGenerator.STRING.next(), LocalDate.now(),  LocalDate.now().plusDays(2),
                ONE_HOUR_ESTIMATE, null, null, null, 
                asList(buildDefendant()), UNALLOCATED);
    }

    private static Defendant createDefendant(final Offence offence) {
        String bailStatus = BailStatus.values()[new Random().nextInt(BailStatus.values().length)].toString();
        return new Defendant(randomUUID().toString(), randomUUID().toString(), RandomGenerator.STRING.next(), RandomGenerator.STRING.next(),
                LocalDate.now(), bailStatus, LocalDate.now(), RandomGenerator.STRING.next(), Collections.singletonList
                (offence));
    }

    private static StatementOfOffence createStatementOfOffence() {
        return new StatementOfOffence(RandomGenerator.STRING.next(), RandomGenerator.STRING.next());
    }

    private static Offence createOffence(final StatementOfOffence statementOfOffence) {
        return Offence.createOffenceBuilder()
                .setId(randomUUID().toString())
                .setOffenceCode(RandomGenerator.STRING.next())
                .setStartDate(LocalDate.now())
                .setEndDate(LocalDate.now())
                .setStatementOfOffence(statementOfOffence)
                .setDefendantId(randomUUID().toString())
                .build();
    }

    private static SimpleOffence createSimpleOffence() {
        return SimpleOffence.createSimpleOffenceBuilder()
                .setId(randomUUID().toString())
                .setDefendantId(randomUUID().toString())
                .build();
    }
}
