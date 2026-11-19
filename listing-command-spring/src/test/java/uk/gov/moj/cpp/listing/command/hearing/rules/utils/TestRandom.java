package uk.gov.moj.cpp.listing.command.hearing.rules.utils;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/** The framework test-utils {@code RandomGenerator} values the ported rule tests use. */
public final class TestRandom {

    public static final Generator<LocalDate> FUTURE_LOCAL_DATE = () -> LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(1, 3650));
    public static final Generator<String> STRING = () -> UUID.randomUUID().toString().replace("-", "");

    private TestRandom() {
    }

    @FunctionalInterface
    public interface Generator<T> extends Supplier<T> {
        default T next() {
            return get();
        }
    }
}
