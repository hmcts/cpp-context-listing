package uk.gov.moj.cpp.listing.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import java.time.LocalDate;
import java.util.Date;

import org.junit.jupiter.api.Test;

class CourtScheduleTest {

    @Test
    void isDraftShouldDefaultToFalse() {
        final CourtSchedule courtSchedule = new CourtSchedule();
        assertThat(courtSchedule.isDraft(), is(false));
    }

    @Test
    void shouldSetAndGetIsDraft() {
        final CourtSchedule courtSchedule = new CourtSchedule();
        courtSchedule.setDraft(true);
        assertThat(courtSchedule.isDraft(), is(true));
    }

    @Test
    void shouldGetAndSetJudiciaries() {
        final CourtSchedule courtSchedule = new CourtSchedule();
        assertThat(courtSchedule.getJudiciaries().isEmpty(), is(true));

        final CourtScheduleJudiciary judiciary = new CourtScheduleJudiciary();
        judiciary.setJudiciaryId("judge-1");
        judiciary.setJudiciaryType("CIRCUIT_JUDGE");
        judiciary.setBenchChairman(true);
        courtSchedule.getJudiciaries().add(judiciary);

        assertThat(courtSchedule.getJudiciaries().size(), is(1));
        assertThat(courtSchedule.getJudiciaries().get(0).getJudiciaryType(), is("CIRCUIT_JUDGE"));
    }

    @Test
    void getTotalSessionDurationShouldReturnZeroWhenTimesAreNull() {
        final CourtSchedule courtSchedule = new CourtSchedule();
        assertThat(courtSchedule.getTotalSessionDurationInMinutes(), is(0));
    }

    @Test
    void getTotalSessionDurationShouldCalculateCorrectly() {
        final CourtSchedule courtSchedule = new CourtSchedule();
        courtSchedule.setSessionStartTime(new Date(0));
        courtSchedule.setSessionEndTime(new Date(2 * 60 * 60 * 1000));
        assertThat(courtSchedule.getTotalSessionDurationInMinutes(), is(120));
    }

    @Test
    void compareToShouldCompareBySessionDate() {
        final CourtSchedule cs1 = new CourtSchedule();
        cs1.setSessionDate(LocalDate.of(2026, 3, 1));
        final CourtSchedule cs2 = new CourtSchedule();
        cs2.setSessionDate(LocalDate.of(2026, 3, 5));
        assertThat(cs1.compareTo(cs2) < 0, is(true));
    }

    @Test
    void compareToShouldReturnZeroWhenSessionDateIsNull() {
        final CourtSchedule cs1 = new CourtSchedule();
        final CourtSchedule cs2 = new CourtSchedule();
        cs2.setSessionDate(LocalDate.now());
        assertThat(cs1.compareTo(cs2), is(0));
    }
}
