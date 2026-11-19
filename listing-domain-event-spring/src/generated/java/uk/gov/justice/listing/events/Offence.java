package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.CivilOffence;

public class Offence implements Serializable {
  private static final long serialVersionUID = -4253264836934810101L;

  private final CivilOffence civilOffence;

  private final CommittingCourt committingCourt;

  private final Integer count;

  private final String endDate;

  private final UUID id;

  private final String indictmentParticular;

  private final LaaReference laaApplnReference;

  private final String laidDate;

  private final Integer listingNumber;

  private final String offenceCode;

  private final String offenceWording;

  private final Integer orderIndex;

  private final List<ReportingRestriction> reportingRestrictions;

  private final Boolean restrictFromCourtList;

  private final SeedingHearing seedingHearing;

  private final Boolean shadowListed;

  private final String startDate;

  private final StatementOfOffence statementOfOffence;

  @JsonCreator
  public Offence(final CivilOffence civilOffence, final CommittingCourt committingCourt, final Integer count, final String endDate, final UUID id, final String indictmentParticular, final LaaReference laaApplnReference, final String laidDate, final Integer listingNumber, final String offenceCode, final String offenceWording, final Integer orderIndex, final List<ReportingRestriction> reportingRestrictions, final Boolean restrictFromCourtList, final SeedingHearing seedingHearing, final Boolean shadowListed, final String startDate, final StatementOfOffence statementOfOffence) {
    this.civilOffence = civilOffence;
    this.committingCourt = committingCourt;
    this.count = count;
    this.endDate = endDate;
    this.id = id;
    this.indictmentParticular = indictmentParticular;
    this.laaApplnReference = laaApplnReference;
    this.laidDate = laidDate;
    this.listingNumber = listingNumber;
    this.offenceCode = offenceCode;
    this.offenceWording = offenceWording;
    this.orderIndex = orderIndex;
    this.reportingRestrictions = reportingRestrictions;
    this.restrictFromCourtList = restrictFromCourtList;
    this.seedingHearing = seedingHearing;
    this.shadowListed = shadowListed;
    this.startDate = startDate;
    this.statementOfOffence = statementOfOffence;
  }

  public CivilOffence getCivilOffence() {
    return civilOffence;
  }

  public CommittingCourt getCommittingCourt() {
    return committingCourt;
  }

  public Integer getCount() {
    return count;
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

  public LaaReference getLaaApplnReference() {
    return laaApplnReference;
  }

  public String getLaidDate() {
    return laidDate;
  }

  public Integer getListingNumber() {
    return listingNumber;
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

  public List<ReportingRestriction> getReportingRestrictions() {
    return reportingRestrictions;
  }

  public Boolean getRestrictFromCourtList() {
    return restrictFromCourtList;
  }

  public SeedingHearing getSeedingHearing() {
    return seedingHearing;
  }

  public Boolean getShadowListed() {
    return shadowListed;
  }

  public String getStartDate() {
    return startDate;
  }

  public StatementOfOffence getStatementOfOffence() {
    return statementOfOffence;
  }

  public static Builder offence() {
    return new uk.gov.justice.listing.events.Offence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.Offence that = (uk.gov.justice.listing.events.Offence) obj;

    return java.util.Objects.equals(this.civilOffence, that.civilOffence) &&
    java.util.Objects.equals(this.committingCourt, that.committingCourt) &&
    java.util.Objects.equals(this.count, that.count) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.indictmentParticular, that.indictmentParticular) &&
    java.util.Objects.equals(this.laaApplnReference, that.laaApplnReference) &&
    java.util.Objects.equals(this.laidDate, that.laidDate) &&
    java.util.Objects.equals(this.listingNumber, that.listingNumber) &&
    java.util.Objects.equals(this.offenceCode, that.offenceCode) &&
    java.util.Objects.equals(this.offenceWording, that.offenceWording) &&
    java.util.Objects.equals(this.orderIndex, that.orderIndex) &&
    java.util.Objects.equals(this.reportingRestrictions, that.reportingRestrictions) &&
    java.util.Objects.equals(this.restrictFromCourtList, that.restrictFromCourtList) &&
    java.util.Objects.equals(this.seedingHearing, that.seedingHearing) &&
    java.util.Objects.equals(this.shadowListed, that.shadowListed) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.statementOfOffence, that.statementOfOffence);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(civilOffence, committingCourt, count, endDate, id, indictmentParticular, laaApplnReference, laidDate, listingNumber, offenceCode, offenceWording, orderIndex, reportingRestrictions, restrictFromCourtList, seedingHearing, shadowListed, startDate, statementOfOffence);}

