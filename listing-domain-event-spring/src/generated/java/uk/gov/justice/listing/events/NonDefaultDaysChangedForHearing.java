package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.non-default-days-changed-for-hearing")
public class NonDefaultDaysChangedForHearing implements Serializable {
  private static final long serialVersionUID = -397535061145360238L;

  private final UUID hearingId;

  private final Boolean isPublicEvent;

  private final List<NonDefaultDay> nonDefaultDays;

  @JsonCreator
  public NonDefaultDaysChangedForHearing(final UUID hearingId, final Boolean isPublicEvent, final List<NonDefaultDay> nonDefaultDays) {
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

  public static Builder nonDefaultDaysChangedForHearing() {
    return new uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing that = (uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing) obj;

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

    public Builder withValuesFrom(final NonDefaultDaysChangedForHearing nonDefaultDaysChangedForHearing) {
      this.hearingId = nonDefaultDaysChangedForHearing.getHearingId();
      this.isPublicEvent = nonDefaultDaysChangedForHearing.getIsPublicEvent();
      this.nonDefaultDays = nonDefaultDaysChangedForHearing.getNonDefaultDays();
      return this;
    }

    public NonDefaultDaysChangedForHearing build() {
      return new uk.gov.justice.listing.events.NonDefaultDaysChangedForHearing(hearingId, isPublicEvent, nonDefaultDays);
    }
  }
}
