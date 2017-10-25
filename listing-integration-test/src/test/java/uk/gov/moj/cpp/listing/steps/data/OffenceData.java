package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.UUID;

public class OffenceData {

    private final UUID offenceId;
    private final String offenceCode;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String statementOfOffenceTitle;
    private final String statementOfOffenceLegislation;

    public OffenceData(final UUID offenceId, final String offenceCode,
                       final LocalDate startDate, final LocalDate endDate, final String
                               statementOfOffenceTitle, final String statementOfOffenceLegislation) {

        this.endDate = endDate;
        this.offenceCode = offenceCode;
        this.offenceId = offenceId;
        this.startDate = startDate;
        this.statementOfOffenceLegislation = statementOfOffenceLegislation;
        this.statementOfOffenceTitle = statementOfOffenceTitle;
    }

    public UUID getOffenceId() { return offenceId; }

    public String getOffenceCode() { return offenceCode; }

    public LocalDate getStartDate() { return startDate; }

    public LocalDate getEndDate() { return endDate; }

    public String getStatementOfOffenceTitle() { return statementOfOffenceTitle; }

    public String getStatementOfOffenceLegislation() { return statementOfOffenceLegislation; }
}
