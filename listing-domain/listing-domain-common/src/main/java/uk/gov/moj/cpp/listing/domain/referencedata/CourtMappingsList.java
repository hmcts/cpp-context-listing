package uk.gov.moj.cpp.listing.domain.referencedata;

import java.util.List;

public class CourtMappingsList {

    private List<CourtMapping> cpXhibitCourtMappings;

    public CourtMappingsList(final List<CourtMapping> cpXhibitCourtMappings) {
        this.cpXhibitCourtMappings = cpXhibitCourtMappings;
    }

    public List<CourtMapping> getCpXhibitCourtMappings() {
        return cpXhibitCourtMappings;
    }
}
