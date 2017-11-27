package uk.gov.moj.cpp.listing.domain.aggregate.utils;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.Hearing;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class DomainBuilder {

    private static final boolean UNALLOCATED = false;
    private static final int ONE_HOUR_ESTIMATE = 60;

    private DomainBuilder() {}

    public static List<Hearing> buildHearings() {
        return buildHearings(1);
    }
    public static List<Hearing> buildHearings(int total) {
        return IntStream.range(0, total-1).mapToObj(i -> buildHearing()).collect(toList());
    }

    public static Defendant buildDefendant() {
        StatementOfOffence statementOfOffence = createStatementOfOffence();
        Offence offence = createOffence(statementOfOffence);
        return createDefendant(offence);
    }

    private static Hearing buildHearing() {
        return new Hearing(randomUUID().toString(), RandomGenerator.STRING.next(), RandomGenerator.STRING.next(), RandomGenerator.STRING.next(),
                LocalDate.now(), ONE_HOUR_ESTIMATE, asList(buildDefendant()), UNALLOCATED);
    }

    private static Defendant createDefendant(final Offence offence) {
        return new Defendant(randomUUID().toString(), randomUUID().toString(), RandomGenerator.STRING.next(), RandomGenerator.STRING.next(),
                LocalDate.now(), RandomGenerator.STRING.next(), LocalDate.now(), RandomGenerator.STRING.next(), Collections.singletonList
                (offence));
    }

    private static StatementOfOffence createStatementOfOffence() {
        return new StatementOfOffence(RandomGenerator.STRING.next(), RandomGenerator.STRING.next());
    }

    private static Offence createOffence(final StatementOfOffence statementOfOffence) {
        return new Offence(randomUUID().toString(),  RandomGenerator.STRING.next(), LocalDate.now
                (), LocalDate.now(), statementOfOffence);
    }
}
