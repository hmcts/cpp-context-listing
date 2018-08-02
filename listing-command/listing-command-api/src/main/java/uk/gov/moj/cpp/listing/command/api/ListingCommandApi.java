package uk.gov.moj.cpp.listing.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class ListingCommandApi {

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("listing.command.send-case-for-listing")
    public void sendCaseForListing(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.update-hearing-for-listing")
    public void updateHearingForListing(final JsonEnvelope envelope) {
        final JsonEnvelope newEnvelope = enveloper.withMetadataFrom(envelope, "listing.command.handler.update-hearing-for-listing").apply(envelope.payloadAsJsonObject());
        sender.send(newEnvelope);
    }

    @Handles("listing.command.update-case-defendant-details")
    public void updateCaseDefendantDetails(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.update-case-defendant-offences")
    public void updateCaseDefendantOffencesDetails(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.update-defendants-for-hearing")
    public void updateDefendantForHearing(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.update-offences-for-hearing")
    public void updateOffencesForHearing(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.delete-offences-for-hearing")
    public void deleteOffencesForHearing(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.add-offences-for-hearing")
    public void addOffencesForHearing(final JsonEnvelope envelope) {
        sender.send(envelope);
    }
}
