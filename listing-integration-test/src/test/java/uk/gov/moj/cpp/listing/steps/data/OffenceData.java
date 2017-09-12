package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public class OffenceData {

    private final UUID offenceId;
    private final String offenceCode;
    private final String plea;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String statementOfOffenceTitle;
    private final String statementOfOffenceLegislation;

    public OffenceData(final UUID offenceId, final String offenceCode, final String plea,
                       final LocalDate startDate, final LocalDate endDate, final String
                       statementOfOffenceTitle, final String statementOfOffenceLegislation) {

        this.endDate = endDate;
        this.offenceCode = offenceCode;
        this.offenceId = offenceId;
        this.plea = plea;
        this.startDate = startDate;
        this.statementOfOffenceLegislation = statementOfOffenceLegislation;
        this.statementOfOffenceTitle = statementOfOffenceTitle;
    }

    public UUID getOffenceId() { return offenceId; }

    public String getOffenceCode() { return offenceCode; }

    public String getPlea() { return plea; }

    public LocalDate getStartDate() { return startDate; }

    public LocalDate getEndDate() { return endDate; }

    public String getStatementOfOffenceTitle() { return statementOfOffenceTitle; }

    public String getStatementOfOffenceLegislation() { return statementOfOffenceLegislation; }
}
