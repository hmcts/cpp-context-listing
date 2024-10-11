package uk.gov.moj.cpp.listing.event.processor;


import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings.publicListingNewDefendantAddedForCourtProceedings;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.core.courts.ConfirmedHearing;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Hearing;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.AddApplicationToHearingCommand;
import uk.gov.justice.listing.commands.AddHearingToCaseCommand;
import uk.gov.justice.listing.commands.LinkedToCases;
import uk.gov.justice.listing.commands.UpdateLinkedCaseInHearing;
import uk.gov.justice.listing.courts.HearingChangesSaved;
import uk.gov.justice.listing.courts.HearingConfirmed;
import uk.gov.justice.listing.courts.HearingUpdated;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
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
import uk.gov.justice.listing.events.JudicialRole;
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
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.event.processor.azure.data.SlotDetail;
import uk.gov.moj.cpp.listing.event.processor.command.AddCourtApplicationToHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddDefendantsForCourtProceedingsCommand;
import uk.gov.moj.cpp.listing.event.processor.command.AddDefendantsForCourtProceedingsCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddHearingToCaseCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.AddOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.AddOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.DeleteOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.DeleteOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateCaseMarkersForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateCaseMarkersForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateDefendantsForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommand;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateOffencesForHearingCommandCollectionConverter;
import uk.gov.moj.cpp.listing.event.processor.command.UpdateUnallocatedHearingPartiallyCommandConverter;
import uk.gov.moj.cpp.listing.event.processor.factory.PublicHearingPartiallyUpdatedFactory;
import uk.gov.moj.cpp.listing.event.processor.util.HearingListedToUpdateHearingForListingCommand;
import uk.gov.moj.cpp.listing.event.processor.util.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.listing.event.processor.util.JudicialRoleDomainToEventConverter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.slf4j.Logger;

@SuppressWarnings("squid:S2629")
@ServiceComponent(EVENT_PROCESSOR)
public class ListingEventProcessor {

    static final String PUBLIC_EVENT_HEARING_CONFIRMED = "public.listing.hearing-confirmed";
    static final String PUBLIC_EVENT_HEARING_UPDATED = "public.listing.hearing-updated";
    static final String PUBLIC_EVENT_VACATED_TRIAL_UPDATED = "public.listing.vacated-trial-updated";
    static final String PUBLIC_EVENT_HEARING_CHANGES_SAVED = "public.listing.hearing-changes-saved";
    static final String PUBLIC_EVENT_LISTING_EVENTS_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED = "public.events.listing.hearing-days-without-court-centre-corrected";
    static final String COMMAND_UPDATE_CASE_DEFENDANT_DETAILS = "listing.command.update-case-defendant-details"; //command back of public event
    static final String COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES = "listing.command.update-case-defendant-offences";
    static final String COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS = "listing.command.add-defendants-to-court-proceedings";
    static final String COMMAND_UPDATE_COURT_APPLICATION = "listing.command.update-court-application";
    static final String COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING = "listing.command.add-court-application-for-hearing";
    static final String COMMAND_CASE_OR_APPLICATION_EJECTED = "listing.command.eject-case-or-application";
    static final String COMMAND_CASE_EJECTED = "listing.command.eject-case";
    static final String COMMAND_APPLICATION_EJECTED = "listing.command.eject-application";
    static final String COMMAND_UPDATE_HEARING_TO_CASE = "listing.command.update-hearing-to-case";
    static final String PRIVATE_COMMAND_HEARING_VACATE_TRIAL = "listing.command.hearing-vacate-trial";

    private static final String COMMAND_PAYLOAD_DEBUG_STRING = "Sending '{}' command with payload {}";
    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    private static final String HEARING_IDS = "hearingIds";
    private static final String PROSECUTION_CASE_ID = "prosecutionCaseId";
    private static final String REMOVAL_REASON = "removalReason";
    private static final String PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED = "public.progression.defendant-offences-changed";
    private static final String PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed"; //public
    private static final String PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED = "public.progression.court-application-changed";
    private static final String PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED = "public.progression.case-markers-updated";
    private static final String PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED = "public.progression.hearing-resulted-case-updated";
    private static final String PUBLIC_EVENT_RESTRICT_COURT_LIST = "public.listing.court-list-restricted";
    private static final String PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION = "public.progression.events.hearing-extended";
    private static final String PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";
    private static final String PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED = "public.progression.events.case-or-application-ejected";
    private static final String PUBLIC_EVENT_PROGRESSION_CASE_LINKED = "public.progression.case-linked";
    private static final String PUBLIC_EVENT_HEARING_TRIAL_VACATED = "public.hearing.trial-vacated";
    private static final String PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT = "public.events.hearing.marked-as-duplicate";
    private static final String LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS = "listing.events.case-ejected-for-hearings";
    private static final String LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS = "listing.events.application-ejected-for-hearings";
    private static final String PRIVATE_EVENT_HEARING_CHANGES_SAVED = "listing.events.hearing-changes-saved";
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
    private static final String PRIVATE_EVENT_LINKED_CASES_TO_BE_UPDATED = "listing.events.linked-cases-to-be-updated";
    private static final String PRIVATE_EVENT_TRIAL_VACATED = "listing.events.trial-vacated";
    private static final String PRIVATE_EVENT_HEARING_REQUESTED_FOR_LISTING = "listing.events.hearing-requested-for-listing";
    private static final String PRIVATE_EVENT_HEARING_MARKED_AS_DUPLICATE = "listing.events.hearing-marked-as-duplicate";
    private static final String PRIVATE_EVENT_HEARING_RESCHEDULED = "listing.events.hearing-rescheduled";
    private static final String PRIVATE_EVENT_HEARING_UNALLOCATED_FOR_LISTING = "listing.events.hearing-unallocated-for-listing";
    private static final String LISTING_EVENTS_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED = "listing.events.case-resulted-defendant-proceedings-updated";
    private static final String LISTING_EVENTS_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED = "listing.events.hearing-days-without-court-centre-corrected";
    private static final String COMMAND_ADD_HEARING_TO_CASE = "listing.command.add-hearing-to-case";
    private static final String COMMAND_ADD_COURT_APPLICATION_TO_HEARING = "listing.command.add-court-application-to-hearing";
    private static final String COMMAND_UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing"; // command back of private event
    private static final String COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING = "listing.command.add-defendants-to-court-proceedings-for-hearing";
    private static final String COMMAND_UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    private static final String COMMAND_DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    private static final String COMMAND_ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";
    private static final String COMMAND_UPDATE_COURT_APPLICATION_FOR_HEARINGS = "listing.command.update-court-application-for-hearings";
    private static final String COMMAND_UPDATE_CASE_MARKERS_TO_HEARING = "listing.command.update-case-markers-to-hearing";
    private static final String COMMAND_UPDATE_CASE_MARKERS = "listing.command.update-case-markers";
    private static final String COMMAND_UPDATE_DEFENDANT_COURT_PROCEEDINGS = "listing.command.update-defendant-court-proceedings";
    private static final String COMMAND_UPDATE_LINKED_CASES = "listing.command.update-linked-cases";
    private static final String COMMAND_UPDATE_LINKED_CASE_IN_HEARING = "listing.command.update-linked-case-in-hearing";
    private static final String COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED = "listing.command.update-hearing-for-listing-enriched";
    private static final String COMMAND_MARK_HEARING_AS_DUPLICATE = "listing.command.mark-hearing-as-duplicate";
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
    private static final String PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED = "public.progression.defendant-legalaid-status-updated";
    private static final String COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS = "listing.command.update-defendant-legalaid-status";
    private static final String COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS_FOR_HEARING = "listing.command.update-defendant-legalaid-status-for-hearing";
    private static final String LISTING_EVENTS_DEFENDANT_LEGALAID_STATUS_UPDATED = "listing.events.defendant-legalaid-status-updated";
    private static final String COMMAND_UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED = "listing.command.update-case-resulted-defendant-proceedings-concluded";
    private static final String LISTING_COMMAND_ADD_CASES_TO_HEARING = "listing.command.add-cases-to-hearing";
    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING = "listing.events.allocated-hearing-extended-for-listing";
    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING_V2 = "listing.events.allocated-hearing-extended-for-listing-v2";

