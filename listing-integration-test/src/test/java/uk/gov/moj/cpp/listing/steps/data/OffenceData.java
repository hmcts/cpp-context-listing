package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.core.courts.CustodyTimeLimit;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    private final Optional<CustodyTimeLimit> custodyTimeLimit;
    private final Optional<LaaReferenceData> laaApplnReference;
    private final LocalDate laidDate;
    private Optional<Boolean> shadowListed;
    private List<ReportingRestrictionData> reportingRestriction;

    public OffenceData(final UUID offenceId, final String offenceCode,
                       final LocalDate startDate, final LocalDate endDate, final String statementOfOffenceTitle,
                       final String statementOfOffenceTitleWelsh, final String offenceWording,
                       final int count, UUID offenceDefinitionId, Optional<CustodyTimeLimit> custodyTimeLimit,
                       final Optional<LaaReferenceData> laaApplnReference, final LocalDate laidDate,
                       final Optional<Boolean> shadowListed,final List<ReportingRestrictionData> reportingRestrictionData) {

        this.endDate = endDate;
        this.offenceCode = offenceCode;
        this.offenceId = offenceId;
        this.laaApplnReference = laaApplnReference;
        this.randomOffenceId = UUID.randomUUID();
        this.startDate = startDate;
        this.offenceWording = offenceWording;
        this.statementOfOffenceTitleWelsh = statementOfOffenceTitleWelsh;
        this.statementOfOffenceTitle = statementOfOffenceTitle;
        this.count = count;
        this.offenceDefinitionId = offenceDefinitionId;
        this.custodyTimeLimit =  custodyTimeLimit;
        this.laidDate = laidDate;
        this.shadowListed = shadowListed;
        this.reportingRestriction = reportingRestrictionData;
    }

    public Optional<LaaReferenceData> getLaaApplnReference() {
        return laaApplnReference;
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

    public Optional<CustodyTimeLimit> getCustodyTimeLimit() { return custodyTimeLimit; }

    public LocalDate getLaidDate() {
        return laidDate;
    }

    public Optional<Boolean> getShadowListed() { return shadowListed; }

    public void setShadowListed(Optional<Boolean> shadowListed) {
        this.shadowListed = shadowListed;
    }

    public List<ReportingRestrictionData> getReportingRestrictionDataList() {
        return reportingRestriction;
    }

    public void setReportingRestrictionDataList(final List<ReportingRestrictionData> reportingRestrictionDataList) {
        this.reportingRestriction = reportingRestrictionDataList;
    }
}
