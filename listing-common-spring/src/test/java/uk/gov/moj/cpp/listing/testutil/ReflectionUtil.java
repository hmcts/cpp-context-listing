package uk.gov.moj.cpp.listing.testutil;

import java.lang.reflect.Field;

/** Test-only replacement for the framework's ReflectionUtil (sets private, including final, fields). */
public final class ReflectionUtil {

    private ReflectionUtil() {
    }

    public static void setField(final Object target, final String name, final Object value) {
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                final Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (final NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalArgumentException("No field " + name);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getValueOfField(final Object target, final String name, final Class<T> type) {
        Class<?> c = target.getClass();
        while (c != null) {
            try {
                final Field field = c.getDeclaredField(name);
                field.setAccessible(true);
                return (T) field.get(target);
            } catch (final NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalArgumentException("No field " + name);
    }
}
