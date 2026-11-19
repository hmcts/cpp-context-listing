package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.jurisdiction-changed-for-hearing")
public class JurisdictionChangedForHearing implements Serializable {
  private static final long serialVersionUID = -6479895282480777759L;

  private final UUID hearingId;

  private final JurisdictionType jurisdictionType;

  @JsonCreator
  public JurisdictionChangedForHearing(final UUID hearingId, final JurisdictionType jurisdictionType) {
    this.hearingId = hearingId;
    this.jurisdictionType = jurisdictionType;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public static Builder jurisdictionChangedForHearing() {
    return new uk.gov.justice.listing.events.JurisdictionChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.JurisdictionChangedForHearing that = (uk.gov.justice.listing.events.JurisdictionChangedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, jurisdictionType);}

  public static class Builder {
    private UUID hearingId;

    private JurisdictionType jurisdictionType;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
      this.jurisdictionType = jurisdictionType;
      return this;
    }

    public Builder withValuesFrom(final JurisdictionChangedForHearing jurisdictionChangedForHearing) {
      this.hearingId = jurisdictionChangedForHearing.getHearingId();
      this.jurisdictionType = jurisdictionChangedForHearing.getJurisdictionType();
      return this;
    }

    public JurisdictionChangedForHearing build() {
      return new uk.gov.justice.listing.events.JurisdictionChangedForHearing(hearingId, jurisdictionType);
    }
  }
}
