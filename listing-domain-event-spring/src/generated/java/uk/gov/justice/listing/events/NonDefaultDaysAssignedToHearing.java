package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class NonDefaultDaysAssignedToHearing implements Serializable {
  private static final long serialVersionUID = 3346433370104317500L;

  private final UUID hearingId;

  private final Boolean isPublicEvent;

  private final List<NonDefaultDay> nonDefaultDays;

  @JsonCreator
  public NonDefaultDaysAssignedToHearing(final UUID hearingId, final Boolean isPublicEvent, final List<NonDefaultDay> nonDefaultDays) {
    this.hearingId = hearingId;
    this.isPublicEvent = isPublicEvent;
    this.nonDefaultDays = nonDefaultDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public Boolean getIsPublicEvent() {
    return isPublicEvent;
  }

  public List<NonDefaultDay> getNonDefaultDays() {
    return nonDefaultDays;
  }

  public static Builder nonDefaultDaysAssignedToHearing() {
    return new uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing that = (uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.isPublicEvent, that.isPublicEvent) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, isPublicEvent, nonDefaultDays);}

  public static class Builder {
    private UUID hearingId;

    private Boolean isPublicEvent;

    private List<NonDefaultDay> nonDefaultDays;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withIsPublicEvent(final Boolean isPublicEvent) {
      this.isPublicEvent = isPublicEvent;
      return this;
    }

    public Builder withNonDefaultDays(final List<NonDefaultDay> nonDefaultDays) {
      this.nonDefaultDays = nonDefaultDays;
      return this;
    }

    public Builder withValuesFrom(final NonDefaultDaysAssignedToHearing nonDefaultDaysAssignedToHearing) {
      this.hearingId = nonDefaultDaysAssignedToHearing.getHearingId();
      this.isPublicEvent = nonDefaultDaysAssignedToHearing.getIsPublicEvent();
      this.nonDefaultDays = nonDefaultDaysAssignedToHearing.getNonDefaultDays();
      return this;
    }

    public NonDefaultDaysAssignedToHearing build() {
      return new uk.gov.justice.listing.events.NonDefaultDaysAssignedToHearing(hearingId, isPublicEvent, nonDefaultDays);
    }
  }
}
