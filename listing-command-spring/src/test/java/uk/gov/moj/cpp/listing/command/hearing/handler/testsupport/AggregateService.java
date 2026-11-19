package uk.gov.moj.cpp.listing.command.hearing.handler.testsupport;

/** Test stand-in for the framework aggregate service: the hearing rules (state) a test prepared for a stream. */
public interface AggregateService {
    <T> T get(EventStream stream, Class<T> type);
}
