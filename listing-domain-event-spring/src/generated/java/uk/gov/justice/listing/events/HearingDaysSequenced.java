package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearing-days-sequenced")
public class HearingDaysSequenced implements Serializable {
  private static final long serialVersionUID = -2742716499266741370L;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  @JsonCreator
  public HearingDaysSequenced(final List<HearingDay> hearingDays, final UUID hearingId) {
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingDaysSequenced() {
    return new uk.gov.justice.listing.events.HearingDaysSequenced.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingDaysSequenced that = (uk.gov.justice.listing.events.HearingDaysSequenced) obj;

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

    public Builder withValuesFrom(final HearingDaysSequenced hearingDaysSequenced) {
      this.hearingDays = hearingDaysSequenced.getHearingDays();
      this.hearingId = hearingDaysSequenced.getHearingId();
      return this;
    }

    public HearingDaysSequenced build() {
      return new uk.gov.justice.listing.events.HearingDaysSequenced(hearingDays, hearingId);
    }
  }
}
