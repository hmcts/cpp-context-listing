package uk.gov.moj.cpp.listing.command.hearing.api;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * The framework test-utils the ported command-API tests used ({@code RandomGenerator},
 * {@code MetadataBuilderFactory}, {@code JsonEnvelope.metadataBuilder}), on the framework-free types.
 */
public final class TestSupport {

    public static final Generator<LocalDate> FUTURE_LOCAL_DATE = () -> LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(1, 3650));
    public static final Generator<ZonedDateTime> FUTURE_ZONED_DATE_TIME = () -> ZonedDateTime.now()
            .plusMinutes(ThreadLocalRandom.current().nextInt(1, 5_000_000)).truncatedTo(ChronoUnit.MILLIS);
    public static final Generator<ZonedDateTime> FUTURE_UTC_DATE_TIME = () -> ZonedDateTime.now(ZoneOffset.UTC)
            .plusMinutes(ThreadLocalRandom.current().nextInt(1, 5_000_000)).truncatedTo(ChronoUnit.MILLIS);
    public static final Generator<Integer> INTEGER = () -> ThreadLocalRandom.current().nextInt(0, 10_000);
    public static final Generator<String> STRING = () -> UUID.randomUUID().toString().replace("-", "");

    private TestSupport() {
    }

    public static Metadata.Builder metadataBuilder() {
        return Metadata.builder();
    }

    public static Metadata.Builder metadataWithDefaults() {
        return Metadata.builder().withId(randomUUID()).withName("listing.test").withUserId(randomUUID().toString());
    }

    public static Metadata.Builder metadataWithRandomUUID(final String name) {
        return Metadata.builder().withId(randomUUID()).withName(name);
    }

    public static Metadata.Builder metadataWithRandomUUIDAndName() {
        return Metadata.builder().withId(randomUUID()).withName("listing.test");
    }

    @FunctionalInterface
    public interface Generator<T> extends Supplier<T> {
        default T next() {
            return get();
        }
    }
}
