package uk.gov.moj.cpp.listing.query.view.hearing;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.mockito.BDDMockito.given;

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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HearingSummaryConverterTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID OFFENCE_ID = UUID.randomUUID();
    private static final UUID LISTING_OFFENCE_ID = UUID.randomUUID();
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
    private static final LocalDate NON_SITTING_DAY = LocalDate.now().plusDays(1);
    private static final LocalDate END_DATE = LocalDate.now().plusDays(2);
    private static final LocalTime START_TIME = LocalTime.now();
    private static final Boolean ALLOCATED = Boolean.TRUE;
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @InjectMocks
    private HearingSummaryConverter hearingSummaryConverter;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Mock
    private StartTimesJsonConverter startTimesConverter;

    @Mock
    private NonSittingDaysJsonConverter nonSittingDaysConverter;


    @Test
    public void shouldConvertToHearingSummary() throws Exception {
        final Hearing hearing = createHearing();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(START_DATE, START_TIME, UTC);

        given(startTimesConverter.convertStartTimesFrom(hearing.getStartTimes())).willReturn(Arrays.asList(zonedDateTime));
        given(nonSittingDaysConverter.convertNonSittingDays(hearing.getNonSittingDays())).willReturn(Arrays.asList(NON_SITTING_DAY));

        HearingSummary hearingSummary = hearingSummaryConverter.convert(hearing);

        assertThat(hearingSummary.getId().toString(), is(hearing.getId().toString()));
        assertThat(hearingSummary.getCaseId().toString(), is(hearing.getListingCaseId().toString()));
        assertThat(hearingSummary.getType(), is(hearing.getType()));
        assertThat(hearingSummary.getStartDate(), is(hearing.getStartDate()));
        assertThat(hearingSummary.getEndDate(), is(hearing.getEndDate()));
        assertThat(hearingSummary.getNonSittingDays(), is(Arrays.asList(NON_SITTING_DAY)));
        assertThat(hearingSummary.getStartTimes(), is(Arrays.asList(zonedDateTime.format(DATE_TIME_FORMAT))));
        assertThat(hearingSummary.getCourtCentreId(), is(hearing.getCourtCentreId()));
        assertThat(hearingSummary.getCourtRoomId(), is(hearing.getCourtRoomId()));
        assertThat(hearingSummary.getJudgeId(), is(hearing.getJudgeId()));
        assertThat(hearingSummary.getEstimateMinutes(), is(hearing.getEstimateMinutes()));
        assertThat(hearingSummary.getDefendants().size(), is(1));
        assertThat(hearingSummary.getDefendants(), contains(allOf(
                hasProperty("hearingId", is(HEARING_ID)),
                hasProperty("defendantId", is(DEFENDANT_ID)),
                hasProperty("firstName", is(FIRST_NAME)),
                hasProperty("lastName", is(LAST_NAME)),
                hasProperty("bailStatus", is(BAIL_STATUS)))));

        List<DefendantSummary> defendantSummaries = hearingSummary.getDefendants().stream().limit(1).collect(Collectors.toList());

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
        return new DefendantBuilder()
                .setCompositeDefendantId(compositeDefendantId)
                .setBailStatus(BAIL_STATUS)
                .setPersonId(PERSON_ID)
                .setFirstName(FIRST_NAME)
                .setLastName(LAST_NAME)
                .setDefenceOrganisation(DEFENCE_ORGANISATION)
                .setDateOfBirth(DATE)
                .setOffences(new HashSet<>(Arrays.asList(offence)))
                .build();
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
                .setStartTimes(Arrays.asList(ZonedDateTime.of(START_DATE, START_TIME, ZoneId.of("UTC"))))
                .setListingCaseId(CASE_ID)
                .setType(TYPE)
                .setAllocated(ALLOCATED)
                .setDefendants(new HashSet<>(Arrays.asList(defendant)))
                .build();
    }
}