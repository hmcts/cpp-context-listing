package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.non-sitting-days-assigned-to-hearing")
public class NonSittingDaysAssignedToHearing implements Serializable {
  private static final long serialVersionUID = 4146292248671170350L;

  private final UUID hearingId;

  private final List<LocalDate> nonSittingDays;

  @JsonCreator
  public NonSittingDaysAssignedToHearing(final UUID hearingId, final List<LocalDate> nonSittingDays) {
    this.hearingId = hearingId;
    this.nonSittingDays = nonSittingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<LocalDate> getNonSittingDays() {
    return nonSittingDays;
  }

  public static Builder nonSittingDaysAssignedToHearing() {
    return new uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing that = (uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.nonSittingDays, that.nonSittingDays);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, nonSittingDays);}

  public static class Builder {
    private UUID hearingId;

    private List<LocalDate> nonSittingDays;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withNonSittingDays(final List<LocalDate> nonSittingDays) {
      this.nonSittingDays = nonSittingDays;
      return this;
    }

    public Builder withValuesFrom(final NonSittingDaysAssignedToHearing nonSittingDaysAssignedToHearing) {
      this.hearingId = nonSittingDaysAssignedToHearing.getHearingId();
      this.nonSittingDays = nonSittingDaysAssignedToHearing.getNonSittingDays();
      return this;
    }

    public NonSittingDaysAssignedToHearing build() {
      return new uk.gov.justice.listing.events.NonSittingDaysAssignedToHearing(hearingId, nonSittingDays);
    }
  }
}
