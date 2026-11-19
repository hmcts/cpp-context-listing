package uk.gov.moj.cpp.listing.command.hearing.processor;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.messaging.envelope.Metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The framework test utilities the ported JEE processor tests used (MBD refactor P2.3.2):
 * {@code MetadataBuilderFactory}, {@code RandomGenerator}, {@code ReflectionUtil.setField},
 * {@code FileUtil.givenPayload}, on the framework-free {@link Metadata}.
 */
public final class ProcessorTestSupport {

    public static final Generator<String> STRING = () -> randomString(10);
    public static final Generator<Integer> INTEGER = () -> ThreadLocalRandom.current().nextInt(1, 10_000);
    public static final Generator<UUID> UUID = java.util.UUID::randomUUID;
    public static final Generator<LocalDate> FUTURE_LOCAL_DATE = () -> LocalDate.now().plusDays(ThreadLocalRandom.current().nextInt(1, 3650));

    private ProcessorTestSupport() {
    }

    public static Metadata.Builder metadataWithRandomUUID(final String name) {
        return Metadata.builder().withId(randomUUID()).withName(name);
    }

    public static Metadata.Builder metadataWithRandomUUIDAndName() {
        return metadataWithRandomUUID(randomString(10));
    }

    public static Generator<String> string(final int length) {
        return () -> randomString(length);
    }

    public static void setField(final Object target, final String name, final Object value) {
        ReflectionTestUtils.setField(target, name, value);
    }

    public static JsonObject givenPayload(final String path) {
        try (InputStream in = ProcessorTestSupport.class.getResourceAsStream(path)) {
            return Json.createReader(in).readObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String randomString(final int length) {
        final StringBuilder s = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            s.append((char) ('a' + ThreadLocalRandom.current().nextInt(26)));
        }
        return s.toString();
    }

    @FunctionalInterface
    public interface Generator<T> extends Supplier<T> {
        default T next() {
            return get();
        }
    }
}
