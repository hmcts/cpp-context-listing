package uk.gov.moj.cpp.listing.it.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * The single time authority for the listing integration-test suite.
 *
 * <p>Every test that needs "today" or "now" must read it from here rather than calling
 * {@code LocalDate.now()} / {@code ZonedDateTime.now()} directly. Two problems are solved:</p>
 *
 * <ul>
 *   <li><b>Midnight safety.</b> {@link #today()} is captured <em>once</em> per JVM, so a test can
 *       never straddle midnight between the moment it builds a hearing date and the moment it asserts
 *       on it. The historic "00:00-01:00 BST band" flakes came from two independent {@code now()}
 *       reads landing on different calendar days during that hour.</li>
 *   <li><b>Host independence.</b> The deployed Wildfly/Postgres run in UTC. With the failsafe
 *       {@code -Duser.timezone=UTC} pin in place, the canonical zone here is UTC too, so the test JVM
 *       and the server agree on "today" regardless of whether the host is a UK laptop (Europe/London)
 *       or a UTC CI agent.</li>
 * </ul>
 *
 * <p><b>Simulation.</b> Pass {@code -Dit.clock=2026-06-15T00:30:00+01:00[Europe/London]} (forwarded
 * to the forked JVM by the {@code listing-integration-test} failsafe profile) to freeze the clock at
 * an arbitrary instant. This is what makes the 00:00-01:00 band testable on demand without waiting
 * for real midnight (see {@code run-it-midnight.sh}). When the property is absent or blank the clock
 * is the live system UTC clock.</p>
 *
 * <p>Elapsed-time measurement (e.g. {@code System.currentTimeMillis()} in {@code QueueUtil} or
 * {@code Instant.now()} inside {@code TestDurationListener}/{@code ServerLogTestMarkerExtension}) is
 * deliberately NOT routed through here — those measure durations, not calendar dates.</p>
 */
public final class ItClock {

    public static final ZoneId LONDON = ZoneId.of("Europe/London");
    public static final ZoneId UTC = ZoneOffset.UTC;

    /** Override key, e.g. {@code -Dit.clock=2026-06-15T00:30:00+01:00[Europe/London]}. */
    public static final String CLOCK_OVERRIDE_PROPERTY = "it.clock";

    private static final Clock CLOCK = resolveClock();

    /** Captured ONCE per JVM/run, in UTC — no test can straddle midnight between build and assert. */
    private static final LocalDate TODAY = LocalDate.now(CLOCK);

    private ItClock() {
    }

    /** The single anchored "today", in UTC. Stable for the whole run. */
    public static LocalDate today() {
        return TODAY;
    }

    /** {@code today()} shifted by {@code days} (negative = past). */
    public static LocalDate todayPlusDays(final long days) {
        return TODAY.plusDays(days);
    }

    /** "Now" as a UTC {@link ZonedDateTime}; replaces {@code ZonedDateTime.now()} / {@code now(ZoneOffset.UTC)}. */
    public static ZonedDateTime nowUtc() {
        return ZonedDateTime.now(CLOCK);
    }

    /** "Now" as a Europe/London {@link ZonedDateTime}; replaces {@code ZonedDateTime.now(ZoneId.of("Europe/London"))}. */
    public static ZonedDateTime nowLondon() {
        return ZonedDateTime.now(CLOCK.withZone(LONDON));
    }

    /** "Now" as an {@link Instant}; replaces a data-bearing {@code Instant.now()} (NOT elapsed-time measurement). */
    public static Instant nowInstant() {
        return CLOCK.instant();
    }

    /** "Now" as a UTC {@link LocalDateTime}; replaces a data-bearing {@code LocalDateTime.now()}. */
    public static LocalDateTime nowLocalDateTime() {
        return LocalDateTime.now(CLOCK);
    }

    /**
     * The ONE court-calendar-date-and-time to UTC-instant-string conversion rule.
     * A court "calendar date" at a given London wall-clock time, expressed as the equivalent UTC instant —
     * mirrors what {@code CourtSchedulerServiceStub} does and what the server stores.
     */
    public static String utc(final LocalDate date, final LocalTime londonTime) {
        return date.atTime(londonTime).atZone(LONDON).withZoneSameInstant(UTC).toString();
    }

    private static Clock resolveClock() {
        final String override = System.getProperty(CLOCK_OVERRIDE_PROPERTY);
        if (override == null || override.isBlank()) {
            return Clock.system(UTC);
        }
        return Clock.fixed(ZonedDateTime.parse(override).toInstant(), UTC);
    }
}
