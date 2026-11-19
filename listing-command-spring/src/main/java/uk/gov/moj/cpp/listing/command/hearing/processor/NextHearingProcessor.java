package uk.gov.moj.cpp.listing.command.hearing.processor;

import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorPort;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import org.springframework.stereotype.Component;
import static java.util.Objects.nonNull;
import static jakarta.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.command.hearing.flow.Envelopes.metadataFrom;
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
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ListToJsonArrayConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectToJsonValueConverter;
import uk.gov.moj.cpp.listing.command.hearing.flow.ProcessorSender;
import uk.gov.moj.cpp.listing.messaging.envelope.Envelope;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Port of the JEE {@code NextHearingProcessor} (MBD refactor P2.3.2, ADR 015), logic unchanged: the
 * public messages of the next-hearing/deletion events and the follow-up commands (sent through
 * {@link ProcessorSender}, to the Hearing and Case command services).
 */
@Component
public class NextHearingProcessor implements ProcessorPort {

    @Override
    public void register(final EventHandlers processors) {
        processors
                .onJson(uk.gov.justice.listing.events.NextHearingRequested.class, this::handleNextHearingRequested)
                .onJson(uk.gov.justice.listing.events.UnscheduledNextHearingRequested.class, this::handleUnscheduledNextHearingRequested)
                .onJson(uk.gov.justice.listing.events.DeleteNextHearingRequested.class, this::handleDeleteNextHearingRequested)
                .onJson(uk.gov.justice.listing.events.AllocatedHearingDeleted.class, this::handleAllocatedHearingDeleted)
                .onJson(uk.gov.justice.listing.events.UnallocatedHearingDeleted.class, this::handleUnallocatedHearingDeleted)
                .onJson(uk.gov.justice.listing.events.HearingDeleted.class, this::handleHearingDeleted)
                .onJson(uk.gov.justice.listing.events.OffencesRemovedFromHearing.class, this::handleOffencesRemovedFromHearing)
                .onJson(uk.gov.justice.listing.events.NextHearingDayChanged.class, this::handleSeedHearingEarliestNextHearingDayUpdated)
                .onJson(uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested.class, this::handleRemoveOffencesFromExistingHearingRequestedEvent)
                .onJson(uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing.class, this::handleOffencesRemovedFromExistingAllocatedHearingEvent)
                .onJson(uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing.class, this::handleOffencesRemovedFromExistingUnallocatedHearingEvent)
                .onJson(uk.gov.justice.listing.events.NextHearingReplaced.class, this::handleNextHearingReplaced);
    }


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

    @Autowired
    private ProcessorSender sender;

    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectToObjectConverter();

    private ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter();

    private ListToJsonArrayConverter listToJsonArrayConverter = new ListToJsonArrayConverter();



    @SuppressWarnings("squid:S3655")
    public void handleNextHearingRequested(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_NEXT_HEARING_LISTING_REQUESTED);

