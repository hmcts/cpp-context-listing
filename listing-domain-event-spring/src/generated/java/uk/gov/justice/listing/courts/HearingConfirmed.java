package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.core.courts.ConfirmedHearing;

public class HearingConfirmed implements Serializable {
  private static final long serialVersionUID = -4327865879152535554L;

  private final ConfirmedHearing confirmedHearing;

  private final Boolean sendNotificationToParties;

  @JsonCreator
  public HearingConfirmed(final ConfirmedHearing confirmedHearing, final Boolean sendNotificationToParties) {
    this.confirmedHearing = confirmedHearing;
    this.sendNotificationToParties = sendNotificationToParties;
  }

  public ConfirmedHearing getConfirmedHearing() {
    return confirmedHearing;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public static Builder hearingConfirmed() {
    return new uk.gov.justice.listing.courts.HearingConfirmed.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.HearingConfirmed that = (uk.gov.justice.listing.courts.HearingConfirmed) obj;

    return java.util.Objects.equals(this.confirmedHearing, that.confirmedHearing) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(confirmedHearing, sendNotificationToParties);}

  public static class Builder {
    private ConfirmedHearing confirmedHearing;

    private Boolean sendNotificationToParties;

    public Builder withConfirmedHearing(final ConfirmedHearing confirmedHearing) {
      this.confirmedHearing = confirmedHearing;
      return this;
    }

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withValuesFrom(final HearingConfirmed hearingConfirmed) {
      this.confirmedHearing = hearingConfirmed.getConfirmedHearing();
      this.sendNotificationToParties = hearingConfirmed.getSendNotificationToParties();
      return this;
    }

    public HearingConfirmed build() {
      return new uk.gov.justice.listing.courts.HearingConfirmed(confirmedHearing, sendNotificationToParties);
    }
  }
}
