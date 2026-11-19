package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class HearingDeleted implements Serializable {
  private static final long serialVersionUID = 226094610856485753L;

  private final UUID hearingIdToBeDeleted;

  @JsonCreator
  public HearingDeleted(final UUID hearingIdToBeDeleted) {
    this.hearingIdToBeDeleted = hearingIdToBeDeleted;
  }

  public UUID getHearingIdToBeDeleted() {
    return hearingIdToBeDeleted;
  }

  public static Builder hearingDeleted() {
    return new uk.gov.justice.listing.events.HearingDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingDeleted that = (uk.gov.justice.listing.events.HearingDeleted) obj;

    return java.util.Objects.equals(this.hearingIdToBeDeleted, that.hearingIdToBeDeleted);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIdToBeDeleted);}

  public static class Builder {
    private UUID hearingIdToBeDeleted;

    public Builder withHearingIdToBeDeleted(final UUID hearingIdToBeDeleted) {
      this.hearingIdToBeDeleted = hearingIdToBeDeleted;
      return this;
    }

    public Builder withValuesFrom(final HearingDeleted hearingDeleted) {
      this.hearingIdToBeDeleted = hearingDeleted.getHearingIdToBeDeleted();
      return this;
    }

    public HearingDeleted build() {
      return new uk.gov.justice.listing.events.HearingDeleted(hearingIdToBeDeleted);
    }
  }
}
