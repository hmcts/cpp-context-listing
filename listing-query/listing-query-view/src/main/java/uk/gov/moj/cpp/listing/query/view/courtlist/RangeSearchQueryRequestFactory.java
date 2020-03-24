package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ApplicationScoped
public class RangeSearchQueryRequestFactory {

    @Inject
    private Enveloper enveloper;

    public JsonEnvelope buildRangeSearchQueryEnvelope(final UUID courtCentreId,
                                                      final PublishCourtListType listType,
                                                      final LocalDate startDate,
                                                      final JsonEnvelope envelope) {

        final JsonObjectBuilder rangeSearchQueryPayloadBuilder = createObjectBuilder()
                .add("allocated", true)
                .add("courtCentreId", courtCentreId.toString())
                .add("isCrownCourt", true);

        switch (listType) {
            case DRAFT:
            case FINAL:
                rangeSearchQueryPayloadBuilder
                        .add("startDate", startDate.toString())
                        .add("endDate", startDate.toString());
                break;
            case FIRM:
            case WARN:
                addWeekCommencingParameters(startDate, rangeSearchQueryPayloadBuilder);
                break;
            default:
                throw new UnsupportedOperationException("Request not supported:" + listType);
        }

        final JsonObject queryPayload = rangeSearchQueryPayloadBuilder
                .build();

        return enveloper.withMetadataFrom(envelope, "listing.search.hearings").apply(queryPayload);
    }

    private void addWeekCommencingParameters(final LocalDate startDate, final JsonObjectBuilder rangeSearchQueryPayloadBuilder) {
        final LocalDate weekCommencingEndDate = startDate.plusDays(4);  // The start date is a Monday. Therefore end date should be the Friday.

        rangeSearchQueryPayloadBuilder
                .add("jurisdictionType", CROWN.name())
                .add("weekCommencingStartDate", startDate.toString())
                .add("weekCommencingEndDate", weekCommencingEndDate.toString());
    }
}
