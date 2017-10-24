package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class HearingData {

    private final UUID id;
    private final String courtCentreId;
    private final String hearingType;
    private final LocalDate hearingStartDate;
    private final int hearingEstimateMinutes;
    private final List<DefendantData> defendants;

    public HearingData(final UUID id, final String courtCentreId, final String hearingType,
                       final LocalDate hearingStartDate, final int hearingEstimateMinutes,
                       final List<DefendantData> defendants) {

        this.id = id;
        this.courtCentreId = courtCentreId;
        this.hearingEstimateMinutes = hearingEstimateMinutes;
        this.hearingStartDate = hearingStartDate;
        this.hearingType = hearingType;
        this.defendants = defendants;
    }

    public UUID getId() { return id; }

    public String getCourtCentreId() { return courtCentreId; }

    public String getHearingType() { return hearingType; }

    public LocalDate getHearingStartDate() { return hearingStartDate; }

    public int getHearingEstimateMinutes() { return hearingEstimateMinutes; }

    public List<DefendantData> getDefendants() {
        return defendants;
    }
}
