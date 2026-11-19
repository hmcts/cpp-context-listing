package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.core.courts.ConfirmedHearing;

public class HearingUpdated implements Serializable {
  private static final long serialVersionUID = 8037702448954531374L;

  private final Boolean isNotificationAllocationFieldUpdated;

  private final Boolean sendNotificationToParties;

  private final ConfirmedHearing updatedHearing;

  @JsonCreator
  public HearingUpdated(final Boolean isNotificationAllocationFieldUpdated, final Boolean sendNotificationToParties, final ConfirmedHearing updatedHearing) {
    this.isNotificationAllocationFieldUpdated = isNotificationAllocationFieldUpdated;
    this.sendNotificationToParties = sendNotificationToParties;
    this.updatedHearing = updatedHearing;
  }

  public Boolean getIsNotificationAllocationFieldUpdated() {
    return isNotificationAllocationFieldUpdated;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public ConfirmedHearing getUpdatedHearing() {
    return updatedHearing;
  }

  public static Builder hearingUpdated() {
    return new uk.gov.justice.listing.courts.HearingUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.HearingUpdated that = (uk.gov.justice.listing.courts.HearingUpdated) obj;

    return java.util.Objects.equals(this.isNotificationAllocationFieldUpdated, that.isNotificationAllocationFieldUpdated) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.updatedHearing, that.updatedHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(isNotificationAllocationFieldUpdated, sendNotificationToParties, updatedHearing);}

  public static class Builder {
    private Boolean isNotificationAllocationFieldUpdated;

    private Boolean sendNotificationToParties;

    private ConfirmedHearing updatedHearing;

    public Builder withIsNotificationAllocationFieldUpdated(final Boolean isNotificationAllocationFieldUpdated) {
      this.isNotificationAllocationFieldUpdated = isNotificationAllocationFieldUpdated;
      return this;
    }

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withUpdatedHearing(final ConfirmedHearing updatedHearing) {
      this.updatedHearing = updatedHearing;
      return this;
    }

    public Builder withValuesFrom(final HearingUpdated hearingUpdated) {
      this.isNotificationAllocationFieldUpdated = hearingUpdated.getIsNotificationAllocationFieldUpdated();
      this.sendNotificationToParties = hearingUpdated.getSendNotificationToParties();
      this.updatedHearing = hearingUpdated.getUpdatedHearing();
      return this;
    }

    public HearingUpdated build() {
      return new uk.gov.justice.listing.courts.HearingUpdated(isNotificationAllocationFieldUpdated, sendNotificationToParties, updatedHearing);
    }
  }
}
