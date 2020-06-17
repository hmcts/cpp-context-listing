package uk.gov.moj.cpp.listing.event.processor;


import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import org.apache.commons.collections.CollectionUtils;
import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.listing.commands.AddApplicationToHearingCommand;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.commands.LinkedToCases;
import uk.gov.justice.listing.commands.UpdateLinkedCaseInHearing;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.courts.UpdateHearingToCaseCommand;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListing;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;
import uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtListRestricted;
import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.LinkedCasesToBeUpdated;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.progression.courts.HearingExtended;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
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
import uk.gov.moj.cpp.listing.event.processor.command.ExtendHearingToListedCaseCommandConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateCaseMarkersForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateCaseMarkersForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommandCollectionConverter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S2629")
@ServiceComponent(EVENT_PROCESSOR)
public class ListingEventProcessor {

    public static final String COMMAND_PAYLOAD_DEBUG_STRING = "Sending '{}' command with payload {}";
    public static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    public static final String HEARING_IDS = "hearingIds";
    public static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    public static final String REMOVAL_REASON = "removalReason";
    static final String PUBLIC_EVENT_HEARING_LISTED = "public.listing.hearing-listed";
    static final String PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed"; //public
    static final String PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED = "public.progression.court-application-changed";
    static final String PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED = "public.progression.case-markers-updated";
    static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";
    static final String PUBLIC_EVENT_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    static final String PUBLIC_EVENT_HEARING_UPDATED = "public.listing.hearing-updated";
    static final String PUBLIC_EVENT_VACATED_TRIAL_UPDATED = "public.listing.vacated-trial-updated";
    static final String PUBLIC_EVENT_RESTRICT_COURT_LIST = "public.listing.court-list-restricted";
    static final String PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION = "public.progression.events.hearing-extended";
    static final String PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";
    static final String PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED = "public.progression.events.case-or-application-ejected";
    static final String PUBLIC_EVENT_PROGRESSION_CASE_LINKED = "public.progression.case-linked";
    static final String PUBLIC_EVENT_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    static final String LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS = "listing.events.case-ejected-for-hearings";
    static final String LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS = "listing.events.application-ejected-for-hearings";
    static final String PRIVATE_EVENT_HEARING_LISTED = "listing.events.hearing-listed";
    static final String PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING = "listing.events.allocated-hearing-updated-for-listing";
    static final String PRIVATE_EVENT_COURT_APPLICATION_ADDED_FOR_LISTED_HEARING = "listing.events.court-application-added-for-hearing";
    static final String PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED = "listing.events.court-application-to-be-updated";
    static final String PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    static final String PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED = "listing.events.defendants-to-be-updated"; //private event back of command
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED = "listing.events.offences-to-be-updated";
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_DELETED = "listing.events.offences-to-be-deleted";
    static final String PRIVATE_EVENT_OFFENCES_TO_BE_ADDED = "listing.events.offences-to-be-added";
    static final String PRIVATE_EVENT_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS = "listing.events.defendants-to-be-added-for-court-proceedings";
    static final String PRIVATE_EVENT_RESTRICT_COURT_LIST = "listing.events.court-list-restricted";
    static final String PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED = "listing.events.case-markers-to-be-updated";
    static final String PRIVATE_EVENT_LINKED_CASES_TO_BE_UPDATED = "listing.events.linked-cases-to-be-updated";
    static final String PRIVATE_EVENT_TRIAL_VACATED = "listing.events.trial-vacated";
    static final String PRIVATE_EVENT_HEARING_RESCHEDULED = "listing.events.hearing-rescheduled";
    static final String LISTING_EVENTS_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED = "listing.events.case-resulted-defendant-proceedings-updated";
    static final String COMMAND_ADD_HEARING_TO_CASE = "listing.command.add-hearing-to-case";
    static final String COMMAND_ADD_COURT_APPLICATION_TO_HEARING = "listing.command.add-court-application-to-hearing";
    static final String COMMAND_UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing"; // command back of private event
    static final String COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING = "listing.command.add-defendants-to-court-proceedings-for-hearing";
    static final String COMMAND_UPDATE_CASE_DEFENDANT_DETAILS = "listing.command.update-case-defendant-details"; //command back of public event
    static final String COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES = "listing.command.update-case-defendant-offences";
    static final String COMMAND_UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    static final String COMMAND_DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    static final String COMMAND_ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";
    static final String COMMAND_UPDATE_COURT_APPLICATION_FOR_HEARINGS = "listing.command.update-court-application-for-hearings";
    static final String COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS = "listing.command.add-defendants-to-court-proceedings";
    static final String COMMAND_UPDATE_COURT_APPLICATION = "listing.command.update-court-application";
    static final String COMMAND_UPDATE_CASE_MARKERS_TO_HEARING = "listing.command.update-case-markers-to-hearing";
    static final String COMMAND_UPDATE_CASE_MARKERS = "listing.command.update-case-markers";
    static final String COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING = "listing.command.add-court-application-for-hearing";
    static final String COMMAND_CASE_OR_APPLICATION_EJECTED = "listing.command.eject-case-or-application";
    static final String COMMAND_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED = "listing.command.case-update-defendant-proceedings-updated";
    static final String COMMAND_CASE_EJECTED = "listing.command.eject-case";
    static final String COMMAND_APPLICATION_EJECTED = "listing.command.eject-application";
    static final String COMMAND_UPDATE_LINKED_CASES = "listing.command.update-linked-cases";
    static final String COMMAND_UPDATE_LINKED_CASE_IN_HEARING = "listing.command.update-linked-case-in-hearing";
    static final String COMMAND_UPDATE_HEARING_TO_CASE = "listing.command.update-hearing-to-case";
    static final String PRIVATE_COMMAND_VACATE_TRIAL = "listing.command.vacate-trial-enriched";
    static final String PRIVATE_COMMAND_HEARING_VACATE_TRIAL = "listing.command.hearing-vacate-trial";
    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventProcessor.class);
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "hearingId";
    private static final String IS_VACATED = "isVacated";
    private static final String ALLOCATED = "allocated";
    private static final String REASON_ID = "vacatedTrialReasonId";
    private static final String LOG_PUBLISHING = "Publishing '{}' public event with payload {}";
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final String COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS = "listing.command.update-defendant-legalaid-status";
    private static final String COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS_FOR_HEARING = "listing.command.update-defendant-legalaid-status-for-hearing";
    private static final String LISTING_EVENTS_DEFENDANT_LEGALAID_STATUS_UPDATED = "listing.events.defendant-legalaid-status-updated";
    private static final String COMMAND_UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED = "listing.command.update-case-resulted-defendant-proceedings-concluded";
    private static final String PRIVATE_COMMAND_ADD_CASES_FOR_HEARING = "listing.command.add-cases-for-hearing";
    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING = "listing.events.allocated-hearing-extended-for-listing";
    private static final String LEGAL_AID_STATUS = "legalAidStatus";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID = "caseId";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private AllocatedHearingUpdatedFactory allocatedHearingUpdatedFactory;

    @Inject
    private AllocatedHearingExtendedFactory allocatedHearingExtendedFactory;

    @Inject
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Inject
    private AddHearingToCaseCommandCollectionConverter listHearingCommandConverter;

    @Inject
    private ExtendHearingToListedCaseCommandConverter extendHearingToListedCaseCommandConverter;

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
    private UpdateCaseMarkersForHearingCommandCollectionConverter updateCaseMarkersForHearingCommandCollectionConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private SlotUpdater slotUpdater;

    @Handles(PRIVATE_EVENT_HEARING_LISTED)
    public void handleHearingListedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_LISTED, envelope.toObfuscatedDebugString());
        }

        sendCommandAddHearingToCase(envelope);
        sendCommandAddApplicationToHearing(envelope);
    }

    @Handles(PRIVATE_EVENT_TRIAL_VACATED)
    public void trialVacated(final JsonEnvelope envelope) {
        LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_TRIAL_VACATED, envelope.toObfuscatedDebugString());

        final TrialVacated trialVacated = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), TrialVacated.class);

        final JsonObject vacatedTrialUpdatedPayload = Json.createObjectBuilder()
                .add(HEARING_ID, trialVacated.getHearingId().toString())
                .add(IS_VACATED, true)
                .add(REASON_ID, trialVacated.getVacatedTrialReasonId().toString())
                .add(ALLOCATED, trialVacated.getAllocated())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_VACATED_TRIAL_UPDATED),
                vacatedTrialUpdatedPayload));
    }

    @Handles(PRIVATE_EVENT_HEARING_RESCHEDULED)
    public void hearingRescheduled(final JsonEnvelope envelope) {

        final HearingRescheduled hearingRescheduled = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingRescheduled.class);

        final JsonObject vacatedTrialUpdatedPayload = Json.createObjectBuilder()
                .add(HEARING_ID, hearingRescheduled.getHearingId().toString())
                .add(IS_VACATED, false)
                .add(ALLOCATED, hearingRescheduled.getAllocated())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_VACATED_TRIAL_UPDATED),
                vacatedTrialUpdatedPayload));
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

        final HearingConfirmed hearingConfirmed = getHearingConfirmed(envelope);
        final HearingAllocatedForListing hearingAllocatedForListing = getHearingAllocatedForListing(envelope);
        final boolean isSlotUpdated = hearingAllocatedForListing.getUpdateSlot().orElse(false);
        final boolean isForAdjournmentHearing = hearingAllocatedForListing.getHasAdjournmentDate().orElse(false);

        LOGGER.debug("HearingConfirmed confirmedHearing used for slot update: {}", hearingConfirmed.getConfirmedHearing());

        slotUpdater.updateSlot(envelope, hearingConfirmed.getConfirmedHearing(), isSlotUpdated, isForAdjournmentHearing);
    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING)
    public void handleAllocatedHearingUpdatedForListingMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING, envelope.toObfuscatedDebugString());
        }

        publishHearingUpdatedPublicEvent(envelope);

        final HearingUpdated hearingUpdated = getHearingUpdated(envelope);
        final boolean isSlotUpdated = getAllocatedHearingUpdatedForListing(envelope).getUpdateSlot().orElse(false);

        LOGGER.debug("HearingUpdated confirmedHearing used for slot update: {}", hearingUpdated.getUpdatedHearing());

        slotUpdater.updateSlot(envelope, hearingUpdated.getUpdatedHearing(), isSlotUpdated, false);
    }

    @Handles(PRIVATE_EVENT_RESTRICT_COURT_LIST)
    public void handleRestrictCourtListMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_RESTRICT_COURT_LIST, envelope.toObfuscatedDebugString());
        }

        publishCourtListRestrictedPublicEvent(envelope);
    }

    @Handles(PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED)
    public void handleCaseMarkerUpdateMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }

        sendCaseMarkerUpdatedForHearing(envelope);
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED)
    public void handleCaseDefendantChangedMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_DEFENDANT_DETAILS),
                envelope.payloadAsJsonObject()));
    }


    @Handles(PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED)
    public void handleDefendantOffencesChanged(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED)
    public void handleCourtApplicationChanged(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_COURT_APPLICATION),
                envelope.payloadAsJsonObject()));

    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    @Handles(PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED)
    public void handleCaseMarkerUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_MARKERS),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED)
    public void handleCourtApplicationToBeUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_COURT_APPLICATION_FOR_HEARINGS),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION)
    public void handleExtendListedHearingForCourtApplication(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION, envelope.toObfuscatedDebugString());
        }

        final JsonObject payload = envelope.payloadAsJsonObject();
        final HearingExtended hearingExtended = jsonObjectConverter.convert(payload, HearingExtended.class);

        if (isNotEmpty(hearingExtended.getProsecutionCases())) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PRIVATE_COMMAND_ADD_CASES_FOR_HEARING),
                    envelope.payloadAsJsonObject()));
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_COMMAND_ADD_CASES_FOR_HEARING, envelope.toObfuscatedDebugString());
        } else {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING),
                    envelope.payloadAsJsonObject()));
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING, envelope.toObfuscatedDebugString());
        }
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS)
    public void handleDefendantAddedForCourtProceedings(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED)
    public void handleEventsCaseOrApplicationEjected(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CASE_OR_APPLICATION_EJECTED),
                envelope.payloadAsJsonObject()));
    }

    @Handles(LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS)
    public void handleEventsCaseEjectedForAllHearings(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS, envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            hearingIds.forEach(hearingId -> {
                final String caseId = payload.getString(PROSECUTION_CASE_ID);
                final JsonObject caseEjectedCommandPayload = Json.createObjectBuilder()
                        .add(PROSECUTION_CASE_ID, caseId)
                        .add(HEARING_ID, hearingId)
                        .add(REMOVAL_REASON, payload.getString(REMOVAL_REASON))
                        .build();
                sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CASE_EJECTED),
                        caseEjectedCommandPayload));
            });
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("The Payload for event " + LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS
                        + "has been ignored as it does not contains hearing ids  : {}", envelope.toObfuscatedDebugString());
            }
        }

    }

    @Handles(LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS)
    public void handleEventsApplicationEjectedForAllHearings(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS, envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            hearingIds.forEach(hearingId -> {
                final String applicationId = payload.getString(APPLICATION_ID);
                final JsonObject applicationEjectedCommandPayload = Json.createObjectBuilder()
                        .add(APPLICATION_ID, applicationId)
                        .add(HEARING_ID, hearingId)
                        .add(REMOVAL_REASON, payload.getString(REMOVAL_REASON))
                        .build();

                sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_APPLICATION_EJECTED),
                        applicationEjectedCommandPayload));

            });
        } else {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("The Payload for event " + LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS
                        + "has been ignored as it does not contains hearing ids : {}", envelope.toObfuscatedDebugString());
            }
        }

    }

    @Handles(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED)
    public void defendantLegalStatusUpdate(final JsonEnvelope envelop) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("progression.defendant-legalaid-status-updated event received {}", envelop.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom(metadataFrom(envelop.metadata()).withName(COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS),
                envelop.payloadAsJsonObject()));
    }

    @Handles(LISTING_EVENTS_DEFENDANT_LEGALAID_STATUS_UPDATED)
    public void handleDefendantLegalStatusUpdateForHearings(final JsonEnvelope envelop) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("listing.events.defendant-legalaid-status-updated event received {}", envelop.toObfuscatedDebugString());
        }
        final JsonObject eventPayload = envelop.payloadAsJsonObject();
        final JsonArray hearingIds = eventPayload.getJsonArray("hearingIds");
        hearingIds.stream().forEach(hearingId -> {
            final JsonObject commandPayload = Json.createObjectBuilder()
                    .add(HEARING_ID, hearingId)
                    .add(CASE_ID, eventPayload.getString(CASE_ID))
                    .add(DEFENDANT_ID, eventPayload.getString(DEFENDANT_ID))
                    .add(LEGAL_AID_STATUS, eventPayload.getString(LEGAL_AID_STATUS))
                    .build();
            this.sender.send(envelopeFrom(metadataFrom(envelop.metadata()).withName(COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS_FOR_HEARING),
                    commandPayload));
        });
    }

    @Handles(PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED)
    public void handleHearingResultedAndCaseUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.progression.hearing-resulted-case-updated event received {}", envelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED),
                envelope.payloadAsJsonObject()));

    }

    @Handles(PUBLIC_EVENT_HEARING_TRIAL_VACATED)
    public void handleHearingTrialVacated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.hearing.trial-vacated received {}", envelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PRIVATE_COMMAND_HEARING_VACATE_TRIAL),
                envelope.payloadAsJsonObject()));

    }

    @Handles(LISTING_EVENTS_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED)
    public void handleCaseResultedAndDefendantProceedingsUpdated(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("listing.events.case-resulted-defendant-proceedings-updated event received {}", envelope.toObfuscatedDebugString());
        }
        final CaseResultedDefendantProceedingsConcluded caseResultedDefendantProceedingsConcluded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CaseResultedDefendantProceedingsConcluded.class);
        final List<UUID> hearingIds = caseResultedDefendantProceedingsConcluded.getHearingIds();
        hearingIds.forEach(hearingId -> {
            final JsonObject commandPayload = Json.createObjectBuilder()
                    .add(HEARING_ID, hearingId.toString())
                    .add(PROSECUTION_CASE, objectToJsonObjectConverter.convert(caseResultedDefendantProceedingsConcluded.getProsecutionCase()))
                    .build();

            this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CASE_UPDATE_DEFENDANT_PROCEEDINGS_UPDATED),
                    (commandPayload)));

        });
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_CASE_LINKED)
    public void handleCaseLinkedPublicEvent(final JsonEnvelope envelope) {
        LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_CASE_LINKED, envelope.toObfuscatedDebugString());
        sender.send(Enveloper.envelop(envelope.payloadAsJsonObject()).withName(COMMAND_UPDATE_LINKED_CASES).withMetadataFrom(envelope));
    }

    @Handles(PRIVATE_EVENT_LINKED_CASES_TO_BE_UPDATED)
    public void handleCaseLinkedPrivateEvent(final JsonEnvelope envelope) {
        LOGGER.info("listing.events.cases-linked {}", envelope.toObfuscatedDebugString());
        final LinkedCasesToBeUpdated linkedCasesToBeUpdated = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), LinkedCasesToBeUpdated.class);
        for (final UUID hearingId : linkedCasesToBeUpdated.getHearingIds()) {
            final UpdateLinkedCaseInHearing command = UpdateLinkedCaseInHearing.updateLinkedCaseInHearing()
                    .withLinkActionType(linkedCasesToBeUpdated.getLinkActionType())
                    .withCaseId(linkedCasesToBeUpdated.getCaseId())
                    .withCaseUrn(linkedCasesToBeUpdated.getCaseUrn())
                    .withHearingId(hearingId)
                    .withLinkedToCases(linkedCasesToBeUpdated.getLinkedToCases().stream()
                            .map(linkedToCases -> LinkedToCases.linkedToCases()
                                    .withCaseId(linkedToCases.getCaseId())
                                    .withCaseUrn(linkedToCases.getCaseUrn())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
            sender.send(Enveloper.envelop(objectToJsonObjectConverter.convert(command)).withName(COMMAND_UPDATE_LINKED_CASE_IN_HEARING).withMetadataFrom(envelope));
        }
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
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_HEARING_TO_CASE),
                            objectToJsonValueConverter.convert(listHearingCommand)));
                }
        );
    }


    private void sendCaseMarkerUpdatedForHearing(final JsonEnvelope envelope) {
        final CaseMarkersToBeUpdated caseMarkerEvent = getCaseMarkerEvent(envelope);
        final List<UpdateCaseMarkersForHearingCommand> updateCaseMarkersForHearingCommands =
                updateCaseMarkersForHearingCommandCollectionConverter.convert(caseMarkerEvent);

        updateCaseMarkersForHearingCommands.forEach(command -> {
            LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_CASE_MARKERS_TO_HEARING, command);
            sender.send(envelop(objectToJsonValueConverter.convert(command)).withName(COMMAND_UPDATE_CASE_MARKERS_TO_HEARING).withMetadataFrom(envelope));
        });
    }

    private void sendCommandAddApplicationToHearing(JsonEnvelope envelope) {
        final HearingListed event = getHearingListedEvent(envelope);
        final List<AddApplicationToHearingCommand> addApplicationToHearingCommands = addCourtApplicationToHearingCommandCollectionConverter.convert(event);
        addApplicationToHearingCommands.forEach(addApplicationToHearingCommand -> {
            LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_COURT_APPLICATION_TO_HEARING, addApplicationToHearingCommand);
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_COURT_APPLICATION_TO_HEARING),
                    objectToJsonValueConverter.convert(addApplicationToHearingCommand)));

        });
    }

    private void sendCommandAddApplicationToListedHearing(JsonEnvelope envelope) {
        final CourtApplicationAddedForHearing event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtApplicationAddedForHearing.class);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_COURT_APPLICATION_TO_HEARING),
                (createObjectBuilder()
                        .add(APPLICATION_ID, event.getCourtApplication().getId().toString())
                        .add(HEARING_ID, event.getHearingId().toString()).build()))
        );
    }

    /*
     *
     * For each hearingId in the 'defendants-to-be-updated' event, extract it
     * and send each one through as a separate 'update-defendants-for-Hearing' command.
     */
    private void sendUpdateDefendantsForHearings(final JsonEnvelope envelope) {
        final DefendantsToBeUpdated event = getDefendantsToBeUpdated(envelope);
        final List<UpdateDefendantsForHearingCommand> commands = updateDefendantsForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                updateDefendantsForHearingCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_DEFENDANTS_FOR_HEARING, updateDefendantsForHearingCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_DEFENDANTS_FOR_HEARING),
                            objectToJsonValueConverter.convert(updateDefendantsForHearingCommand)));
                }
        );
    }

    private void sendAddDefendantsForCourtProceedings(final JsonEnvelope envelope) {
        final DefendantsToBeAddedForCourtProceedings event = getDefendantsToBeAddedForCourtProceedings(envelope);
        final List<AddDefendantsForCourtProceedingsCommand> commands = addDefendantsForCourtProceedingsCommandCollectionConverter.convert(event);

        commands.forEach(
                addDefendantsForCourtProceedingsCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING, addDefendantsForCourtProceedingsCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING),
                            objectToJsonValueConverter.convert(addDefendantsForCourtProceedingsCommand)));
                }
        );
    }

    private void sendUpdatedOffencesForHearings(final JsonEnvelope envelope) {
        final OffencesToBeUpdated event = getOffencesToBeUpdated(envelope);
        final List<UpdateOffencesForHearingCommand> commands = updateOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                updateOffencesForHearingCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_OFFENCES_FOR_HEARING, updateOffencesForHearingCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_OFFENCES_FOR_HEARING),
                            objectToJsonValueConverter.convert(updateOffencesForHearingCommand)));
                }
        );
    }

    private void sendDeletedOffencesForHearings(final JsonEnvelope envelope) {
        final OffencesToBeDeleted event = getOffencesToBeDeleted(envelope);
        final List<DeleteOffencesForHearingCommand> commands = deleteOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                deleteOffencesForHearingCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_DELETE_OFFENCES_FOR_HEARING, deleteOffencesForHearingCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_DELETE_OFFENCES_FOR_HEARING),
                            objectToJsonValueConverter.convert(deleteOffencesForHearingCommand)));
                }
        );
    }

    private void sendAddedOffencesForHearings(final JsonEnvelope envelope) {
        final OffencesToBeAdded event = getOffencesToBeAdded(envelope);
        final List<AddOffencesForHearingCommand> commands = addOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                addOffencesForHearingCommand -> {
                    LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_OFFENCES_FOR_HEARING, addOffencesForHearingCommand);

                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_OFFENCES_FOR_HEARING),
                            objectToJsonValueConverter.convert(addOffencesForHearingCommand)));
                }
        );
    }


    /*
     * Publish a public event to notify that the hearing has been confirmed.
     */
    private void publishHearingConfirmedPublicEvent(final JsonEnvelope envelope) {
        final HearingConfirmed hearingConfirmed = getHearingConfirmed(envelope);

        LOGGER.info(LOG_PUBLISHING, PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_CONFIRMED),
                objectToJsonValueConverter.convert(hearingConfirmed)));
    }

    private HearingConfirmed getHearingConfirmed(final JsonEnvelope envelope) {
        final HearingAllocatedForListing hearingAllocatedForListing = getHearingAllocatedForListing(envelope);
        return getHearingConfirmed(hearingAllocatedForListing, envelope);
    }

    private HearingAllocatedForListing getHearingAllocatedForListing(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingAllocatedForListing.class);
    }

    private HearingConfirmed getHearingConfirmed(final HearingAllocatedForListing hearingAllocatedForListing, final JsonEnvelope envelope) {
        return hearingConfirmedFactory.create(hearingAllocatedForListing, envelope);
    }

    /*
     * Publish a public event to notify that the hearing has been updated.
     */
    private void publishHearingUpdatedPublicEvent(final JsonEnvelope envelope) {
        final HearingUpdated hearingUpdated = getHearingUpdated(envelope);

        LOGGER.info(LOG_PUBLISHING, PUBLIC_EVENT_HEARING_UPDATED, hearingUpdated);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_UPDATED),
                objectToJsonValueConverter.convert(hearingUpdated)));
    }

    private HearingUpdated getHearingUpdated(final JsonEnvelope envelope) {
        final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing = getAllocatedHearingUpdatedForListing(envelope);

        return allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing, envelope);
    }


    private AllocatedHearingUpdatedForListing getAllocatedHearingUpdatedForListing(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AllocatedHearingUpdatedForListing.class);
    }

    /*
     * Publish a public event to notify that the hearing has been updated with restrictions on court listing.
     */
    private void publishCourtListRestrictedPublicEvent(final JsonEnvelope envelope) {
        final CourtListRestricted courtListRestricted =
                jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtListRestricted.class);
        LOGGER.info(LOG_PUBLISHING, PUBLIC_EVENT_RESTRICT_COURT_LIST, courtListRestricted);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_RESTRICT_COURT_LIST),
                objectToJsonValueConverter.convert(courtListRestricted)));
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

    private CaseMarkersToBeUpdated getCaseMarkerEvent(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CaseMarkersToBeUpdated.class);
    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING)
    public void handleAllocatedHearingExtendedForListingMessage(final JsonEnvelope envelope) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING, envelope.toObfuscatedDebugString());
        }

        publishHearingConfirmedPublicEventForExtendHearing(envelope);
    }

    private void publishHearingConfirmedPublicEventForExtendHearing(final JsonEnvelope envelope) {
        final AllocatedHearingExtendedForListing hearingExtendedForListing = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AllocatedHearingExtendedForListing.class);
        final HearingConfirmed hearingConfirmed = allocatedHearingExtendedFactory.create(hearingExtendedForListing, envelope);

        updateHearingToCase(envelope, hearingConfirmed);

        LOGGER.info("Publishing '{}' public event for extend hearing with payload {}", PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        final JsonValue payload = objectToJsonValueConverter.convert(hearingConfirmed);
        sender.send(Enveloper.envelop(payload)
                .withName(PUBLIC_EVENT_HEARING_CONFIRMED)
                .withMetadataFrom(envelope));
    }

    private void updateHearingToCase(JsonEnvelope envelope, HearingConfirmed hearingConfirmed) {

        final ConfirmedHearing confirmedHearing = hearingConfirmed.getConfirmedHearing();

        for (final ConfirmedProsecutionCase prosecutionCase : confirmedHearing.getProsecutionCases()) {

            final Optional<UUID> allocatedHearingIdOpt = confirmedHearing.getExistingHearingId();
            if (allocatedHearingIdOpt.isPresent()) {

                final UUID allocatedHearingId = allocatedHearingIdOpt.get();

                final UpdateHearingToCaseCommand updateHearingToCaseCommand =
                        new UpdateHearingToCaseCommand(prosecutionCase.getId(), allocatedHearingId, confirmedHearing.getId());
                LOGGER.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_HEARING_TO_CASE, updateHearingToCaseCommand);

                sender.send(Enveloper.envelop(objectToJsonValueConverter.convert(updateHearingToCaseCommand))
                        .withName(COMMAND_UPDATE_HEARING_TO_CASE)
                        .withMetadataFrom(envelope));
            }
        }
    }

}
