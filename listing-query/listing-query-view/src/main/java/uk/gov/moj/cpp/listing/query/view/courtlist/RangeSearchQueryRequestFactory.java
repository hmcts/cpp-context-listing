package uk.gov.moj.cpp.listing.query.view.courtlist;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.listing.domain.JurisdictionType.CROWN;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

@SuppressWarnings({"squid:CallToDeprecatedMethod"})
@ApplicationScoped
public class RangeSearchQueryRequestFactory {

    @Inject
    private Enveloper enveloper;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @SuppressWarnings({"squid:S128","squid:S1192","squid:S128","deprecation"})
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
                        .add("endDate", startDate.toString())
                        .add("noPagination", true);
                break;
            case FIRM:
                rangeSearchQueryPayloadBuilder
                        .add("noPagination", true);
                addWeekCommencingParameters(startDate, rangeSearchQueryPayloadBuilder);
                break;
            case WARN:
                addWeekCommencingParameters(startDate, rangeSearchQueryPayloadBuilder);
                rangeSearchQueryPayloadBuilder.add("noPagination", true);
                break;
            default:
                throw new UnsupportedOperationException("Request not supported:" + listType);
        }

        final JsonObject queryPayload = rangeSearchQueryPayloadBuilder
                .build();

        return enveloper.withMetadataFrom(envelope, "listing.search.hearings").apply(queryPayload);
    }

    private void addWeekCommencingParameters(LocalDate startDate, final JsonObjectBuilder rangeSearchQueryPayloadBuilder) {
        LocalDate weekCommencingEndDate = null;
        if(startDate.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            weekCommencingEndDate = startDate.plusDays(6);
            logger.info("WeekCommencingStartDate is Monday, hence end date is  [{}]  " ,weekCommencingEndDate);
        }else{
            startDate = startDate.minusDays(1);
            weekCommencingEndDate = startDate.plusDays(5);
            logger.info("WeekCommencingStartDate is not Monday, hence WeekCommencingStartDate is [{}] and WeekCommencingEndDate is  [{}]  " ,startDate, weekCommencingEndDate);
        }

        rangeSearchQueryPayloadBuilder
                .add("jurisdictionType", CROWN.name())
                .add("weekCommencingStartDate", startDate.toString())
                .add("weekCommencingEndDate", weekCommencingEndDate.toString());
    }
}
