package uk.gov.moj.cpp.listing.domain.referencedata;

import java.util.List;

public class HearingTypesList {

    private List<HearingType> hearingTypes;

    public HearingTypesList(final List<HearingType> hearingTypes) {
        this.hearingTypes = hearingTypes;
    }

    public List<HearingType> getHearingTypes() {
        return hearingTypes;
    }

    public void setHearingTypes(final List<HearingType> hearingTypes) {
        this.hearingTypes = hearingTypes;
    }
}
