package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class HearingListed implements Serializable {
  private static final long serialVersionUID = -7075021598336332236L;

  private final List<CaseUrns> caseUrns;

  private final UUID hearingId;

  private final String hearingType;

  @JsonCreator
  public HearingListed(final List<CaseUrns> caseUrns, final UUID hearingId, final String hearingType) {
    this.caseUrns = caseUrns;
    this.hearingId = hearingId;
    this.hearingType = hearingType;
  }

  public List<CaseUrns> getCaseUrns() {
    return caseUrns;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getHearingType() {
    return hearingType;
  }

  public static Builder hearingListed() {
    return new uk.gov.justice.listing.courts.HearingListed.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.HearingListed that = (uk.gov.justice.listing.courts.HearingListed) obj;

    return java.util.Objects.equals(this.caseUrns, that.caseUrns) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.hearingType, that.hearingType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseUrns, hearingId, hearingType);}

  public static class Builder {
    private List<CaseUrns> caseUrns;

    private UUID hearingId;

    private String hearingType;

    public Builder withCaseUrns(final List<CaseUrns> caseUrns) {
      this.caseUrns = caseUrns;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withHearingType(final String hearingType) {
      this.hearingType = hearingType;
      return this;
    }

    public Builder withValuesFrom(final HearingListed hearingListed) {
      this.caseUrns = hearingListed.getCaseUrns();
      this.hearingId = hearingListed.getHearingId();
      this.hearingType = hearingListed.getHearingType();
      return this;
    }

    public HearingListed build() {
      return new uk.gov.justice.listing.courts.HearingListed(caseUrns, hearingId, hearingType);
    }
  }
}
