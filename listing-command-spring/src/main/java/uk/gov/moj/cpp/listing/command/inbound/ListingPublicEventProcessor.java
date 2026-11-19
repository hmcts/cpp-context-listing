package uk.gov.moj.cpp.listing.command.inbound;

import static jakarta.json.Json.createObjectBuilder;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelop;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.justice.progression.courts.ApplicationLaaReferenceUpdatedForApplication;
import uk.gov.justice.progression.courts.ApplicationOffencesUpdated;
import uk.gov.justice.progression.courts.HearingExtended;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorQueue;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.UUID;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The inbound {@code public.*} handlers of the JEE {@code ListingEventProcessor} (MBD refactor
 * P2.4; its private-event handlers are {@code command.hearing.processor.ListingEventProcessor},
 * P2.3.2). Logic unchanged: each turns the upstream event into the internal command(s) it sent to
 * {@code listing.handler.command}; {@link ProcessorSender} now calls the P2.3 command service
 * directly, in the listener's transaction. The command keeps the event's metadata under the
 * command name ({@code metadataFrom(event.metadata()).withName(…)}), as JEE did.
 */
@SuppressWarnings("squid:S2629")
@Component
public class ListingPublicEventProcessor extends InboundProcessor {

    static final String PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";
    static final String PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED = "public.progression.defendant-offences-changed";
    static final String PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED = "public.progression.court-application-changed";
    static final String PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED = "public.progression.case-markers-updated";
    static final String PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION = "public.progression.events.hearing-extended";
    static final String PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";
    static final String PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED = "public.progression.events.case-or-application-ejected";
    static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";
    static final String PUBLIC_EVENT_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    static final String PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED = "public.progression.application-offences-updated";
    static final String PUBLIC_PROGRESSION_APPLICATION_LAA_REFERENCE_UPDATED_FOR_APPLICATION = "public.progression.application-laa-reference-updated-for-application";
    static final String PUBLIC_EVENT_PROGRESSION_CASE_LINKED = "public.progression.case-linked";
    static final String PUBLIC_EVENT_HEARING_DAYS_CANCELLED = "public.hearing.hearing-days-cancelled";
    static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    static final String PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING = "public.hearing.selected-offences-removed-from-existing-hearing";
    static final String PUBLIC_PROGRESSION_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING = "public.progression.offences-removed-from-existing-allocated-hearing";

