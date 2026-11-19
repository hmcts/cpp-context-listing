package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.non-sitting-days-changed-for-hearing")
public class NonSittingDaysChangedForHearing implements Serializable {
  private static final long serialVersionUID = -2615147028292342632L;

  private final UUID hearingId;

  private final List<LocalDate> nonSittingDays;

  @JsonCreator
  public NonSittingDaysChangedForHearing(final UUID hearingId, final List<LocalDate> nonSittingDays) {
    this.hearingId = hearingId;
    this.nonSittingDays = nonSittingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<LocalDate> getNonSittingDays() {
    return nonSittingDays;
  }

  public static Builder nonSittingDaysChangedForHearing() {
    return new uk.gov.justice.listing.events.NonSittingDaysChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NonSittingDaysChangedForHearing that = (uk.gov.justice.listing.events.NonSittingDaysChangedForHearing) obj;

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

    public Builder withValuesFrom(final NonSittingDaysChangedForHearing nonSittingDaysChangedForHearing) {
      this.hearingId = nonSittingDaysChangedForHearing.getHearingId();
      this.nonSittingDays = nonSittingDaysChangedForHearing.getNonSittingDays();
      return this;
    }

    public NonSittingDaysChangedForHearing build() {
      return new uk.gov.justice.listing.events.NonSittingDaysChangedForHearing(hearingId, nonSittingDays);
    }
  }
}
