package uk.gov.moj.cpp.listing.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.listing.commands.CourtCentreDetails;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.UpdateHearingForListingEnriched;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;
import uk.gov.justice.listing.courts.ListCourtHearing;
import uk.gov.justice.listing.courts.ListCourtHearingEnriched;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class ListingCommandApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingCommandApi.class);
    static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING_ENRICHED = "listing.command.update-hearing-for-listing-enriched";
    static final String LISTING_COMMAND_LIST_COURT_HEARING_ENRICHED = "listing.command.list-court-hearing-enriched";

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
        if(LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.command.list-court-hearing' received with payload {}", envelope.toObfuscatedDebugString());
        }

        final ListCourtHearing listCourtHearing = jsonObjectConverter.convert(payload, ListCourtHearing.class);
        LOGGER.info("'listing.command.list-court-hearing' listCourtHearing: {}", listCourtHearing);
        Set<CourtCentreDetails> courtCentres = new HashSet<>();

        for (final HearingListingNeeds commandHearing : listCourtHearing.getHearings()) {
            courtCentres.add(courtCentreFactory.getCourtCentre(commandHearing.getCourtCentre().getId(), envelope));
        }
        final ListCourtHearingEnriched listCourtHearingEnriched = ListCourtHearingEnriched.listCourtHearingEnriched()
                .withCourtCentresDetails(new ArrayList<>(courtCentres))
                .withListCourtHearing(listCourtHearing)
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, LISTING_COMMAND_LIST_COURT_HEARING_ENRICHED)
                .apply(objectToJsonValueConverter.convert(listCourtHearingEnriched)));
    }

    @Handles("listing.command.update-hearing-for-listing")
    public void updateHearingForListing(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        if(LOGGER.isInfoEnabled()) {
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

    @Handles("listing.command.change-judiciary-for-hearings")
    public void changeJudiciaryForHeraings(final JsonEnvelope envelope) {
        sender.send(envelope);
    }

    @Handles("listing.command.sequence-hearings")
    public void sequenceHearings(final JsonEnvelope envelope) {sender.send(envelope);}
}
