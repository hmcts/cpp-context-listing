package uk.gov.moj.cpp.listing.command.hearing.flow;

/**
 * A ported JEE event-processor component (MBD refactor P2.3.2, ADR 015): registers, per event
 * class, its {@code @Handles} method (the public messages, the follow-up commands — now direct
 * calls — and the other contexts it called). Run in the command's transaction after every listener
 * change of the command, in the order the events were raised ({@link ProcessorQueue}).
 */
public interface ProcessorPort {

    void register(EventHandlers processors);
}
