package uk.gov.moj.cpp.listing.query.view.hearing;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.moj.cpp.listing.persistence.entity.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

@RunWith(MockitoJUnitRunner.class)
public class HearingCaseSummaryTest {
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID DEFENDANT_ID = UUID.randomUUID();
    private static final UUID PERSON_ID = UUID.randomUUID();
    private static final String TYPE = RandomGenerator.STRING.next();
    private static final String FIRST_NAME = RandomGenerator.STRING.next();
    private static final String LAST_NAME = "Testing";
    private static final String BAIL_STATUS = RandomGenerator.STRING.next();
    private static final String DEFENCE_ORGANISATION = RandomGenerator.STRING.next();
    private static final LocalDate DATE = LocalDate.now();
    private static final String OFFENCE_CODE = RandomGenerator.STRING.next();
    private static final String LEGISLATION = RandomGenerator.STRING.next();
    private static final String TITLE = RandomGenerator.STRING.next();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.now();
    private static final LocalDate END_DATE = LocalDate.now().plusDays(2);
    private static final LocalTime START_TIME = LocalTime.now();
    private static final Boolean ALLOCATED = Boolean.TRUE;
    private static final UUID LISTING_DEFENDANT_ID = UUID.randomUUID();
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final LocalDate NON_SITTING_DAY = LocalDate.now().plusDays(1);
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");


    @InjectMocks
    private HearingSummaryConverter hearingSummaryConverter;

    @InjectMocks
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;


    @Test
    public void shouldConvertToHearingCaseSummaryFromHearingSummary() throws Exception {
       //Given
        final Hearing hearing = createHearing();
        final HearingSummary arbitraryHearingSummary = hearingSummaryConverter.convert(hearing);
        final String arbitraryCaseUrn = "FOO";
        final ZonedDateTime zonedDateTime = ZonedDateTime.of(START_DATE, START_TIME, UTC);

        //When
        HearingCaseSummary testObj = new HearingCaseSummary(arbitraryHearingSummary,arbitraryCaseUrn);

//        assertThat(testObj.getDefendantId().toString(), is(hearing.getDefendantId().toString()));
        assertThat(testObj.getUrn(), is(arbitraryCaseUrn));
        assertThat(testObj.getType(), is(hearing.getType()));
        assertThat(testObj.getStartDate(), is(hearing.getStartDate()));
        assertThat(testObj.getEndDate(), is(hearing.getEndDate()));
        assertThat(testObj.getNonSittingDays(), is(Arrays.asList(NON_SITTING_DAY)));
        assertThat(testObj.getStartTimes(), is(Arrays.asList(zonedDateTime.format(DATE_TIME_FORMAT))));
        assertThat(testObj.getCourtCentreId(), is(hearing.getCourtCentreId()));
        assertThat(testObj.getCourtRoomId(), is(hearing.getCourtRoomId()));
        assertThat(testObj.getJudgeId(), is(hearing.getJudgeId()));
        assertThat(testObj.getEstimateMinutes(), is(hearing.getEstimateMinutes()));
        assertThat(testObj.getDefendants().size(), is(1));
        assertThat(testObj.getDefendants(), contains(allOf(
                hasProperty("hearingId", is(HEARING_ID)),
                hasProperty("defendantId", is(DEFENDANT_ID)),
                hasProperty("firstName", is(FIRST_NAME)),
                hasProperty("lastName", is(LAST_NAME)),
                hasProperty("bailStatus", is(BAIL_STATUS)))));

        List<DefendantSummary> defendantSummaries = testObj.getDefendants().stream().limit(1).collect(Collectors.toList());

        assertThat(defendantSummaries.get(0).getOffences(), contains(allOf(
                hasProperty("offenceId", is(OFFENCE_ID.toString())),
                hasProperty("defendantId", is(DEFENDANT_ID.toString())),
                hasProperty("title", is(TITLE)))));
    }


    private Offence createOffence(final StatementOfOffence sof) {
        CompositeOffenceId compositeOffenceId = createCompositeOffenceId();
        Offence offence = new OffenceBuilder()
                .setId(compositeOffenceId)
                .setStatementOfOffence(sof)
                .setOffenceCode(OFFENCE_CODE)
                .setEndDate(DATE)
                .setStartDate(DATE)
                .build();
        return offence;
    }

    private CompositeOffenceId createCompositeOffenceId() {
        return new CompositeOffenceIdBuilder()
                .setHearingId(HEARING_ID)
                .setOffenceId(OFFENCE_ID)
                .setDefendantId(DEFENDANT_ID)
                .build();
    }

    private Defendant createDefendant(final Offence offence) {
        CompositeDefendantId compositeDefendantId = createCompositeDefendantId();
        Defendant defandant = new DefendantBuilder()
                .setCompositeDefendantId(compositeDefendantId)
                .setBailStatus(BAIL_STATUS)
                .setPersonId(PERSON_ID)
                .setFirstName(FIRST_NAME)
                .setLastName(LAST_NAME)
                .setDefenceOrganisation(DEFENCE_ORGANISATION)
                .setDateOfBirth(DATE)
                .setOffences(new HashSet<>(Arrays.asList(offence)))
                .build();
        return defandant;
    }

    private CompositeDefendantId createCompositeDefendantId() {
        return new CompositeDefendantIdBuilder()
                .setHearingId(HEARING_ID)
                .setDefendantId(DEFENDANT_ID)
                .build();
    }

    private StatementOfOffence createStatementOfOffence() {
        StatementOfOffence statementOfOffence = new StatementOfOffenceBuilder()
                .setLegislation(LEGISLATION)
                .setTitle(TITLE)
                .build();
        return statementOfOffence;
    }

    private Hearing createHearing() {
        StatementOfOffence sof = createStatementOfOffence();
        Offence offence = createOffence(sof);
        Defendant defendant = createDefendant(offence);

        return new HearingBuilder()
                .setId(HEARING_ID)
                .setEstimateMinutes(RandomGenerator.INTEGER.next())
                .setCourtCentreId(COURT_CENTRE_ID)
                .setCourtRoomId(COURT_ROOM_ID)
                .setJudgeId(JUDGE_ID)
                .setStartDate(START_DATE)
                .setEndDate(END_DATE)
                .setNonSittingDays(Arrays.asList(NON_SITTING_DAY))
                .setStartTimes(Arrays.asList(ZonedDateTime.of(START_DATE, START_TIME, UTC)))
                .setListingCaseId(CASE_ID)
                .setType(TYPE)
                .setAllocated(ALLOCATED)
                .setDefendants(new HashSet<>(Arrays.asList(defendant)))
                .build();
    }
}