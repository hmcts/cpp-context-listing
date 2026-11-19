package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.HearingDay;

public class CancelHearingDays implements Serializable {
  private static final long serialVersionUID = -7860065290997377873L;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  @JsonCreator
  public CancelHearingDays(final List<HearingDay> hearingDays, final UUID hearingId) {
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder cancelHearingDays() {
    return new uk.gov.justice.listing.courts.CancelHearingDays.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CancelHearingDays that = (uk.gov.justice.listing.courts.CancelHearingDays) obj;

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

    public Builder withValuesFrom(final CancelHearingDays cancelHearingDays) {
      this.hearingDays = cancelHearingDays.getHearingDays();
      this.hearingId = cancelHearingDays.getHearingId();
      return this;
    }

    public CancelHearingDays build() {
      return new uk.gov.justice.listing.courts.CancelHearingDays(hearingDays, hearingId);
    }
  }
}
