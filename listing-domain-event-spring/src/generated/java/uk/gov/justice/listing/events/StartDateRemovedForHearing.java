package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.start-date-removed-for-hearing")
public class StartDateRemovedForHearing implements Serializable {
  private static final long serialVersionUID = -5916793171023091435L;

  private final UUID hearingId;

  private final Boolean unscheduled;

  @JsonCreator
  public StartDateRemovedForHearing(final UUID hearingId, final Boolean unscheduled) {
    this.hearingId = hearingId;
    this.unscheduled = unscheduled;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public Boolean getUnscheduled() {
    return unscheduled;
  }

  public static Builder startDateRemovedForHearing() {
    return new uk.gov.justice.listing.events.StartDateRemovedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.StartDateRemovedForHearing that = (uk.gov.justice.listing.events.StartDateRemovedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.unscheduled, that.unscheduled);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, unscheduled);}

  public static class Builder {
    private UUID hearingId;

    private Boolean unscheduled;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withUnscheduled(final Boolean unscheduled) {
      this.unscheduled = unscheduled;
      return this;
    }

    public Builder withValuesFrom(final StartDateRemovedForHearing startDateRemovedForHearing) {
      this.hearingId = startDateRemovedForHearing.getHearingId();
      this.unscheduled = startDateRemovedForHearing.getUnscheduled();
      return this;
    }

    public StartDateRemovedForHearing build() {
      return new uk.gov.justice.listing.events.StartDateRemovedForHearing(hearingId, unscheduled);
    }
  }
}