        final NextHearingRequested event = jsonObjectConverter.convert(envelope.payload(), NextHearingRequested.class);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_LIST_NEXT_HEARING),
                buildListNextHearingCommand(event)));
    }

    public void handleUnscheduledNextHearingRequested(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_UNSCHEDULED_NEXT_HEARING_LISTING_REQUESTED);

        final UnscheduledNextHearingRequested event = jsonObjectConverter.convert(envelope.payload(), UnscheduledNextHearingRequested.class);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_LIST_UNSCHEDULED_NEXT_HEARING),
                buildListUnscheduledNextHearingCommand(event)));
    }

    public void handleDeleteNextHearingRequested(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_DELETE_NEXT_HEARING_REQUESTED);

        final DeleteNextHearingRequested event = jsonObjectConverter.convert(envelope.payload(), DeleteNextHearingRequested.class);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_DELETE_SEEDED_HEARING),
                objectToJsonValueConverter.convert(DeleteHearingCommand.deleteHearingCommand()
                        .withHearingId(event.getHearingId())
                        .withSeedingHearingId(event.getSeedingHearingId())
                        .build())));
    }

    public void handleAllocatedHearingDeleted(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_ALLOCATED_HEARING_DELETED);

        final AllocatedHearingDeleted event = jsonObjectConverter.convert(envelope.payload(), AllocatedHearingDeleted.class);
        final UUID hearingId = event.getHearingId();

        publishPublicAllocatedHearingDeleted(envelope, hearingId);

        if (isNotEmpty(event.getCaseIds())) {
            event.getCaseIds().forEach(caseId ->
                    sendUpdateCaseWithDuplicateHearing(envelope, hearingId, caseId));
        }
    }

    public void handleUnallocatedHearingDeleted(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_UNALLOCATED_HEARING_DELETED);

        final UnallocatedHearingDeleted event = jsonObjectConverter.convert(envelope.payload(), UnallocatedHearingDeleted.class);
        final UUID hearingId = event.getHearingId();

        publishPublicUnallocatedHearingDeleted(envelope, hearingId);

        if (isNotEmpty(event.getCaseIds())) {
            event.getCaseIds().forEach(caseId ->
                    sendUpdateCaseWithDuplicateHearing(envelope, hearingId, caseId));
        }
    }

    public void handleHearingDeleted(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_HEARING_DELETED);

        final HearingDeleted event = jsonObjectConverter.convert(envelope.payload(), HearingDeleted.class);
        final UUID hearingId = event.getHearingIdToBeDeleted();

        publishPublicHearingDeleted(envelope, hearingId);
    }

    @SuppressWarnings({"squid:S3655"})
    public void handleOffencesRemovedFromHearing(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_OFFENCES_REMOVED_FROM_HEARING);

        final OffencesRemovedFromHearing event = jsonObjectConverter.convert(envelope.payload(), OffencesRemovedFromHearing.class);
        final UUID hearingId = event.getHearingId();
        final List<UUID> seededOffences = event.getSeededOffences();

        if (nonNull(event.getUnallocated()) && event.getUnallocated()) {
            publishPublicOffencesRemovedFromUnallocatedHearing(envelope, hearingId, seededOffences);
        } else {
            final JsonObjectBuilder payloadBuilder = createObjectBuilder();
            payloadBuilder.add("hearingId", hearingId.toString() );
            payloadBuilder.add("offenceIds", envelope.payload().getJsonArray("seededOffences"));
            // This public event uses for multiple purpose, we need to know it is raised by amend-reshare flow.
            payloadBuilder.add("isResultFlow", true);
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING), payloadBuilder.build()));
        }

        if (isNotEmpty(event.getCaseIdsSeededByOnlySeedingHearingId())) {
            event.getCaseIdsSeededByOnlySeedingHearingId().forEach(caseId ->
                    sendUpdateCaseWithDuplicateHearing(envelope, hearingId, caseId));
        }
    }

    public void handleSeedHearingEarliestNextHearingDayUpdated(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_NEXT_HEARING_DAY_CHANGED);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_NEXT_HEARING_DAY_CHANGED),
                envelope.payload()));

    }

    public void handleRemoveOffencesFromExistingHearingRequestedEvent(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_REMOVE_OFFENCES_FROM_EXISTING_HEARING_REQUESTED);

        final RemoveOffencesFromExistingHearingRequested event = jsonObjectConverter.convert(envelope.payload(), RemoveOffencesFromExistingHearingRequested.class);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_REMOVE_OFFENCES_FROM_EXISTING_HEARING),
                objectToJsonValueConverter.convert(DeleteOffenceFromExistingHearing.deleteOffenceFromExistingHearing()
                        .withHearingId(event.getHearingId())
                        .withSeedingHearingId(event.getSeedingHearingId())
                        .build())));
    }

    public void handleOffencesRemovedFromExistingAllocatedHearingEvent(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING);
        final String sourceContext = envelope.payload().getString("sourceContext", "LISTING");
        final JsonObjectBuilder payloadBuilder = createObjectBuilder();
        envelope.payload().keySet().stream().filter(s -> !"sourceContext".equals(s))
                .forEach(s -> payloadBuilder.add(s, envelope.payload().get(s)));

        if ("Listing".equals(sourceContext) ) {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_OFFENCES_REMOVED_FROM_EXISTING_ALLOCATED_HEARING), payloadBuilder.build()));
        } else {
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENTS_LISTING_OFFENCES_REMOVED_FROM_ALLOCATED_HEARING), payloadBuilder.build()));
        }
    }

    public void handleOffencesRemovedFromExistingUnallocatedHearingEvent(final Envelope envelope) {
        logEventReceived(envelope, PRIVATE_EVENT_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_OFFENCES_REMOVED_FROM_EXISTING_UNALLOCATED_HEARING), envelope.payload()));
    }

    public void handleNextHearingReplaced(final Envelope envelope){
        logEventReceived(envelope,LISTING_EVENTS_NEXT_HEARING_REPLACED);

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.offences-moved-to-next-hearing"), envelope.payload()));
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

        if (nonNull(event.getPtphDetail())) {
            addJsonValueToJsonObjectNullSafe(jsonObjectBuilder, "ptphDetail", objectToJsonValueConverter.convert(event.getPtphDetail()));
        }

        return jsonObjectBuilder.build();
    }

    private void sendUpdateCaseWithDuplicateHearing(final Envelope envelope, final UUID hearingId, final UUID caseId) {
        final JsonObject hearingMarkedAsDuplicateForCase = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("caseId", caseId.toString())
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(COMMAND_MARK_HEARING_AS_DUPLICATE_FOR_CASE),
                hearingMarkedAsDuplicateForCase));
    }

    private void publishPublicAllocatedHearingDeleted(final Envelope envelope, final UUID hearingId) {

        final uk.gov.justice.listing.courts.AllocatedHearingDeleted allocatedHearingDeleted = uk.gov.justice.listing.courts.AllocatedHearingDeleted.allocatedHearingDeleted()
                .withHearingId(hearingId)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_ALLOCATED_HEARING_DELETED, allocatedHearingDeleted);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_ALLOCATED_HEARING_DELETED),
                objectToJsonValueConverter.convert(allocatedHearingDeleted)));
    }

    private void publishPublicUnallocatedHearingDeleted(final Envelope envelope, final UUID hearingId) {

        final uk.gov.justice.listing.courts.UnallocatedHearingDeleted unallocatedHearingDeleted = uk.gov.justice.listing.courts.UnallocatedHearingDeleted.unallocatedHearingDeleted()
                .withHearingId(hearingId)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_UNALLOCATED_HEARING_DELETED, unallocatedHearingDeleted);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_UNALLOCATED_HEARING_DELETED),
                objectToJsonValueConverter.convert(unallocatedHearingDeleted)));
    }


    private void publishPublicHearingUnallocated(final Envelope envelope, final UUID hearingId, final List<UUID> seededOffences) {
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

    private void publishPublicHearingDeleted(final Envelope envelope, final UUID hearingId) {

        final uk.gov.justice.listing.courts.HearingDeleted hearingDeleted = uk.gov.justice.listing.courts.HearingDeleted.hearingDeleted()
                .withHearingId(hearingId)
                .build();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, PUBLIC_EVENT_HEARING_DELETED, hearingDeleted);
        }

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_EVENT_HEARING_DELETED),
                objectToJsonValueConverter.convert(hearingDeleted)));
    }

    private void publishPublicOffencesRemovedFromUnallocatedHearing(final Envelope envelope, final UUID hearingId, final List<UUID> seededOffences) {
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

    private void logEventReceived(final Envelope envelope, final String privateEventUnscheduledNextHearingListingRequested) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(EVENT_PAYLOAD_DEBUG_STRING, privateEventUnscheduledNextHearingListingRequested, envelope.metadata());
        }
    }
}
