package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.UUID;

public class OffenceData {

    private final UUID offenceId;
    private final UUID randomOffenceId;
    private final String offenceCode;
    private final String offenceWording;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String statementOfOffenceTitle;
    private final String statementOfOffenceTitleWelsh;
    private final int count;
    private final UUID offenceDefinitionId;

    public OffenceData(final UUID offenceId, final String offenceCode,
                       final LocalDate startDate, final LocalDate endDate, final String statementOfOffenceTitle,
                       final String statementOfOffenceTitleWelsh, final String offenceWording,
                       final int count, UUID offenceDefinitionId) {

        this.endDate = endDate;
        this.offenceCode = offenceCode;
        this.offenceId = offenceId;
        this.randomOffenceId = UUID.randomUUID();
        this.startDate = startDate;
        this.offenceWording = offenceWording;
        this.statementOfOffenceTitleWelsh = statementOfOffenceTitleWelsh;
        this.statementOfOffenceTitle = statementOfOffenceTitle;
        this.count = count;
        this.offenceDefinitionId = offenceDefinitionId;
    }

    public UUID getOffenceId() { return offenceId; }

    public String getOffenceWording() {
        return offenceWording;
    }

    public UUID getRandomOffenceId() { return randomOffenceId; }

    public String getNewOffenceCode() { return offenceCode + "-new"; }

    public String getOffenceCode() { return offenceCode; }

    public String getChangedOffenceCode() { return offenceCode + "-changed"; }

    public LocalDate getStartDate() { return startDate; }

    public LocalDate getEndDate() { return endDate; }

    public String getStatementOfOffenceTitle() { return statementOfOffenceTitle; }

    public String getAddedStatementOfOffenceTitle() { return statementOfOffenceTitle + "-added"; }

    public String getChangedStatementOfOffenceTitle() { return statementOfOffenceTitle + "-changed"; }

    public String getStatementOfOffenceTitleWelsh() { return statementOfOffenceTitleWelsh; }

    public int getCount() { return count;  }

    public UUID getOffenceDefinitionId() { return offenceDefinitionId; }
}
