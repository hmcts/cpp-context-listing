package uk.gov.moj.cpp.listing.query.view;


import static java.lang.String.format;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingCase;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.ListingCaseRepository;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingCaseSummary;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingSummaryConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ServiceComponent(Component.QUERY_VIEW)
public class HearingQueryView {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryView.class);
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";

    @Inject
    private HearingRepository repository;

    @Inject
    private ListingCaseRepository listingCaseRepository;

    @Inject
    private HearingSummaryConverter hearingSummaryConverter;

    @Inject
    private Converter<List<HearingCaseSummary>, JsonArray> jsonConverter;

    @Inject
    private Enveloper enveloper;


    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {

        final UUID courtCentreId = fromString(query.payloadAsJsonObject().getString(COURT_CENTRE_ID));
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER);

        LOGGER.info(format("Query params - courtCentreId: %s , allocated: %s", courtCentreId, allocated));

        final List<Hearing> hearings = repository.findByAllocatedAndCourtCentreId(allocated,
                courtCentreId);

        //Added check to avoid server side error earlier in case query return blank
        if (hearings.isEmpty()) {
            return enveloper.withMetadataFrom(query, "listing.search.hearings").apply(
                    Json.createObjectBuilder()
                            .add("hearings", jsonConverter.convert(new ArrayList<>()))
                            .build()
            );
        }

        
        final Map<UUID, String> caseUrns = getCaseUrns(hearings);

        final List<HearingCaseSummary> hearingSummaryList = hearings.stream()
                .map(hearingSummary -> hearingSummaryConverter.convert(hearingSummary))
                .map(hearingCaseSummary -> new HearingCaseSummary(hearingCaseSummary, caseUrns.get(hearingCaseSummary.getCaseId())))
                .collect(toList());

        return enveloper.withMetadataFrom(query, "listing.search.hearings").apply(
                Json.createObjectBuilder()
                        .add("hearings", jsonConverter.convert(hearingSummaryList))
                        .build()
        );
    }

    private Map<UUID, String> getCaseUrns(final List<Hearing> hearings) {
        List<UUID> caseIds = hearings.stream().map(Hearing::getListingCaseId).collect(toList());

        return listingCaseRepository.findByCaseIds(caseIds).stream().collect(groupingBy(ListingCase::getCaseId, mapping(ListingCase::getUrn, joining())));
    }


}
