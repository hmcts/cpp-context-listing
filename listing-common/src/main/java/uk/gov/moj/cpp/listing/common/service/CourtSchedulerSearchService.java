package uk.gov.moj.cpp.listing.common.service;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.dispatcher.SystemUserProvider;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;

@SuppressWarnings("squid:S6813")
@ApplicationScoped
public class CourtSchedulerSearchService {

    private static final String SEARCH_AVAILABLE_JUDICIARIES_RESOURCE = "/judiciaries/search-available";
    private static final String COURTSCHEDULER_SEARCH_AVAILABLE_JUDICIARIES =
            "application/vnd.courtscheduler.search.available.judiciaries+json";

    @Inject
    @Value(key = "courtscheduler.base.url", defaultValue = "http://localhost:8080/listingcourtscheduler-api/rest/courtscheduler")
    protected String baseUri;

    @Inject
    SystemUserProvider systemUserProvider;

    @Inject
    StringToJsonObjectConverter stringToJsonObjectConverter;

    public Response searchAvailableJudiciaries(final Map<String, String> params) {
        return CourtSchedulerJsonGetQuerySupport.executeQuery(
                baseUri,
                systemUserProvider,
                stringToJsonObjectConverter,
                SEARCH_AVAILABLE_JUDICIARIES_RESOURCE,
                COURTSCHEDULER_SEARCH_AVAILABLE_JUDICIARIES,
                params,
                "CourtScheduler");
    }
}
