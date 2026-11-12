package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.persistence.entity.query.JudiciaryHearingDay;
import uk.gov.moj.cpp.listing.query.view.service.JudiciarySessionService.JudiciarySessions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import javax.json.JsonArrayBuilder;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.commons.lang3.reflect.FieldUtils;

@ExtendWith(MockitoExtension.class)
class JudiciarySessionServiceTest {

    private static final LocalDate SEARCH_DATE = LocalDate.parse("2026-11-11");
    private static final String COURT_ROOM_ID = "22222222-2222-4222-8222-222222222222";

    private static final String HEARING_1 = "10000000-0000-4000-8000-000000000001";
    private static final String HEARING_2 = "10000000-0000-4000-8000-000000000002";
    private static final String HEARING_3 = "10000000-0000-4000-8000-000000000003";
    private static final String SCHEDULE_AM = "c0000000-0000-4000-8000-0000000000a1";
    private static final String SCHEDULE_PM = "c0000000-0000-4000-8000-0000000000b1";
    private static final String SCHEDULE_AD = "c0000000-0000-4000-8000-0000000000c1";
    private static final String JUDGE_1 = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String JUDGE_2 = "aaaaaaaa-0000-4000-8000-000000000002";
    private static final String JUDGE_3 = "aaaaaaaa-0000-4000-8000-000000000003";

    // 2026-11-11 is GMT, 2026-07-01 is BST
    private static final Instant GMT_10_00 = Instant.parse("2026-11-11T10:00:00Z");
    private static final Instant GMT_12_59 = Instant.parse("2026-11-11T12:59:00Z");
    private static final Instant GMT_13_00 = Instant.parse("2026-11-11T13:00:00Z");
    private static final Instant BST_12_30 = Instant.parse("2026-07-01T11:30:00Z");
    private static final Instant BST_13_00 = Instant.parse("2026-07-01T12:00:00Z");

    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @InjectMocks
    private JudiciarySessionService judiciarySessionService;

    @Captor
    private ArgumentCaptor<Collection<String>> courtScheduleIdsCaptor;

    @BeforeEach
    void setUp() throws IllegalAccessException {
        FieldUtils.writeField(judiciarySessionService, "pmSessionStartTime", "13:00", true);
    }

    @Test
    void shouldPlaceJudiciaryByCourtSchedulerSession() {
        givenCourtSchedulerSessions(Map.of(SCHEDULE_AM, "AM", SCHEDULE_PM, "PM"));

        final JudiciarySessions sessions = split(List.of(
                day(HEARING_1, SCHEDULE_AM, GMT_13_00, JUDGE_1),
                day(HEARING_2, SCHEDULE_PM, GMT_10_00, JUDGE_2)));

        assertThat(sessions.amJudicialIds(), contains(JUDGE_1));
        assertThat(sessions.pmJudicialIds(), contains(JUDGE_2));
    }

    @Test
    void shouldPlaceAllDaySessionInBothSessions() {
        givenCourtSchedulerSessions(Map.of(SCHEDULE_AD, "AD"));

        final JudiciarySessions sessions = split(List.of(day(HEARING_1, SCHEDULE_AD, GMT_10_00, JUDGE_1)));

        assertThat(sessions.amJudicialIds(), contains(JUDGE_1));
        assertThat(sessions.pmJudicialIds(), contains(JUDGE_1));
    }

    @Test
    void shouldLookUpEachDistinctCourtScheduleIdOnce() {
        givenCourtSchedulerSessions(Map.of(SCHEDULE_AM, "AM", SCHEDULE_PM, "PM"));

        split(List.of(
                day(HEARING_1, SCHEDULE_AM, GMT_10_00, JUDGE_1),
                day(HEARING_2, SCHEDULE_AM, GMT_10_00, JUDGE_2),
                day(HEARING_3, SCHEDULE_PM, GMT_13_00, JUDGE_3),
                day(HEARING_3, null, GMT_13_00, JUDGE_3)));

        verify(courtSchedulerServiceAdapter).searchCourtSchedulesById(courtScheduleIdsCaptor.capture());
        assertThat(courtScheduleIdsCaptor.getValue(), containsInAnyOrder(SCHEDULE_AM, SCHEDULE_PM));
    }

    @Test
    void shouldLookUpCourtSchedulesInBatchesOfFiftyAndPlaceEveryHearingDay() {
        final List<JudiciaryHearingDay> hearingDays = new ArrayList<>();
        final Map<String, String> sessionsByCourtScheduleId = new HashMap<>();
        IntStream.range(0, 120).forEach(i -> {
            final String courtScheduleId = "c0000000-0000-4000-8000-%012d".formatted(i);
            // start time says AM, Court Scheduler says PM: every id must be resolved for all judges to land in PM
            hearingDays.add(day(HEARING_1, courtScheduleId, GMT_10_00, "aaaaaaaa-0000-4000-8000-%012d".formatted(i)));
            sessionsByCourtScheduleId.put(courtScheduleId, "PM");
        });
        givenCourtSchedulerSessions(sessionsByCourtScheduleId);

        final JudiciarySessions sessions = split(hearingDays);

        verify(courtSchedulerServiceAdapter, times(3)).searchCourtSchedulesById(courtScheduleIdsCaptor.capture());
        assertThat(courtScheduleIdsCaptor.getAllValues().stream().map(Collection::size).toList(), contains(50, 50, 20));
        assertThat(sessions.amJudicialIds(), is(empty()));
        assertThat(sessions.pmJudicialIds(), hasSize(120));
    }

