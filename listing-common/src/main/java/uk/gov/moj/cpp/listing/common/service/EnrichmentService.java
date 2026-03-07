package uk.gov.moj.cpp.listing.common.service;

import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;

/**
 * Common interface for enrichment services in the listing. Enrichment services are responsible for
 * enhancing or completing data by fetching additional information from various contexts and
 * sources.
 * <p>
 * Each implementation of this interface should: - Be annotated with @ApplicationScoped - Implement
 * its own version of needsEnrichment method - Handle specific enrichment logic for its domain
 */

public interface EnrichmentService {
    /**
     * boolean needsEnrichment(T data))
     * Determines whether the current data needs enrichment.
     * Implementations can define their own parameters based on their specific needs.
     * You can use this logic to reduce the number of calls to other contexts. For example,
     * if you need to call listingcourtscheduler only for MAGS jurisdiction type, then you can implement needsEnrichment
     * to check this and avoid unnecessary calls to listingcourtscheduler for CROWN.
     *
     * @return true if enrichment is needed, false otherwise
     */

    /**
     * Enriches a list of HearingListingNeeds with additional information. This method is used for
     * batch enrichment operations.
     *
     * @param hearingListingNeeds The list of hearings to enrich
     * @param envelope            The JSON envelope containing context information
     * @return The enriched list of hearings
     */
    default HearingListingNeeds enrichHearings(HearingListingNeeds hearingListingNeeds, JsonEnvelope envelope) {
        return hearingListingNeeds;
    }

    /**
     * Enriches a single UpdateHearingForListing with additional information. This method is used
     * for single hearing enrichment operations.
     *
     * @param updateHearingForListing The hearing to enrich
     * @param envelope                The JSON envelope containing context information
     * @return The enriched hearing
     */
    default UpdateHearingForListing enrichHearing(UpdateHearingForListing updateHearingForListing, JsonEnvelope envelope) {
        return updateHearingForListing;
    }
}
