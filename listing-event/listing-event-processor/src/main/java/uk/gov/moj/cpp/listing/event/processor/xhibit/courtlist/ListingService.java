package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ListingService {

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Requester requester;

    public List<JsonObject> getHearingsForPublishing(final JsonEnvelope envelope,
                                                     final PublishCourtListRequestParameters publishCourtListRequestParameters) {


        final JsonObjectBuilder restRequestParametersBuilder = createObjectBuilder()
                .add("allocated", true)
                .add("courtCentreId", publishCourtListRequestParameters.getCourtCentreId().toString())
                .add("jurisdictionType", "CROWN");

        switch (publishCourtListRequestParameters.getPublishCourtListType()) {
            case DRAFT:
            case WARN:
                restRequestParametersBuilder
                        .add("startDate", publishCourtListRequestParameters.getStartDate().format(ISO_LOCAL_DATE))
                        .add("endDate", publishCourtListRequestParameters.getEndDate().format(ISO_LOCAL_DATE));
                break;
            case FIRM:
            case FINAL:
                restRequestParametersBuilder
                        .add("weekCommencingStartDate", publishCourtListRequestParameters.getStartDate().format(ISO_LOCAL_DATE))
                        .add("weekCommencingEndDate", publishCourtListRequestParameters.getEndDate().format(ISO_LOCAL_DATE));
                break;
            default:
                throw new UnsupportedOperationException("Request not supported:" + publishCourtListRequestParameters.getPublishCourtListType());
        }

        final JsonEnvelope response = requester.request(envelop(restRequestParametersBuilder.build()).withName("listing.range.search.hearings").withMetadataFrom(envelope));

        return response.payloadAsJsonObject().getJsonArray("hearings").getValuesAs(JsonObject.class);
    }
}
