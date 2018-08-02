package uk.gov.moj.cpp.listing.event.processor;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.events.*;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.external.HearingConfirmed;
import uk.gov.moj.cpp.listing.event.external.HearingUpdated;
import uk.gov.moj.cpp.listing.event.processor.command.*;

import javax.inject.Inject;
import java.util.List;

import static java.lang.String.format;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

@ServiceComponent(EVENT_PROCESSOR)
public class ListingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventProcessor.class);

    static final String PUBLIC_EVENT_CASE_SENT_FOR_LISTING = "public.listing.case-sent-for-listing";
    static final String PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    static final String PUBLIC_EVENT_PROGRESSION_DEFENDANT_OFFENCES_CHANGED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_EVENT_HEARING_CONFIRMED = "public.hearing-confirmed";
    static final String PUBLIC_EVENT_HEARING_UPDATED = "public.hearing-updated";
    static final String PRIVATE_EVENT_CASE_SENT_FOR_LISTING = "listing.events.case-sent-for-listing";
    static final String PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING = "listing.events.allocated-hearing-updated-for-listing";
    static final String PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    static final String PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED = "listing.events.defendants-to-be-updated";
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED = "listing.events.offences-to-be-updated";
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_DELETED = "listing.events.offences-to-be-deleted";
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_ADDED = "listing.events.offences-to-be-added";
    static final String COMMAND_LIST_HEARING = "listing.command.list-hearing";
    static final String COMMAND_UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing";
    static final String COMMAND_UPDATE_CASE_DEFENDANT_DETAILS = "listing.command.update-case-defendant-details";
    static final String COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES = "listing.command.update-case-defendant-offences";
    static final String COMMAND_UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    static final String COMMAND_DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    static final String COMMAND_ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AllocatedHearingUpdatedFactory allocatedHearingUpdatedFactory;

    @Inject
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Inject
    private ListHearingCommandCollectionConverter listHearingCommandConverter;

    @Inject
    private UpdateDefendantsForHearingCommandCollectionConverter updateDefendantsForHearingCommandCollectionConverter;

    @Inject
    private UpdateOffencesForHearingCommandCollectionConverter updateOffencesForHearingCommandCollectionConverter;

    @Inject
    private AddOffencesForHearingCommandCollectionConverter addOffencesForHearingCommandCollectionConverter;

    @Inject DeleteOffencesForHearingCommandCollectionConverter deleteOffencesForHearingCommandCollectionConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Handles(PRIVATE_EVENT_CASE_SENT_FOR_LISTING)
    public void handleCaseSentForListingMessage(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PRIVATE_EVENT_CASE_SENT_FOR_LISTING, envelope.payloadAsJsonObject());

        sendListHearingCommands(envelope);

        publishCaseSentForListingPublicEvent(envelope);
    }

    @Handles(PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED)
    public void handleDefendantsToBeUpdatedMessage(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED, envelope.payloadAsJsonObject());

        sendUpdateDefendantsForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED)
    public void handleOffencesToBeUpdatedMessage(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED, envelope.payloadAsJsonObject());

        sendUpdatedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_DELETED)
    public void handleOffencesToBeDeletedMessage(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PRIVATE_EVENT_OFFENCES_TO_BE_DELETED, envelope.payloadAsJsonObject());

        sendDeletedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_ADDED)
    public void handleOffencesToBeAddedMessage(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PRIVATE_EVENT_OFFENCES_TO_BE_ADDED, envelope.payloadAsJsonObject());

        sendAddedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING)
    public void handleHearingAllocatedForListingMessage(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING, envelope.payloadAsJsonObject());

        publishHearingConfirmedPublicEvent(envelope);
    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING)
    public void handleAllocatedHearingUpdatedForListingMessage(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING, envelope.payloadAsJsonObject());

        publishHearingUpdatedPublicEvent(envelope);
    }


    @Handles(PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED)
    public void handleCaseDefendantChangedMessage(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED, envelope.payloadAsJsonObject());
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_CASE_DEFENDANT_DETAILS).apply(envelope.payloadAsJsonObject()));
    }


    @Handles(PUBLIC_EVENT_PROGRESSION_DEFENDANT_OFFENCES_CHANGED)
    public void handleDefendantOffencesChanged(final JsonEnvelope envelope) {
        LOGGER.info("Received '{}' event with payload {}", PUBLIC_EVENT_PROGRESSION_DEFENDANT_OFFENCES_CHANGED, envelope.payloadAsJsonObject());
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES).apply(envelope.payloadAsJsonObject()));
    }

    /*
     * For each hearing in the 'case-sent-for-listing' event, extract it
     * and send each one through as a separate 'list-hearing' command.
     */
    private void sendListHearingCommands(final JsonEnvelope envelope) {
        final CaseSentForListing event = getCaseSentForListing(envelope);
        final List<ListHearingCommand> listHearingCommands = listHearingCommandConverter.convert(event);

        listHearingCommands.forEach(
                listHearingCommand -> {
                    LOGGER.debug("Sending '{}' command with payload {}", COMMAND_LIST_HEARING, listHearingCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_LIST_HEARING)
                            .apply(objectToJsonValueConverter.convert(listHearingCommand)));
                }
        );
    }

    /*
     * For each hearingId in the 'defendants-to-be-updated' event, extract it
     * and send each one through as a separate 'update-defendants-for-Hearing' command.
     */
    private void sendUpdateDefendantsForHearings(final JsonEnvelope envelope) {
        final DefendantsToBeUpdated event = getDefendantsToBeUpdated(envelope);
        final List<UpdateDefendantsForHearingCommand> commands = updateDefendantsForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                updateDefendantsForHearingCommand -> {
                    LOGGER.debug("Sending '{}' command with payload {}", COMMAND_UPDATE_DEFENDANTS_FOR_HEARING, updateDefendantsForHearingCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_DEFENDANTS_FOR_HEARING)
                            .apply(objectToJsonValueConverter.convert(updateDefendantsForHearingCommand)));
                }
        );
    }

    private void sendUpdatedOffencesForHearings(final JsonEnvelope envelope) {
        final OffencesToBeUpdated event = getOffencesToBeUpdated(envelope);
        final List<UpdateOffencesForHearingCommand> commands = updateOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                updateOffencesForHearingCommand -> {
                    LOGGER.debug("Sending '{}' command with payload {}", COMMAND_UPDATE_OFFENCES_FOR_HEARING, updateOffencesForHearingCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_OFFENCES_FOR_HEARING)
                            .apply(objectToJsonValueConverter.convert(updateOffencesForHearingCommand)));
                }
        );
    }

    private void sendDeletedOffencesForHearings(final JsonEnvelope envelope) {
        final OffencesToBeDeleted event = getOffencesToBeDeleted(envelope);
        final List<DeleteOffencesForHearingCommand> commands = deleteOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                deleteOffencesForHearingCommand -> {
                    LOGGER.debug("Sending '{}' command with payload {}", COMMAND_DELETE_OFFENCES_FOR_HEARING, deleteOffencesForHearingCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_DELETE_OFFENCES_FOR_HEARING)
                            .apply(objectToJsonValueConverter.convert(deleteOffencesForHearingCommand)));
                }
        );
    }

    private void sendAddedOffencesForHearings(final JsonEnvelope envelope) {
        final OffencesToBeAdded event = getOffencesToBeAdded(envelope);
        final List<AddOffencesForHearingCommand> commands = addOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                addOffencesForHearingCommand -> {
                    LOGGER.debug("Sending '{}' command with payload {}", COMMAND_ADD_OFFENCES_FOR_HEARING, addOffencesForHearingCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_ADD_OFFENCES_FOR_HEARING)
                            .apply(objectToJsonValueConverter.convert(addOffencesForHearingCommand)));
                }
        );
    }

    /*
     * Publish a public event to notify that the case has been listed.
     */
    private void publishCaseSentForListingPublicEvent(final JsonEnvelope envelope) {
        final CaseSentForListing event = getCaseSentForListing(envelope);
        LOGGER.info("Publishing '{}' public event with payload {}", PUBLIC_EVENT_CASE_SENT_FOR_LISTING, event);
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_CASE_SENT_FOR_LISTING).apply(event));
    }

    /*
     * Publish a public event to notify that the hearing has been confirmed.
     */
    private void publishHearingConfirmedPublicEvent(final JsonEnvelope envelope) {
        final HearingAllocatedForListing hearingAllocatedForListing = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingAllocatedForListing.class);
        final HearingConfirmed hearingConfirmed = hearingConfirmedFactory.create(hearingAllocatedForListing);

        LOGGER.info("Publishing '{}' public event with payload {}", PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_HEARING_CONFIRMED).apply(hearingConfirmed));
    }

    /*
     * Publish a public event to notify that the hearing has been updated.
     */
    private void publishHearingUpdatedPublicEvent(final JsonEnvelope envelope) {
        final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing =
                jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AllocatedHearingUpdatedForListing.class);
        final HearingUpdated hearingUpdated = allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing);

        LOGGER.info("Publishing '{}' public event with payload {}", PUBLIC_EVENT_HEARING_UPDATED, hearingUpdated);
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_HEARING_UPDATED).apply(hearingUpdated));
    }

    private CaseSentForListing getCaseSentForListing(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CaseSentForListing.class);
    }

    private DefendantsToBeUpdated getDefendantsToBeUpdated(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantsToBeUpdated.class);
    }

    private OffencesToBeUpdated getOffencesToBeUpdated(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), OffencesToBeUpdated.class);
    }

    private OffencesToBeDeleted getOffencesToBeDeleted(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), OffencesToBeDeleted.class);
    }

    private OffencesToBeAdded getOffencesToBeAdded(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), OffencesToBeAdded.class);
    }


}
