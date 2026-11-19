package uk.gov.moj.cpp.listing.command.hearing.handler.testsupport;

import java.util.UUID;

/** Test stand-in for the framework event source the ported JEE handler tests stub (one stream per hearing). */
public interface EventSource {
    EventStream getStreamById(UUID id);
}
