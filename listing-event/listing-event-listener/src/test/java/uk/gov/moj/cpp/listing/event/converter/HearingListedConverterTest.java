package uk.gov.moj.cpp.listing.event.converter;


import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.listing.events.BailStatus.CONDITIONAL;
import static uk.gov.justice.listing.events.Defendant.defendant;
import static uk.gov.justice.listing.events.HearingListed.hearingListed;
import static uk.gov.justice.listing.events.Offence.offence;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.StatementOfOffence;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

;

public class HearingListedConverterTest {

    private static final UUID HEARING_ID = UUID.randomUUID();
    private static final UUID CASE_ID = UUID.randomUUID();
    private static final UUID COURT_ROOM_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final LocalDate START_DATE = LocalDate.parse("2018-06-01");
    private static final ZonedDateTime START_TIME = ZonedDateTime.of(START_DATE, LocalTime.now(), ZoneId.of("UTC"));
    private static final int ESTIMATE_MINUTES = 7200;
    private static final String TYPE = "Sentence";

    private HearingListedConverter hearingListedConverter = new HearingListedConverter();

    @Test
    public void shouldConvertHearingListedToHearing() throws Exception {
        final HearingListed hearingListed = hearingListed()
                .withHearingId(HEARING_ID)
                .withType(TYPE)
                .withStartDate(START_DATE)
                .withEstimateMinutes(ESTIMATE_MINUTES)
                .withCaseId(CASE_ID)
                .withCourtRoomId(COURT_ROOM_ID)
                .withJudgeId(JUDGE_ID)
                .withStartTimes(Arrays.asList(START_TIME))
                .withCourtCentreId(COURT_CENTRE_ID)
                .withDefendants(singletonList(defendant()
                        .withBailStatus(CONDITIONAL)
                        .withDateOfBirth(now().toString())
                        .withCustodyTimeLimit(of(now().toString()))
                        .withOffences(ImmutableList.of(offence()
                                .withId(randomUUID())
                                .withOffenceCode(STRING.next())
                                .withStartDate(now().toString())
                                .withEndDate(Optional.ofNullable(now().toString()))
                                .withStatementOfOffence(StatementOfOffence.statementOfOffence()
                                        .withLegislation(STRING.next())
                                        .withTitle(STRING.next())
                                        .build())
                                .build()))
                        .build()))
                .build();

        Hearing actual = hearingListedConverter.convert(hearingListed);

        assertThat(actual.getId(), is(equalTo(HEARING_ID)));
        assertThat(actual.getListingCaseId(), is(equalTo(CASE_ID)));
        assertThat(actual.getCourtCentreId(), is(equalTo(COURT_CENTRE_ID)));
        assertThat(actual.getStartDate(), is(equalTo(START_DATE)));
        assertThat(actual.getStartTimes().contains(START_TIME.toInstant().toString()), is(true));
        assertThat(actual.getEstimateMinutes(), is(equalTo(ESTIMATE_MINUTES)));
        assertThat(actual.getType(), is(equalTo(TYPE)));
        assertThat(actual.getJudgeId(), is(equalTo(JUDGE_ID)));
        assertThat(actual.getCourtRoomId(), is(equalTo(COURT_ROOM_ID)));
    }
}
