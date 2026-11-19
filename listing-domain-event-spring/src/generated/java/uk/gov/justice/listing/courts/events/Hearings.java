package uk.gov.justice.listing.courts.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;

public class Hearings implements Serializable {
  private static final long serialVersionUID = 3881700311939688169L;

  private final CaseIdentifier caseIdentifier;

  private final UUID committingCourtCentreId;

  private final List<Defendant> defendants;

  private final String endTime;

  private final Boolean hasVideoLink;

  private final HearingType hearingType;

  private final Boolean restrictFromCourtList;

  private final String startTime;

  private final Boolean weekCommencing;

  @JsonCreator
  public Hearings(final CaseIdentifier caseIdentifier, final UUID committingCourtCentreId, final List<Defendant> defendants, final String endTime, final Boolean hasVideoLink, final HearingType hearingType, final Boolean restrictFromCourtList, final String startTime, final Boolean weekCommencing) {
    this.caseIdentifier = caseIdentifier;
    this.committingCourtCentreId = committingCourtCentreId;
    this.defendants = defendants;
    this.endTime = endTime;
    this.hasVideoLink = hasVideoLink;
    this.hearingType = hearingType;
    this.restrictFromCourtList = restrictFromCourtList;
    this.startTime = startTime;
    this.weekCommencing = weekCommencing;
  }

  public CaseIdentifier getCaseIdentifier() {
    return caseIdentifier;
  }

  public UUID getCommittingCourtCentreId() {
    return committingCourtCentreId;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public String getEndTime() {
    return endTime;
  }

  public Boolean getHasVideoLink() {
    return hasVideoLink;
  }

  public HearingType getHearingType() {
    return hearingType;
  }

  public Boolean getRestrictFromCourtList() {
    return restrictFromCourtList;
  }

  public String getStartTime() {
    return startTime;
  }

  public Boolean getWeekCommencing() {
    return weekCommencing;
  }

  public static Builder hearings() {
    return new uk.gov.justice.listing.courts.events.Hearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.events.Hearings that = (uk.gov.justice.listing.courts.events.Hearings) obj;

    return java.util.Objects.equals(this.caseIdentifier, that.caseIdentifier) &&
    java.util.Objects.equals(this.committingCourtCentreId, that.committingCourtCentreId) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.endTime, that.endTime) &&
    java.util.Objects.equals(this.hasVideoLink, that.hasVideoLink) &&
    java.util.Objects.equals(this.hearingType, that.hearingType) &&
    java.util.Objects.equals(this.restrictFromCourtList, that.restrictFromCourtList) &&
    java.util.Objects.equals(this.startTime, that.startTime) &&
    java.util.Objects.equals(this.weekCommencing, that.weekCommencing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseIdentifier, committingCourtCentreId, defendants, endTime, hasVideoLink, hearingType, restrictFromCourtList, startTime, weekCommencing);}

  public static class Builder {
    private CaseIdentifier caseIdentifier;

    private UUID committingCourtCentreId;

    private List<Defendant> defendants;

    private String endTime;

    private Boolean hasVideoLink;

    private HearingType hearingType;

    private Boolean restrictFromCourtList;

    private String startTime;

    private Boolean weekCommencing;

    public Builder withCaseIdentifier(final CaseIdentifier caseIdentifier) {
      this.caseIdentifier = caseIdentifier;
      return this;
    }

    public Builder withCommittingCourtCentreId(final UUID committingCourtCentreId) {
      this.committingCourtCentreId = committingCourtCentreId;
      return this;
    }

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withEndTime(final String endTime) {
      this.endTime = endTime;
      return this;
    }

    public Builder withHasVideoLink(final Boolean hasVideoLink) {
      this.hasVideoLink = hasVideoLink;
      return this;
    }

    public Builder withHearingType(final HearingType hearingType) {
      this.hearingType = hearingType;
      return this;
    }

    public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList) {
      this.restrictFromCourtList = restrictFromCourtList;
      return this;
    }

    public Builder withStartTime(final String startTime) {
      this.startTime = startTime;
      return this;
    }

    public Builder withWeekCommencing(final Boolean weekCommencing) {
      this.weekCommencing = weekCommencing;
      return this;
    }

    public Builder withValuesFrom(final Hearings hearings) {
      this.caseIdentifier = hearings.getCaseIdentifier();
      this.committingCourtCentreId = hearings.getCommittingCourtCentreId();
      this.defendants = hearings.getDefendants();
      this.endTime = hearings.getEndTime();
      this.hasVideoLink = hearings.getHasVideoLink();
      this.hearingType = hearings.getHearingType();
      this.restrictFromCourtList = hearings.getRestrictFromCourtList();
      this.startTime = hearings.getStartTime();
      this.weekCommencing = hearings.getWeekCommencing();
      return this;
    }

    public Hearings build() {
      return new uk.gov.justice.listing.courts.events.Hearings(caseIdentifier, committingCourtCentreId, defendants, endTime, hasVideoLink, hearingType, restrictFromCourtList, startTime, weekCommencing);
    }
  }
}
