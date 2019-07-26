package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.core.courts.CourtApplication;

import java.util.List;
import java.util.UUID;

public class CourtApplicationUpdateData {

    private final CourtApplication courtApplication;

    public CourtApplicationUpdateData(CourtApplication courtApplication) {
        this.courtApplication = courtApplication;
    }


    public CourtApplication getCourtApplication() {
        return courtApplication;
    }
}
