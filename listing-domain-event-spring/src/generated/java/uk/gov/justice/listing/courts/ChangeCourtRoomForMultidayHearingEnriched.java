package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class ChangeCourtRoomForMultidayHearingEnriched implements Serializable {
  private static final long serialVersionUID = 5252722378752488892L;

  private final List<ChangedDays> changedDays;

  private final UUID hearingId;

  private final List<NonDefaultDays> nonDefaultDays;

  private final Boolean sendNotificationToParties;

  @JsonCreator
  public ChangeCourtRoomForMultidayHearingEnriched(final List<ChangedDays> changedDays, final UUID hearingId, final List<NonDefaultDays> nonDefaultDays, final Boolean sendNotificationToParties) {
    this.changedDays = changedDays;
    this.hearingId = hearingId;
    this.nonDefaultDays = nonDefaultDays;
    this.sendNotificationToParties = sendNotificationToParties;
  }

  public List<ChangedDays> getChangedDays() {
    return changedDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<NonDefaultDays> getNonDefaultDays() {
    return nonDefaultDays;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public static Builder changeCourtRoomForMultidayHearingEnriched() {
    return new uk.gov.justice.listing.courts.ChangeCourtRoomForMultidayHearingEnriched.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ChangeCourtRoomForMultidayHearingEnriched that = (uk.gov.justice.listing.courts.ChangeCourtRoomForMultidayHearingEnriched) obj;

    return java.util.Objects.equals(this.changedDays, that.changedDays) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(changedDays, hearingId, nonDefaultDays, sendNotificationToParties);}

  public static class Builder {
    private List<ChangedDays> changedDays;

    private UUID hearingId;

    private List<NonDefaultDays> nonDefaultDays;

    private Boolean sendNotificationToParties;

    public Builder withChangedDays(final List<ChangedDays> changedDays) {
      this.changedDays = changedDays;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withNonDefaultDays(final List<NonDefaultDays> nonDefaultDays) {
      this.nonDefaultDays = nonDefaultDays;
      return this;
    }

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withValuesFrom(final ChangeCourtRoomForMultidayHearingEnriched changeCourtRoomForMultidayHearingEnriched) {
      this.changedDays = changeCourtRoomForMultidayHearingEnriched.getChangedDays();
      this.hearingId = changeCourtRoomForMultidayHearingEnriched.getHearingId();
      this.nonDefaultDays = changeCourtRoomForMultidayHearingEnriched.getNonDefaultDays();
      this.sendNotificationToParties = changeCourtRoomForMultidayHearingEnriched.getSendNotificationToParties();
      return this;
    }

    public ChangeCourtRoomForMultidayHearingEnriched build() {
      return new uk.gov.justice.listing.courts.ChangeCourtRoomForMultidayHearingEnriched(changedDays, hearingId, nonDefaultDays, sendNotificationToParties);
    }
  }
}
