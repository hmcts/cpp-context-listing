package uk.gov.moj.cpp.listing.persistence.repository;

import static com.vladmihalcea.hibernate.type.json.internal.JacksonUtil.toJsonNode;
import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.MAGISTRATES;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.HearingDays;
import uk.gov.moj.cpp.listing.persistence.entity.query.JudiciarySession;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class JudiciarySessionRepositoryIT extends BaseTransactionalTest {

    private static final String JUDGE_1 = "aaaaaaaa-0000-4000-8000-000000000001";
    private static final String JUDGE_2 = "aaaaaaaa-0000-4000-8000-000000000002";
    private static final String JUDGE_3 = "aaaaaaaa-0000-4000-8000-000000000003";

    // GMT: UK local time is UTC
    private static final LocalDate WINTER_DATE = LocalDate.of(2030, 1, 15);
    // BST: UK local time is UTC + 1
    private static final LocalDate SUMMER_DATE = LocalDate.of(2030, 7, 15);

    @Inject
    public HearingRepository hearingRepository;

    @Inject
    public JudiciarySessionRepository judiciarySessionRepository;

    private final UUID courtCentreId = randomUUID();
    private final UUID courtRoomId = randomUUID();

    @Test
    public void shouldPlaceJudiciaryInSessionByHearingDayStartAndEndTimes() {
        givenAllocatedHearing(List.of(JUDGE_1), day(WINTER_DATE, "10:00", "12:59"));
        givenAllocatedHearing(List.of(JUDGE_2), day(WINTER_DATE, "13:00", "15:00"));
        givenAllocatedHearing(List.of(JUDGE_3), day(WINTER_DATE, "11:00", "13:00"));

        assertThat(findJudiciarySessions(WINTER_DATE), contains(
                new JudiciarySession(JUDGE_1, "AM"),
                new JudiciarySession(JUDGE_2, "PM"),
                new JudiciarySession(JUDGE_3, "AD")));
    }

    @Test
    public void shouldPlaceJudiciaryInSessionByUkLocalTimeDuringBritishSummerTime() {
        // 11:30-11:59 UTC is 12:30-12:59 BST, 12:00 UTC is 13:00 BST
        givenAllocatedHearing(List.of(JUDGE_1), day(SUMMER_DATE, "11:30", "11:59"));
        givenAllocatedHearing(List.of(JUDGE_2), day(SUMMER_DATE, "12:00", "12:30"));

        assertThat(findJudiciarySessions(SUMMER_DATE), contains(
                new JudiciarySession(JUDGE_1, "AM"),
                new JudiciarySession(JUDGE_2, "PM")));
    }

    @Test
    public void shouldPlaceJudiciaryAllDayWhenHearingDayHasNoTimes() {
        givenAllocatedHearing(List.of(JUDGE_1), day(WINTER_DATE, null, null));

        assertThat(findJudiciarySessions(WINTER_DATE), contains(new JudiciarySession(JUDGE_1, "AD")));
    }

    @Test
    public void shouldReturnEachJudiciarySessionOnceInJudicialIdOrder() {
        givenAllocatedHearing(List.of(JUDGE_3, JUDGE_1), day(WINTER_DATE, "10:00", "11:00"));
        givenAllocatedHearing(List.of(JUDGE_1, JUDGE_2), day(WINTER_DATE, "11:00", "12:00"));
        givenAllocatedHearing(List.of(JUDGE_1), day(WINTER_DATE, "14:00", "15:00"));

        final List<JudiciarySession> judiciarySessions = findJudiciarySessions(WINTER_DATE);

        assertThat(judiciarySessions.stream().map(JudiciarySession::judicialId).toList(), contains(JUDGE_1, JUDGE_1, JUDGE_2, JUDGE_3));
        assertThat(Set.copyOf(judiciarySessions), is(Set.of(
                new JudiciarySession(JUDGE_1, "AM"),
                new JudiciarySession(JUDGE_1, "PM"),
                new JudiciarySession(JUDGE_2, "AM"),
                new JudiciarySession(JUDGE_3, "AM"))));
    }

    @Test
    public void shouldOnlyReturnJudiciaryOfSearchedDayOfMultiDayHearing() {
        givenHearing(courtCentreId, courtRoomId, true, false, judiciary(JUDGE_1),
                day(WINTER_DATE.minusDays(1), "14:00", "15:00"),
                day(WINTER_DATE, "10:00", "11:00"),
                day(WINTER_DATE.plusDays(1), "14:00", "15:00"));

        assertThat(findJudiciarySessions(WINTER_DATE), contains(new JudiciarySession(JUDGE_1, "AM")));
    }

    @Test
    public void shouldExcludeVacatedUnallocatedAndOtherCourtRoomHearingsAndJudiciaryWithoutJudicialId() {
        givenHearing(courtCentreId, courtRoomId, true, true, judiciary(JUDGE_1), day(WINTER_DATE, "10:00", "11:00"));
        givenHearing(courtCentreId, courtRoomId, false, false, judiciary(JUDGE_1), day(WINTER_DATE, "10:00", "11:00"));
        givenHearing(courtCentreId, randomUUID(), true, false, judiciary(JUDGE_1), day(WINTER_DATE, "10:00", "11:00"));
        givenHearing(randomUUID(), courtRoomId, true, false, judiciary(JUDGE_1), day(WINTER_DATE, "10:00", "11:00"));
        givenHearing(courtCentreId, courtRoomId, true, false, "{}", day(WINTER_DATE, "10:00", "11:00"));
        givenHearing(courtCentreId, courtRoomId, true, false, "{\"judiciary\":[{\"isDeputy\":false}]}", day(WINTER_DATE, "10:00", "11:00"));

        assertThat(findJudiciarySessions(WINTER_DATE), is(empty()));
    }

    private List<JudiciarySession> findJudiciarySessions(final LocalDate searchDate) {
        return judiciarySessionRepository.findByCourtCentreAndRoomAndSearchDate(courtCentreId, courtRoomId, searchDate);
    }

    private void givenAllocatedHearing(final List<String> judicialIds, final HearingDays hearingDay) {
        givenHearing(courtCentreId, courtRoomId, true, false, judiciary(judicialIds.toArray(String[]::new)), hearingDay);
    }

    private void givenHearing(final UUID hearingCourtCentreId, final UUID hearingCourtRoomId, final boolean allocated, final boolean vacated,
                              final String properties, final HearingDays... hearingDays) {
        final Hearing hearing = Hearing.builder()
                .withId(randomUUID())
                .withProperties(toJsonNode(properties))
                .withTypeId(randomUUID())
                .withCourtCentreId(hearingCourtCentreId)
                .withCourtRoomId(hearingCourtRoomId)
                .withStartDate(hearingDays[0].getHearingDate())
                .withEndDate(hearingDays[hearingDays.length - 1].getHearingDate())
                .withIsVacatedTrial(vacated)
                .withAllocated(allocated)
                .withJurisdictionType(MAGISTRATES.toString())
                .withUnscheduled(false)
                .withIsPossibleDisqualification(false)
                .build();
        Arrays.stream(hearingDays).forEach(hearingDay -> {
            hearingDay.setCourtCentreId(hearingCourtCentreId);
            hearingDay.setCourtRoomId(hearingCourtRoomId);
        });
        hearing.setHearingDays(Set.of(hearingDays));

        hearingRepository.save(hearing);
        hearingRepository.flush();
    }

    private static String judiciary(final String... judicialIds) {
        return Arrays.stream(judicialIds)
                .map(judicialId -> "{\"judicialId\":\"" + judicialId + "\"}")
                .collect(Collectors.joining(",", "{\"judiciary\":[", "]}"));
    }

    /**
     * A hearing day sitting between the given UTC times, or without times when they are null.
     */
    private static HearingDays day(final LocalDate hearingDate, final String utcStartTime, final String utcEndTime) {
        return HearingDays.builder()
                .withId(randomUUID())
                .withSequence(0)
                .withHearingDate(hearingDate)
                .withStartTime(utcTime(hearingDate, utcStartTime))
                .withEndTime(utcTime(hearingDate, utcEndTime))
                .build();
    }

    private static ZonedDateTime utcTime(final LocalDate date, final String time) {
        return time == null ? null : LocalDateTime.of(date, LocalTime.parse(time)).atZone(UTC);
    }
}