    private static final String PUBLIC_EVENT_HEARING_DAYS_CANCELLED = "public.hearing.hearing-days-cancelled";
    private static final String COMMAND_CANCEL_HEARING_DAYS = "listing.command.cancel-hearing-days";
    private static final String LEGAL_AID_STATUS = "legalAidStatus";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String CASE_ID = "caseId";
    private static final String PROSECUTION_CASE = "prosecutionCase";
    private static final String PROSECUTION_CASE_IDS = "prosecutionCaseIds";
    private static final String PRIVATE_EVENT_NEW_DEFENDANT_ADDED_FOR_COURT_PROCEEDINGS = "listing.events.new-defendant-added-for-court-proceedings";
    private static final String PUBLIC_EVENT_NEW_DEFENDANT_ADDED_FOR_COURT_PROCEEDINGS = "public.listing.new-defendant-added-for-court-proceedings";
    private static final String PRIVATE_EVENT_HEARING_MARKED_AS_DELETED = "listing.events.hearing-marked-as-deleted";
    private static final String PRIVATE_EVENT_HEARING_MARKED_FOR_PARTIAL_UPDATE = "listing.events.hearing-marked-for-partial-update";
    private static final String PRIVATE_EVENTS_HEARING_ADDED_TO_CASE = "listing.events.hearing-added-to-case";
    static final String COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS = "listing.command.change-judiciary-for-hearings";
    public static final String PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING = "public.hearing.selected-offences-removed-from-existing-hearing";
    private static final String COURT_CENTRE_ID_FIELD = "courtCentreId";
    private static final String PRIVATE_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING = "listing.events.hearing-days-changed-for-hearing";
    private static final String PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING = "public.listing.hearing-days-changed-for-hearing";

    private static final String PUBLIC_LISTING_HEARING_LISTED = "public.listing.hearing-listed";

    private static final String PRIVATE_LISTING_HEARING_PARTIALLY_UPDATED = "listing.events.hearing-partially-updated";
    private static final String PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED = "public.listing.hearing-partially-updated";
    public static final String COURT_APPLICATION = "courtApplication";
    public static final String PUBLIC_LISTING_HEARING_ADDED_TO_CASE = "public.listing.hearing-added-to-case";


    @Inject
    private Sender sender;

    @Inject
    private AllocatedHearingUpdatedFactory allocatedHearingUpdatedFactory;

    @Inject
    private AllocatedHearingExtendedFactory allocatedHearingExtendedFactory;

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
    private UpdateCaseMarkersForHearingCommandCollectionConverter updateCaseMarkersForHearingCommandCollectionConverter;

    @Inject
    private HearingListedToUpdateHearingForListingCommand hearingListedToUpdateHearingForListingCommand;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Inject
    private SlotUpdater slotUpdater;

    @Inject
    private UpdateUnallocatedHearingPartiallyCommandConverter updateUnallocatedHearingPartiallyCommandConverter;

    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Inject
    private HearingObjectsListingToCoreConverter hearingListingToCoreConverter;

    @Inject
    private PublicHearingPartiallyUpdatedFactory publicHearingPartiallyUpdatedFactory;

    @Inject
    private Logger logger;

