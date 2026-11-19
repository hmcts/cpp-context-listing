package uk.gov.moj.cpp.listing.command.hearing.handler.testsupport;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** The framework test-utils {@code RandomGenerator} values the ported handler tests use. */
public final class RandomGenerator {

    public static final Generator<String> STRING = () -> UUID.randomUUID().toString().replace("-", "");
    public static final Generator<Integer> INTEGER = () -> ThreadLocalRandom.current().nextInt(1, 10_000);
    public static final Generator<LocalDate> FUTURE_LOCAL_DATE = () -> LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(1, 3650));
    public static final Generator<ZonedDateTime> FUTURE_UTC_DATE_TIME = () -> ZonedDateTime.now(ZoneOffset.UTC)
            .plusMinutes(ThreadLocalRandom.current().nextInt(1, 5_000_000)).truncatedTo(ChronoUnit.SECONDS);

    private RandomGenerator() {
    }

    @FunctionalInterface
    public interface Generator<T> extends Supplier<T> {
        default T next() {
            return get();
        }
    }
}
