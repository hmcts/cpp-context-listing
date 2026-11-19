package uk.gov.moj.cpp.listing.command.hearing.processor;


import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;
import org.springframework.stereotype.Component;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static jakarta.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings.publicListingNewDefendantAddedForCourtProceedings;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelop;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.commands.AddApplicationToHearingCommand;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.commands.LinkedToCases;
import uk.gov.justice.listing.commands.UpdateLinkedCaseInHearing;
import uk.gov.justice.listing.courts.CaseUrns;
import uk.gov.justice.listing.courts.HearingChangesSaved;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.courts.UpdateHearingToCaseCommand;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListing;
import uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing;
import uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2;
import uk.gov.justice.listing.events.CaseMarkersToBeUpdated;
import uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtListRestricted;
import uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings;
import uk.gov.justice.listing.events.DefendantsToBeUpdated;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysChangedForHearing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingMarkedAsDeleted;
import uk.gov.justice.listing.events.HearingMarkedAsDuplicate;
import uk.gov.justice.listing.events.HearingMarkedForPartialUpdate;
import uk.gov.justice.listing.events.HearingPartiallyUpdated;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.LinkedCasesToBeUpdated;
import uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.OffenceIds;
import uk.gov.justice.listing.events.OffencesToBeAdded;
import uk.gov.justice.listing.events.OffencesToBeDeleted;
import uk.gov.justice.listing.events.OffencesToBeUpdated;
import uk.gov.justice.listing.events.ProsecutionCaseDefendantOffenceIdsV2;
import uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.progression.courts.HearingExtended;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonValueConverter;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.AddCourtApplicationToHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.AddDefendantsForCourtProceedingsCommand;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.AddDefendantsForCourtProceedingsCommandCollectionConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.AddHearingToCaseCommandCollectionConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.AddOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.AddOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.DeleteOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.DeleteOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.UpdateCaseMarkersForHearingCommand;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.UpdateCaseMarkersForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.UpdateDefendantsForHearingCommand;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.UpdateDefendantsForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.UpdateOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.UpdateOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.command.UpdateUnallocatedHearingPartiallyCommandConverter;
import uk.gov.moj.cpp.listing.command.hearing.processor.factory.PublicHearingPartiallyUpdatedFactory;
import uk.gov.moj.cpp.listing.command.support.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of the JEE {@code ListingEventProcessor} (MBD refactor P2.3.2, ADR 015): its handlers of the
 * listing domain events, run for each event the Hearing rules raise, in the command's transaction
 * ({@code ProcessorQueue}). Unchanged logic: same public messages (names, payloads, conditions,
 * order); the internal commands it sent go to the command services through {@link ProcessorSender}.
 * The inbound {@code public.*} handlers of the JEE class are {@code command.inbound.ListingPublicEventProcessor} (P2.4). A failing
 * reference-data or progression query fails the command (JEE retried the step, then dropped it).
 */
@SuppressWarnings("squid:S2629")
@Component
public class ListingEventProcessor implements ProcessorPort {

    /** The conversion the JEE {@code ProgressionService.getProsecutionCaseByCaseId} did itself. */
    private static final JsonObjectToObjectConverter PROSECUTION_CASE_CONVERTER = new JsonObjectToObjectConverter();

    @Override
    public void register(final EventHandlers processors) {
        processors
                .onJson(uk.gov.justice.listing.events.HearingListed.class, this::handleHearingListedMessage)
                .onJson(uk.gov.justice.listing.events.TrialVacated.class, this::trialVacated)
                .onJson(uk.gov.justice.listing.events.HearingRescheduled.class, this::hearingRescheduled)
                .onJson(uk.gov.justice.listing.events.HearingRequestedForListing.class, this::handleHearingRequestedForListing)
                .onJson(uk.gov.justice.listing.events.HearingMarkedAsDuplicate.class, this::handlePrivateHearingMarkedAsDuplicate)
                .onJson(uk.gov.justice.listing.events.CourtApplicationAddedForHearing.class, this::handleCourtApplicationAddedForListedHearing)
                .onJson(uk.gov.justice.listing.events.DefendantsToBeUpdated.class, this::handleDefendantsToBeUpdatedMessage)
                .onJson(uk.gov.justice.listing.events.DefendantsToBeAddedForCourtProceedings.class, this::handleDefendantsToBeAddedForCourtProceedingsMessage)
                .onJson(uk.gov.justice.listing.events.OffencesToBeUpdated.class, this::handleOffencesToBeUpdatedMessage)
                .onJson(uk.gov.justice.listing.events.OffencesToBeDeleted.class, this::handleOffencesToBeDeletedMessage)
                .onJson(uk.gov.justice.listing.events.OffencesToBeAdded.class, this::handleOffencesToBeAddedMessage)
                .onJson(uk.gov.justice.listing.events.HearingAllocatedForListing.class, this::handleHearingAllocatedForListingMessage)
                .onJson(uk.gov.justice.listing.events.HearingAllocatedForListingV2.class, this::handleHearingAllocatedForListingV2Message)
                .onJson(uk.gov.justice.listing.events.AllocatedHearingUpdatedForListing.class, this::handleAllocatedHearingUpdatedForListingMessage)
                .onJson(uk.gov.justice.listing.events.AllocatedHearingUpdatedForListingV2.class, this::handleAllocatedHearingUpdatedForListingV2Message)
                .onJson(uk.gov.justice.listing.events.CourtListRestricted.class, this::handleRestrictCourtListMessage)
                .onJson(uk.gov.justice.listing.events.CaseMarkersToBeUpdated.class, this::handleCaseMarkerUpdateMessage)
                .onJson(uk.gov.justice.listing.events.CourtApplicationToBeUpdated.class, this::handleCourtApplicationToBeUpdated)
                .onJson(uk.gov.justice.listing.events.CaseEjectedForHearings.class, this::handleEventsCaseEjectedForAllHearings)
                .onJson(uk.gov.justice.listing.events.ApplicationEjectedForHearings.class, this::handleEventsApplicationEjectedForAllHearings)
                .onJson(uk.gov.justice.listing.events.DefendantLegalaidStatusUpdated.class, this::handleDefendantLegalStatusUpdateForHearings)
                .onJson(uk.gov.justice.listing.events.CaseResultedDefendantProceedingsConcluded.class, this::handleCaseResultedAndDefendantProceedingsUpdated)
                .onJson(uk.gov.justice.listing.events.LinkedCasesToBeUpdated.class, this::handleCaseLinkedPrivateEvent)
                .onJson(uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings.class, this::handleNewDefendantAddedForCourtProceedings)
                .onJson(uk.gov.justice.listing.events.HearingUnallocatedForListing.class, this::handleHearingUnallocatedForListing)
                .onJson(uk.gov.justice.listing.events.HearingMarkedAsDeleted.class, this::handleHearingMarkedAsDeleted)
                .onJson(uk.gov.justice.listing.events.HearingMarkedForPartialUpdate.class, this::handleHearingMarkedForPartialUpdate)
                .onJson(uk.gov.justice.listing.events.HearingPartiallyUpdated.class, this::handleHearingPartiallyUpdate)
                .onJson(uk.gov.justice.listing.events.HearingChangesSaved.class, this::handleHearingChangesSaved)
                .onJson(uk.gov.justice.listing.events.HearingsUpdateCompleted.class, this::handleHearingsUpdateCompletedEvent)
                .onJson(uk.gov.justice.listing.events.HearingAddedToCase.class, this::handleHearingAddedToCase)
                .onJson(uk.gov.justice.listing.events.AllocatedHearingExtendedForListing.class, this::handleAllocatedHearingExtendedForListingMessage)
                .onJson(uk.gov.justice.listing.events.AllocatedHearingExtendedForListingV2.class, this::handleAllocatedHearingExtendedForListingV2Message)
                .onJson(uk.gov.justice.listing.events.HearingDaysChangedForHearing.class, this::handleListingCourtRoomNotSelectedEvent);
    }


