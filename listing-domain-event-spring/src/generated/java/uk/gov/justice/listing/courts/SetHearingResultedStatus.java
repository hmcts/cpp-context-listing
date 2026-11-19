package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class SetHearingResultedStatus implements Serializable {
  private static final long serialVersionUID = -7806463680976493949L;

  private final UUID hearingId;

  @JsonCreator
  public SetHearingResultedStatus(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder setHearingResultedStatus() {
    return new uk.gov.justice.listing.courts.SetHearingResultedStatus.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.SetHearingResultedStatus that = (uk.gov.justice.listing.courts.SetHearingResultedStatus) obj;

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

    public Builder withValuesFrom(final SetHearingResultedStatus setHearingResultedStatus) {
      this.hearingId = setHearingResultedStatus.getHearingId();
      return this;
    }

    public SetHearingResultedStatus build() {
      return new uk.gov.justice.listing.courts.SetHearingResultedStatus(hearingId);
    }
  }
}