  public static class Builder {
    private CivilOffence civilOffence;

    private CommittingCourt committingCourt;

    private Integer count;

    private String endDate;

    private UUID id;

    private String indictmentParticular;

    private LaaReference laaApplnReference;

    private String laidDate;

    private Integer listingNumber;

    private String offenceCode;

    private String offenceWording;

    private Integer orderIndex;

    private List<ReportingRestriction> reportingRestrictions;

    private Boolean restrictFromCourtList;

    private SeedingHearing seedingHearing;

    private Boolean shadowListed;

    private String startDate;

    private StatementOfOffence statementOfOffence;

    public Builder withCivilOffence(final CivilOffence civilOffence) {
      this.civilOffence = civilOffence;
      return this;
    }

    public Builder withCommittingCourt(final CommittingCourt committingCourt) {
      this.committingCourt = committingCourt;
      return this;
    }

    public Builder withCount(final Integer count) {
      this.count = count;
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

    public Builder withLaaApplnReference(final LaaReference laaApplnReference) {
      this.laaApplnReference = laaApplnReference;
      return this;
    }

    public Builder withLaidDate(final String laidDate) {
      this.laidDate = laidDate;
      return this;
    }

    public Builder withListingNumber(final Integer listingNumber) {
      this.listingNumber = listingNumber;
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

    public Builder withReportingRestrictions(final List<ReportingRestriction> reportingRestrictions) {
      this.reportingRestrictions = reportingRestrictions;
      return this;
    }

    public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList) {
      this.restrictFromCourtList = restrictFromCourtList;
      return this;
    }

    public Builder withSeedingHearing(final SeedingHearing seedingHearing) {
      this.seedingHearing = seedingHearing;
      return this;
    }

    public Builder withShadowListed(final Boolean shadowListed) {
      this.shadowListed = shadowListed;
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
      this.committingCourt = offence.getCommittingCourt();
      this.count = offence.getCount();
      this.endDate = offence.getEndDate();
      this.id = offence.getId();
      this.indictmentParticular = offence.getIndictmentParticular();
      this.laaApplnReference = offence.getLaaApplnReference();
      this.laidDate = offence.getLaidDate();
      this.listingNumber = offence.getListingNumber();
      this.offenceCode = offence.getOffenceCode();
      this.offenceWording = offence.getOffenceWording();
      this.orderIndex = offence.getOrderIndex();
      this.reportingRestrictions = offence.getReportingRestrictions();
      this.restrictFromCourtList = offence.getRestrictFromCourtList();
      this.seedingHearing = offence.getSeedingHearing();
      this.shadowListed = offence.getShadowListed();
      this.startDate = offence.getStartDate();
      this.statementOfOffence = offence.getStatementOfOffence();
      return this;
    }

    public Offence build() {
      return new uk.gov.justice.listing.events.Offence(civilOffence, committingCourt, count, endDate, id, indictmentParticular, laaApplnReference, laidDate, listingNumber, offenceCode, offenceWording, orderIndex, reportingRestrictions, restrictFromCourtList, seedingHearing, shadowListed, startDate, statementOfOffence);
    }
  }
}
