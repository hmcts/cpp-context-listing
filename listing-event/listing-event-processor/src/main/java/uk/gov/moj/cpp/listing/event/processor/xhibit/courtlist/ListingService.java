package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ListingService {

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Requester requester;

    public JsonObject getUnpublishedCourtListForCourtCentre(final JsonEnvelope envelope,
                                                          final PublishCourtListRequestParameters publishCourtListRequestParameters) {

        final JsonObjectBuilder restRequestParametersBuilder = createObjectBuilder()
                .add("courtCentreId", publishCourtListRequestParameters.getCourtCentreId().toString())
                .add("startDate", publishCourtListRequestParameters.getStartDate().format(ISO_LOCAL_DATE))
                .add("publishCourtListType", publishCourtListRequestParameters.getPublishCourtListType().name());

        final JsonEnvelope response = requester.request(envelop(restRequestParametersBuilder.build()).withName("listing.courtlist").withMetadataFrom(envelope));

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

        final JsonEnvelope response = requester.request(envelop(restRequestParametersBuilder.build()).withName("listing.courtlist").withMetadataFrom(envelope));

        return response.payloadAsJsonObject();
    }
}
