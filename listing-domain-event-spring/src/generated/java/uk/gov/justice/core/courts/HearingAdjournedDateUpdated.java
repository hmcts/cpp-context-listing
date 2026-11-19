package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class HearingAdjournedDateUpdated implements Serializable {
  private static final long serialVersionUID = 183866708891697742L;

  private final UUID hearingId;

  @JsonCreator
  public HearingAdjournedDateUpdated(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingAdjournedDateUpdated() {
    return new uk.gov.justice.core.courts.HearingAdjournedDateUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.HearingAdjournedDateUpdated that = (uk.gov.justice.core.courts.HearingAdjournedDateUpdated) obj;

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

    public Builder withValuesFrom(final HearingAdjournedDateUpdated hearingAdjournedDateUpdated) {
      this.hearingId = hearingAdjournedDateUpdated.getHearingId();
      return this;
    }

    public HearingAdjournedDateUpdated build() {
      return new uk.gov.justice.core.courts.HearingAdjournedDateUpdated(hearingId);
    }
  }
}
