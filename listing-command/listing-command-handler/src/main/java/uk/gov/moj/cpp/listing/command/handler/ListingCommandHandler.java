package uk.gov.moj.cpp.listing.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;

@ServiceComponent(COMMAND_HANDLER)
public class ListingCommandHandler {

    @Handles("listing.list-case-for-hearing")
    public void listCaseForHearing(final JsonEnvelope command) throws EventStreamException {

    }

}