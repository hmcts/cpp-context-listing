package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.events.CivilOffence;
import uk.gov.justice.listing.events.ReportingRestriction;
import uk.gov.justice.listing.events.SeedingHearing;

public class Offence implements Serializable {
  private static final long serialVersionUID = -2619644771746664968L;

  private final CivilOffence civilOffence;

  private final Integer count;

  private final CustodyTimeLimit custodyTimeLimit;

  private final String endDate;

  private final UUID id;

  private final String indictmentParticular;

  private final Boolean isDiscontinued;

  private final Boolean isIntroduceAfterInitialProceedings;

  private final LaaReference laaApplnReference;

  private final String offenceCode;

  private final String offenceWording;

  private final Integer orderIndex;

  private final Boolean proceedingsConcluded;

  private final List<ReportingRestriction> reportingRestrictions;

  private final SeedingHearing seedingHearing;

  private final String startDate;

  private final StatementOfOffence statementOfOffence;

  @JsonCreator
  public Offence(final CivilOffence civilOffence, final Integer count, final CustodyTimeLimit custodyTimeLimit, final String endDate, final UUID id, final String indictmentParticular, final Boolean isDiscontinued, final Boolean isIntroduceAfterInitialProceedings, final LaaReference laaApplnReference, final String offenceCode, final String offenceWording, final Integer orderIndex, final Boolean proceedingsConcluded, final List<ReportingRestriction> reportingRestrictions, final SeedingHearing seedingHearing, final String startDate, final StatementOfOffence statementOfOffence) {
    this.civilOffence = civilOffence;
    this.count = count;
    this.custodyTimeLimit = custodyTimeLimit;
    this.endDate = endDate;
    this.id = id;
    this.indictmentParticular = indictmentParticular;
    this.isDiscontinued = isDiscontinued;
    this.isIntroduceAfterInitialProceedings = isIntroduceAfterInitialProceedings;
    this.laaApplnReference = laaApplnReference;
    this.offenceCode = offenceCode;
    this.offenceWording = offenceWording;
    this.orderIndex = orderIndex;
    this.proceedingsConcluded = proceedingsConcluded;
    this.reportingRestrictions = reportingRestrictions;
    this.seedingHearing = seedingHearing;
    this.startDate = startDate;
    this.statementOfOffence = statementOfOffence;
  }

  public CivilOffence getCivilOffence() {
    return civilOffence;
  }

  public Integer getCount() {
    return count;
  }

  public CustodyTimeLimit getCustodyTimeLimit() {
    return custodyTimeLimit;
  }

  public String getEndDate() {
    return endDate;
  }

  public UUID getId() {
    return id;
  }

  public String getIndictmentParticular() {
    return indictmentParticular;
  }

  public Boolean getIsDiscontinued() {
    return isDiscontinued;
  }

  public Boolean getIsIntroduceAfterInitialProceedings() {
    return isIntroduceAfterInitialProceedings;
  }

  public LaaReference getLaaApplnReference() {
    return laaApplnReference;
  }

  public String getOffenceCode() {
    return offenceCode;
  }

  public String getOffenceWording() {
    return offenceWording;
  }

  public Integer getOrderIndex() {
    return orderIndex;
  }

  public Boolean getProceedingsConcluded() {
    return proceedingsConcluded;
  }

