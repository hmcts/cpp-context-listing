package uk.gov.justice.hearing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.HearingDay;

public class HearingDaysCancelled implements Serializable {
  private static final long serialVersionUID = -141754892260234327L;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  @JsonCreator
  public HearingDaysCancelled(final List<HearingDay> hearingDays, final UUID hearingId) {
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingDaysCancelled() {
    return new uk.gov.justice.hearing.courts.HearingDaysCancelled.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.hearing.courts.HearingDaysCancelled that = (uk.gov.justice.hearing.courts.HearingDaysCancelled) obj;

    return java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingDays, hearingId);}

  public static class Builder {
    private List<HearingDay> hearingDays;

    private UUID hearingId;

    public Builder withHearingDays(final List<HearingDay> hearingDays) {
      this.hearingDays = hearingDays;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final HearingDaysCancelled hearingDaysCancelled) {
      this.hearingDays = hearingDaysCancelled.getHearingDays();
      this.hearingId = hearingDaysCancelled.getHearingId();
      return this;
    }

    public HearingDaysCancelled build() {
      return new uk.gov.justice.hearing.courts.HearingDaysCancelled(hearingDays, hearingId);
    }
  }
}
