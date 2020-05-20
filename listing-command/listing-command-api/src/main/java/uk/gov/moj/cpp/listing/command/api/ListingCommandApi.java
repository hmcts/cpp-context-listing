package uk.gov.moj.cpp.listing.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.ExtendHearingForHearing;
import uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.listing.courts.ListCourtHearingEnriched;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class ListingCommandApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingCommandApi.class);
    static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED = "listing.command.update-hearing-for-listing-enriched";
    static final String LISTING_COMMAND_LIST_COURT_HEARING_ENRICHED = "listing.command.list-court-hearing-enriched";
    static final String LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING_ENRICHED = "listing.command.extend-hearing-for-hearing-enriched";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private CourtCentreFactory courtCentreFactory;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Handles("listing.command.list-court-hearing")
    public void listCourtHearing(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();

        LOGGER.info("'listing.command.list-court-hearing' received with payload {}", envelope);

        final ListCourtHearing listCourtHearing = jsonObjectConverter.convert(payload, ListCourtHearing.class);
        LOGGER.info("'listing.command.list-court-hearing' listCourtHearing: {}", listCourtHearing);
        final Set<CourtCentreDetails> courtCentres = new HashSet<>();

        for (final HearingListingNeeds commandHearing : listCourtHearing.getHearings()) {
            courtCentres.add(courtCentreFactory.getCourtCentre(commandHearing.getCourtCentre().getId(), envelope));
        }
        final ListCourtHearingEnriched listCourtHearingEnriched = ListCourtHearingEnriched.listCourtHearingEnriched()
                .withCourtCentresDetails(new ArrayList<>(courtCentres))
                .withListCourtHearing(listCourtHearing)
                .withAdjournedFromDate(listCourtHearing.getAdjournedFromDate())
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, LISTING_COMMAND_LIST_COURT_HEARING_ENRICHED)
                .apply(objectToJsonValueConverter.convert(listCourtHearingEnriched)));
    }

    @Handles("listing.command.update-hearing-for-listing")
    public void updateHearingForListing(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.update-hearing-for-listing' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final UpdateHearingForListing updateHearingForListing = jsonObjectConverter.convert(payload, UpdateHearingForListing.class);

        final CourtCentreDetails courtCentre = courtCentreFactory.getCourtCentre(updateHearingForListing.getCourtCentreId(), envelope);
        final UpdateHearingForListingEnriched updateHearingForListingEnriched = UpdateHearingForListingEnriched.updateHearingForListingEnriched()
                .withCourtCentreDetails(courtCentre)
                .withUpdateHearingForListing(updateHearingForListing)
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED)
                .apply(objectToJsonValueConverter.convert(updateHearingForListingEnriched)));
    }

    @Handles("listing.command.extend-hearing-for-hearing")
    public void extendHearingForHearing(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final String unAllocatedHearingId = payload.getString("hearingId", null);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.extend-hearing-for-hearing' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final ExtendHearingForHearing extendHearingForHearing = jsonObjectConverter.convert(payload, ExtendHearingForHearing.class);
        LOGGER.info("'listing.command.extend-hearing-for-hearing' extendHearingForHearing: {}", extendHearingForHearing);

        final UUID allocatedHearingId = extendHearingForHearing.getAllocatedHearingId();

        final ExtendHearingForHearingEnriched extendHearingForHearingEnriched = ExtendHearingForHearingEnriched
                .extendHearingForHearingEnriched().withAllocatedHearingId(allocatedHearingId)
                .withUnAllocatedHearingId(UUID.fromString(unAllocatedHearingId))
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, LISTING_COMMAND_EXTEND_HEARING_FOR_HEARING_ENRICHED)
                .apply(objectToJsonValueConverter.convert(extendHearingForHearingEnriched)));
    }

    @Handles("listing.command.change-judiciary-for-hearings")
    public void changeJudiciaryForHeraings(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.sequence-hearings")
    public void sequenceHearings(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.restrict-court-list")
    public void restrictCourtList(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

    @Handles("listing.command.publish-court-list")
    public void publishCourtList(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

    @Handles("listing.command.publish-court-lists-for-crown-courts")
    @SuppressWarnings("WeakerAccess") // Must be public for the framework
    public void publishCourtListForCrownCourts(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

    @Handles("listing.command.court-list-request-export")
    public void courtListRequestExport(final JsonEnvelope jsonEnvelope) {
        sender.send(jsonEnvelope);
    }

}
