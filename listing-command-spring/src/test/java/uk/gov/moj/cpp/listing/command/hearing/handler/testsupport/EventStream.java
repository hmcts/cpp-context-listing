package uk.gov.moj.cpp.listing.command.hearing.handler.testsupport;

import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.stream.Stream;

/** Test stand-in for a framework event stream: what the handler raised for one hearing ({@link HearingFlowTestBridge}). */
public interface EventStream {
    void append(Stream<Envelope> events);
}
