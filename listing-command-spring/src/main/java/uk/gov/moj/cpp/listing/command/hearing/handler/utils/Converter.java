package uk.gov.moj.cpp.listing.command.hearing.handler.utils;

/** The framework's {@code Converter} contract the ported handler converters implement. */
@FunctionalInterface
public interface Converter<S, T> {
    T convert(S source);
}
