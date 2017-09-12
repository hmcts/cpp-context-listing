package uk.gov.moj.cpp.listing.command.api;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

@ServiceComponent(COMMAND_API)
public class ReferenceDataCommandApi {

    @Inject
    private Sender sender;

    @Handles("listing.command.add-judge")
    public void addJudge(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.add-court-centre")
    public void addCourtCentre(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.add-court-room")
    public void addCourtRoom(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

}