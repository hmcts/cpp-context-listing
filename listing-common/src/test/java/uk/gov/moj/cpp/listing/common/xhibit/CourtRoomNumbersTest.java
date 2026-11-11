package uk.gov.moj.cpp.listing.common.xhibit;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static uk.gov.moj.cpp.listing.common.xhibit.CourtRoomNumbers.UNMAPPED_COURT_ROOM_NUMBER;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

class CourtRoomNumbersTest {

    @Test
    void shouldParseTrailingCourtRoomNumber() {
        assertThat(CourtRoomNumbers.trailingNumber("Court 3"), is(of(3)));
        assertThat(CourtRoomNumbers.trailingNumber("Court 10"), is(of(10)));
        assertThat(CourtRoomNumbers.trailingNumber("Court 7  "), is(of(7)));
        assertThat(CourtRoomNumbers.trailingNumber("Annex"), is(empty()));
        assertThat(CourtRoomNumbers.trailingNumber("Court 1A"), is(empty()));
        assertThat(CourtRoomNumbers.trailingNumber("   "), is(empty()));
        assertThat(CourtRoomNumbers.trailingNumber(""), is(empty()));
        assertThat(CourtRoomNumbers.trailingNumber(null), is(empty()));
        assertThat(CourtRoomNumbers.trailingNumber("Court 99999999999"), is(empty()));
    }

    @Test
    void shouldParseTrailingNumberOfVeryLongCourtRoomNameQuickly() {
        final String longDigitsThenText = "1".repeat(100_000) + " x" + " ".repeat(100_000);
        final String longNameEndingInNumber = " ".repeat(100_000) + "Court 4";

        assertTimeout(Duration.ofSeconds(1), () -> {
            assertThat(CourtRoomNumbers.trailingNumber(longDigitsThenText), is(empty()));
            assertThat(CourtRoomNumbers.trailingNumber(longNameEndingInNumber), is(of(4)));
        });
    }

    @Test
    void shouldSortUnmappedCourtRoomsLast() {
        final List<Integer> numbers = new ArrayList<>(List.of(10, UNMAPPED_COURT_ROOM_NUMBER, 2, 99, 1));

        numbers.sort(Comparator.comparingInt(CourtRoomNumbers::sortKey));

        assertThat(numbers, contains(1, 2, 10, 99, UNMAPPED_COURT_ROOM_NUMBER));
    }
}
