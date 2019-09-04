package uk.gov.moj.cpp.listing.event.processor;


import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.listing.commands.AddApplicationToHearingCommand;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtListRestricted;
import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.command.AddCourtApplicationToHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddDefendantsForCourtProceedingsCommand;
import uk.gov.moj.cpp.listing.event.processor.command.AddDefendantsForCourtProceedingsCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddHearingToCaseCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.AddOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.DeleteOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.DeleteOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommandCollectionConverter;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ListingEventProcessor {

    public static final String COMMAND_PAYLOAD_DEBUG_STRING = "Sending '{}' command with payload {}";
    public static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    static final String PUBLIC_EVENT_HEARING_LISTED = "public.listing.hearing-listed";
    static final String PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    static final String PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED = "public.progression.court-application-changed";
    static final String PUBLIC_EVENT_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    static final String PUBLIC_EVENT_HEARING_UPDATED = "public.listing.hearing-updated";
    static final String PUBLIC_EVENT_RESTRICT_COURT_LIST = "public.listing.court-list-restricted";
    static final String PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION = "public.progression.events.hearing-extended";
    static final String PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";
    static final String PRIVATE_EVENT_HEARING_LISTED = "listing.events.hearing-listed";
    static final String PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING = "listing.events.allocated-hearing-updated-for-listing";
    static final String PRIVATE_EVENT_COURT_APPLICATION_ADDED_FOR_LISTED_HEARING = "listing.events.court-application-added-for-hearing";
    static final String PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED = "listing.events.court-application-to-be-updated";
    static final String PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    static final String PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED = "listing.events.defendants-to-be-updated";
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED = "listing.events.offences-to-be-updated";
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_DELETED = "listing.events.offences-to-be-deleted";
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_ADDED = "listing.events.offences-to-be-added";
    static final String PRIVATE_EVENT_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS ="listing.events.defendants-to-be-added-for-court-proceedings" ;
    static final String PRIVATE_EVENT_RESTRICT_COURT_LIST = "listing.events.court-list-restricted";
    static final String COMMAND_ADD_HEARING_TO_CASE = "listing.command.add-hearing-to-case";
    static final String COMMAND_ADD_COURT_APPLICATION_TO_HEARING = "listing.command.add-court-application-to-hearing";
    static final String COMMAND_UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing";
    static final String COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING="listing.command.add-defendants-to-court-proceedings-for-hearing" ;
    static final String COMMAND_UPDATE_CASE_DEFENDANT_DETAILS = "listing.command.update-case-defendant-details";
    static final String COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES = "listing.command.update-case-defendant-offences";
    static final String COMMAND_UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    static final String COMMAND_DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    static final String COMMAND_ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";
    static final String COMMAND_UPDATE_COURT_APPLICATION_FOR_HEARINGS = "listing.command.update-court-application-for-hearings";
    static final String COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS="listing.command.add-defendants-to-court-proceedings";
    static final String COMMAND_UPDATE_COURT_APPLICATION = "listing.command.update-court-application";
    static final String COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING = "listing.command.add-court-application-for-hearing";
    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventProcessor.class);
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "hearingId";
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AllocatedHearingUpdatedFactory allocatedHearingUpdatedFactory;

    @Inject
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Inject
    private AddHearingToCaseCommandCollectionConverter listHearingCommandConverter;

    @Inject
    private UpdateDefendantsForHearingCommandCollectionConverter updateDefendantsForHearingCommandCollectionConverter;

    @Inject
    private AddDefendantsForCourtProceedingsCommandCollectionConverter addDefendantsForCourtProceedingsCommandCollectionConverter;

    @Inject
    private UpdateOffencesForHearingCommandCollectionConverter updateOffencesForHearingCommandCollectionConverter;

    @Inject
    private AddOffencesForHearingCommandCollectionConverter addOffencesForHearingCommandCollectionConverter;

    @Inject
    private DeleteOffencesForHearingCommandCollectionConverter deleteOffencesForHearingCommandCollectionConverter;

    @Inject
    private AddCourtApplicationToHearingCommandCollectionConverter addCourtApplicationToHearingCommandCollectionConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Handles(PRIVATE_EVENT_HEARING_LISTED)
    public void handleHearingListedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_LISTED, envelope.toObfuscatedDebugString());
        }

        sendCommandAddHearingToCase(envelope);
        sendCommandAddApplicationToHearing(envelope);
    }

    @Handles(PRIVATE_EVENT_COURT_APPLICATION_ADDED_FOR_LISTED_HEARING)
    public void handleCourtApplicationAddedForListedHearing(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_COURT_APPLICATION_ADDED_FOR_LISTED_HEARING, envelope.toObfuscatedDebugString());
        }

        sendCommandAddApplicationToListedHearing(envelope);
    }

    @Handles(PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED)
    public void handleDefendantsToBeUpdatedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }
        sendUpdateDefendantsForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS)
    public void handleDefendantsToBeAddedForCourtProceedingsMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS, envelope.toObfuscatedDebugString());
        }
        sendAddDefendantsForCourtProceedings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED)
    public void handleOffencesToBeUpdatedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }

        sendUpdatedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_DELETED)
    public void handleOffencesToBeDeletedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_DELETED, envelope.toObfuscatedDebugString());
        }

        sendDeletedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_ADDED)
    public void handleOffencesToBeAddedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_ADDED, envelope.toObfuscatedDebugString());
        }

        sendAddedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING)
    public void handleHearingAllocatedForListingMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING, envelope.toObfuscatedDebugString());
        }

        publishHearingConfirmedPublicEvent(envelope);
    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING)
    public void handleAllocatedHearingUpdatedForListingMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING, envelope.toObfuscatedDebugString());
        }

        publishHearingUpdatedPublicEvent(envelope);
    }

    @Handles(PRIVATE_EVENT_RESTRICT_COURT_LIST)
    public void handleRestrictCourtListMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_RESTRICT_COURT_LIST, envelope.toObfuscatedDebugString());
        }

        publishCourtListRestrictedPublicEvent(envelope);
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED)
    public void handleCaseDefendantChangedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_CASE_DEFENDANT_DETAILS).apply(envelope.payloadAsJsonObject()));
    }


    @Handles(PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED)
    public void handleDefendantOffencesChanged(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES).apply(envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED)
    public void handleCourtApplicationChanged(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_COURT_APPLICATION).apply(envelope.payloadAsJsonObject()));
    }

    @Handles(PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED)
    public void handleCourtApplicationToBeUpdated(final JsonEnvelope envelope){
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_COURT_APPLICATION_FOR_HEARINGS).apply(envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION)
    public void handleExtendListedHearingForCourtApplication(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION, envelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING).apply(envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS)
    public void handleDefendantAddedForCourtProceedings(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS, envelope.toObfuscatedDebugString());
        }
        sender.send(enveloper.withMetadataFrom(envelope, COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS).apply(envelope.payloadAsJsonObject()));
    }

    /*
     * For each hearing in the 'case-sent-for-listing' event, extract it
     * and send each one through as a separate 'list-hearing' command.
     */
    private void sendCommandAddHearingToCase(final JsonEnvelope envelope) {
        final HearingListed event = getHearingListedEvent(envelope);
        final List<AddHearingToCaseCommand> listHearingCommands = listHearingCommandConverter.convert(event);

        listHearingCommands.forEach(
                listHearingCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_HEARING_TO_CASE, listHearingCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_ADD_HEARING_TO_CASE)
                            .apply(objectToJsonValueConverter.convert(listHearingCommand)));
                }
        );
    }

    private void sendCommandAddApplicationToHearing(JsonEnvelope envelope) {
        final HearingListed event = getHearingListedEvent(envelope);
        final List<AddApplicationToHearingCommand> addApplicationToHearingCommands = addCourtApplicationToHearingCommandCollectionConverter.convert(event);
        addApplicationToHearingCommands.forEach(addApplicationToHearingCommand ->{
            LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_COURT_APPLICATION_TO_HEARING, addApplicationToHearingCommand);
            sender.send(enveloper.withMetadataFrom(envelope,
                    COMMAND_ADD_COURT_APPLICATION_TO_HEARING).apply(objectToJsonValueConverter.convert(addApplicationToHearingCommand)));
        });
    }
    private void sendCommandAddApplicationToListedHearing(JsonEnvelope envelope) {
        final CourtApplicationAddedForHearing event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtApplicationAddedForHearing.class);
        sender.send(enveloper.withMetadataFrom(envelope,
                COMMAND_ADD_COURT_APPLICATION_TO_HEARING).apply(createObjectBuilder()
                .add(APPLICATION_ID, event.getCourtApplication().getId().toString())
                .add(HEARING_ID, event.getHearingId().toString()).build()));
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
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_DEFENDANTS_FOR_HEARING, updateDefendantsForHearingCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_UPDATE_DEFENDANTS_FOR_HEARING)
                            .apply(objectToJsonValueConverter.convert(updateDefendantsForHearingCommand)));
                }
        );
    }

    private void sendAddDefendantsForCourtProceedings(final JsonEnvelope envelope) {
        final DefendantsToBeAddedForCourtProceedings event = getDefendantsToBeAddedForCourtProceedings(envelope);
        final List<AddDefendantsForCourtProceedingsCommand> commands = addDefendantsForCourtProceedingsCommandCollectionConverter.convert(event);

        commands.forEach(
                addDefendantsForCourtProceedingsCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING, addDefendantsForCourtProceedingsCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING)
                            .apply(objectToJsonValueConverter.convert(addDefendantsForCourtProceedingsCommand)));
                }
        );
    }
    private void sendUpdatedOffencesForHearings(final JsonEnvelope envelope) {
        final OffencesToBeUpdated event = getOffencesToBeUpdated(envelope);
        final List<UpdateOffencesForHearingCommand> commands = updateOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                updateOffencesForHearingCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_OFFENCES_FOR_HEARING, updateOffencesForHearingCommand);
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
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_DELETE_OFFENCES_FOR_HEARING, deleteOffencesForHearingCommand);
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
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_OFFENCES_FOR_HEARING, addOffencesForHearingCommand);
                    sender.send(enveloper.withMetadataFrom(envelope, COMMAND_ADD_OFFENCES_FOR_HEARING)
                            .apply(objectToJsonValueConverter.convert(addOffencesForHearingCommand)));
                }
        );
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

    /*
     * Publish a public event to notify that the hearing has been updated with restrictions on court listing.
     */
    private void publishCourtListRestrictedPublicEvent(final JsonEnvelope envelope) {
        final CourtListRestricted courtListRestricted =
                jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtListRestricted.class);
        LOGGER.info("Publishing '{}' public event with payload {}", PUBLIC_EVENT_RESTRICT_COURT_LIST, courtListRestricted);
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_RESTRICT_COURT_LIST).apply(courtListRestricted));
    }

    private HearingListed getHearingListedEvent(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingListed.class);
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

    private DefendantsToBeAddedForCourtProceedings getDefendantsToBeAddedForCourtProceedings(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantsToBeAddedForCourtProceedings.class);
    }

}
