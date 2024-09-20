package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.core.courts.CourtApplication;

public class CourtApplicationUpdateData {

    private final CourtApplication courtApplication;

    public CourtApplicationUpdateData(CourtApplication courtApplication) {
        this.courtApplication = courtApplication;
    }


    public CourtApplication getCourtApplication() {
        return courtApplication;
    }
}