    @Test
    void shouldFallBackOnStartTimeWhenNoCourtScheduleIdWithoutCallingCourtScheduler() {
        final JudiciarySessions sessions = split(List.of(
                day(HEARING_1, null, GMT_12_59, JUDGE_1),
                day(HEARING_2, null, GMT_13_00, JUDGE_2)));

        assertThat(sessions.amJudicialIds(), contains(JUDGE_1));
        assertThat(sessions.pmJudicialIds(), contains(JUDGE_2));
        verify(courtSchedulerServiceAdapter, never()).searchCourtSchedulesById(any());
    }

    @Test
    void shouldFallBackOnStartTimeInUkLocalTimeDuringBritishSummerTime() {
        final JudiciarySessions sessions = split(List.of(
                day(HEARING_1, null, BST_12_30, JUDGE_1),
                day(HEARING_2, null, BST_13_00, JUDGE_2)));

        assertThat(sessions.amJudicialIds(), contains(JUDGE_1));
        assertThat(sessions.pmJudicialIds(), contains(JUDGE_2));
    }

    @Test
    void shouldUseConfiguredPmSessionStartTime() throws IllegalAccessException {
        FieldUtils.writeField(judiciarySessionService, "pmSessionStartTime", "12:30", true);

        final JudiciarySessions sessions = split(List.of(day(HEARING_1, null, Instant.parse("2026-11-11T12:45:00Z"), JUDGE_1)));

        assertThat(sessions.amJudicialIds(), is(empty()));
        assertThat(sessions.pmJudicialIds(), contains(JUDGE_1));
    }

    @Test
    void shouldFallBackOnStartTimeWhenCourtScheduleIdNotReturnedByCourtScheduler() {
        givenCourtSchedulerSessions(Map.of(SCHEDULE_AM, "AM"));

        final JudiciarySessions sessions = split(List.of(day(HEARING_1, SCHEDULE_PM, GMT_13_00, JUDGE_1)));

        assertThat(sessions.amJudicialIds(), is(empty()));
        assertThat(sessions.pmJudicialIds(), contains(JUDGE_1));
    }

    @Test
    void shouldFallBackOnStartTimeWhenCourtSchedulerReturnsError() {
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(500);
        when(courtSchedulerServiceAdapter.searchCourtSchedulesById(any())).thenReturn(response);

        final JudiciarySessions sessions = split(List.of(day(HEARING_1, SCHEDULE_PM, GMT_10_00, JUDGE_1)));

        assertThat(sessions.amJudicialIds(), contains(JUDGE_1));
        assertThat(sessions.pmJudicialIds(), is(empty()));
    }

    @Test
    void shouldFallBackOnStartTimeWhenCourtSchedulerCallThrows() {
        when(courtSchedulerServiceAdapter.searchCourtSchedulesById(any())).thenThrow(new IllegalStateException("timeout"));

        final JudiciarySessions sessions = split(List.of(day(HEARING_1, SCHEDULE_AM, GMT_13_00, JUDGE_1)));

        assertThat(sessions.amJudicialIds(), is(empty()));
        assertThat(sessions.pmJudicialIds(), contains(JUDGE_1));
    }

    @Test
    void shouldDeduplicateAndOrderByDistinctHearingCountThenEarliestStartTime() {
        final JudiciarySessions sessions = split(List.of(
                // JUDGE_3 starts earliest but sits in one hearing only
                day(HEARING_1, null, Instant.parse("2026-11-11T09:00:00Z"), JUDGE_3),
                // JUDGE_1 and JUDGE_2 sit in two hearings each; JUDGE_2 starts earlier
                day(HEARING_1, null, Instant.parse("2026-11-11T11:00:00Z"), JUDGE_1),
                day(HEARING_2, null, Instant.parse("2026-11-11T11:30:00Z"), JUDGE_1),
                day(HEARING_2, null, Instant.parse("2026-11-11T09:30:00Z"), JUDGE_2),
                day(HEARING_3, null, Instant.parse("2026-11-11T10:00:00Z"), JUDGE_2),
                // same hearing twice (two days on the date) counts once
                day(HEARING_3, null, Instant.parse("2026-11-11T10:30:00Z"), JUDGE_2)));

        assertThat(sessions.amJudicialIds(), contains(JUDGE_2, JUDGE_1, JUDGE_3));
        assertThat(sessions.pmJudicialIds(), is(empty()));
    }

    @Test
    void shouldReturnEmptySessionsWhenNoHearingDays() {
        final JudiciarySessions sessions = split(emptyList());

        assertThat(sessions.amJudicialIds(), is(empty()));
        assertThat(sessions.pmJudicialIds(), is(empty()));
    }

    private JudiciarySessions split(final List<JudiciaryHearingDay> hearingDays) {
        return judiciarySessionService.splitIntoSessions(hearingDays, COURT_ROOM_ID, SEARCH_DATE);
    }

    private static JudiciaryHearingDay day(final String hearingId, final String courtScheduleId, final Instant startTime, final String judicialId) {
        return new JudiciaryHearingDay(hearingId, courtScheduleId, startTime, judicialId);
    }

    private void givenCourtSchedulerSessions(final Map<String, String> sessionsByCourtScheduleId) {
        final JsonArrayBuilder courtSchedules = createArrayBuilder();
        sessionsByCourtScheduleId.forEach((courtScheduleId, courtSession) -> courtSchedules.add(createObjectBuilder()
                .add("courtScheduleId", courtScheduleId)
                .add("courtSession", courtSession)
                .add("isDraft", false)));
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(200);
        when(response.getEntity()).thenReturn(createObjectBuilder().add("courtSchedules", courtSchedules).build());
        when(courtSchedulerServiceAdapter.searchCourtSchedulesById(any())).thenReturn(response);
    }
}
