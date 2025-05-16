package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Objects.nonNull;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.listing.domain.utils.JsonUtils.addJsonValueToJsonObjectNullSafe;
import static uk.gov.moj.cpp.listing.domain.utils.JsonUtils.addStringToJsonObjectNullSafe;

import uk.gov.justice.listing.courts.DeleteHearingCommand;
import uk.gov.justice.listing.courts.DeleteOffenceFromExistingHearing;
import uk.gov.justice.listing.courts.HearingUnallocated;
import uk.gov.justice.listing.courts.OffencesRemovedFromUnallocatedHearing;
import uk.gov.justice.listing.events.AllocatedHearingDeleted;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.NextHearingRequested;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested;
import uk.gov.justice.listing.events.UnallocatedHearingDeleted;
import uk.gov.justice.listing.events.UnscheduledNextHearingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class NextHearingProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NextHearingProcessor.class);
    private static final String PRIVATE_EVENT_NEXT_HEARING_LISTING_REQUESTED = "listing.events.next-hearing-requested";
    private static final String PRIVATE_EVENT_UNSCHEDULED_NEXT_HEARING_LISTING_REQUESTED = "listing.events.unscheduled-next-hearing-requested";
    private static final String PRIVATE_EVENT_DELETE_NEXT_HEARING_REQUESTED = "listing.events.delete-next-hearing-requested";
    private static final String PRIVATE_EVENT_REMOVE_OFFENCES_FROM_EXISTING_HEARING_REQUESTED = "listing.events.remove-offences-from-existing-hearing-requested";
    private static final String PRIVATE_EVENT_ALLOCATED_HEARING_DELETED = "listing.events.allocated-hearing-deleted";
    private static final String PRIVATE_EVENT_UNALLOCATED_HEARING_DELETED = "listing.events.unallocated-hearing-deleted";
    private static final String PRIVATE_EVENT_HEARING_DELETED = "listing.events.hearing-deleted";
    private static final String PRIVATE_EVENT_OFFENCES_REMOVED_FROM_HEARING = "listing.events.offences-removed-from-hearing";
    private static final String PRIVATE_EVENT_NEXT_HEARING_DAY_CHANGED = "listing.events.next-hearing-day-changed";

    private static final String PUBLIC_EVENT_UNALLOCATED_HEARING_DELETED = "public.events.listing.unallocated-hearing-deleted";
    private static final String PUBLIC_EVENT_ALLOCATED_HEARING_DELETED = "public.events.listing.allocated-hearing-deleted";

    private static final String PUBLIC_EVENT_HEARING_DELETED = "public.events.listing.hearing-deleted";

    private static final String PUBLIC_EVENT_HEARING_UNALLOCATED = "public.events.listing.hearing-unallocated";
    private static final String PUBLIC_EVENT_OFFENCES_REMOVED_FROM_UNALLOCATED_HEARING = "public.events.listing.offences-removed-from-unallocated-hearing";
    private static final String PUBLIC_EVENT_NEXT_HEARING_DAY_CHANGED = "public.events.listing.next-hearing-day-changed";

    private static final String EVENT_PAYLOAD_DEBUG_STRING = "Received '{}' event with payload {}";
    private static final String COMMAND_LIST_NEXT_HEARING = "listing.command.list-next-hearing";
    private static final String COMMAND_DELETE_SEEDED_HEARING = "listing.command.delete-seeded-hearing";
    private static final String COMMAND_REMOVE_OFFENCES_FROM_EXISTING_HEARING = "listing.command.remove-offences-from-existing-hearing";
    private static final String COMMAND_LIST_UNSCHEDULED_NEXT_HEARING = "listing.command.list-unscheduled-next-hearing";
    private static final String COMMAND_MARK_HEARING_AS_DUPLICATE_FOR_CASE = "listing.command.mark-hearing-as-duplicate-for-case";

    private static final String PRIVATE_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING = "listing.events.offences-removed-from-existing-allocated-hearing";
    private static final String PRIVATE_EVENT_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING = "listing.events.offences-removed-from-existing-unallocated-hearing";
    private static final String PUBLIC_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING = "public.events.listing.offences-removed-from-existing-allocated-hearing";
    private static final String PUBLIC_EVENT_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING = "public.events.listing.offences-removed-from-existing-unallocated-hearing";
    public static final String PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING = "public.events.listing.offences-removed-from-allocated-hearing";
    public static final String LISTING_EVENTS_NEXT_HEARING_REPLACED = "listing.events.next-hearing-replaced";

    @Inject
    private Sender sender;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;



    @SuppressWarnings("squid:S3655")
    @Handles(PRIVATE_EVENT_NEXT_HEARING_LISTING_REQUESTED)
    public void handleNextHearingRequested(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_NEXT_HEARING_LISTING_REQUESTED);

        final NextHearingRequested event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), NextHearingRequested.class);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_LIST_NEXT_HEARING),
                buildListNextHearingCommand(event)));
    }

    @Handles(PRIVATE_EVENT_UNSCHEDULED_NEXT_HEARING_LISTING_REQUESTED)
    public void handleUnscheduledNextHearingRequested(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_UNSCHEDULED_NEXT_HEARING_LISTING_REQUESTED);

        final UnscheduledNextHearingRequested event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UnscheduledNextHearingRequested.class);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_LIST_UNSCHEDULED_NEXT_HEARING),
                buildListUnscheduledNextHearingCommand(event)));
    }

    @Handles(PRIVATE_EVENT_DELETE_NEXT_HEARING_REQUESTED)
    public void handleDeleteNextHearingRequested(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_DELETE_NEXT_HEARING_REQUESTED);

        final DeleteNextHearingRequested event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), DeleteNextHearingRequested.class);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_DELETE_SEEDED_HEARING),
                objectToJsonValueConverter.convert(DeleteHearingCommand.deleteHearingCommand()
                        .withHearingId(event.getHearingId())
                        .withSeedingHearingId(event.getSeedingHearingId())
                        .build())));
    }

    @Handles(PRIVATE_EVENT_ALLOCATED_HEARING_DELETED)
    public void handleAllocatedHearingDeleted(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_ALLOCATED_HEARING_DELETED);

        final AllocatedHearingDeleted event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), AllocatedHearingDeleted.class);
        final UUID hearingId = event.getHearingId();

        publishPublicAllocatedHearingDeleted(envelope, hearingId);

        if (isNotEmpty(event.getCaseIds())) {
            event.getCaseIds().forEach(caseId ->
                    sendUpdateCaseWithDuplicateHearing(envelope, hearingId, caseId));
        }
    }

    @Handles(PRIVATE_EVENT_UNALLOCATED_HEARING_DELETED)
    public void handleUnallocatedHearingDeleted(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_UNALLOCATED_HEARING_DELETED);

        final UnallocatedHearingDeleted event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UnallocatedHearingDeleted.class);
        final UUID hearingId = event.getHearingId();

        publishPublicUnallocatedHearingDeleted(envelope, hearingId);

        if (isNotEmpty(event.getCaseIds())) {
            event.getCaseIds().forEach(caseId ->
                    sendUpdateCaseWithDuplicateHearing(envelope, hearingId, caseId));
        }
    }

    @Handles(PRIVATE_EVENT_HEARING_DELETED)
    public void handleHearingDeleted(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_HEARING_DELETED);

        final HearingDeleted event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), HearingDeleted.class);
        final UUID hearingId = event.getHearingIdToBeDeleted();

        publishPublicHearingDeleted(envelope, hearingId);
    }

    @SuppressWarnings({"squid:S3655"})
    @Handles(PRIVATE_EVENT_OFFENCES_REMOVED_FROM_HEARING)
    public void handleOffencesRemovedFromHearing(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_OFFENCES_REMOVED_FROM_HEARING);

        final OffencesRemovedFromHearing event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), OffencesRemovedFromHearing.class);
        final UUID hearingId = event.getHearingId();
        final List<UUID> seededOffences = event.getSeededOffences();

        if (nonNull(event.getUnallocated()) && event.getUnallocated()) {
            publishPublicOffencesRemovedFromUnallocatedHearing(envelope, hearingId, seededOffences);
        } else {
            final JsonObjectBuilder payloadBuilder = createObjectBuilder();
            payloadBuilder.add("hearingId", hearingId.toString() );
            payloadBuilder.add("offenceIds", envelope.payloadAsJsonObject().getJsonArray("seededOffences"));
            // This public event uses for multiple purpose, we need to know it is raised by amend-reshare flow.
            payloadBuilder.add("isResultFlow", true);
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING), payloadBuilder.build()));
        }

        if (isNotEmpty(event.getCaseIdsSeededByOnlySeedingHearingId())) {
            event.getCaseIdsSeededByOnlySeedingHearingId().forEach(caseId ->
                    sendUpdateCaseWithDuplicateHearing(envelope, hearingId, caseId));
        }
    }

    @Handles(PRIVATE_EVENT_NEXT_HEARING_DAY_CHANGED)
    public void handleSeedHearingEarliestNextHearingDayUpdated(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_NEXT_HEARING_DAY_CHANGED);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_NEXT_HEARING_DAY_CHANGED),
                envelope.payloadAsJsonObject()));

    }

    @Handles(PRIVATE_EVENT_REMOVE_OFFENCES_FROM_EXISTING_HEARING_REQUESTED)
    public void handleRemoveOffencesFromExistingHearingRequestedEvent(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_REMOVE_OFFENCES_FROM_EXISTING_HEARING_REQUESTED);

        final RemoveOffencesFromExistingHearingRequested event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), RemoveOffencesFromExistingHearingRequested.class);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_REMOVE_OFFENCES_FROM_EXISTING_HEARING),
                objectToJsonValueConverter.convert(DeleteOffenceFromExistingHearing.deleteOffenceFromExistingHearing()
                        .withHearingId(event.getHearingId())
                        .withSeedingHearingId(event.getSeedingHearingId())
                        .build())));
    }

    @Handles(PRIVATE_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING)
    public void handleOffencesRemovedFromExistingAllocatedHearingEvent(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING);
        final String sourceContext = envelope.payloadAsJsonObject().getString("sourceContext", "LISTING");
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        envelope.payloadAsJsonObject().keySet().stream().filter(s -> !"sourceContext".equals(s))
                .forEach(s -> payloadBuilder.add(s, envelope.payloadAsJsonObject().get(s)));

        if ("Listing".equals(sourceContext) ) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING), payloadBuilder.build()));
        } else {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING), payloadBuilder.build()));
        }
    }

    @Handles(PRIVATE_EVENT_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING)
    public void handleOffencesRemovedFromExistingUnallocatedHearingEvent(final JsonEnvelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING), envelope.payloadAsJsonObject()));
    }

    @Handles(LISTING_EVENTS_NEXT_HEARING_REPLACED)
    public void handleNextHearingReplaced(final JsonEnvelope envelope){
        logEventReceived(envelope,LISTING_EVENTS_NEXT_HEARING_REPLACED);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.offences-moved-to-next-hearing"), envelope.payloadAsJsonObject()));
    }


    private JsonObject buildListNextHearingCommand(final NextHearingRequested event) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        addStringToJsonObjectNullSafe(jsonObjectBuilder, "adjournedFromDate", Optional.ofNullable(event.getAdjournedFromDate()));
        if (CollectionUtils.isNotEmpty(event.getCourtCentreDetails())) {
            addJsonValueToJsonObjectNullSafe(jsonObjectBuilder, "courtCentresDetails", listToJsonArrayConverter.convert(event.getCourtCentreDetails()));
        }
        if (CollectionUtils.isNotEmpty(event.getShadowListedOffences())) {
            addJsonValueToJsonObjectNullSafe(jsonObjectBuilder, "shadowListedOffences", objectToJsonValueConverter.convert(event.getShadowListedOffences()));
        }
        addJsonValueToJsonObjectNullSafe(jsonObjectBuilder, "hearing", objectToJsonValueConverter.convert(event.getHearing()));


        return jsonObjectBuilder.build();
    }

    private JsonObject buildListUnscheduledNextHearingCommand(final UnscheduledNextHearingRequested event) {
        final JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();

        if (CollectionUtils.isNotEmpty(event.getCourtCentreDetails())) {
            addJsonValueToJsonObjectNullSafe(jsonObjectBuilder, "courtCentresDetails", listToJsonArrayConverter.convert(event.getCourtCentreDetails()));
        }
        addJsonValueToJsonObjectNullSafe(jsonObjectBuilder, "hearing", objectToJsonValueConverter.convert(event.getHearing()));

        return jsonObjectBuilder.build();
    }

    private void sendUpdateCaseWithDuplicateHearing(final JsonEnvelope envelope, final UUID hearingId, final UUID caseId) {
        final JsonObject hearingMarkedAsDuplicateForCase = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("caseId", caseId.toString())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DUPLICATE_FOR_CASE),
                hearingMarkedAsDuplicateForCase));
    }

    private void publishPublicAllocatedHearingDeleted(final JsonEnvelope envelope, final UUID hearingId) {

        final uk.gov.justice.listing.courts.AllocatedHearingDeleted allocatedHearingDeleted = uk.gov.justice.listing.courts.AllocatedHearingDeleted.allocatedHearingDeleted()
                .withHearingId(hearingId)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_ALLOCATED_HEARING_DELETED, allocatedHearingDeleted);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_ALLOCATED_HEARING_DELETED),
                objectToJsonValueConverter.convert(allocatedHearingDeleted)));
    }

    private void publishPublicUnallocatedHearingDeleted(final JsonEnvelope envelope, final UUID hearingId) {

        final uk.gov.justice.listing.courts.UnallocatedHearingDeleted unallocatedHearingDeleted = uk.gov.justice.listing.courts.UnallocatedHearingDeleted.unallocatedHearingDeleted()
                .withHearingId(hearingId)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_UNALLOCATED_HEARING_DELETED, unallocatedHearingDeleted);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_UNALLOCATED_HEARING_DELETED),
                objectToJsonValueConverter.convert(unallocatedHearingDeleted)));
    }


    private void publishPublicHearingUnallocated(final JsonEnvelope envelope, final UUID hearingId, final List<UUID> seededOffences) {
        final HearingUnallocated hearingUnallocated = HearingUnallocated.hearingUnallocated()
                .withHearingId(hearingId)
                .withOffenceIds(seededOffences)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_HEARING_UNALLOCATED, hearingUnallocated);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_UNALLOCATED),
                objectToJsonValueConverter.convert(hearingUnallocated)));
    }

    private void publishPublicHearingDeleted(final JsonEnvelope envelope, final UUID hearingId) {

        final uk.gov.justice.listing.courts.HearingDeleted hearingDeleted = uk.gov.justice.listing.courts.HearingDeleted.hearingDeleted()
                .withHearingId(hearingId)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_HEARING_DELETED, hearingDeleted);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_DELETED),
                objectToJsonValueConverter.convert(hearingDeleted)));
    }

    private void publishPublicOffencesRemovedFromUnallocatedHearing(final JsonEnvelope envelope, final UUID hearingId, final List<UUID> seededOffences) {
        final OffencesRemovedFromUnallocatedHearing offencesRemovedFromUnallocatedHearing = OffencesRemovedFromUnallocatedHearing.offencesRemovedFromUnallocatedHearing()
                .withHearingId(hearingId)
                .withOffenceIds(seededOffences)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_OFFENCES_REMOVED_FROM_UNALLOCATED_HEARING, offencesRemovedFromUnallocatedHearing);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_OFFENCES_REMOVED_FROM_UNALLOCATED_HEARING),
                objectToJsonValueConverter.convert(offencesRemovedFromUnallocatedHearing)));
    }

    private void logEventReceived(final JsonEnvelope envelope, final String privateEventUnscheduledNextHearingListingRequested) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, privateEventUnscheduledNextHearingListingRequested, envelope.toObfuscatedDebugString());
        }
    }
}