    static final String PUBLIC_EVENT_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    static final String PUBLIC_EVENT_HEARING_UPDATED = "public.listing.hearing-updated";
    static final String PUBLIC_EVENT_VACATED_TRIAL_UPDATED = "public.listing.vacated-trial-updated";
    static final String PUBLIC_EVENT_HEARING_CHANGES_SAVED = "public.listing.hearing-changes-saved";
    static final String COMMAND_CASE_EJECTED = "listing.command.eject-case";
    static final String COMMAND_APPLICATION_EJECTED = "listing.command.eject-application";
    static final String COMMAND_UPDATE_HEARING_TO_CASE = "listing.command.update-hearing-to-case";

    private static final String COMMAND_PAYLOAD_DEBUG_STRING = "Sending '{}' command with payload {}";
    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    private static final String HEARING_IDS = "hearingIds";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String REMOVAL_REASON = "removalReason";
    private static final String PUBLIC_EVENT_RESTRICT_COURT_LIST = "public.listing.court-list-restricted";
    private static final String PUBLIC_EVENT_HEARINGS_UPDATED_COMPLETED = "public.listing.hearings-update-completed";
    private static final String LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS = "listing.events.case-ejected-for-hearings";
    private static final String LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS = "listing.events.application-ejected-for-hearings";
    private static final String PRIVATE_EVENT_HEARING_CHANGES_SAVED = "listing.events.hearing-changes-saved";
    private static final String PRIVATE_EVENT_HEARINGS_UPDATE_COMPLETED = "listing.events.hearings-update-completed";
    private static final String PRIVATE_EVENT_HEARING_LISTED = "listing.events.hearing-listed";
    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING = "listing.events.allocated-hearing-updated-for-listing";
    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING_V2 = "listing.events.allocated-hearing-updated-for-listing-v2";
    private static final String PRIVATE_EVENT_COURT_APPLICATION_ADDED_FOR_LISTED_HEARING = "listing.events.court-application-added-for-hearing";
    private static final String PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED = "listing.events.court-application-to-be-updated";
    private static final String PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING = "listing.events.hearing-allocated-for-listing";
    private static final String PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING_V2 = "listing.events.hearing-allocated-for-listing-v2";
    private static final String PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED = "listing.events.defendants-to-be-updated"; //private event back of command
    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED = "listing.events.offences-to-be-updated";
    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_DELETED = "listing.events.offences-to-be-deleted";
    private static final String PRIVATE_EVENT_OFFENCES_TO_BE_ADDED = "listing.events.offences-to-be-added";
    private static final String PRIVATE_EVENT_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS = "listing.events.defendants-to-be-added-for-court-proceedings";
    private static final String PRIVATE_EVENT_RESTRICT_COURT_LIST = "listing.events.court-list-restricted";
    private static final String PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED = "listing.events.case-markers-to-be-updated";
    private static final String PRIVATE_EVENT_TRIAL_VACATED = "listing.events.trial-vacated";
    private static final String PRIVATE_EVENT_HEARING_REQUESTED_FOR_LISTING = "listing.events.hearing-requested-for-listing";
    private static final String COMMAND_ADD_HEARING_TO_CASE = "listing.command.add-hearing-to-case";
    private static final String COMMAND_ADD_COURT_APPLICATION_TO_HEARING = "listing.command.add-court-application-to-hearing";
    private static final String COMMAND_UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing"; // command back of private event
    private static final String COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING = "listing.command.add-defendants-to-court-proceedings-for-hearing";
    private static final String COMMAND_UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    private static final String COMMAND_DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    private static final String COMMAND_ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";
    private static final String COMMAND_UPDATE_COURT_APPLICATION_FOR_HEARINGS = "listing.command.update-court-application-for-hearings";
    private static final String COMMAND_UPDATE_CASE_MARKERS_TO_HEARING = "listing.command.update-case-markers-to-hearing";
    private static final String COMMAND_UPDATE_DEFENDANT_COURT_PROCEEDINGS = "listing.command.update-defendant-court-proceedings";
    private static final String COMMAND_UPDATE_LINKED_CASE_IN_HEARING = "listing.command.update-linked-case-in-hearing";
    private static final String COMMAND_MARK_HEARING_AS_DUPLICATE_FOR_CASE = "listing.command.mark-hearing-as-duplicate-for-case";
    private static final String COMMAND_CHANGE_NEXT_HEARING_DAY = "listing.command.change-next-hearing-day";
    private static final String COMMAND_MARK_HEARING_AS_DELETED = "listing.command.mark-hearing-as-deleted";
    private static final String COMMAND_REMOVE_PARTIALLY_MERGED_OFFENCES = "listing.command.remove-partially-merged-offences-from-original-hearing";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "hearingId";
    private static final String HEARING_ID_TO_MARK = "hearingIdToMarkAsDeleted";
    private static final String IS_VACATED = "isVacated";
    private static final String ALLOCATED = "allocated";
    private static final String REASON_ID = "vacatedTrialReasonId";
    public static final String LOG_PUBLISHING = "Publishing '{}' public event with payload {}";
    private static final String COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS_FOR_HEARING = "listing.command.update-defendant-legalaid-status-for-hearing";
    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING = "listing.events.allocated-hearing-extended-for-listing";
    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING_V2 = "listing.events.allocated-hearing-extended-for-listing-v2";

