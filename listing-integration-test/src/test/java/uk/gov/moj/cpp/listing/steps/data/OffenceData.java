package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.UUID;

public class OffenceData {

    private final UUID offenceId;
    private final UUID randomOffenceId;
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
        this.randomOffenceId = UUID.randomUUID();
        this.startDate = startDate;
        this.statementOfOffenceLegislation = statementOfOffenceLegislation;
        this.statementOfOffenceTitle = statementOfOffenceTitle;
    }

    public UUID getOffenceId() { return offenceId; }

    public UUID getRandomOffenceId() { return randomOffenceId; }

    public String getNewOffenceCode() { return offenceCode + "-new"; }

    public String getOffenceCode() { return offenceCode; }

    public String getChangedOffenceCode() { return offenceCode + "-changed"; }

    public LocalDate getStartDate() { return startDate; }

    public LocalDate getEndDate() { return endDate; }

    public String getStatementOfOffenceTitle() { return statementOfOffenceTitle; }

    public String getChangedStatementOfOffenceTitle() { return statementOfOffenceTitle + "-changed"; }

    public String getStatementOfOffenceLegislation() { return statementOfOffenceLegislation; }

    public String getChangedStatementOfOffenceLegislation() { return statementOfOffenceLegislation + "-changed"; }
}
