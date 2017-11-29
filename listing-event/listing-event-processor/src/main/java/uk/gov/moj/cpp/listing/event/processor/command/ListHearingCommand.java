package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.moj.cpp.listing.domain.Defendant;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ListHearingCommand {

    private final String hearingId;
    private final String type;
    private final LocalDate startDate;
    private final Integer estimateMinutes;
    private final String caseId;
    private final String courtCentreId;
    private final List<Defendant> defendants;


    public ListHearingCommand(final String hearingId, final String type,
                              final LocalDate startDate, final Integer estimateMinutes,
                              final String caseId, final String courtCentreId,
                              final List<Defendant> defendants) {
        this.hearingId = hearingId;
        this.type = type;
        this.startDate = startDate;
        this.estimateMinutes = estimateMinutes;
        this.caseId = caseId;
        this.courtCentreId = courtCentreId;
        this.defendants = defendants;
    }

    public String getHearingId() {
        return hearingId;
    }

    public String getType() { return type; }

    public LocalDate getStartDate() { return startDate; }

    public Integer getEstimateMinutes() { return estimateMinutes; }

    public String getCaseId() {
        return caseId;
    }

    public String getCourtCentreId() { return courtCentreId; }

    public List<Defendant> getDefendants() {
        return new ArrayList(defendants);
    }

}
