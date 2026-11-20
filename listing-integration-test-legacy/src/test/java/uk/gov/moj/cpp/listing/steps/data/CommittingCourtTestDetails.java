package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

/**
 * Optional committing court for integration tests (Xhibit / cp-xhibit reference data paths).
 * {@code courtHouseType} uses the same strings as reference data / Xhibit, e.g. {@code MAGISTRATES_COURT}, {@code CROWN_COURT}.
 */
public final class CommittingCourtTestDetails {

    private final UUID courtCentreId;
    private final String courtHouseName;
    private final String courtHouseType;

    public CommittingCourtTestDetails(final UUID courtCentreId, final String courtHouseName, final String courtHouseType) {
        this.courtCentreId = courtCentreId;
        this.courtHouseName = courtHouseName;
        this.courtHouseType = courtHouseType;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public String getCourtHouseName() {
        return courtHouseName;
    }

    public String getCourtHouseType() {
        return courtHouseType;
    }
}
