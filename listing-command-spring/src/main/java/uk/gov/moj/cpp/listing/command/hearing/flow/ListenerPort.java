package uk.gov.moj.cpp.listing.command.hearing.flow;

/**
 * A ported JEE event-listener component (MBD refactor P2.3.2, ADR 015): registers, per event class,
 * the viewstore change its {@code @Handles} method made. Run synchronously, in the command's
 * transaction, for each event the rules raise.
 */
public interface ListenerPort {

    void register(EventHandlers listeners);
}
