package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearing-unallocated-courtroom-removed")
public class HearingUnallocatedCourtroomRemoved implements Serializable {
  private static final long serialVersionUID = -2702642296609804889L;

  private final Integer estimatedMinutes;

  private final UUID hearingId;

  @JsonCreator
  public HearingUnallocatedCourtroomRemoved(final Integer estimatedMinutes, final UUID hearingId) {
    this.estimatedMinutes = estimatedMinutes;
    this.hearingId = hearingId;
  }

  public Integer getEstimatedMinutes() {
    return estimatedMinutes;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingUnallocatedCourtroomRemoved() {
    return new uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved that = (uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved) obj;

    return java.util.Objects.equals(this.estimatedMinutes, that.estimatedMinutes) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(estimatedMinutes, hearingId);}

  public static class Builder {
    private Integer estimatedMinutes;

    private UUID hearingId;

    public Builder withEstimatedMinutes(final Integer estimatedMinutes) {
      this.estimatedMinutes = estimatedMinutes;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final HearingUnallocatedCourtroomRemoved hearingUnallocatedCourtroomRemoved) {
      this.estimatedMinutes = hearingUnallocatedCourtroomRemoved.getEstimatedMinutes();
      this.hearingId = hearingUnallocatedCourtroomRemoved.getHearingId();
      return this;
    }

    public HearingUnallocatedCourtroomRemoved build() {
      return new uk.gov.justice.listing.events.HearingUnallocatedCourtroomRemoved(estimatedMinutes, hearingId);
    }
  }
}