  public List<ReportingRestriction> getReportingRestrictions() {
    return reportingRestrictions;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public String getStartDate() {
    return startDate;
  }

  public StatementOfOffence getStatementOfOffence() {
    return statementOfOffence;
  }

  public static Builder offence() {
    return new uk.gov.justice.listing.commands.Offence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.Offence that = (uk.gov.justice.listing.commands.Offence) obj;

    return java.util.Objects.equals(this.civilOffence, that.civilOffence) &&
    java.util.Objects.equals(this.count, that.count) &&
    java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.indictmentParticular, that.indictmentParticular) &&
    java.util.Objects.equals(this.isDiscontinued, that.isDiscontinued) &&
    java.util.Objects.equals(this.isIntroduceAfterInitialProceedings, that.isIntroduceAfterInitialProceedings) &&
    java.util.Objects.equals(this.laaApplnReference, that.laaApplnReference) &&
    java.util.Objects.equals(this.offenceCode, that.offenceCode) &&
    java.util.Objects.equals(this.offenceWording, that.offenceWording) &&
    java.util.Objects.equals(this.orderIndex, that.orderIndex) &&
    java.util.Objects.equals(this.proceedingsConcluded, that.proceedingsConcluded) &&
    java.util.Objects.equals(this.reportingRestrictions, that.reportingRestrictions) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.statementOfOffence, that.statementOfOffence);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(civilOffence, count, custodyTimeLimit, endDate, id, indictmentParticular, isDiscontinued, isIntroduceAfterInitialProceedings, laaApplnReference, offenceCode, offenceWording, orderIndex, proceedingsConcluded, reportingRestrictions, seedingHearing, startDate, statementOfOffence);}

  public static class Builder {
    private CivilOffence civilOffence;

    private Integer count;

    private CustodyTimeLimit custodyTimeLimit;

    private String endDate;

    private UUID id;

    private String indictmentParticular;

    private Boolean isDiscontinued;

    private Boolean isIntroduceAfterInitialProceedings;

    private LaaReference laaApplnReference;

    private String offenceCode;

    private String offenceWording;

    private Integer orderIndex;

    private Boolean proceedingsConcluded;

    private List<ReportingRestriction> reportingRestrictions;

    private SeedingHearing seedingHearing;

    private String startDate;

    private StatementOfOffence statementOfOffence;

    public Builder withCivilOffence(final CivilOffence civilOffence) {
      this.civilOffence = civilOffence;
      return this;
    }

    public Builder withCount(final Integer count) {
      this.count = count;
      return this;
    }

    public Builder withCustodyTimeLimit(final CustodyTimeLimit custodyTimeLimit) {
      this.custodyTimeLimit = custodyTimeLimit;
      return this;
    }

    public Builder withEndDate(final String endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIndictmentParticular(final String indictmentParticular) {
      this.indictmentParticular = indictmentParticular;
      return this;
    }

    public Builder withIsDiscontinued(final Boolean isDiscontinued) {
      this.isDiscontinued = isDiscontinued;
      return this;
    }

    public Builder withIsIntroduceAfterInitialProceedings(final Boolean isIntroduceAfterInitialProceedings) {
      this.isIntroduceAfterInitialProceedings = isIntroduceAfterInitialProceedings;
      return this;
    }

    public Builder withLaaApplnReference(final LaaReference laaApplnReference) {
      this.laaApplnReference = laaApplnReference;
      return this;
    }

    public Builder withOffenceCode(final String offenceCode) {
      this.offenceCode = offenceCode;
      return this;
    }

    public Builder withOffenceWording(final String offenceWording) {
      this.offenceWording = offenceWording;
      return this;
    }

    public Builder withOrderIndex(final Integer orderIndex) {
      this.orderIndex = orderIndex;
      return this;
    }

    public Builder withProceedingsConcluded(final Boolean proceedingsConcluded) {
      this.proceedingsConcluded = proceedingsConcluded;
      return this;
    }

    public Builder withReportingRestrictions(final List<ReportingRestriction> reportingRestrictions) {
      this.reportingRestrictions = reportingRestrictions;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withStartDate(final String startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withStatementOfOffence(final StatementOfOffence statementOfOffence) {
      this.statementOfOffence = statementOfOffence;
      return this;
    }

    public Builder withValuesFrom(final Offence offence) {
      this.civilOffence = offence.getCivilOffence();
      this.count = offence.getCount();
      this.custodyTimeLimit = offence.getCustodyTimeLimit();
      this.endDate = offence.getEndDate();
      this.id = offence.getId();
      this.indictmentParticular = offence.getIndictmentParticular();
      this.isDiscontinued = offence.getIsDiscontinued();
      this.isIntroduceAfterInitialProceedings = offence.getIsIntroduceAfterInitialProceedings();
      this.laaApplnReference = offence.getLaaApplnReference();
      this.offenceCode = offence.getOffenceCode();
      this.offenceWording = offence.getOffenceWording();
      this.orderIndex = offence.getOrderIndex();
      this.proceedingsConcluded = offence.getProceedingsConcluded();
      this.reportingRestrictions = offence.getReportingRestrictions();
      this.seedingHearing = offence.getSeedingHearing();
      this.startDate = offence.getStartDate();
      this.statementOfOffence = offence.getStatementOfOffence();
      return this;
    }

    public Offence build() {
      return new uk.gov.justice.listing.commands.Offence(civilOffence, count, custodyTimeLimit, endDate, id, indictmentParticular, isDiscontinued, isIntroduceAfterInitialProceedings, laaApplnReference, offenceCode, offenceWording, orderIndex, proceedingsConcluded, reportingRestrictions, seedingHearing, startDate, statementOfOffence);
    }
  }
}
