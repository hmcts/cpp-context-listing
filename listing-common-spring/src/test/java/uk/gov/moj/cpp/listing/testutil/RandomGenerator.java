package uk.gov.moj.cpp.listing.testutil;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

/** Test-only replacement for the framework's RandomGenerator generators used by the ported tests. */
public final class RandomGenerator {

    private static final Random RANDOM = new Random();

    public static final Generator<String> STRING = () -> UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    public static final Generator<Boolean> BOOLEAN = RANDOM::nextBoolean;
    public static final Generator<ZonedDateTime> FUTURE_ZONED_DATE_TIME =
            () -> ZonedDateTime.now(ZoneOffset.UTC).plusDays(1 + RANDOM.nextInt(365)).withNano(0);

    private RandomGenerator() {
    }

    public static Generator<Integer> integer(final int max) {
        return () -> RANDOM.nextInt(max);
    }

    public static Generator<Integer> integer(final int min, final int max) {
        return () -> min + RANDOM.nextInt(max - min + 1);
    }

    public static final Generator<Integer> INTEGER = () -> RANDOM.nextInt(10_000);

    public static Generator<String> string(final int length) {
        return () -> {
            final StringBuilder b = new StringBuilder();
            while (b.length() < length) {
                b.append(UUID.randomUUID().toString().replace("-", ""));
            }
            return b.substring(0, length);
        };
    }

    /** A value supplier with the framework's {@code next()} method. */
    @FunctionalInterface
    public interface Generator<T> extends Supplier<T> {
        T next();

        @Override
        default T get() {
            return next();
        }
    }
}
