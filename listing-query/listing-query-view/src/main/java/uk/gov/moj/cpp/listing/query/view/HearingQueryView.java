package uk.gov.moj.cpp.listing.query.view;


import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingSummary;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingSummaryConverter;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.UUID.fromString;


@ServiceComponent(Component.QUERY_VIEW)
public class HearingQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryView.class);
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";

    @Inject
    private HearingRepository repository;

    @Inject
    private HearingSummaryConverter hearingSummaryConverter;

    @Inject
    private Converter<List<HearingSummary>, JsonArray> jsonConverter;

    @Inject
    private Enveloper enveloper;


    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {

        final UUID courtCentreId = fromString(query.payloadAsJsonObject().getString(COURT_CENTRE_ID));
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER);

        LOGGER.info("Query params - courtCentreId: %s , allocated: %s", courtCentreId, allocated);

        final List<Hearing> hearings = repository.findByAllocatedAndCourtCentreId(allocated,
                courtCentreId);

        final List<HearingSummary> hearingSummaryList = hearings.stream()
                .map(h -> hearingSummaryConverter.convert(h))
                .collect(Collectors.toList());

        return enveloper.withMetadataFrom(query, "listing.search.hearings").apply(
                Json.createObjectBuilder()
                        .add("hearings", jsonConverter.convert(hearingSummaryList))
                        .build()
        );
    }
}
