package uk.gov.moj.cpp.listing.event.processor;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.listing.domain.utils.HmiConstants.SOURCE_HMI;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.LOG_PUBLISHING;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.SelectedCourtCentre;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.listing.events.DeletedHearingInStagingHmi;
import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.listing.events.JudicialRole;
import uk.gov.justice.listing.events.JudicialRoleType;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.listing.events.RequestedHearingFromStagingHmi;
import uk.gov.justice.listing.events.UpdatedHearingInStagingHmi;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.staginghmi.courts.HearingUpdatedFromHmi;
import uk.gov.justice.staginghmi.courts.NonDefaultDays;
import uk.gov.moj.cpp.listing.event.processor.courtcenter.CourtCentreFactory;
import uk.gov.moj.cpp.listing.event.processor.service.HearingService;
import uk.gov.moj.cpp.listing.event.processor.util.HearingListedToUpdateHearingForListingCommand;
import uk.gov.moj.cpp.listing.event.processor.util.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.staginghmi.common.StagingHmiService;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class StagingHmiEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingHmiEventProcessor.class);

    private static final String PUBLIC_REQUEST_HEARING_FROM_STAGING_HMI = "public.listing.requested-hearing-from-staging-hmi";

    private static final String PUBLIC_UPDATE_HEARING_IN_STAGING_HMI = "public.listing.updated-hearing-in-staging-hmi";

    private static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED = "listing.command.update-hearing-for-listing-enriched";

    @Inject
    private StagingHmiService stagingHmiService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private HearingConfirmedFactory hearingConfirmedFactory;

    @Inject
    private Sender sender;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private CourtCentreFactory courtCentreFactory;

    @Inject
    private HearingObjectsListingToCoreConverter hearingObjectsListingToCoreConverter;

    @Inject
    private HearingService hearingService;

    @Inject
    private HearingListedToUpdateHearingForListingCommand hearingListedToUpdateHearingForListingCommand;

    @Handles("listing.events.requested-hearing-from-staging-hmi")
    public void requestHearingFromStagingHmi(final JsonEnvelope envelope) {
        final RequestedHearingFromStagingHmi requestHearingFromStagingHmi = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), RequestedHearingFromStagingHmi.class);
        final Hearing listingHearingEvent = requestHearingFromStagingHmi.getHearing();
        if (isHmiListingEnabled(listingHearingEvent.getCourtCentreId(), listingHearingEvent.getCourtRoomId(), envelope)) {
            final uk.gov.justice.listing.courts.RequestedHearingFromStagingHmi publicPayload = uk.gov.justice.listing.courts.RequestedHearingFromStagingHmi.requestedHearingFromStagingHmi()
                    .withHearing(hearingObjectsListingToCoreConverter.convert(requestHearingFromStagingHmi.getHearing()))
                    .build();
            final JsonValue publicPayloadJson =  objectToJsonValueConverter.convert(publicPayload);
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_PUBLISHING, PUBLIC_REQUEST_HEARING_FROM_STAGING_HMI, publicPayloadJson.toString());
            }

            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_REQUEST_HEARING_FROM_STAGING_HMI),
                    publicPayloadJson));
        }
    }

    @Handles("listing.events.updated-hearing-in-staging-hmi")
    public void updateHearingFromStagingHmi(final JsonEnvelope envelope) {
        final UpdatedHearingInStagingHmi updateHearingInStagingHmi = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), UpdatedHearingInStagingHmi.class);
        final Hearing listingHearingEvent = updateHearingInStagingHmi.getHearing();
        if (isHmiListingEnabled(listingHearingEvent.getCourtCentreId(), listingHearingEvent.getCourtRoomId(), envelope)) {
            final uk.gov.justice.listing.courts.UpdatedHearingInStagingHmi publicPayload = uk.gov.justice.listing.courts.UpdatedHearingInStagingHmi.updatedHearingInStagingHmi()
                    .withHearing(hearingObjectsListingToCoreConverter.convert(updateHearingInStagingHmi.getHearing()))
                    .build();
            final JsonValue publicPayloadJson =  objectToJsonValueConverter.convert(publicPayload);
            if(LOGGER.isInfoEnabled()) {
                LOGGER.info(LOG_PUBLISHING, PUBLIC_UPDATE_HEARING_IN_STAGING_HMI, publicPayloadJson.toString());
            }
            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_UPDATE_HEARING_IN_STAGING_HMI),
                    publicPayloadJson));
        }
    }

    @Handles("listing.events.deleted-hearing-in-staging-hmi")
    public void deletedHearingFromStagingHmi(final JsonEnvelope envelope) {
        final DeletedHearingInStagingHmi deletedHearingInStagingHmi = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), DeletedHearingInStagingHmi.class);

        if (isHmiListingEnabled(deletedHearingInStagingHmi.getCourtCentreId(), deletedHearingInStagingHmi.getCourtRoomId(), envelope)) {
            LOGGER.info(LOG_PUBLISHING, "public.listing.delete-hearing-in-staging-hmi", deletedHearingInStagingHmi);
            final JsonArrayBuilder builder = createArrayBuilder();
            deletedHearingInStagingHmi.getCaseAndApplicationIds().forEach(builder::add );
            final JsonObject publicEvent = Json.createObjectBuilder()
                    .add("hearingId", deletedHearingInStagingHmi.getHearingId().toString())
                    .add("cancellationReasonCode", deletedHearingInStagingHmi.getCancellationReasonCode())
                    .add("caseAndApplicationIds", builder.build())
                    .build();

            sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName("public.listing.deleted-hearing-in-staging-hmi"),
                    publicEvent));
        }
    }

    @Handles("public.staginghmi.hearing-updated-from-hmi")
    public void updateHearingFromHmi(final JsonEnvelope envelope){
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("'public.staginghmi.updated-hearing-for-listingi' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final HearingUpdatedFromHmi hearingUpdatedFromHmi = jsonObjectConverter.convert(payload, HearingUpdatedFromHmi.class);

        final Hearing storedHearing = Hearing.hearing().withValuesFrom(hearingService.getHearing(hearingUpdatedFromHmi.getHearingId(), envelope))
                .withCourtCentreId(hearingUpdatedFromHmi.getCourtCentreId())
                .withCourtRoomId(hearingUpdatedFromHmi.getCourtRoomId())
                .withEndDate(ofNullable(hearingUpdatedFromHmi.getEndDate()).map(LocalDate::parse).orElse(null))
                .withStartDate(ofNullable(hearingUpdatedFromHmi.getStartDate()).map(LocalDate::parse).orElse(null))
                .withWeekCommencingStartDate(ofNullable(hearingUpdatedFromHmi.getWeekCommencingStartDate()).map(LocalDate::parse).orElse(null))
                .withWeekCommencingDurationInWeeks(hearingUpdatedFromHmi.getWeekCommencingDurationInWeeks())
                .withWeekCommencingEndDate(ofNullable(hearingUpdatedFromHmi.getWeekCommencingEndDate()).map(LocalDate::parse).orElse(null))
                .withNonDefaultDays(isNull(hearingUpdatedFromHmi.getNonDefaultDays()) ? new ArrayList<>() : convertNonDefaultDays(hearingUpdatedFromHmi.getNonDefaultDays()))
                .withJudiciary(isNull(hearingUpdatedFromHmi.getJudiciary()) ? Collections.emptyList() : convertJudiciary(hearingUpdatedFromHmi.getJudiciary()))
                .build();


        UpdateHearingForListing updateHearingForListing = hearingListedToUpdateHearingForListingCommand.convert(storedHearing).getUpdateHearingForListing();

        final UpdateHearingForListing.Builder updateHearingForListingBuilder = UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(updateHearingForListing)
                .withSource(SOURCE_HMI);
        if(hearingUpdatedFromHmi.getWeekCommencingStartDate() == null && hearingUpdatedFromHmi.getStartDate() == null){
            updateHearingForListingBuilder.withStartDateAndWeekCommencingOptional(true);
        }
        if(nonNull(hearingUpdatedFromHmi.getCourtRoomId())){
            updateHearingForListingBuilder.withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre()
                    .withId(hearingUpdatedFromHmi.getCourtCentreId())
                    .withCourtRoomId(hearingUpdatedFromHmi.getCourtRoomId())
                    .build());
        }
        updateHearingForListing = updateHearingForListingBuilder.build();

        final CourtCentreDetails courtCentre = courtCentreFactory.getCourtCentre(getCourtCentreId(updateHearingForListing), envelope);
        final UpdateHearingForListingEnriched updateHearingForListingEnriched = UpdateHearingForListingEnriched.updateHearingForListingEnriched()
                .withCourtCentreDetails(courtCentre)
                .withUpdateHearingForListing(updateHearingForListing)
                .build();

        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED),
                objectToJsonValueConverter.convert(updateHearingForListingEnriched)));
    }

    private List<NonDefaultDay> convertNonDefaultDays(final List<NonDefaultDays> nonDefaultDays) {
        return nonDefaultDays.stream().map(nonDefaultDay -> NonDefaultDay.nonDefaultDay()
                .withRoomId(nonDefaultDay.getRoomId())
                .withDuration(nonDefaultDay.getDuration())
                .withStartTime(nonDefaultDay.getStartTime())
                .withCourtScheduleId(nonDefaultDay.getCourtScheduleId())
                .withCourtRoomId(nonDefaultDay.getCourtRoomId())
                .withOucode(nonDefaultDay.getOucode())
                .withSession(nonDefaultDay.getSession())
                .withCourtCentreId(nonDefaultDay.getCourtCentreId())
                .build()).collect(Collectors.toList());
    }

    private List<JudicialRole> convertJudiciary(final List<uk.gov.justice.core.courts.JudicialRole> judicialRoles) {
        LOGGER.info("Converting Judiciary");
        return judicialRoles.stream().map(judicialRole -> JudicialRole.judicialRole()
                .withJudicialId(judicialRole.getJudicialId())
                .withIsBenchChairman(judicialRole.getIsBenchChairman())
                .withIsDeputy(judicialRole.getIsDeputy())
                .withJudicialRoleType(isNull(judicialRole.getJudicialRoleType()) ? null :convertJudicialRoleType(judicialRole.getJudicialRoleType()))
                .withUserId(judicialRole.getUserId())
                .build()).collect(Collectors.toList());
    }

    private JudicialRoleType convertJudicialRoleType(final uk.gov.justice.core.courts.JudicialRoleType judicialRoleType){
        return JudicialRoleType.judicialRoleType()
                .withJudicialRoleTypeId(judicialRoleType.getJudicialRoleTypeId())
                .withJudiciaryType(judicialRoleType.getJudiciaryType())
                .build();
    }

    private boolean isHmiListingEnabled(final UUID courtCentreId, final UUID courtRoomId, final JsonEnvelope envelope) {
        final CourtCentre courtCentre = hearingConfirmedFactory.buildCourtCentreWithAdmin(courtCentreId, courtRoomId, envelope);
        return stagingHmiService.isHmiListingEnabled(ofNullable(courtCentre.getCode()));
    }

    private UUID getCourtCentreId(final UpdateHearingForListing updateHearingForListing) {
        final UUID courtCentreId = updateHearingForListing.getCourtCentreId();
        final Optional<SelectedCourtCentre> selectedCourtCentre = Optional.ofNullable(updateHearingForListing.getSelectedCourtCentre());
        if (selectedCourtCentre.isPresent() && CROWN.equals(updateHearingForListing.getJurisdictionType())) {
            return selectedCourtCentre.get().getId();
        }
        return courtCentreId;
    }
}