    private static final String LEGAL_AID_STATUS = "legalAidStatus";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID = "caseId";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String PROSECUTION_CASE_IDS = "prosecutionCaseIds";
    private static final String PUBLIC_EVENT_NEW_DEFENDANT_ADDED_FOR_COURT_PROCEEDINGS = "public.listing.new-defendant-added-for-court-proceedings";
    private static final String PRIVATE_EVENT_HEARING_MARKED_FOR_PARTIAL_UPDATE = "listing.events.hearing-marked-for-partial-update";
    private static final String PRIVATE_EVENTS_HEARING_ADDED_TO_CASE = "listing.events.hearing-added-to-case";
    private static final String PRIVATE_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING = "listing.events.hearing-days-changed-for-hearing";
    private static final String PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING = "public.listing.hearing-days-changed-for-hearing";

    private static final String PUBLIC_LISTING_HEARING_LISTED = "public.listing.hearing-listed";

    private static final String PRIVATE_LISTING_HEARING_PARTIALLY_UPDATED = "listing.events.hearing-partially-updated";
    private static final String PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED = "public.listing.hearing-partially-updated";
    public static final String PUBLIC_LISTING_HEARING_ADDED_TO_CASE = "public.listing.hearing-added-to-case";


    @Autowired
    private ProcessorSender sender;

    @Autowired
    private AllocatedHearingUpdatedFactory allocatedHearingUpdatedFactory;

    @Autowired
    private AllocatedHearingExtendedFactory allocatedHearingExtendedFactory;

    @Autowired
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Autowired
    private AddHearingToCaseCommandCollectionConverter listHearingCommandConverter;

    @Autowired
    private UpdateDefendantsForHearingCommandCollectionConverter updateDefendantsForHearingCommandCollectionConverter;

    @Autowired
    private AddDefendantsForCourtProceedingsCommandCollectionConverter addDefendantsForCourtProceedingsCommandCollectionConverter;

    @Autowired
    private UpdateOffencesForHearingCommandCollectionConverter updateOffencesForHearingCommandCollectionConverter;

    @Autowired
    private AddOffencesForHearingCommandCollectionConverter addOffencesForHearingCommandCollectionConverter;

    @Autowired
    private DeleteOffencesForHearingCommandCollectionConverter deleteOffencesForHearingCommandCollectionConverter;

    @Autowired
    private AddCourtApplicationToHearingCommandCollectionConverter addCourtApplicationToHearingCommandCollectionConverter;

    @Autowired
    private UpdateCaseMarkersForHearingCommandCollectionConverter updateCaseMarkersForHearingCommandCollectionConverter;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter();

    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new ObjectToJsonObjectConverter();

    @Autowired
    private UpdateUnallocatedHearingPartiallyCommandConverter updateUnallocatedHearingPartiallyCommandConverter;

    @Autowired
    private HearingObjectsListingToCoreConverter hearingListingToCoreConverter;

    @Autowired
    private PublicHearingPartiallyUpdatedFactory publicHearingPartiallyUpdatedFactory;

    private static final Logger logger = LoggerFactory.getLogger(ListingEventProcessor.class);

    @Autowired
    private ProgressionService progressionService;

