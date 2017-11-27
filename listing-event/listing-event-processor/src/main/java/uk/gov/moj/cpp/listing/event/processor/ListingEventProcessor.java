package uk.gov.moj.cpp.listing.event.processor;


import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.Defendant;
import uk.gov.moj.cpp.listing.event.CaseSentForListing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class ListingEventProcessor {

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
        sendHearingsForListing(envelope);

        publishCaseSentForListingPublicEvent(envelope);
    }

    /*
     * For each hearing in the 'case-sent-for-listing' event, extract it
     * and send each one through as a separate 'list-hearing' command.
     */
    private void sendHearingsForListing(JsonEnvelope envelope) {
        final CaseSentForListing event = getCaseSentForListing(envelope);

        final List<ListHearingCommand> listHearingsCommands = convertCaseSentForListingToListHearingCommands(event);
        listHearingsCommands.forEach(
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


    private static class ListHearingCommand {

        private final String hearingId;
        private final String type;
        private final LocalDate startDate;
        private final Integer estimateMinutes;
        private final String caseId;
        private final String courtCentreId;
        private final List<Defendant> defendants;


        public ListHearingCommand(final String hearingId, final String type,
                                  final LocalDate startDate, final Integer estimateMinutes,
                                  final String caseId, final String courtCentreId,
                                  final List<Defendant> defendants) {
            this.hearingId = hearingId;
            this.type = type;
            this.startDate = startDate;
            this.estimateMinutes = estimateMinutes;
            this.caseId = caseId;
            this.courtCentreId = courtCentreId;
            this.defendants = defendants;
        }

        public String getHearingId() {
            return hearingId;
        }

        public String getType() { return type; }

        public LocalDate getStartDate() { return startDate; }

        public Integer getEstimateMinutes() { return estimateMinutes; }

        public String getCaseId() {
            return caseId;
        }

        public String getCourtCentreId() { return courtCentreId; }

        public List<Defendant> getDefendants() {
            return new ArrayList(defendants);
        }
    }
}