    static final String COMMAND_UPDATE_CASE_DEFENDANT_DETAILS = "listing.command.update-case-defendant-details";
    static final String COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES = "listing.command.update-case-defendant-offences";
    static final String COMMAND_UPDATE_COURT_APPLICATION = "listing.command.update-court-application";
    static final String COMMAND_UPDATE_CASE_MARKERS = "listing.command.update-case-markers";
    static final String LISTING_COMMAND_ADD_CASES_TO_HEARING = "listing.command.add-cases-to-hearing";
    static final String COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING = "listing.command.add-court-application-for-hearing";
    static final String COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS = "listing.command.add-defendants-to-court-proceedings";
    static final String COMMAND_CASE_OR_APPLICATION_EJECTED = "listing.command.eject-case-or-application";
    static final String COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS = "listing.command.update-defendant-legalaid-status";
    static final String COMMAND_UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED = "listing.command.update-case-resulted-defendant-proceedings-concluded";
    static final String PRIVATE_COMMAND_HEARING_VACATE_TRIAL = "listing.command.hearing-vacate-trial";
    static final String COMMAND_UPDATE_LAA_REFERENCE_FOR_APPLICATION = "listing.command.update-laa-reference-for-application";
    static final String COMMAND_UPDATE_LINKED_CASES = "listing.command.update-linked-cases";
    static final String COMMAND_CANCEL_HEARING_DAYS = "listing.command.cancel-hearing-days";
    static final String COMMAND_MARK_HEARING_AS_DUPLICATE = "listing.command.mark-hearing-as-duplicate";
    static final String COMMAND_REMOVE_SELECTED_OFFENCES_FROM_EXISTING_HEARING = "listing.command.remove-selected-offences-from-existing-hearing";

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingPublicEventProcessor.class);
    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    private static final String COURT_APPLICATION = "courtApplication";
    private static final String HEARING_ID = "hearingId";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String LEGAL_AID_STATUS = "legalAidStatus";
    private static final String PROSECUTION_CASE_IDS = "prosecutionCaseIds";

    private final ProcessorSender sender;
    private final HearingSlotsService hearingSlotsService;
    private final JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();
    private final ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    public ListingPublicEventProcessor(final ProcessorQueue processorQueue, final ProcessorSender sender,
                                       final HearingSlotsService hearingSlotsService) {
        super(processorQueue);
        this.sender = sender;
        this.hearingSlotsService = hearingSlotsService;
        on(PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED, this::handleCaseDefendantChangedMessage);
        on(PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, this::handleDefendantOffencesChanged);
        on(PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED, this::handleCourtApplicationChanged);
        on(PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED, this::handleCaseMarkerUpdated);
        on(PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION, this::handleExtendListedHearingForCourtApplication);
        on(PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS, this::handleDefendantAddedForCourtProceedings);
        on(PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED, this::handleEventsCaseOrApplicationEjected);
        on(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED, this::defendantLegalStatusUpdate);
        on(PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED, this::handleHearingResultedAndCaseUpdated);
        on(PUBLIC_EVENT_HEARING_TRIAL_VACATED, this::handleHearingTrialVacated);
        on(PUBLIC_PROGRESSION_APPLICATION_OFFENCES_UPDATED, this::handleApplicationOffenceUpdated);
        on(PUBLIC_PROGRESSION_APPLICATION_LAA_REFERENCE_UPDATED_FOR_APPLICATION, this::handleApplicationLaaReferenceUpdatedForApplication);
        on(PUBLIC_EVENT_PROGRESSION_CASE_LINKED, this::handleCaseLinkedPublicEvent);
        on(PUBLIC_EVENT_HEARING_DAYS_CANCELLED, this::handleHearingDaysCancelledPublicEvent);
        on(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, this::handleHearingMarkedAsDuplicate);
        on(PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING, this::offencesRemovedFromExistingHearing);
        on(PUBLIC_PROGRESSION_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING, this::offencesRemovedFromExistingAllocatedHearingByProgression);
    }

    /** JEE {@code ListingEventProcessor:496}. */
    public void handleCaseDefendantChangedMessage(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_DEFENDANT_DETAILS),
                envelope.payload()));
    }

    /** JEE {@code ListingEventProcessor:506}. */
    public void handleDefendantOffencesChanged(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES),
                envelope.payload()));
    }

    /** JEE {@code ListingEventProcessor:515}. */
    public void handleCourtApplicationChanged(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_COURT_APPLICATION),
                envelope.payload()));
    }

    /** JEE {@code ListingEventProcessor:527}. */
    public void handleCaseMarkerUpdated(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_MARKERS),
                envelope.payload()));
    }

    /**
     * JEE {@code ListingEventProcessor:545}: not for box work; prosecution cases → add-cases-to-hearing
     * (the payload without {@code courtApplication}); a court application → add-court-application-for-hearing
     * (the whole payload).
     */
    public void handleExtendListedHearingForCourtApplication(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION, envelope.metadata());
        }

        final JsonObject payload = envelope.payload();
        final HearingExtended hearingExtended = jsonObjectConverter.convert(payload, HearingExtended.class);

        if (isNotBoxWorkRequest(hearingExtended)) {
            if (isNotEmpty(hearingExtended.getProsecutionCases())) {
                final JsonObjectBuilder builder = createObjectBuilder();
                payload.entrySet().stream().filter(entry -> !entry.getKey().equals(COURT_APPLICATION)).forEach(entry -> builder.add(entry.getKey(), entry.getValue()));

                sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_ADD_CASES_TO_HEARING),
                        builder.build()));
                LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, LISTING_COMMAND_ADD_CASES_TO_HEARING, envelope.metadata());
            }
            if (nonNull(hearingExtended.getCourtApplication())) {
                sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING),
                        payload));
                LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING, envelope.metadata());
            }
        }
    }

    private static boolean isNotBoxWorkRequest(final HearingExtended hearingExtended) {
        return isNull(hearingExtended.getIsBoxWorkRequest()) || !hearingExtended.getIsBoxWorkRequest();
    }

    /** JEE {@code ListingEventProcessor:575}. */
    public void handleDefendantAddedForCourtProceedings(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS),
                envelope.payload()));
    }

    /** JEE {@code ListingEventProcessor:584}. */
    public void handleEventsCaseOrApplicationEjected(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CASE_OR_APPLICATION_EJECTED),
                envelope.payload()));
    }

    /** JEE {@code ListingEventProcessor:649}: only caseId, defendantId and legalAidStatus are passed on. */
    public void defendantLegalStatusUpdate(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("progression.defendant-legalaid-status-updated event received {}", envelope.metadata());
        }

        final JsonObject jsonObject = envelope.payload();
        final JsonObject commandPayload = createObjectBuilder()
                .add(CASE_ID, jsonObject.getString(CASE_ID))
                .add(DEFENDANT_ID, jsonObject.getString(DEFENDANT_ID))
                .add(LEGAL_AID_STATUS, jsonObject.getString(LEGAL_AID_STATUS))
                .build();
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS),
                commandPayload));
    }

    /** JEE {@code ListingEventProcessor:685}. */
    public void handleHearingResultedAndCaseUpdated(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.progression.hearing-resulted-case-updated event received {}", envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED),
                envelope.payload()));
    }

    /** JEE {@code ListingEventProcessor:695}. */
    public void handleHearingTrialVacated(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("public.hearing.trial-vacated received {}", envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PRIVATE_COMMAND_HEARING_VACATE_TRIAL),
                envelope.payload()));
    }

    /**
     * JEE {@code ListingEventProcessor:705} ({@code Envelope<ApplicationOffencesUpdated>}): the framework
     * read the payload into the POJO and the sender wrote the POJO back, so the command payload is the
     * POJO's JSON (unknown fields dropped), reproduced here.
     */
    public void handleApplicationOffenceUpdated(final Envelope envelope) {
        final ApplicationOffencesUpdated payload = jsonObjectConverter.convert(envelope.payload(), ApplicationOffencesUpdated.class);
        final JsonObject commandPayload = objectToJsonObjectConverter.convert(payload);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.progression.application-offences-updated event received {}", commandPayload);
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_LAA_REFERENCE_FOR_APPLICATION),
                commandPayload));
    }

    /** JEE {@code ListingEventProcessor:716} ({@code Envelope<ApplicationLaaReferenceUpdatedForApplication>}, as above). */
    public void handleApplicationLaaReferenceUpdatedForApplication(final Envelope envelope) {
        final ApplicationLaaReferenceUpdatedForApplication payload =
                jsonObjectConverter.convert(envelope.payload(), ApplicationLaaReferenceUpdatedForApplication.class);
        final JsonObject commandPayload = objectToJsonObjectConverter.convert(payload);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("public.progression.application-laa-reference-updated-for-application event received {}", commandPayload);
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_LAA_REFERENCE_FOR_APPLICATION),
                commandPayload));
    }

    /** JEE {@code ListingEventProcessor:745}. */
    public void handleCaseLinkedPublicEvent(final Envelope envelope) {
        LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_CASE_LINKED, envelope.metadata());
        sender.send(envelop(envelope.payload()).withName(COMMAND_UPDATE_LINKED_CASES).withMetadataFrom(envelope));
    }

    /** JEE {@code ListingEventProcessor:772}. */
    public void handleHearingDaysCancelledPublicEvent(final Envelope envelope) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_HEARING_DAYS_CANCELLED, envelope.metadata());
        }
        sender.send(envelop(envelope.payload()).withName(COMMAND_CANCEL_HEARING_DAYS).withMetadataFrom(envelope));
    }

    /**
     * JEE {@code ListingEventProcessor:780}: releases the hearing's courtscheduler sessions (REST
     * DELETE as the system user, D19; failures are logged and swallowed by the service), then sends
     * mark-hearing-as-duplicate with the hearing id and, if present, the case ids.
     */
    public void handleHearingMarkedAsDuplicate(final Envelope envelope) {
        LOGGER.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, envelope.metadata());

        final UUID hearingId = UUID.fromString(envelope.payload().getString(HEARING_ID));
        hearingSlotsService.delete(hearingId);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DUPLICATE),
                generatePayloadForCommandMarkHearingAsDuplicate(envelope)));
    }

    private static JsonObject generatePayloadForCommandMarkHearingAsDuplicate(final Envelope envelope) {
        final JsonObjectBuilder commandPayloadBuilder = createObjectBuilder();
        commandPayloadBuilder.add(HEARING_ID, envelope.payload().getString(HEARING_ID));
        if (envelope.payload().containsKey(PROSECUTION_CASE_IDS)) {
            commandPayloadBuilder.add(PROSECUTION_CASE_IDS, envelope.payload().getJsonArray(PROSECUTION_CASE_IDS));
        }
        return commandPayloadBuilder.build();
    }

    /** JEE {@code ListingEventProcessor:836}. */
    public void offencesRemovedFromExistingHearing(final Envelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_REMOVE_SELECTED_OFFENCES_FROM_EXISTING_HEARING),
                envelope.payload()));
    }

    /**
     * JEE {@code ListingEventProcessor:844}: progression publishes the same {@code {hearingId,
     * offenceIds}} payload when a split moves offences off the original hearing, so it routes to the
     * same command as the hearing-context sibling above.
     */
    public void offencesRemovedFromExistingAllocatedHearingByProgression(final Envelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_REMOVE_SELECTED_OFFENCES_FROM_EXISTING_HEARING),
                envelope.payload()));
    }
}
