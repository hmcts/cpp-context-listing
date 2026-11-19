package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class HearingChangesSaved implements Serializable {
  private static final long serialVersionUID = 4333010407170567521L;

  private final UUID hearingId;

  @JsonCreator
  public HearingChangesSaved(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingChangesSaved() {
    return new uk.gov.justice.listing.events.HearingChangesSaved.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingChangesSaved that = (uk.gov.justice.listing.events.HearingChangesSaved) obj;

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

    public Builder withValuesFrom(final HearingChangesSaved hearingChangesSaved) {
      this.hearingId = hearingChangesSaved.getHearingId();
      return this;
    }

    public HearingChangesSaved build() {
      return new uk.gov.justice.listing.events.HearingChangesSaved(hearingId);
    }
  }
}