    public void handleHearingListedMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_LISTED, envelope.metadata());
        }

        sendCommandAddHearingToCase(envelope);
        sendCommandAddApplicationToHearing(envelope);
        publishPublicHearingListedEvent(envelope);
    }


    public void trialVacated(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_TRIAL_VACATED, envelope.metadata());
        }
        final TrialVacated trialVacated = jsonObjectConverter.convert(envelope.payload(), TrialVacated.class);

        final JsonObject vacatedTrialUpdatedPayload = createObjectBuilder()
                .add(HEARING_ID, trialVacated.getHearingId().toString())
                .add(IS_VACATED, true)
                .add(REASON_ID, trialVacated.getVacatedTrialReasonId().toString())
                .add(ALLOCATED, trialVacated.getAllocated())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_VACATED_TRIAL_UPDATED),
                vacatedTrialUpdatedPayload));
    }

    public void hearingRescheduled(final Envelope envelope) {

        final HearingRescheduled hearingRescheduled = jsonObjectConverter.convert(envelope.payload(), HearingRescheduled.class);

        final JsonObject vacatedTrialUpdatedPayload = createObjectBuilder()
                .add(HEARING_ID, hearingRescheduled.getHearingId().toString())
                .add(IS_VACATED, false)
                .add(ALLOCATED, hearingRescheduled.getAllocated())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_VACATED_TRIAL_UPDATED),
                vacatedTrialUpdatedPayload));
    }

    /**
     * No longer produced: the only emitter was the SPLIT path in update-hearing-for-listing, now
     * torn down. Kept so historic streams still replay and because progression consumes the public
     * contract; remove only once no stream carries the private event.
     *
     * @deprecated splits are performed via progression, not through update-hearing-for-listing.
     */
    @Deprecated
    public void handleHearingRequestedForListing(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_REQUESTED_FOR_LISTING, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.hearing-requested-for-listing"),
                envelope.payload()));
    }

    public void handlePrivateHearingMarkedAsDuplicate(final Envelope envelope) {
        final HearingMarkedAsDuplicate hearingMarkedAsDuplicate = jsonObjectConverter.convert(envelope.payload(), HearingMarkedAsDuplicate.class);
        final UUID hearingId = hearingMarkedAsDuplicate.getHearingId();

        if (isNotEmpty(hearingMarkedAsDuplicate.getCaseIds())) {
            hearingMarkedAsDuplicate.getCaseIds().forEach(caseId ->
                    sendUpdateCaseWithDuplicateHearing(envelope, hearingId, caseId));
        }
    }

    public void handleCourtApplicationAddedForListedHearing(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_COURT_APPLICATION_ADDED_FOR_LISTED_HEARING, envelope.metadata());
        }

        sendCommandAddApplicationToListedHearing(envelope);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.court-application-added-for-hearing"), envelope.payload()));
    }

    public void handleDefendantsToBeUpdatedMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED, envelope.metadata());
        }
        sendUpdateDefendantsForHearings(envelope);
    }

    public void handleDefendantsToBeAddedForCourtProceedingsMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS, envelope.metadata());
        }
        sendAddDefendantsForCourtProceedings(envelope);
    }

    public void handleOffencesToBeUpdatedMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED, envelope.metadata());
        }

        sendUpdatedOffencesForHearings(envelope);
    }

    public void handleOffencesToBeDeletedMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_DELETED, envelope.metadata());
        }

        sendDeletedOffencesForHearings(envelope);
    }

    public void handleOffencesToBeAddedMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_ADDED, envelope.metadata());
        }

        sendAddedOffencesForHearings(envelope);
    }

    public void handleHearingAllocatedForListingMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING, envelope.metadata());
        }
        final HearingConfirmed hearingConfirmed = getHearingConfirmed(envelope);
        final HearingAllocatedForListing hearingAllocatedForListing = getHearingAllocatedForListing(envelope);

        logger.debug("HearingConfirmed confirmedHearing used for slot update: {}", hearingConfirmed.getConfirmedHearing());

        publishHearingConfirmedPublicEvent(envelope, hearingConfirmed);
        publishHearingChangesSavedPublicEvent(envelope, hearingAllocatedForListing.getHearingId());
    }

    public void handleHearingAllocatedForListingV2Message(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING_V2, envelope.metadata());
        }

        HearingConfirmed hearingConfirmed = getHearingConfirmedV2(envelope);
        final HearingAllocatedForListingV2 hearingAllocatedForListing = getHearingAllocatedForListingV2(envelope);
        final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantOffenceIds = hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds();
        final UUID hearingId = hearingAllocatedForListing.getHearingId();
        hearingConfirmed = updateSendNotificationFlagForNonSummonCivilCaseCreation(envelope, hearingConfirmed);
        publishHearingConfirmedPublicEvent(envelope, hearingConfirmed);
        sendChangeNextHearingDayIfHearingIsSeeded(envelope, prosecutionCaseDefendantOffenceIds, hearingId);
        publishHearingChangesSavedPublicEvent(envelope, hearingAllocatedForListing.getHearingId());
    }

    public void handleAllocatedHearingUpdatedForListingMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING, envelope.metadata());
        }
        final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing = getAllocatedHearingUpdatedForListing(envelope);
        final HearingUpdated hearingUpdated = getHearingUpdated(envelope, allocatedHearingUpdatedForListing);

        logger.debug("HearingUpdated confirmedHearing used for slot update: {}", hearingUpdated.getUpdatedHearing());
        publishHearingUpdatedPublicEvent(envelope, hearingUpdated);
    }

    public void handleAllocatedHearingUpdatedForListingV2Message(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING_V2, envelope.metadata());
        }
        final AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListing = getAllocatedHearingUpdatedForListingV2(envelope);
        final HearingUpdated hearingUpdated = getHearingUpdatedV2(envelope, allocatedHearingUpdatedForListing);
        final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantOffenceIds = allocatedHearingUpdatedForListing.getProsecutionCaseDefendantsOffenceIds();
        final UUID hearingId = allocatedHearingUpdatedForListing.getHearingId();

        logger.debug("HearingUpdated confirmedHearing used for slot update: {}", hearingUpdated.getUpdatedHearing());

        publishHearingUpdatedPublicEvent(envelope, hearingUpdated);
        sendChangeNextHearingDayIfHearingIsSeeded(envelope, prosecutionCaseDefendantOffenceIds, hearingId);
    }

    public void handleRestrictCourtListMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_RESTRICT_COURT_LIST, envelope.metadata());
        }

        publishCourtListRestrictedPublicEvent(envelope);
    }

    public void handleCaseMarkerUpdateMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED, envelope.metadata());
        }

        sendCaseMarkerUpdatedForHearing(envelope);
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")

    public void handleCourtApplicationToBeUpdated(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED, envelope.metadata());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_COURT_APPLICATION_FOR_HEARINGS),
                envelope.payload()));
    }

    private boolean isNotBoxWorkRequest(final HearingExtended hearingExtended) {
        return isNull(hearingExtended.getIsBoxWorkRequest()) || !hearingExtended.getIsBoxWorkRequest();
    }

    public void handleEventsCaseEjectedForAllHearings(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS, envelope.metadata());
        }
        final JsonObject payload = envelope.payload();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            hearingIds.forEach(hearingId -> {
                final String caseId = payload.getString(PROSECUTION_CASE_ID);
                final JsonObject caseEjectedCommandPayload = createObjectBuilder()
                        .add(PROSECUTION_CASE_ID, caseId)
                        .add(HEARING_ID, hearingId)
                        .add(REMOVAL_REASON, payload.getString(REMOVAL_REASON))
                        .build();
                sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CASE_EJECTED),
                        caseEjectedCommandPayload));
            });
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("The Payload for event " + LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS
                        + "has been ignored as it does not contains hearing ids  : {}", envelope.metadata());
            }
        }

    }

    public void handleEventsApplicationEjectedForAllHearings(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS, envelope.metadata());
        }
        final JsonObject payload = envelope.payload();
        if (payload.containsKey(HEARING_IDS)) {
            final JsonArray hearingIds = payload.getJsonArray(HEARING_IDS);
            hearingIds.forEach(hearingId -> {
                final String applicationId = payload.getString(APPLICATION_ID);
                final JsonObject applicationEjectedCommandPayload = createObjectBuilder()
                        .add(APPLICATION_ID, applicationId)
                        .add(HEARING_ID, hearingId)
                        .add(REMOVAL_REASON, payload.getString(REMOVAL_REASON))
                        .build();

                sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_APPLICATION_EJECTED),
                        applicationEjectedCommandPayload));

            });
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("The Payload for event " + LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS
                        + "has been ignored as it does not contains hearing ids : {}", envelope.metadata());
            }
        }

    }

    public void handleDefendantLegalStatusUpdateForHearings(final Envelope envelop) {
        if (logger.isDebugEnabled()) {
            logger.debug("listing.events.defendant-legalaid-status-updated event received {}", envelop.metadata());
        }
        final JsonObject eventPayload = envelop.payload();
        final JsonArray hearingIds = eventPayload.getJsonArray(HEARING_IDS);
        hearingIds.forEach(hearingId -> {
            final JsonObject commandPayload = createObjectBuilder()
                    .add(HEARING_ID, hearingId)
                    .add(CASE_ID, eventPayload.getString(CASE_ID))
                    .add(DEFENDANT_ID, eventPayload.getString(DEFENDANT_ID))
                    .add(LEGAL_AID_STATUS, eventPayload.getString(LEGAL_AID_STATUS))
                    .build();
            this.sender.send(envelopeFrom(metadataFrom(envelop.metadata()).withName(COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS_FOR_HEARING),
                    commandPayload));
        });
    }

    public void handleCaseResultedAndDefendantProceedingsUpdated(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug("listing.events.case-resulted-defendant-proceedings-updated event received {}", envelope.metadata());
        }
        final CaseResultedDefendantProceedingsConcluded caseResultedDefendantProceedingsConcluded = jsonObjectConverter.convert(envelope.payload(), CaseResultedDefendantProceedingsConcluded.class);
        final List<UUID> hearingIds = caseResultedDefendantProceedingsConcluded.getHearingIds();
        hearingIds.forEach(hearingId -> {
            final JsonObject commandPayload = createObjectBuilder()
                    .add(HEARING_ID, hearingId.toString())
                    .add(PROSECUTION_CASE, objectToJsonObjectConverter.convert(caseResultedDefendantProceedingsConcluded.getProsecutionCase()))
                    .build();

            this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_DEFENDANT_COURT_PROCEEDINGS),
                    (commandPayload)));

        });
    }

    public void handleCaseLinkedPrivateEvent(final Envelope envelope) {
        logger.info("listing.events.cases-linked {}", envelope.metadata());
        final LinkedCasesToBeUpdated linkedCasesToBeUpdated = jsonObjectConverter.convert(envelope.payload(), LinkedCasesToBeUpdated.class);
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
            sender.send(envelop(objectToJsonObjectConverter.convert(command)).withName(COMMAND_UPDATE_LINKED_CASE_IN_HEARING).withMetadataFrom(envelope));
        }
    }

    /**
     * Added specifically to complete the loop w.r.t progression service being able to generate
     * summons of the back of anew defendant getting successfully added to an existing hearing.
     * <p>
     * Defensively coded to only raise the event if court centre and hearing start time available
     *
     * @param envelope
     */
    public void handleNewDefendantAddedForCourtProceedings(final Envelope envelope) {
        logger.info("Processing event '{}'", PUBLIC_EVENT_NEW_DEFENDANT_ADDED_FOR_COURT_PROCEEDINGS);

        final NewDefendantAddedForCourtProceedings payload = jsonObjectConverter.convert(envelope.payload(), NewDefendantAddedForCourtProceedings.class);
        final Optional<UUID> optionalCourtCentreId = ofNullable(payload.getCourtCentreId());
        final Optional<UUID> optionalCourtRoomId = ofNullable(payload.getCourtRoomId());
        final Optional<ZonedDateTime> hearingDateTime = ofNullable(payload.getHearingDateTime());
        final boolean hearingAllocated = optionalCourtCentreId.isPresent() && optionalCourtRoomId.isPresent() && hearingDateTime.isPresent();
        if (hearingAllocated) {
            final CourtCentre courtCentre = hearingConfirmedFactory.buildCourtCentre(optionalCourtCentreId.get(), payload.getCourtRoomId(), envelope);
            final PublicListingNewDefendantAddedForCourtProceedings payloadForPublicEvent = publicListingNewDefendantAddedForCourtProceedings()
                    .withDefendantId(payload.getDefendant().getId())
                    .withCourtCentre(courtCentre)
                    .withCaseId(payload.getCaseId())
                    .withHearingId(payload.getHearingId())
                    .withHearingDateTime(hearingDateTime.get())
                    .build();
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_NEW_DEFENDANT_ADDED_FOR_COURT_PROCEEDINGS), objectToJsonObjectConverter.convert(payloadForPublicEvent)));
        } else {
            logger.info("Hearing '{}' is not allocated as its missing either court centre / room / date+time.  Not raising public event '{}'", payload.getHearingId(), PUBLIC_EVENT_NEW_DEFENDANT_ADDED_FOR_COURT_PROCEEDINGS);
        }


    }

    @SuppressWarnings("squid:S3655")
    public void handleHearingUnallocatedForListing(final Envelope envelope) {
        logger.debug("listing.events.hearing-unallocated-for-listing {}", envelope.metadata());
        final HearingUnallocatedForListing hearingUnallocatedForListing = jsonObjectConverter.convert(envelope.payload(), HearingUnallocatedForListing.class);
        if (nonNull(hearingUnallocatedForListing.getSeededHearing()) && hearingUnallocatedForListing.getSeededHearing()) {
            changeNextHearingDay(envelope, hearingUnallocatedForListing.getHearingId());
        }
    }

    // Progression publishes the same {hearingId, offenceIds} payload when a split moves offences off
    // the original hearing, so it routes to the same command as the hearing-context sibling above.


    public void handleHearingMarkedAsDeleted(final Envelope envelope) {

        final HearingMarkedAsDeleted hearingMarkedAsDeleted = jsonObjectConverter.convert(envelope.payload(), HearingMarkedAsDeleted.class);
        final UUID hearingIdToDelete = hearingMarkedAsDeleted.getHearingIdToDelete();

        final JsonObject commandPayload = createObjectBuilder()
                .add(HEARING_ID_TO_MARK, hearingIdToDelete.toString())
                .build();

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DELETED),
                (commandPayload)));

    }

    public void handleHearingMarkedForPartialUpdate(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_MARKED_FOR_PARTIAL_UPDATE, envelope.metadata());
        }

        final JsonObject payload = envelope.payload();

        final HearingMarkedForPartialUpdate hearingMarkedForPartialUpdate = jsonObjectConverter.convert(payload, HearingMarkedForPartialUpdate.class);

        final JsonObject commandPayload = updateUnallocatedHearingPartiallyCommandConverter.convertPartialUpdateEventToCommand(hearingMarkedForPartialUpdate);

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_REMOVE_PARTIALLY_MERGED_OFFENCES),
                (commandPayload)));

    }

    public void handleHearingPartiallyUpdate(final Envelope jsonEnvelope) {

        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_LISTING_HEARING_PARTIALLY_UPDATED, jsonEnvelope.metadata());
        }

        final JsonObject payload = jsonEnvelope.payload();

        final HearingPartiallyUpdated hearingPartiallyUpdated = jsonObjectConverter.convert(payload, HearingPartiallyUpdated.class);

        final uk.gov.justice.listing.courts.HearingPartiallyUpdated pubEvent = publicHearingPartiallyUpdatedFactory.create(hearingPartiallyUpdated);

        //This public event is used by UI only as of now,
        final Envelope publicEvent = envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED),
                objectToJsonValueConverter.convert(pubEvent));
        if (logger.isDebugEnabled()) {
            logger.debug(LOG_PUBLISHING, PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED, publicEvent.metadata());
        }
        sender.send(publicEvent);

        //Unified public event is used by UI only as of now
        publishHearingChangesSavedPublicEvent(jsonEnvelope, hearingPartiallyUpdated.getHearingIdToBeUpdated());

    }

    public void handleHearingChangesSaved(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_CHANGES_SAVED, envelope.metadata());
        }
        final JsonObject payload = envelope.payload();

        final uk.gov.justice.listing.events.HearingChangesSaved hearingChangesSaved = jsonObjectConverter.convert(payload, uk.gov.justice.listing.events.HearingChangesSaved.class);

        publishHearingChangesSavedPublicEvent(envelope, hearingChangesSaved.getHearingId());
    }

    public void handleHearingsUpdateCompletedEvent(Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARINGS_UPDATE_COMPLETED, envelope.metadata());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata())
                .withId(UUID.randomUUID())
                .withName(PUBLIC_EVENT_HEARINGS_UPDATED_COMPLETED).build(), envelope.payload()));

    }


    public void handleHearingAddedToCase(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENTS_HEARING_ADDED_TO_CASE, envelope.metadata());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withId(randomUUID()).withName(PUBLIC_LISTING_HEARING_ADDED_TO_CASE).build(),
                envelope.payload()));
    }

    private JsonObject generatePayloadForCommandMarkHearingAsDuplicate(final Envelope envelope) {
        final JsonObjectBuilder commandPayloadBuilder = createObjectBuilder();
        commandPayloadBuilder.add(HEARING_ID, envelope.payload().getString(HEARING_ID));
        if (envelope.payload().containsKey(PROSECUTION_CASE_IDS)) {
            commandPayloadBuilder.add(PROSECUTION_CASE_IDS, envelope.payload().getJsonArray(PROSECUTION_CASE_IDS));
        }

        return commandPayloadBuilder.build();
    }

    /*
     * For each hearing in the 'case-sent-for-listing' event, extract it
     * and send each one through as a separate 'list-hearing' command.
     */
    private void sendCommandAddHearingToCase(final Envelope envelope) {
        final HearingListed event = getHearingListedEvent(envelope);
        final List<AddHearingToCaseCommand> listHearingCommands = listHearingCommandConverter.convert(event);

        listHearingCommands.forEach(
                listHearingCommand -> {
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_HEARING_TO_CASE, listHearingCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_HEARING_TO_CASE),
                            objectToJsonValueConverter.convert(listHearingCommand)));
                }
        );
    }


    private void sendCaseMarkerUpdatedForHearing(final Envelope envelope) {
        final CaseMarkersToBeUpdated caseMarkerEvent = getCaseMarkerEvent(envelope);
        final List<UpdateCaseMarkersForHearingCommand> updateCaseMarkersForHearingCommands =
                updateCaseMarkersForHearingCommandCollectionConverter.convert(caseMarkerEvent);

        updateCaseMarkersForHearingCommands.forEach(command -> {
            logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_CASE_MARKERS_TO_HEARING, command);
            sender.send(envelop(objectToJsonValueConverter.convert(command)).withName(COMMAND_UPDATE_CASE_MARKERS_TO_HEARING).withMetadataFrom(envelope));
        });
    }

    private void sendCommandAddApplicationToHearing(Envelope envelope) {
        final HearingListed event = getHearingListedEvent(envelope);
        final List<AddApplicationToHearingCommand> addApplicationToHearingCommands = addCourtApplicationToHearingCommandCollectionConverter.convert(event);
        addApplicationToHearingCommands.forEach(addApplicationToHearingCommand -> {
            logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_COURT_APPLICATION_TO_HEARING, addApplicationToHearingCommand);
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_COURT_APPLICATION_TO_HEARING),
                    objectToJsonValueConverter.convert(addApplicationToHearingCommand)));

        });
    }

    private void sendCommandAddApplicationToListedHearing(Envelope envelope) {
        final CourtApplicationAddedForHearing event = jsonObjectConverter.convert(envelope.payload(), CourtApplicationAddedForHearing.class);
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
    private void sendUpdateDefendantsForHearings(final Envelope envelope) {
        final DefendantsToBeUpdated event = getDefendantsToBeUpdated(envelope);
        final List<UpdateDefendantsForHearingCommand> commands = updateDefendantsForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                updateDefendantsForHearingCommand -> {
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_DEFENDANTS_FOR_HEARING, updateDefendantsForHearingCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_DEFENDANTS_FOR_HEARING),
                            objectToJsonValueConverter.convert(updateDefendantsForHearingCommand)));
                }
        );
    }

    private void sendAddDefendantsForCourtProceedings(final Envelope envelope) {
        final DefendantsToBeAddedForCourtProceedings event = getDefendantsToBeAddedForCourtProceedings(envelope);
        final List<AddDefendantsForCourtProceedingsCommand> commands = addDefendantsForCourtProceedingsCommandCollectionConverter.convert(event);

        commands.forEach(
                addDefendantsForCourtProceedingsCommand -> {
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING, addDefendantsForCourtProceedingsCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING),
                            objectToJsonValueConverter.convert(addDefendantsForCourtProceedingsCommand)));
                }
        );
    }

    private void sendUpdatedOffencesForHearings(final Envelope envelope) {
        final OffencesToBeUpdated event = getOffencesToBeUpdated(envelope);
        final List<UpdateOffencesForHearingCommand> commands = updateOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                updateOffencesForHearingCommand -> {
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_OFFENCES_FOR_HEARING, updateOffencesForHearingCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_OFFENCES_FOR_HEARING),
                            objectToJsonValueConverter.convert(updateOffencesForHearingCommand)));
                }
        );
    }

    private void sendDeletedOffencesForHearings(final Envelope envelope) {
        final OffencesToBeDeleted event = getOffencesToBeDeleted(envelope);
        final List<DeleteOffencesForHearingCommand> commands = deleteOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                deleteOffencesForHearingCommand -> {
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_DELETE_OFFENCES_FOR_HEARING, deleteOffencesForHearingCommand);
                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_DELETE_OFFENCES_FOR_HEARING),
                            objectToJsonValueConverter.convert(deleteOffencesForHearingCommand)));
                }
        );
    }

    private void sendAddedOffencesForHearings(final Envelope envelope) {
        final OffencesToBeAdded event = getOffencesToBeAdded(envelope);
        final List<AddOffencesForHearingCommand> commands = addOffencesForHearingCommandCollectionConverter.convert(event);

        commands.forEach(
                addOffencesForHearingCommand -> {
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_OFFENCES_FOR_HEARING, addOffencesForHearingCommand);

                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_OFFENCES_FOR_HEARING),
                            objectToJsonValueConverter.convert(addOffencesForHearingCommand)));
                }
        );
    }

    @SuppressWarnings("squid:S3655")
    private void sendChangeNextHearingDayIfHearingIsSeeded(final Envelope envelope, final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantsOffenceIds, final UUID hearingId) {
        if (nonNull(prosecutionCaseDefendantsOffenceIds)) {
            final Optional<OffenceIds> seededOffence = prosecutionCaseDefendantsOffenceIds.stream()
                    .flatMap(pc -> pc.getDefendants().stream())
                    .flatMap(defendantOffenceIdsV2 -> defendantOffenceIdsV2.getOffenceIds().stream())
                    .filter(offenceIds -> nonNull(offenceIds.getSeedingHearing()))
                    .findFirst();

            if (seededOffence.isPresent()) {
                changeNextHearingDay(envelope, hearingId);
            }
        }
    }

    private void changeNextHearingDay(final Envelope envelope, final UUID hearingId) {
        final JsonObject changeNextHearingDay = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CHANGE_NEXT_HEARING_DAY),
                changeNextHearingDay));
    }


    /*
     * Publish a public event to notify that the hearing has been confirmed.
     */
    private void publishHearingConfirmedPublicEvent(final Envelope envelope, final HearingConfirmed hearingConfirmed) {
        logger.info(LOG_PUBLISHING, PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_CONFIRMED),
                objectToJsonValueConverter.convert(hearingConfirmed)));
    }

    private void publishHearingChangesSavedPublicEvent(final Envelope envelope, final UUID hearingId) {
        final HearingChangesSaved hearingChangesSaved = HearingChangesSaved.hearingChangesSaved().withHearingId(hearingId).build();


        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withId(randomUUID()).withName(PUBLIC_EVENT_HEARING_CHANGES_SAVED).build(),
                objectToJsonValueConverter.convert(hearingChangesSaved)));
        logger.info(LOG_PUBLISHING, PUBLIC_EVENT_HEARING_CHANGES_SAVED, hearingChangesSaved);
    }

    private HearingConfirmed getHearingConfirmed(final Envelope envelope) {
        final HearingAllocatedForListing hearingAllocatedForListing = getHearingAllocatedForListing(envelope);
        return getHearingConfirmed(hearingAllocatedForListing, envelope);
    }

    private HearingConfirmed getHearingConfirmedV2(final Envelope envelope) {
        final HearingAllocatedForListingV2 hearingAllocatedForListing = getHearingAllocatedForListingV2(envelope);
        return getHearingConfirmedV2(hearingAllocatedForListing, envelope);
    }

    private HearingAllocatedForListing getHearingAllocatedForListing(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), HearingAllocatedForListing.class);
    }

    private HearingAllocatedForListingV2 getHearingAllocatedForListingV2(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), HearingAllocatedForListingV2.class);
    }

    private HearingConfirmed getHearingConfirmed(final HearingAllocatedForListing hearingAllocatedForListing, final Envelope envelope) {
        return hearingConfirmedFactory.create(hearingAllocatedForListing, envelope);
    }

    private HearingConfirmed getHearingConfirmedV2(final HearingAllocatedForListingV2 hearingAllocatedForListing, final Envelope envelope) {
        return hearingConfirmedFactory.createV2(hearingAllocatedForListing, envelope);
    }

    /*
     * Publish a public event to notify that the hearing has been updated.
     */
    private void publishHearingUpdatedPublicEvent(final Envelope envelope, final HearingUpdated hearingUpdated) {

        logger.info(LOG_PUBLISHING, PUBLIC_EVENT_HEARING_UPDATED, hearingUpdated);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_UPDATED),
                objectToJsonValueConverter.convert(hearingUpdated)));
    }

    private HearingUpdated getHearingUpdated(final Envelope envelope, final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing) {
        return allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing, envelope);
    }

    private HearingUpdated getHearingUpdatedV2(final Envelope envelope, final AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListing) {
        return allocatedHearingUpdatedFactory.createV2(allocatedHearingUpdatedForListing, envelope);
    }


    private AllocatedHearingUpdatedForListing getAllocatedHearingUpdatedForListing(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), AllocatedHearingUpdatedForListing.class);
    }

    private AllocatedHearingUpdatedForListingV2 getAllocatedHearingUpdatedForListingV2(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), AllocatedHearingUpdatedForListingV2.class);
    }

    /*
     * Publish a public event to notify that the hearing has been updated with restrictions on court listing.
     */
    private void publishCourtListRestrictedPublicEvent(final Envelope envelope) {
        final CourtListRestricted courtListRestricted =
                jsonObjectConverter.convert(envelope.payload(), CourtListRestricted.class);
        logger.info(LOG_PUBLISHING, PUBLIC_EVENT_RESTRICT_COURT_LIST, courtListRestricted);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_RESTRICT_COURT_LIST),
                objectToJsonValueConverter.convert(courtListRestricted)));
    }

    private HearingListed getHearingListedEvent(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), HearingListed.class);
    }

    private DefendantsToBeUpdated getDefendantsToBeUpdated(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), DefendantsToBeUpdated.class);
    }

    private OffencesToBeUpdated getOffencesToBeUpdated(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), OffencesToBeUpdated.class);
    }

    private OffencesToBeDeleted getOffencesToBeDeleted(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), OffencesToBeDeleted.class);
    }

    private OffencesToBeAdded getOffencesToBeAdded(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), OffencesToBeAdded.class);
    }

    private DefendantsToBeAddedForCourtProceedings getDefendantsToBeAddedForCourtProceedings(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), DefendantsToBeAddedForCourtProceedings.class);
    }

    private CaseMarkersToBeUpdated getCaseMarkerEvent(final Envelope envelope) {
        return jsonObjectConverter.convert(envelope.payload(), CaseMarkersToBeUpdated.class);
    }

    public void handleAllocatedHearingExtendedForListingMessage(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING, envelope.metadata());
        }

        publishHearingConfirmedPublicEventForExtendHearing(envelope);
    }

    public void handleAllocatedHearingExtendedForListingV2Message(final Envelope envelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING_V2, envelope.metadata());
        }

        publishHearingConfirmedPublicEventForExtendHearingAllocatedHearingExtendedV2(envelope);
    }

    private void publishHearingConfirmedPublicEventForExtendHearing(final Envelope envelope) {
        final AllocatedHearingExtendedForListing hearingExtendedForListing = jsonObjectConverter.convert(envelope.payload(), AllocatedHearingExtendedForListing.class);
        final HearingConfirmed hearingConfirmed = allocatedHearingExtendedFactory.create(hearingExtendedForListing, envelope);

        updateHearingToCase(envelope, hearingConfirmed);

        logger.info("Publishing '{}' public event for extend hearing with payload {}", PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        final JsonValue payload = objectToJsonValueConverter.convert(hearingConfirmed);
        sender.send(envelop(payload)
                .withName(PUBLIC_EVENT_HEARING_CONFIRMED)
                .withMetadataFrom(envelope));
    }

    private void publishHearingConfirmedPublicEventForExtendHearingAllocatedHearingExtendedV2(final Envelope envelope) {
        final AllocatedHearingExtendedForListingV2 allocatedHearingExtendedForListingV2 = jsonObjectConverter.convert(envelope.payload(), AllocatedHearingExtendedForListingV2.class);
        final HearingConfirmed hearingConfirmed = allocatedHearingExtendedFactory.create(allocatedHearingExtendedForListingV2, envelope);

        updateHearingToCase(envelope, hearingConfirmed);

        logger.info("Publishing '{}' public event for extend hearing with payload {}", PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        final JsonValue payload = objectToJsonValueConverter.convert(hearingConfirmed);
        sender.send(envelop(payload)
                .withName(PUBLIC_EVENT_HEARING_CONFIRMED)
                .withMetadataFrom(envelope));
    }

    private void updateHearingToCase(Envelope envelope, HearingConfirmed hearingConfirmed) {

        final ConfirmedHearing confirmedHearing = hearingConfirmed.getConfirmedHearing();

        for (final ConfirmedProsecutionCase prosecutionCase : confirmedHearing.getProsecutionCases()) {

            final Optional<UUID> allocatedHearingIdOpt = ofNullable(confirmedHearing.getExistingHearingId());
            if (allocatedHearingIdOpt.isPresent()) {

                final UUID allocatedHearingId = allocatedHearingIdOpt.get();

                final UpdateHearingToCaseCommand updateHearingToCaseCommand =
                        new UpdateHearingToCaseCommand(prosecutionCase.getId(), allocatedHearingId, confirmedHearing.getId());
                logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_HEARING_TO_CASE, updateHearingToCaseCommand);

                final JsonValue payload = objectToJsonValueConverter.convert(updateHearingToCaseCommand);
                sender.send(envelop(payload)
                        .withName(COMMAND_UPDATE_HEARING_TO_CASE)
                        .withMetadataFrom(envelope));
            }
        }
    }

    private void sendUpdateCaseWithDuplicateHearing(final Envelope envelope, final UUID hearingId, final UUID caseId) {
        final JsonObject hearingMarkedAsDuplicateForCase = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(CASE_ID, caseId.toString())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DUPLICATE_FOR_CASE),
                hearingMarkedAsDuplicateForCase));
    }

    public void handleListingCourtRoomNotSelectedEvent(Envelope jsonEnvelope) {
        if (logger.isDebugEnabled()) {
            logger.debug(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING, jsonEnvelope.metadata());
        }
        final HearingDaysChangedForHearing hearingDaysChangedForHearing = jsonObjectConverter.convert(jsonEnvelope.payload(), HearingDaysChangedForHearing.class);
        convertHearingDayEventToCourtEvent(jsonEnvelope, hearingDaysChangedForHearing);
        publishHearingChangesSavedPublicEvent(jsonEnvelope, hearingDaysChangedForHearing.getHearingId());
    }

    private void convertHearingDayEventToCourtEvent(final Envelope jsonEnvelope, final HearingDaysChangedForHearing hearingDaysChangedForHearing) {

        final List<uk.gov.justice.core.courts.HearingDay> courtHearingDays = new ArrayList<>();
        for (final HearingDay hearingDayEvent : hearingDaysChangedForHearing.getHearingDays()) {
            final uk.gov.justice.core.courts.HearingDay courtHearingDay = allocatedHearingExtendedFactory.buildHearingDay(hearingDayEvent);
            courtHearingDays.add(courtHearingDay);
        }

        final uk.gov.justice.listing.courts.HearingDaysChangedForHearing hearingDaysForHearingChanged = uk.gov.justice.listing.courts.HearingDaysChangedForHearing.hearingDaysChangedForHearing().withHearingDays(courtHearingDays).withHearingId(hearingDaysChangedForHearing.getHearingId()).build();

        //This public event is used by UI only as of now,
        final Envelope publicEvent = envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING),
                objectToJsonValueConverter.convert(hearingDaysForHearingChanged));
        if (logger.isDebugEnabled()) {
            logger.debug(LOG_PUBLISHING, PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING, publicEvent.metadata());
        }

        sender.send(publicEvent);
    }

    private void publishPublicHearingListedEvent(final Envelope jsonEnvelope) {

        final HearingListed listingHearingListed = jsonObjectConverter.convert(jsonEnvelope.payload(), HearingListed.class);

        final Hearing courtDomainHearing = hearingListingToCoreConverter.convert(listingHearingListed.getHearing());
        final List<ProsecutionCase> prosecutionCases = courtDomainHearing.getProsecutionCases();

        // Create caseUrns from ProsecutionCase list
        List<CaseUrns> caseUrns = Optional.ofNullable(prosecutionCases)
                .orElse(Collections.emptyList())
                .stream()
                .filter(prosecutionCase -> nonNull(prosecutionCase)
                        && nonNull(prosecutionCase.getProsecutionCaseIdentifier())
                        && nonNull(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN()))
                .map(prosecutionCase -> CaseUrns.caseUrns()
                        .withCaseURN(prosecutionCase.getProsecutionCaseIdentifier().getCaseURN())
                        .build()).toList();

        if (caseUrns != null && !caseUrns.isEmpty()) {
            final uk.gov.justice.listing.courts.HearingListed courtHearingListed = uk.gov.justice.listing.courts.HearingListed.
                    hearingListed()
                    .withHearingId(courtDomainHearing.getId())
                    .withHearingType(courtDomainHearing.getType().getDescription())
                    .withCaseUrns(caseUrns)
                    .build();

            final Envelope publicEvent = envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_HEARING_LISTED),
                    objectToJsonObjectConverter.convert(courtHearingListed));

            if (logger.isDebugEnabled()) {
                logger.debug(LOG_PUBLISHING, PUBLIC_LISTING_HEARING_LISTED, publicEvent.metadata());
            }

            sender.send(publicEvent);
        }

        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withId(randomUUID()).withName("public.listing.court-application-added-for-hearing"), jsonEnvelope.payload()));
    }

    private HearingConfirmed updateSendNotificationFlagForNonSummonCivilCaseCreation(final Envelope envelope, final HearingConfirmed hearingConfirmed) {
        if (isNotEmpty(hearingConfirmed.getConfirmedHearing().getProsecutionCases())) {
            final ConfirmedProsecutionCase confirmedProsecutionCase = hearingConfirmed.getConfirmedHearing().getProsecutionCases().get(0);
            final boolean isCivil = nonNull(confirmedProsecutionCase.getIsCivil()) && confirmedProsecutionCase.getIsCivil();
            boolean isSummonCase;
            if (isCivil && !isBulkCase(confirmedProsecutionCase)) {
                // ProgressionService.getProsecutionCaseByCaseId (as admin): no case fails the step, as before.
                final ProsecutionCase prosecutionCase = progressionService.findProsecutionCaseByCaseId(confirmedProsecutionCase.getId().toString())
                        .map(json -> PROSECUTION_CASE_CONVERTER.convert(json, ProsecutionCase.class))
                        .orElseThrow(() -> new IllegalStateException("No prosecution case " + confirmedProsecutionCase.getId() + " in progression"));
                isSummonCase = nonNull(prosecutionCase.getInitiationCode()) && InitiationCode.S == prosecutionCase.getInitiationCode();
                if (!isSummonCase) {
                    return HearingConfirmed.hearingConfirmed()
                            .withValuesFrom(hearingConfirmed)
                            .withSendNotificationToParties(true)
                            .build();
                }
            }
        }
        return hearingConfirmed;
    }

    private boolean isBulkCase(final ConfirmedProsecutionCase confirmedProsecutionCase) {
        return (nonNull(confirmedProsecutionCase.getIsGroupMaster()) && confirmedProsecutionCase.getIsGroupMaster()) ||
                (nonNull(confirmedProsecutionCase.getIsGroupMember()) && confirmedProsecutionCase.getIsGroupMember());
    }

}
