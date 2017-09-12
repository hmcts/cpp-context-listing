package uk.gov.moj.cpp.listing.steps.data;


public class CourtCentreData {

    private final String id;
    private final String courtCentreName;

    public CourtCentreData(final String id,
                     final String courtCentreName) {
        this.id = id;
        this.courtCentreName = courtCentreName;
    }

    public String getId() { return id; }

    public String getCourtCentreName() { return courtCentreName; }
}
