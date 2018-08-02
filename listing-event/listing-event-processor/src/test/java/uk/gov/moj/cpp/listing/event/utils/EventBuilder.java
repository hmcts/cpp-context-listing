package uk.gov.moj.cpp.listing.event.utils;

import uk.gov.justice.listing.events.*;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

public class EventBuilder {
    public static final UUID CASE_ID = randomUUID();
    public static final UUID COURT_CENTRE_ID = randomUUID();
    public static final UUID COURT_ROOM_ID = randomUUID();
    public static final LocalDate DOB = LocalDate.now().minusYears(25);
    public static final BailStatus BAIL_STATUS = BailStatus.INCUSTODY;
    public static final LocalDate CUSTODY_TIME_LIMIT =  LocalDate.now().plusYears(10);
    public static final UUID PERSON_ID = randomUUID();
    public static final UUID DEFENDANT_ID = randomUUID();
    public static final LocalDate OFFENCE_START_DATE = LocalDate.now().minusMonths(5);
    public static final LocalDate OFFENCE_END_DATE = LocalDate.now().minusMonths(4);
    public static final UUID HEARING_ID = randomUUID();
    public static final UUID JUDGE_ID = randomUUID();
    public static final String OFFENCE_CODE = STRING.next();
    public static final UUID OFFENCE_ID = randomUUID();
    public static final String TITLE = STRING.next();
    public static final String LEGISLATION = STRING.next();
    public static final String HEARING_START_DATE = LocalDate.now().plusWeeks(2).toString();
    public static final String HEARING_END_DATE = LocalDate.now().plusWeeks(2).plusDays(3).toString();
    public static final String HEARING_START_TIME = "10:30";
    public static final Integer ESTIMATE_MINUTES = RandomGenerator.INTEGER.next();
    public static final String TYPE = STRING.next();
    public static final String FIRST_NAME = STRING.next();
    public static final String LAST_NAME = STRING.next();
    public static final String DEFENCE_ORGANISATION = STRING.next();


    public static CaseSentForListing buildCaseSentForListing() {

        StatementOfOffence statementOfOffence = buildStatementOfOffence();
        Offence offence = buildOffence(statementOfOffence);
        Defendant defendant =  buildDefendant(offence);
        Hearing hearing = buildHearing(defendant);

        return CaseSentForListing.caseSentForListing()
                .withCaseId(CASE_ID)
                .withUrn(STRING.next())
                .withHearings(singletonList(hearing))
                .build();
    }

    public static Hearing buildHearing() {
        return buildHearing(buildDefendant());
    }


    public static DefendantsToBeUpdated buildDefendantsToBeUpdated() {
        Defendant defendant = buildDefendant();

        return DefendantsToBeUpdated.defendantsToBeUpdated()
                .withHearings(singletonList(randomUUID()))
                .withDefendants(singletonList(defendant))
                .build();
    }

    private static Hearing buildHearing(Defendant defendant) {
        List<Defendant> defendants = defendant==null ? singletonList(buildDefendant()) : singletonList(defendant);

        return Hearing.hearing()
                .withCaseId(CASE_ID)
                .withCourtCentreId(COURT_CENTRE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withEstimateMinutes(ESTIMATE_MINUTES)
                .withId(HEARING_ID)
                .withJudgeId(JUDGE_ID)
                .withStartDate(HEARING_START_DATE)
                .withEndDate(LocalDate.parse(HEARING_END_DATE))
                .withStartTime(of(HEARING_START_TIME))
                .withType(TYPE)
                .withDefendants(defendants)
                .build();
    }

    public static Defendant buildDefendant() {
        StatementOfOffence statementOfOffence = buildStatementOfOffence();
        Offence offence = buildOffence(statementOfOffence);
        return buildDefendant(offence);
    }


    private static Defendant buildDefendant(final Offence offence) {
        return Defendant.defendant()
                .withBailStatus(BAIL_STATUS)
                .withDateOfBirth(DOB.toString())
                .withCustodyTimeLimit(of(CUSTODY_TIME_LIMIT.toString()))
                .withDefenceOrganisation(DEFENCE_ORGANISATION)
                .withFirstName(FIRST_NAME)
                .withId(DEFENDANT_ID)
                .withLastName(LAST_NAME)
                .withPersonId(PERSON_ID)
                .withOffences(singletonList(offence))
                .build();
    }

    private static StatementOfOffence buildStatementOfOffence() {
        return StatementOfOffence.statementOfOffence()
                .withLegislation(LEGISLATION)
                .withTitle(TITLE)
                .build();
    }

    private static Offence buildOffence(final StatementOfOffence statementOfOffence) {
        return Offence.offence()
                .withStartDate(OFFENCE_START_DATE.toString())
                .withEndDate(Optional.ofNullable(OFFENCE_END_DATE.toString()))
                .withId(OFFENCE_ID)
                .withOffenceCode(OFFENCE_CODE)
                .withDefendantId(DEFENDANT_ID)
                .withStatementOfOffence(statementOfOffence)
                .build();
    }
}
