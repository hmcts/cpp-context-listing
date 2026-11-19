package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class HearingDeleted implements Serializable {
  private static final long serialVersionUID = -5144608019697537573L;

  private final UUID hearingId;

  @JsonCreator
  public HearingDeleted(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingDeleted() {
    return new uk.gov.justice.listing.courts.HearingDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.HearingDeleted that = (uk.gov.justice.listing.courts.HearingDeleted) obj;

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

    public Builder withValuesFrom(final HearingDeleted hearingDeleted) {
      this.hearingId = hearingDeleted.getHearingId();
      return this;
    }

    public HearingDeleted build() {
      return new uk.gov.justice.listing.courts.HearingDeleted(hearingId);
    }
  }
}
