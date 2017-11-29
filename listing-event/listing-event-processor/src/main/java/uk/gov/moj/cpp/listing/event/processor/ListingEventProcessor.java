package uk.gov.moj.cpp.listing.event.processor;


import static java.lang.String.format;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;
import uk.gov.moj.cpp.listing.event.processor.command.ListHearingCommand;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ListingEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingEventProcessor.class);

    static final String PUBLIC_EVENT_CASE_SENT_FOR_LISTING = "listing.case-sent-for-listing";
    static final String COMMAND_LIST_HEARING = "listing.command.list-hearing";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Handles("listing.events.case-sent-for-listing")
    public void handleCaseSentForListingMessage(final JsonEnvelope envelope) {
        LOGGER.debug(format("'listing.events.case-sent-for-listing' event received %s", envelope.payloadAsJsonObject()));

        sendListHearingCommands(envelope);

        publishCaseSentForListingPublicEvent(envelope);
    }

    /*
     * For each hearing in the 'case-sent-for-listing' event, extract it
     * and send each one through as a separate 'list-hearing' command.
     */
    private void sendListHearingCommands(JsonEnvelope envelope) {
        final CaseSentForListing event = getCaseSentForListing(envelope);
        final List<ListHearingCommand> listHearingCommands = convertCaseSentForListingToListHearingCommands(event);

        listHearingCommands.forEach(
                hearings -> sender
                        .send(enveloper.withMetadataFrom(envelope, COMMAND_LIST_HEARING)
                                .apply(objectToJsonValueConverter.convert(hearings)))
        );
    }

    /*
     * Publish a public event to notify  that the case has been listed.
     */
    private void publishCaseSentForListingPublicEvent(final JsonEnvelope envelope) {
        final CaseSentForListing event = getCaseSentForListing(envelope);
        sender.send(enveloper.withMetadataFrom(envelope, PUBLIC_EVENT_CASE_SENT_FOR_LISTING).apply(event));
    }

    private CaseSentForListing getCaseSentForListing(JsonEnvelope envelope) {
        return jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CaseSentForListing.class);
    }

    private List<ListHearingCommand> convertCaseSentForListingToListHearingCommands(final CaseSentForListing event) {
        return event.getHearings().stream().map(hearing -> new ListHearingCommand(hearing.getId(),
                hearing.getType(),
                hearing.getStartDate(),
                hearing.getEstimateMinutes(),
                hearing.getCaseId(),
                hearing.getCourtCentreId(),
                hearing.getDefendants()
        )).collect(Collectors.toList());
    }

}
