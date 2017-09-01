package uk.gov.moj.cpp.listing.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class ListingCommandApi {

    @Inject
    private Sender sender;

    @Handles("listing.list-case-for-hearing")
    public void listCaseForHearing(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

}