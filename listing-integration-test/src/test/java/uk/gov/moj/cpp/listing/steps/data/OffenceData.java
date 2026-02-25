package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.moj.cpp.listing.steps.CivilOffenceData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class OffenceData {

    private final UUID offenceId;
    private final UUID randomOffenceId;
    private String offenceCode;
    private String offenceWording;
    private LocalDate startDate;
    private LocalDate endDate;
    private String statementOfOffenceTitle;
    private String statementOfOffenceTitleWelsh;
    private Integer count;
    private Integer orderIndex;
    private String offenceLegislation;
    private UUID offenceDefinitionId;
    private Optional<CustodyTimeLimit> custodyTimeLimit;
    private Optional<LaaReferenceData> laaApplnReference;
    private LocalDate laidDate;
    private Optional<Boolean> shadowListed;
    private List<ReportingRestrictionData> reportingRestriction;
    private String indictmentParticular;
    private CivilOffenceData civilOffenceData;

    private Integer listingNumber;
    private String maxPenalty;
    private String alcoholReadingAmount;
    private String convictedOn;
    private String adjournedDate;
    private String adjournedHearingType;

    public OffenceData(final UUID offenceId, final String offenceCode,
                       final LocalDate startDate, final LocalDate endDate, final String statementOfOffenceTitle,
                       final String statementOfOffenceTitleWelsh, final String offenceWording,
                       final Integer count, final Integer orderIndex, final String offenceLegislation, UUID offenceDefinitionId, Optional<CustodyTimeLimit> custodyTimeLimit,
                       final Optional<LaaReferenceData> laaApplnReference, final LocalDate laidDate,
                       final Optional<Boolean> shadowListed, final List<ReportingRestrictionData> reportingRestrictionData, final String indictmentParticular, final CivilOffenceData civilOffenceData) {

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
        this.orderIndex = orderIndex;
        this.offenceLegislation = offenceLegislation;
        this.offenceDefinitionId = offenceDefinitionId;
        this.custodyTimeLimit = custodyTimeLimit;
        this.laidDate = laidDate;
        this.shadowListed = shadowListed;
        this.reportingRestriction = reportingRestrictionData;
        this.indictmentParticular = indictmentParticular;
        this.civilOffenceData = civilOffenceData;
    }

    public Optional<LaaReferenceData> getLaaApplnReference() {
        return laaApplnReference;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public UUID getRandomOffenceId() {
        return randomOffenceId;
    }

    public String getNewOffenceCode() {
        return offenceCode + "-new";
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getChangedOffenceCode() {
        return offenceCode + "-changed";
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getStatementOfOffenceTitle() {
        return statementOfOffenceTitle;
    }

    public String getAddedStatementOfOffenceTitle() {
        return statementOfOffenceTitle + "-added";
    }

    public String getChangedStatementOfOffenceTitle() {
        return statementOfOffenceTitle + "-changed";
    }

    public String getStatementOfOffenceTitleWelsh() {
        return statementOfOffenceTitleWelsh;
    }

    public Integer getCount() {
        return count;
    }

    public UUID getOffenceDefinitionId() {
        return offenceDefinitionId;
    }

    public Optional<CustodyTimeLimit> getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public LocalDate getLaidDate() {
        return laidDate;
    }

    public Optional<Boolean> getShadowListed() {
        return shadowListed;
    }

    public void setShadowListed(Optional<Boolean> shadowListed) {
        this.shadowListed = shadowListed;
    }

    public List<ReportingRestrictionData> getReportingRestrictionDataList() {
        return reportingRestriction;
    }

    public void setReportingRestrictionDataList(final List<ReportingRestrictionData> reportingRestrictionDataList) {
        this.reportingRestriction = reportingRestrictionDataList;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public String getOffenceLegislation() {
        return offenceLegislation;
    }

    public String getIndictmentParticular() {
        return indictmentParticular;
    }

    public CivilOffenceData getCivilOffenceData() {
        return civilOffenceData;
    }

    public void setCivilOffenceData(final CivilOffenceData civilOffenceData) {
        this.civilOffenceData = civilOffenceData;
    }

    public Integer getListingNumber() {
        return listingNumber;
    }

    public void setListingNumber(final Integer listingNumber) {
        this.listingNumber = listingNumber;
    }

    public String getMaxPenalty() {
        return maxPenalty;
    }

    public void setMaxPenalty(final String maxPenalty) {
        this.maxPenalty = maxPenalty;
    }

    public String getAlcoholReadingAmount() {
        return alcoholReadingAmount;
    }

    public void setAlcoholReadingAmount(final String alcoholReadingAmount) {
        this.alcoholReadingAmount = alcoholReadingAmount;
    }

    public String getConvictedOn() {
        return convictedOn;
    }

    public void setConvictedOn(final String convictedOn) {
        this.convictedOn = convictedOn;
    }

    public String getAdjournedDate() {
        return adjournedDate;
    }

    public void setAdjournedDate(final String adjournedDate) {
        this.adjournedDate = adjournedDate;
    }

    public String getAdjournedHearingType() {
        return adjournedHearingType;
    }

    public void setAdjournedHearingType(final String adjournedHearingType) {
        this.adjournedHearingType = adjournedHearingType;
    }

    public void copyOffenceData(OffenceData offenceData) {
        this.count = offenceData.getCount();
        this.custodyTimeLimit = offenceData.getCustodyTimeLimit();
        this.offenceCode = offenceData.getOffenceCode();
        this.offenceDefinitionId = offenceData.getOffenceDefinitionId();
        this.endDate = offenceData.getEndDate();
        this.startDate = offenceData.getStartDate();
        this.custodyTimeLimit = offenceData.getCustodyTimeLimit();
        this.indictmentParticular = offenceData.getIndictmentParticular();
        this.laaApplnReference = offenceData.getLaaApplnReference();
        this.laidDate = offenceData.getLaidDate();
        this.offenceLegislation = offenceData.getOffenceLegislation();
        this.offenceWording = offenceData.getOffenceWording();
        this.reportingRestriction = offenceData.getReportingRestrictionDataList();
        this.shadowListed = offenceData.getShadowListed();
        this.statementOfOffenceTitle = offenceData.getStatementOfOffenceTitle();
        this.statementOfOffenceTitleWelsh = offenceData.getStatementOfOffenceTitleWelsh();
        this.civilOffenceData = offenceData.getCivilOffenceData();
        this.listingNumber = offenceData.getListingNumber();
        this.maxPenalty = offenceData.getMaxPenalty();
        this.alcoholReadingAmount = offenceData.getAlcoholReadingAmount();
        this.convictedOn = offenceData.getConvictedOn();
        this.adjournedDate = offenceData.getAdjournedDate();
        this.adjournedHearingType = offenceData.getAdjournedHearingType();
    }
}
