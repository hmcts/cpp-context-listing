package uk.gov.moj.cpp.listing.domain.referencedata;

import java.util.List;

public class CourtRoomMappingsList {

    private List<CourtRoomMapping> cpXhibitCourtRoomMappings;

    public CourtRoomMappingsList(final List<CourtRoomMapping> cpXhibitCourtRoomMappings) {
        this.cpXhibitCourtRoomMappings = cpXhibitCourtRoomMappings;
    }

    public List<CourtRoomMapping> getCpXhibitCourtRoomMappings() {
        return cpXhibitCourtRoomMappings;
    }

    public void setCpXhibitCourtRoomMappings(final List<CourtRoomMapping> cpXhibitCourtRoomMappings) {
        this.cpXhibitCourtRoomMappings = cpXhibitCourtRoomMappings;
    }
}