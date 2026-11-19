package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;

public class ChangeCourtRoomForMultidayHearing implements Serializable {
  private static final long serialVersionUID = -5882894513653551781L;

  private final List<NonDefaultDays> nonDefaultDays;

  private final Boolean sendNotificationToParties;

  @JsonCreator
  public ChangeCourtRoomForMultidayHearing(final List<NonDefaultDays> nonDefaultDays, final Boolean sendNotificationToParties) {
    this.nonDefaultDays = nonDefaultDays;
    this.sendNotificationToParties = sendNotificationToParties;
  }

  public List<NonDefaultDays> getNonDefaultDays() {
    return nonDefaultDays;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public static Builder changeCourtRoomForMultidayHearing() {
    return new uk.gov.justice.listing.courts.ChangeCourtRoomForMultidayHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ChangeCourtRoomForMultidayHearing that = (uk.gov.justice.listing.courts.ChangeCourtRoomForMultidayHearing) obj;

    return java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(nonDefaultDays, sendNotificationToParties);}

  public static class Builder {
    private List<NonDefaultDays> nonDefaultDays;

    private Boolean sendNotificationToParties;

    public Builder withNonDefaultDays(final List<NonDefaultDays> nonDefaultDays) {
      this.nonDefaultDays = nonDefaultDays;
      return this;
    }

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withValuesFrom(final ChangeCourtRoomForMultidayHearing changeCourtRoomForMultidayHearing) {
      this.nonDefaultDays = changeCourtRoomForMultidayHearing.getNonDefaultDays();
      this.sendNotificationToParties = changeCourtRoomForMultidayHearing.getSendNotificationToParties();
      return this;
    }

    public ChangeCourtRoomForMultidayHearing build() {
      return new uk.gov.justice.listing.courts.ChangeCourtRoomForMultidayHearing(nonDefaultDays, sendNotificationToParties);
    }
  }
}
