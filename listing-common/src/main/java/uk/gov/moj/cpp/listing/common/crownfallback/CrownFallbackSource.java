package uk.gov.moj.cpp.listing.common.crownfallback;

/**
 * Identifies which listing command flow invoked the courtscheduler Crown fallback search-and-book.
 * The {@code label} is sent as the {@code source} field to courtscheduler and persisted on the
 * {@code allocated_listings.source} column, so usage can be grepped/aggregated per caller.
 */
public enum CrownFallbackSource {

    LIST_COURT_HEARING("CROWN_FB_LIST"),
    LIST_NEXT_HEARINGS_V2("CROWN_FB_ADJOURN"),
    UPDATE_HEARING_FOR_LISTING("CROWN_FB_UPDATE");

    private final String label;

    CrownFallbackSource(final String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
