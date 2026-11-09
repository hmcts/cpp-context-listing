package uk.gov.moj.cpp.listing.common.xhibit;

import java.util.Optional;

/**
 * CREST courtroom number parsing and ordering, shared by the Crown Daily List ({@code CourtListsBuilder}) and the
 * XHIBIT court list mappers so that both outputs list courtrooms in the same order.
 */
public final class CourtRoomNumbers {

    /**
     * Courtroom number for a room without a usable CREST courtroom mapping. Written as-is to the XHIBIT feed.
     */
    public static final int UNMAPPED_COURT_ROOM_NUMBER = -99;

    private static final int MAX_COURT_ROOM_NUMBER_DIGITS = 9;

    private CourtRoomNumbers() {
    }

    /**
     * The number at the end of a CREST courtroom name (e.g. "Court 10" -> 10), ignoring trailing whitespace.
     * A single backwards scan (linear time, no regex backtracking). Empty when there is no trailing number
     * (e.g. "Annex", "Court 1A") or it is too long to be a courtroom number.
     */
    public static Optional<Integer> trailingNumber(final String courtRoomName) {
        if (courtRoomName == null) {
            return Optional.empty();
        }
        int end = courtRoomName.length();
        while (end > 0 && Character.isWhitespace(courtRoomName.charAt(end - 1))) {
            end--;
        }
        int start = end;
        while (start > 0 && isAsciiDigit(courtRoomName.charAt(start - 1))) {
            start--;
        }
        final int digits = end - start;
        if (digits == 0 || digits > MAX_COURT_ROOM_NUMBER_DIGITS) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(courtRoomName.substring(start, end)));
    }

    /**
     * Sort key for a courtroom number: mapped rooms in numeric order, unmapped rooms (negative numbers,
     * e.g. {@link #UNMAPPED_COURT_ROOM_NUMBER}) last.
     */
    public static int sortKey(final int courtRoomNumber) {
        return courtRoomNumber < 0 ? Integer.MAX_VALUE : courtRoomNumber;
    }

    private static boolean isAsciiDigit(final char character) {
        return character >= '0' && character <= '9';
    }
}