    @Handles(PRIVATE_EVENT_HEARING_LISTED)
    public void handleHearingListedMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_LISTED, envelope.toObfuscatedDebugString());
        }

        sendCommandAddHearingToCase(envelope);
        sendCommandAddApplicationToHearing(envelope);
        sendCommandUpdateHearingForListing(envelope);

        publishPublicHearingListedEvent(envelope);
    }


    @Handles(PRIVATE_EVENT_TRIAL_VACATED)
    public void trialVacated(final JsonEnvelope envelope) {
        logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_TRIAL_VACATED, envelope.toObfuscatedDebugString());

        final TrialVacated trialVacated = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), TrialVacated.class);

        final JsonObject vacatedTrialUpdatedPayload = createObjectBuilder()
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

        final JsonObject vacatedTrialUpdatedPayload = createObjectBuilder()
                .add(HEARING_ID, hearingRescheduled.getHearingId().toString())
                .add(IS_VACATED, false)
                .add(ALLOCATED, hearingRescheduled.getAllocated())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_VACATED_TRIAL_UPDATED),
                vacatedTrialUpdatedPayload));
    }

    @Handles(PRIVATE_EVENT_HEARING_REQUESTED_FOR_LISTING)
    public void handleHearingRequestedForListing(final JsonEnvelope envelope){
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, "listing.events.hearing-requested-for-listing", envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.hearing-requested-for-listing"),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PRIVATE_EVENT_HEARING_MARKED_AS_DUPLICATE)
    public void handlePrivateHearingMarkedAsDuplicate(final JsonEnvelope envelope) {
        final HearingMarkedAsDuplicate hearingMarkedAsDuplicate = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingMarkedAsDuplicate.class);
        final UUID hearingId = hearingMarkedAsDuplicate.getHearingId();

        if (isNotEmpty(hearingMarkedAsDuplicate.getCaseIds())) {
            hearingMarkedAsDuplicate.getCaseIds().forEach(caseId ->
                    sendUpdateCaseWithDuplicateHearing(envelope, hearingId, caseId));
        }
    }

    @Handles(PRIVATE_EVENT_COURT_APPLICATION_ADDED_FOR_LISTED_HEARING)
    public void handleCourtApplicationAddedForListedHearing(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_COURT_APPLICATION_ADDED_FOR_LISTED_HEARING, envelope.toObfuscatedDebugString());
        }

        sendCommandAddApplicationToListedHearing(envelope);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.court-application-added-for-hearing"), envelope.payloadAsJsonObject()));
    }

    @Handles(PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED)
    public void handleDefendantsToBeUpdatedMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_DEFENDANTS_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }
        sendUpdateDefendantsForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS)
    public void handleDefendantsToBeAddedForCourtProceedingsMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS, envelope.toObfuscatedDebugString());
        }
        sendAddDefendantsForCourtProceedings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED)
    public void handleOffencesToBeUpdatedMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }

        sendUpdatedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_DELETED)
    public void handleOffencesToBeDeletedMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_DELETED, envelope.toObfuscatedDebugString());
        }

        sendDeletedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_OFFENCES_TO_BE_ADDED)
    public void handleOffencesToBeAddedMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_OFFENCES_TO_BE_ADDED, envelope.toObfuscatedDebugString());
        }

        sendAddedOffencesForHearings(envelope);
    }

    @Handles(PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING)
    public void handleHearingAllocatedForListingMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING, envelope.toObfuscatedDebugString());
        }
        final HearingConfirmed hearingConfirmed = getHearingConfirmed(envelope);
        final HearingAllocatedForListing hearingAllocatedForListing = getHearingAllocatedForListing(envelope);
        final boolean isSlotUpdated = hearingAllocatedForListing.getUpdateSlot();
        final boolean isForAdjournmentHearing = hearingAllocatedForListing.getHasAdjournmentDate();
        final List<HearingDay> hearingDays = hearingAllocatedForListing.getHearingDays();

        logger.debug("HearingConfirmed confirmedHearing used for slot update: {}", hearingConfirmed.getConfirmedHearing());

        updateSlotAndSendChangeJudiciaryForHearingsIfJudiciariesPresent(envelope, hearingConfirmed, isSlotUpdated, isForAdjournmentHearing, hearingDays, hearingAllocatedForListing.getJurisdictionType(), hearingAllocatedForListing.getJudiciary());

        publishHearingConfirmedPublicEvent(envelope, hearingConfirmed);
        publishHearingChangesSavedPublicEvent(envelope, hearingAllocatedForListing.getHearingId());
    }

    @Handles(PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING_V2)
    public void handleHearingAllocatedForListingV2Message(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_ALLOCATED_FOR_LISTING_V2, envelope.toObfuscatedDebugString());
        }

        final HearingConfirmed hearingConfirmed = getHearingConfirmedV2(envelope);
        final HearingAllocatedForListingV2 hearingAllocatedForListing = getHearingAllocatedForListingV2(envelope);
        final boolean isSlotUpdated = hearingAllocatedForListing.getUpdateSlot();
        final boolean isForAdjournmentHearing = hearingAllocatedForListing.getHasAdjournmentDate();
        final List<HearingDay> hearingDays = hearingAllocatedForListing.getHearingDays();
        final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantOffenceIds = hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds();
        final UUID hearingId = hearingAllocatedForListing.getHearingId();

        logger.debug("HearingConfirmed confirmedHearing used for slot update: {}", hearingConfirmed.getConfirmedHearing());

        updateSlotAndSendChangeJudiciaryForHearingsIfJudiciariesPresent(envelope, hearingConfirmed, isSlotUpdated, isForAdjournmentHearing, hearingDays, hearingAllocatedForListing.getJurisdictionType(), hearingAllocatedForListing.getJudiciary());

        publishHearingConfirmedPublicEvent(envelope, hearingConfirmed);
        sendChangeNextHearingDayIfHearingIsSeeded(envelope, prosecutionCaseDefendantOffenceIds, hearingId);
        publishHearingChangesSavedPublicEvent(envelope, hearingAllocatedForListing.getHearingId());

    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING)
    public void handleAllocatedHearingUpdatedForListingMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING, envelope.toObfuscatedDebugString());
        }
        final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing = getAllocatedHearingUpdatedForListing(envelope);
        final HearingUpdated hearingUpdated = getHearingUpdated(envelope, allocatedHearingUpdatedForListing);
        final boolean isSlotUpdated = getAllocatedHearingUpdatedForListing(envelope).getUpdateSlot();
        final List<HearingDay> hearingDays = allocatedHearingUpdatedForListing.getHearingDays();

        logger.debug("HearingUpdated confirmedHearing used for slot update: {}", hearingUpdated.getUpdatedHearing());

        slotUpdater.updateSlot(envelope, hearingUpdated.getUpdatedHearing(), isSlotUpdated, false, hearingDays);

        publishHearingUpdatedPublicEvent(envelope, hearingUpdated);
    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING_V2)
    public void handleAllocatedHearingUpdatedForListingV2Message(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_UPDATED_FOR_LISTING_V2, envelope.toObfuscatedDebugString());
        }
        final AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListing = getAllocatedHearingUpdatedForListingV2(envelope);
        final HearingUpdated hearingUpdated = getHearingUpdatedV2(envelope, allocatedHearingUpdatedForListing);
        final boolean isSlotUpdated = getAllocatedHearingUpdatedForListingV2(envelope).getUpdateSlot();
        final List<HearingDay> hearingDays = allocatedHearingUpdatedForListing.getHearingDays();
        final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantOffenceIds = allocatedHearingUpdatedForListing.getProsecutionCaseDefendantsOffenceIds();
        final UUID hearingId = allocatedHearingUpdatedForListing.getHearingId();

        logger.debug("HearingUpdated confirmedHearing used for slot update: {}", hearingUpdated.getUpdatedHearing());

        slotUpdater.updateSlot(envelope, hearingUpdated.getUpdatedHearing(), isSlotUpdated, false, hearingDays);

        publishHearingUpdatedPublicEvent(envelope, hearingUpdated);
        sendChangeNextHearingDayIfHearingIsSeeded(envelope, prosecutionCaseDefendantOffenceIds, hearingId);
    }

    @Handles(PRIVATE_EVENT_RESTRICT_COURT_LIST)
    public void handleRestrictCourtListMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_RESTRICT_COURT_LIST, envelope.toObfuscatedDebugString());
        }

        publishCourtListRestrictedPublicEvent(envelope);
    }

    @Handles(PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED)
    public void handleCaseMarkerUpdateMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_CASE_MARKERS_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }

        sendCaseMarkerUpdatedForHearing(envelope);
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED)
    public void handleCaseDefendantChangedMessage(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_CASE_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_DEFENDANT_DETAILS),
                envelope.payloadAsJsonObject()));
    }


    @Handles(PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED)
    public void handleDefendantOffencesChanged(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_DEFENDANT_OFFENCES),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_COURT_APPLICATION_CHANGED)
    public void handleCourtApplicationChanged(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_OFFENCES_FOR_DEFENDANT_CHANGED, envelope.toObfuscatedDebugString());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_COURT_APPLICATION),
                envelope.payloadAsJsonObject()));

    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    @Handles(PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED)
    public void handleCaseMarkerUpdated(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_PROGRESSION_CASE_MARKERS_UPDATED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_MARKERS),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED)
    public void handleCourtApplicationToBeUpdated(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_COURT_APPLICATION_TO_BE_UPDATED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_COURT_APPLICATION_FOR_HEARINGS),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION)
    public void handleExtendListedHearingForCourtApplication(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_EXTEND_LISTED_HEARING_FOR_COURT_APPLICATION, envelope.toObfuscatedDebugString());
        }

        final JsonObject payload = envelope.payloadAsJsonObject();
        final HearingExtended hearingExtended = jsonObjectConverter.convert(payload, HearingExtended.class);

        if (isNotBoxWorkRequest(hearingExtended)) {
            if (isNotEmpty(hearingExtended.getProsecutionCases()) ) {
                final JsonObjectBuilder builder = createObjectBuilder();
                envelope.payloadAsJsonObject().entrySet().stream().filter(entry -> !entry.getKey().equals(COURT_APPLICATION)).forEach(entry -> builder.add(entry.getKey(), entry.getValue()) );

                sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_ADD_CASES_TO_HEARING),
                        builder.build()));
                logger.info(EVENT_PAYLOAD_DEBUG_STRING, LISTING_COMMAND_ADD_CASES_TO_HEARING, envelope.toObfuscatedDebugString());
            }
            if (nonNull(hearingExtended.getCourtApplication())) {
                sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING),
                        envelope.payloadAsJsonObject()));
                logger.info(EVENT_PAYLOAD_DEBUG_STRING, COMMAND_ADD_COURT_APPLICATION_FOR_LISTED_HEARING, envelope.toObfuscatedDebugString());
            }
        }
    }

    private boolean isNotBoxWorkRequest(final HearingExtended hearingExtended) {
        return isNull(hearingExtended.getIsBoxWorkRequest()) || !hearingExtended.getIsBoxWorkRequest();
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS)
    public void handleDefendantAddedForCourtProceedings(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_DEFENDANTS_ADDED_TO_COURT_PROCEEDINGS, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED)
    public void handleEventsCaseOrApplicationEjected(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_EVENTS_CASE_OR_APPLICATION_EJECTED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CASE_OR_APPLICATION_EJECTED),
                envelope.payloadAsJsonObject()));
    }

    @Handles(LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS)
    public void handleEventsCaseEjectedForAllHearings(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS, envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
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
            if (logger.isInfoEnabled()) {
                logger.info("The Payload for event " + LISTING_EVENTS_CASE_EJECTED_FOR_HEARINGS
                        + "has been ignored as it does not contains hearing ids  : {}", envelope.toObfuscatedDebugString());
            }
        }

    }

    @Handles(LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS)
    public void handleEventsApplicationEjectedForAllHearings(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS, envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
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
            if (logger.isInfoEnabled()) {
                logger.info("The Payload for event " + LISTING_EVENTS_APPLICATION_EJECTED_FOR_HEARINGS
                        + "has been ignored as it does not contains hearing ids : {}", envelope.toObfuscatedDebugString());
            }
        }

    }

    @Handles(PUBLIC_PROGRESSION_DEFENDANT_LEGALAID_STATUS_UPDATED)
    public void defendantLegalStatusUpdate(final JsonEnvelope envelop) {

        if (logger.isInfoEnabled()) {
            logger.info("progression.defendant-legalaid-status-updated event received {}", envelop.toObfuscatedDebugString());
        }

        final JsonObject jsonObject = envelop.payloadAsJsonObject();
        final JsonObject commandPayload = createObjectBuilder()
                .add(CASE_ID, jsonObject.getString(CASE_ID))
                .add(DEFENDANT_ID, jsonObject.getString(DEFENDANT_ID))
                .add(LEGAL_AID_STATUS, jsonObject.getString(LEGAL_AID_STATUS))
                .build();
        this.sender.send(envelopeFrom(metadataFrom(envelop.metadata()).withName(COMMAND_UPDATE_DEFENDANT_LEGALAID_STATUS),
                commandPayload));
    }

    @Handles(LISTING_EVENTS_DEFENDANT_LEGALAID_STATUS_UPDATED)
    public void handleDefendantLegalStatusUpdateForHearings(final JsonEnvelope envelop) {
        if (logger.isInfoEnabled()) {
            logger.info("listing.events.defendant-legalaid-status-updated event received {}", envelop.toObfuscatedDebugString());
        }
        final JsonObject eventPayload = envelop.payloadAsJsonObject();
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

    @Handles(PUBLIC_PROGRESSION_HEARING_RESULTED_CASE_UPDATED)
    public void handleHearingResultedAndCaseUpdated(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info("public.progression.hearing-resulted-case-updated event received {}", envelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_CASE_RESULTED_DEFENDANT_PROCEEDINGS_CONCLUDED),
                envelope.payloadAsJsonObject()));
    }

    @Handles(PUBLIC_EVENT_HEARING_TRIAL_VACATED)
    public void handleHearingTrialVacated(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info("public.hearing.trial-vacated received {}", envelope.toObfuscatedDebugString());
        }

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PRIVATE_COMMAND_HEARING_VACATE_TRIAL),
                envelope.payloadAsJsonObject()));
    }

    @Handles(LISTING_EVENTS_CASE_RESULTED_DEFENDANT_PROCEEDINGS_UPDATED)
    public void handleCaseResultedAndDefendantProceedingsUpdated(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info("listing.events.case-resulted-defendant-proceedings-updated event received {}", envelope.toObfuscatedDebugString());
        }
        final CaseResultedDefendantProceedingsConcluded caseResultedDefendantProceedingsConcluded = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CaseResultedDefendantProceedingsConcluded.class);
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

    @Handles(PUBLIC_EVENT_PROGRESSION_CASE_LINKED)
    public void handleCaseLinkedPublicEvent(final JsonEnvelope envelope) {
        logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_PROGRESSION_CASE_LINKED, envelope.toObfuscatedDebugString());
        sender.send(envelop(envelope.payloadAsJsonObject()).withName(COMMAND_UPDATE_LINKED_CASES).withMetadataFrom(envelope));
    }

    @Handles(PRIVATE_EVENT_LINKED_CASES_TO_BE_UPDATED)
    public void handleCaseLinkedPrivateEvent(final JsonEnvelope envelope) {
        logger.info("listing.events.cases-linked {}", envelope.toObfuscatedDebugString());
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
            sender.send(envelop(objectToJsonObjectConverter.convert(command)).withName(COMMAND_UPDATE_LINKED_CASE_IN_HEARING).withMetadataFrom(envelope));
        }
    }

    @Handles(PUBLIC_EVENT_HEARING_DAYS_CANCELLED)
    public void handleHearingDaysCancelledPublicEvent(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_HEARING_DAYS_CANCELLED, envelope.toObfuscatedDebugString());
        }
        sender.send(envelop(envelope.payloadAsJsonObject()).withName(COMMAND_CANCEL_HEARING_DAYS).withMetadataFrom(envelope));
    }

    @Handles(PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT)
    public void handleHearingMarkedAsDuplicate(final JsonEnvelope envelope) {
        logger.info(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_HEARING_MARKED_AS_DUPLICATE_EVENT, envelope.toObfuscatedDebugString());

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DUPLICATE),
                generatePayloadForCommandMarkHearingAsDuplicate(envelope)));

    }

    /**
     * Added specifically to complete the loop w.r.t progression service being able to generate
     * summons of the back of anew defendant getting successfully added to an existing hearing.
     * <p>
     * Defensively coded to only raise the event if court centre and hearing start time available
     *
     * @param envelope
     */
    @Handles(PRIVATE_EVENT_NEW_DEFENDANT_ADDED_FOR_COURT_PROCEEDINGS)
    public void handleNewDefendantAddedForCourtProceedings(final JsonEnvelope envelope) {
        logger.info("Processing event '{}'", PUBLIC_EVENT_NEW_DEFENDANT_ADDED_FOR_COURT_PROCEEDINGS);

        final NewDefendantAddedForCourtProceedings payload = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), NewDefendantAddedForCourtProceedings.class);
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
    @Handles(PRIVATE_EVENT_HEARING_UNALLOCATED_FOR_LISTING)
    public void handleHearingUnallocatedForListing(final JsonEnvelope envelope) {
        logger.debug("listing.events.hearing-unallocated-for-listing {}", envelope.toObfuscatedDebugString());
        final HearingUnallocatedForListing hearingUnallocatedForListing = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingUnallocatedForListing.class);
        if (nonNull(hearingUnallocatedForListing.getSeededHearing()) && hearingUnallocatedForListing.getSeededHearing()) {
            changeNextHearingDay(envelope, hearingUnallocatedForListing.getHearingId());
        }
    }

    @Handles(LISTING_EVENTS_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED)
    public void hearingDaysWithoutCourtCentreCorrected(final JsonEnvelope envelope) {
        final JsonObject publicEvent = Optional.of(envelope.payloadAsJsonObject())
                .map(payload -> createObjectBuilder()
                        .add("id", payload.get("id"))
                        .add("hearingDays", getHearingDays(payload.getJsonArray("hearingDays")))
                        .build()).orElse(createObjectBuilder().build());
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_LISTING_EVENTS_HEARING_DAYS_WITHOUT_COURT_CENTRE_CORRECTED), publicEvent));
    }

    @Handles(PUBLIC_HEARING_OFFENCES_REMOVED_FROM_EXISTING_HEARING)
    public void offencesRemovedFromExistingHearing(final JsonEnvelope envelope) {
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("listing.command.remove-selected-offences-from-existing-hearing"),
                envelope.payloadAsJsonObject()));
    }


    private JsonArray getHearingDays(JsonArray hearingDays) {
        final JsonArrayBuilder builder = createArrayBuilder();
        hearingDays.stream()
                .map(hearingDay -> (JsonObject) hearingDay)
                .map(hearingDay -> createObjectBuilder().add(COURT_CENTRE_ID_FIELD, hearingDay.getString(COURT_CENTRE_ID_FIELD, null))
                        .add("courtRoomId", hearingDay.getString("courtRoomId", null))
                        .add("listedDurationMinutes", hearingDay.getInt("durationMinutes"))
                        .add("listingSequence", hearingDay.getInt("sequence"))
                        .add("sittingDay", hearingDay.getString("startTime"))
                        .build())
                .forEach(builder::add);

        return builder.build();
    }

    @Handles(PRIVATE_EVENT_HEARING_MARKED_AS_DELETED)
    public void handleHearingMarkedAsDeleted(final JsonEnvelope envelope) {

        final HearingMarkedAsDeleted hearingMarkedAsDeleted = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingMarkedAsDeleted.class);
        final UUID hearingIdToDelete = hearingMarkedAsDeleted.getHearingIdToDelete();

        final JsonObject commandPayload = createObjectBuilder()
                .add(HEARING_ID_TO_MARK, hearingIdToDelete.toString())
                .build();

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DELETED),
                (commandPayload)));

    }

    @Handles(PRIVATE_EVENT_HEARING_MARKED_FOR_PARTIAL_UPDATE)
    public void handleHearingMarkedForPartialUpdate(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_MARKED_FOR_PARTIAL_UPDATE, envelope.toObfuscatedDebugString());
        }

        final JsonObject payload = envelope.payloadAsJsonObject();

        final HearingMarkedForPartialUpdate hearingMarkedForPartialUpdate = jsonObjectConverter.convert(payload, HearingMarkedForPartialUpdate.class);

        final JsonObject commandPayload = updateUnallocatedHearingPartiallyCommandConverter.convertPartialUpdateEventToCommand(hearingMarkedForPartialUpdate);

        this.sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_REMOVE_PARTIALLY_MERGED_OFFENCES),
                (commandPayload)));

    }

    @Handles(PRIVATE_LISTING_HEARING_PARTIALLY_UPDATED)
    public void handleHearingPartiallyUpdate(final JsonEnvelope jsonEnvelope) {

        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_LISTING_HEARING_PARTIALLY_UPDATED, jsonEnvelope.toObfuscatedDebugString());
        }

        final JsonObject payload = jsonEnvelope.payloadAsJsonObject();

        final HearingPartiallyUpdated hearingPartiallyUpdated = jsonObjectConverter.convert(payload, HearingPartiallyUpdated.class);

        final uk.gov.justice.listing.courts.HearingPartiallyUpdated pubEvent = publicHearingPartiallyUpdatedFactory.create(hearingPartiallyUpdated);

        //This public event is used by UI only as of now,
        final JsonEnvelope publicEvent = envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED),
                objectToJsonValueConverter.convert(pubEvent));
        if (logger.isInfoEnabled()) {
            logger.info(LOG_PUBLISHING, PUBLIC_LISTING_HEARING_PARTIALLY_UPDATED, publicEvent.metadata());
        }
        sender.send(publicEvent);

        //Unified public event is used by UI only as of now
        publishHearingChangesSavedPublicEvent(jsonEnvelope, hearingPartiallyUpdated.getHearingIdToBeUpdated());

    }

    @Handles(PRIVATE_EVENT_HEARING_CHANGES_SAVED)
    public void handleHearingChangesSaved(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_HEARING_CHANGES_SAVED, envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();

        final uk.gov.justice.listing.events.HearingChangesSaved hearingChangesSaved = jsonObjectConverter.convert(payload, uk.gov.justice.listing.events.HearingChangesSaved.class);

        publishHearingChangesSavedPublicEvent(envelope, hearingChangesSaved.getHearingId());
    }

    @Handles(PRIVATE_EVENTS_HEARING_ADDED_TO_CASE)
    public void handleHearingAddedToCase(final JsonEnvelope envelope){
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENTS_HEARING_ADDED_TO_CASE, envelope.toObfuscatedDebugString());
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withId(UUID.randomUUID()).withName(PUBLIC_LISTING_HEARING_ADDED_TO_CASE).build(),
                envelope.payloadAsJsonObject()));
    }

    private JsonObject generatePayloadForCommandMarkHearingAsDuplicate(final JsonEnvelope envelope) {
        final JsonObjectBuilder commandPayloadBuilder = createObjectBuilder();
        commandPayloadBuilder.add(HEARING_ID, envelope.payloadAsJsonObject().getString(HEARING_ID));
        if (envelope.payloadAsJsonObject().containsKey(PROSECUTION_CASE_IDS)) {
            commandPayloadBuilder.add(PROSECUTION_CASE_IDS, envelope.payloadAsJsonObject().getJsonArray(PROSECUTION_CASE_IDS));
        }

        return commandPayloadBuilder.build();
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
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_HEARING_TO_CASE, listHearingCommand);
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
            logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_CASE_MARKERS_TO_HEARING, command);
            sender.send(envelop(objectToJsonValueConverter.convert(command)).withName(COMMAND_UPDATE_CASE_MARKERS_TO_HEARING).withMetadataFrom(envelope));
        });
    }

    private void sendCommandAddApplicationToHearing(JsonEnvelope envelope) {
        final HearingListed event = getHearingListedEvent(envelope);
        final List<AddApplicationToHearingCommand> addApplicationToHearingCommands = addCourtApplicationToHearingCommandCollectionConverter.convert(event);
        addApplicationToHearingCommands.forEach(addApplicationToHearingCommand -> {
            logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_COURT_APPLICATION_TO_HEARING, addApplicationToHearingCommand);
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_COURT_APPLICATION_TO_HEARING),
                    objectToJsonValueConverter.convert(addApplicationToHearingCommand)));

        });
    }

    private void sendCommandUpdateHearingForListing(JsonEnvelope envelope) {
        final HearingListed event = getHearingListedEvent(envelope);
        final Optional<Boolean> isSlotsBooked = ofNullable(event.getHearing().getIsSlotsBooked());

        if (isSlotsBooked.isPresent() && isSlotsBooked.get()) {
            final UpdateHearingForListingEnriched updateHearingForListingEnriched = hearingListedToUpdateHearingForListingCommand.convert(event.getHearing());
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED),
                    objectToJsonValueConverter.convert(updateHearingForListingEnriched)));
        }
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
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_DEFENDANTS_FOR_HEARING, updateDefendantsForHearingCommand);
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
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS_FOR_HEARING, addDefendantsForCourtProceedingsCommand);
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
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_UPDATE_OFFENCES_FOR_HEARING, updateOffencesForHearingCommand);
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
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_DELETE_OFFENCES_FOR_HEARING, deleteOffencesForHearingCommand);
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
                    logger.debug(COMMAND_PAYLOAD_DEBUG_STRING, COMMAND_ADD_OFFENCES_FOR_HEARING, addOffencesForHearingCommand);

                    sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_ADD_OFFENCES_FOR_HEARING),
                            objectToJsonValueConverter.convert(addOffencesForHearingCommand)));
                }
        );
    }

    @SuppressWarnings("squid:S3655")
    private void sendChangeNextHearingDayIfHearingIsSeeded(final JsonEnvelope envelope, final List<ProsecutionCaseDefendantOffenceIdsV2> prosecutionCaseDefendantsOffenceIds, final UUID hearingId) {
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

    private void changeNextHearingDay(final JsonEnvelope envelope, final UUID hearingId) {
        final JsonObject changeNextHearingDay = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CHANGE_NEXT_HEARING_DAY),
                changeNextHearingDay));
    }


    /*
     * Publish a public event to notify that the hearing has been confirmed.
     */
    private void publishHearingConfirmedPublicEvent(final JsonEnvelope envelope, final HearingConfirmed hearingConfirmed) {
        logger.info(LOG_PUBLISHING, PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_CONFIRMED),
                objectToJsonValueConverter.convert(hearingConfirmed)));
    }

    private void publishHearingChangesSavedPublicEvent(final JsonEnvelope envelope, final UUID hearingId) {
        final HearingChangesSaved hearingChangesSaved = HearingChangesSaved.hearingChangesSaved().withHearingId(hearingId).build();


        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withId(UUID.randomUUID()).withName(PUBLIC_EVENT_HEARING_CHANGES_SAVED).build(),
                objectToJsonValueConverter.convert(hearingChangesSaved)));
        logger.info(LOG_PUBLISHING, PUBLIC_EVENT_HEARING_CHANGES_SAVED, hearingChangesSaved);
    }

    private HearingConfirmed getHearingConfirmed(final JsonEnvelope envelope) {
        final HearingAllocatedForListing hearingAllocatedForListing = getHearingAllocatedForListing(envelope);
        return getHearingConfirmed(hearingAllocatedForListing, envelope);
    }

    private HearingConfirmed getHearingConfirmedV2(final JsonEnvelope envelope) {
        final HearingAllocatedForListingV2 hearingAllocatedForListing = getHearingAllocatedForListingV2(envelope);
        return getHearingConfirmedV2(hearingAllocatedForListing, envelope);
    }

    private HearingAllocatedForListing getHearingAllocatedForListing(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingAllocatedForListing.class);
    }

    private HearingAllocatedForListingV2 getHearingAllocatedForListingV2(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingAllocatedForListingV2.class);
    }

    private HearingConfirmed getHearingConfirmed(final HearingAllocatedForListing hearingAllocatedForListing, final JsonEnvelope envelope) {
        return hearingConfirmedFactory.create(hearingAllocatedForListing, envelope);
    }

    private HearingConfirmed getHearingConfirmedV2(final HearingAllocatedForListingV2 hearingAllocatedForListing, final JsonEnvelope envelope) {
        return hearingConfirmedFactory.createV2(hearingAllocatedForListing, envelope);
    }

    /*
     * Publish a public event to notify that the hearing has been updated.
     */
    private void publishHearingUpdatedPublicEvent(final JsonEnvelope envelope, final HearingUpdated hearingUpdated) {

        logger.info(LOG_PUBLISHING, PUBLIC_EVENT_HEARING_UPDATED, hearingUpdated);
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_UPDATED),
                objectToJsonValueConverter.convert(hearingUpdated)));
    }

    private HearingUpdated getHearingUpdated(final JsonEnvelope envelope, final AllocatedHearingUpdatedForListing allocatedHearingUpdatedForListing) {
        return allocatedHearingUpdatedFactory.create(allocatedHearingUpdatedForListing, envelope);
    }

    private HearingUpdated getHearingUpdatedV2(final JsonEnvelope envelope, final AllocatedHearingUpdatedForListingV2 allocatedHearingUpdatedForListing) {
        return allocatedHearingUpdatedFactory.createV2(allocatedHearingUpdatedForListing, envelope);
    }


    private AllocatedHearingUpdatedForListing getAllocatedHearingUpdatedForListing(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AllocatedHearingUpdatedForListing.class);
    }

    private AllocatedHearingUpdatedForListingV2 getAllocatedHearingUpdatedForListingV2(final JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AllocatedHearingUpdatedForListingV2.class);
    }

    /*
     * Publish a public event to notify that the hearing has been updated with restrictions on court listing.
     */
    private void publishCourtListRestrictedPublicEvent(final JsonEnvelope envelope) {
        final CourtListRestricted courtListRestricted =
                jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtListRestricted.class);
        logger.info(LOG_PUBLISHING, PUBLIC_EVENT_RESTRICT_COURT_LIST, courtListRestricted);
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
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING, envelope.toObfuscatedDebugString());
        }

        publishHearingConfirmedPublicEventForExtendHearing(envelope);
    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING_V2)
    public void handleAllocatedHearingExtendedForListingV2Message(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_ALLOCATED_HEARING_EXTENDED_FOR_HEARING_V2, envelope.toObfuscatedDebugString());
        }

        publishHearingConfirmedPublicEventForExtendHearingAllocatedHearingExtendedV2(envelope);
    }

    private void publishHearingConfirmedPublicEventForExtendHearing(final JsonEnvelope envelope) {
        final AllocatedHearingExtendedForListing hearingExtendedForListing = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AllocatedHearingExtendedForListing.class);
        final HearingConfirmed hearingConfirmed = allocatedHearingExtendedFactory.create(hearingExtendedForListing, envelope);

        updateHearingToCase(envelope, hearingConfirmed);

        logger.info("Publishing '{}' public event for extend hearing with payload {}", PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        final JsonValue payload = objectToJsonValueConverter.convert(hearingConfirmed);
        sender.send(envelop(payload)
                .withName(PUBLIC_EVENT_HEARING_CONFIRMED)
                .withMetadataFrom(envelope));
    }

    private void publishHearingConfirmedPublicEventForExtendHearingAllocatedHearingExtendedV2(final JsonEnvelope envelope) {
        final AllocatedHearingExtendedForListingV2 allocatedHearingExtendedForListingV2 = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AllocatedHearingExtendedForListingV2.class);
        final HearingConfirmed hearingConfirmed = allocatedHearingExtendedFactory.create(allocatedHearingExtendedForListingV2, envelope);

        updateHearingToCase(envelope, hearingConfirmed);

        logger.info("Publishing '{}' public event for extend hearing with payload {}", PUBLIC_EVENT_HEARING_CONFIRMED, hearingConfirmed);
        final JsonValue payload = objectToJsonValueConverter.convert(hearingConfirmed);
        sender.send(envelop(payload)
                .withName(PUBLIC_EVENT_HEARING_CONFIRMED)
                .withMetadataFrom(envelope));
    }

    private void updateHearingToCase(JsonEnvelope envelope, HearingConfirmed hearingConfirmed) {

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

    private void sendUpdateCaseWithDuplicateHearing(final JsonEnvelope envelope, final UUID hearingId, final UUID caseId) {
        final JsonObject hearingMarkedAsDuplicateForCase = createObjectBuilder()
                .add(HEARING_ID, hearingId.toString())
                .add(CASE_ID, caseId.toString())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DUPLICATE_FOR_CASE),
                hearingMarkedAsDuplicateForCase));
    }

    private void updateSlotAndSendChangeJudiciaryForHearingsIfJudiciariesPresent(final JsonEnvelope envelope,
                                                                                 final HearingConfirmed hearingConfirmed,
                                                                                 final boolean isSlotUpdated,
                                                                                 final boolean isForAdjournmentHearing,
                                                                                 final List<HearingDay> hearingDays,
                                                                                 final JurisdictionType jurisdictionType,
                                                                                 final List<JudicialRole> judiciary) {
        final Optional<List<SlotDetail>> slotDetailsOptional = slotUpdater.updateSlot(envelope, hearingConfirmed.getConfirmedHearing(), isSlotUpdated, isForAdjournmentHearing, hearingDays);

        if (JurisdictionType.MAGISTRATES.equals(jurisdictionType) && isEmpty(judiciary)) {
            slotDetailsOptional.ifPresent(slotDetails ->
                    slotDetails.forEach(slotDetail -> {
                        final List<uk.gov.moj.cpp.listing.domain.JudicialRole> judiciariesFromRota = courtSchedulerServiceAdapter.getJudicialRoles(slotDetail.getSessionDate(),
                                slotDetail.getOuCode(),
                                Optional.of(slotDetail.getSession()),
                                hearingConfirmed.getConfirmedHearing().getCourtCentre().getRoomId().toString());


                        if (isNotEmpty(judiciariesFromRota)) {
                            sendChangeJudiciaryForHearings(envelope,
                                    hearingConfirmed.getConfirmedHearing().getId(),
                                    JudicialRoleDomainToEventConverter.convertToEvents(judiciariesFromRota));
                        }
                    })
            );
        }
    }

    private void sendChangeJudiciaryForHearings(final JsonEnvelope envelope, final UUID hearingId, final List<JudicialRole> judicialRoles) {
        final JsonArrayBuilder jsonArrayBuilder = createArrayBuilder();
        judicialRoles.forEach(judicialRole -> jsonArrayBuilder.add(objectToJsonObjectConverter.convert(judicialRole)));

        final JsonObject changeJudiciaryForHearingsJsonObject = createObjectBuilder()
                .add("hearings", createArrayBuilder().add(hearingId.toString()).build())
                .add("judiciary", jsonArrayBuilder)
                .build();

        logger.info(LOG_PUBLISHING, COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS, changeJudiciaryForHearingsJsonObject);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_CHANGE_JUDICIARY_FOR_HEARINGS),
                changeJudiciaryForHearingsJsonObject));
    }


    @Handles(PRIVATE_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING)
    public void handleListingCourtRoomNotSelectedEvent(JsonEnvelope jsonEnvelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING, jsonEnvelope.toObfuscatedDebugString());
        }
        final HearingDaysChangedForHearing hearingDaysChangedForHearing = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingDaysChangedForHearing.class);
        convertHearingDayEventToCourtEvent(jsonEnvelope, hearingDaysChangedForHearing);
        publishHearingChangesSavedPublicEvent(jsonEnvelope, hearingDaysChangedForHearing.getHearingId());
    }

    private void convertHearingDayEventToCourtEvent(final JsonEnvelope jsonEnvelope, final HearingDaysChangedForHearing hearingDaysChangedForHearing) {

        final List<uk.gov.justice.core.courts.HearingDay> courtHearingDays = new ArrayList<>();
        for (final HearingDay hearingDayEvent : hearingDaysChangedForHearing.getHearingDays()) {
            final uk.gov.justice.core.courts.HearingDay courtHearingDay = allocatedHearingExtendedFactory.buildHearingDay(hearingDayEvent);
            courtHearingDays.add(courtHearingDay);
        }

        final uk.gov.justice.listing.courts.HearingDaysChangedForHearing hearingDaysForHearingChanged = uk.gov.justice.listing.courts.HearingDaysChangedForHearing.hearingDaysChangedForHearing().withHearingDays(courtHearingDays).withHearingId(hearingDaysChangedForHearing.getHearingId()).build();

        //This public event is used by UI only as of now,
        final JsonEnvelope publicEvent = envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING),
                objectToJsonValueConverter.convert(hearingDaysForHearingChanged));
        if (logger.isInfoEnabled()) {
            logger.info(LOG_PUBLISHING, PUBLIC_LISTING_HEARING_DAYS_CHANGED_FOR_HEARING, publicEvent.metadata());
        }

        sender.send(publicEvent);
    }

    private void publishPublicHearingListedEvent(final JsonEnvelope jsonEnvelope) {

        final HearingListed listingHearingListed = jsonObjectConverter.convert(jsonEnvelope.payloadAsJsonObject(), HearingListed.class);

        final Hearing courtDomainHearing = hearingListingToCoreConverter.convert(listingHearingListed.getHearing());
        final uk.gov.justice.listing.courts.HearingListed courtHearingListed = uk.gov.justice.listing.courts.HearingListed.
                hearingListed().
                withHearing(courtDomainHearing).
                build();

        final JsonEnvelope publicEvent = envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withName(PUBLIC_LISTING_HEARING_LISTED),
                objectToJsonObjectConverter.convert(courtHearingListed));

        if (logger.isInfoEnabled()) {
            logger.info(LOG_PUBLISHING, PUBLIC_LISTING_HEARING_LISTED, publicEvent.metadata());
        }

        sender.send(publicEvent);
        sender.send(envelopeFrom(metadataFrom(jsonEnvelope.metadata()).withId(UUID.randomUUID()).withName("public.listing.court-application-added-for-hearing"), jsonEnvelope.payloadAsJsonObject()));
    }

}
