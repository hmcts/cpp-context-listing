package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearings-update-completed")
public class HearingsUpdateCompleted implements Serializable {
  private static final long serialVersionUID = -7822841355178111845L;

  private final List<UUID> failedHearingIds;

  @JsonCreator
  public HearingsUpdateCompleted(final List<UUID> failedHearingIds) {
    this.failedHearingIds = failedHearingIds;
  }

  public List<UUID> getFailedHearingIds() {
    return failedHearingIds;
  }

  public static Builder hearingsUpdateCompleted() {
    return new uk.gov.justice.listing.events.HearingsUpdateCompleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingsUpdateCompleted that = (uk.gov.justice.listing.events.HearingsUpdateCompleted) obj;

    return java.util.Objects.equals(this.failedHearingIds, that.failedHearingIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(failedHearingIds);}

  public static class Builder {
    private List<UUID> failedHearingIds;

    public Builder withFailedHearingIds(final List<UUID> failedHearingIds) {
      this.failedHearingIds = failedHearingIds;
      return this;
    }

    public Builder withValuesFrom(final HearingsUpdateCompleted hearingsUpdateCompleted) {
      this.failedHearingIds = hearingsUpdateCompleted.getFailedHearingIds();
      return this;
    }

    public HearingsUpdateCompleted build() {
      return new uk.gov.justice.listing.events.HearingsUpdateCompleted(failedHearingIds);
    }
  }
}
