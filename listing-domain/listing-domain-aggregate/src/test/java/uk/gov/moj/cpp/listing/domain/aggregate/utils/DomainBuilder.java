package uk.gov.moj.cpp.listing.domain.aggregate.utils;

import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.listing.events.BailStatus;
import uk.gov.moj.cpp.listing.domain.CaseOffences;
import uk.gov.moj.cpp.listing.domain.CaseSimpleOffences;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.domain.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.domain.Offence;
import uk.gov.moj.cpp.listing.domain.SimpleOffence;
import uk.gov.moj.cpp.listing.domain.StatementOfOffence;
import uk.gov.moj.cpp.listing.domain.legacy.Hearing;

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
                STRING.next(), LocalDate.now(),  LocalDate.now().plusDays(2),
                ONE_HOUR_ESTIMATE, null, null, null, 
                asList(buildDefendant()), UNALLOCATED);
    }

    private static Defendant createDefendant(final Offence offence) {

        uk.gov.moj.cpp.listing.domain.BailStatus bailStatus = uk.gov.moj.cpp.listing.domain.BailStatus.values()[new Random().nextInt(BailStatus.values().length)];

        return Defendant.defendant()
                .withHearingLanguageNeeds(of(HearingLanguageNeeds.ENGLISH))
                .withDatesToAvoid(of(STRING.next()))
                .withDefenceOrganisation(of(STRING.next()))
                .withOffences(Collections.singletonList(offence))
                .withId(randomUUID())
                .withBailStatus(of(bailStatus))
                .withCustodyTimeLimit(of(STRING.next()))
                .withDateOfBirth(of(STRING.next()))
                .withFirstName(of(STRING.next()))
                .withLastName(of(STRING.next()))
                .withOrganisationName(of(STRING.next()))
                .withSpecificRequirements(of(STRING.next()))
                .build();
    }

    private static StatementOfOffence createStatementOfOffence() {
        return StatementOfOffence.statementOfOffence()
                .withLegislation(of(STRING.next()))
                .withTitle(STRING.next())
                .withWelshLegislation(of(STRING.next()))
                .withWelshTitle(STRING.next())
                .build();
    }

    private static Offence createOffence(final StatementOfOffence statementOfOffence) {
        return Offence.offence()
                .withId(randomUUID())
                .withOffenceCode(STRING.next())
                .withStartDate(LocalDate.now().toString())
                .withEndDate(of(LocalDate.now().toString()))
                .withStatementOfOffence(statementOfOffence)
                .build();
    }

    private static SimpleOffence createSimpleOffence() {
        return SimpleOffence.createSimpleOffenceBuilder()
                .withId(randomUUID())
                .withDefendantId(randomUUID())
                .build();
    }
}
