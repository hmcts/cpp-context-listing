package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class ChangeNextHearingDay implements Serializable {
  private static final long serialVersionUID = 8225079060702106716L;

  private final UUID hearingId;

  @JsonCreator
  public ChangeNextHearingDay(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder changeNextHearingDay() {
    return new uk.gov.justice.listing.courts.ChangeNextHearingDay.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ChangeNextHearingDay that = (uk.gov.justice.listing.courts.ChangeNextHearingDay) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId);}

  public static class Builder {
    private UUID hearingId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final ChangeNextHearingDay changeNextHearingDay) {
      this.hearingId = changeNextHearingDay.getHearingId();
      return this;
    }

    public ChangeNextHearingDay build() {
      return new uk.gov.justice.listing.courts.ChangeNextHearingDay(hearingId);
    }
  }
}
