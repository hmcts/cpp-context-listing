package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.spi.JsonEnvelopeProvider;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListingService {

    @Inject
    private HearingQueryView hearingQueryView;

    private static final Logger LOGGER = LoggerFactory.getLogger(ListingService.class);

    public JsonObject getUnpublishedCourtListForCourtCentre(final JsonEnvelope envelope,
                                                            final PublishCourtListRequestParameters publishCourtListRequestParameters) {

        final Optional<String> endDate = Optional.of(publishCourtListRequestParameters.getEndDate().format(ISO_LOCAL_DATE));

        final JsonObjectBuilder restRequestParametersBuilder = createObjectBuilder()
                .add("courtCentreId", publishCourtListRequestParameters.getCourtCentreId().toString())
                .add("startDate", publishCourtListRequestParameters.getStartDate().format(ISO_LOCAL_DATE))
                .add("publishCourtListType", publishCourtListRequestParameters.getPublishCourtListType().name());

        endDate.ifPresent(s -> LOGGER.info("endDate is: {}", s));

        endDate.ifPresent(pEndDate -> restRequestParametersBuilder.add("endDate", pEndDate));
        final JsonEnvelope response = hearingQueryView.retrieveCourtList(JsonEnvelopeProvider.provider().envelopeFrom(metadataFrom(envelope.metadata()).withName("listing.courtlist"), restRequestParametersBuilder.build()));
        return response.payloadAsJsonObject();
    }

    public JsonObject getPublishedCourtListForCourtCentre(final JsonEnvelope envelope,
                                                          final UUID courtCentreId,
                                                          final PublishCourtListType publishCourtListType,
                                                          final LocalDate startDate) {

        final JsonObjectBuilder restRequestParametersBuilder = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("startDate", startDate.format(ISO_LOCAL_DATE))
                .add("publishCourtListType", publishCourtListType.name())
                .add("published", true);

        final JsonEnvelope response = hearingQueryView.retrieveCourtList(JsonEnvelopeProvider.provider().envelopeFrom(metadataFrom(envelope.metadata()).withName("listing.courtlist"), restRequestParametersBuilder.build()));

        return response.payloadAsJsonObject();
    }
}
