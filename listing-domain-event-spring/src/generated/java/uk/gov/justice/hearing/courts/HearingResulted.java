package uk.gov.justice.hearing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.Hearing;

public class HearingResulted implements Serializable {
  private static final long serialVersionUID = -8762589598591790959L;

  private final Hearing hearing;

  private final String hearingDay;

  private final Boolean isReshare;

  private final List<UUID> shadowListedOffences;

  private final ZonedDateTime sharedTime;

  @JsonCreator
  public HearingResulted(final Hearing hearing, final String hearingDay, final Boolean isReshare, final List<UUID> shadowListedOffences, final ZonedDateTime sharedTime) {
    this.hearing = hearing;
    this.hearingDay = hearingDay;
    this.isReshare = isReshare;
    this.shadowListedOffences = shadowListedOffences;
    this.sharedTime = sharedTime;
  }

  public Hearing getHearing() {
    return hearing;
  }

  public String getHearingDay() {
    return hearingDay;
  }

  public Boolean getIsReshare() {
    return isReshare;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public ZonedDateTime getSharedTime() {
    return sharedTime;
  }

  public static Builder hearingResulted() {
    return new uk.gov.justice.hearing.courts.HearingResulted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.hearing.courts.HearingResulted that = (uk.gov.justice.hearing.courts.HearingResulted) obj;

    return java.util.Objects.equals(this.hearing, that.hearing) &&
    java.util.Objects.equals(this.hearingDay, that.hearingDay) &&
    java.util.Objects.equals(this.isReshare, that.isReshare) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences) &&
    java.util.Objects.equals(this.sharedTime, that.sharedTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearing, hearingDay, isReshare, shadowListedOffences, sharedTime);}

  public static class Builder {
    private Hearing hearing;

    private String hearingDay;

    private Boolean isReshare;

    private List<UUID> shadowListedOffences;

    private ZonedDateTime sharedTime;

    public Builder withHearing(final Hearing hearing) {
      this.hearing = hearing;
      return this;
    }

    public Builder withHearingDay(final String hearingDay) {
      this.hearingDay = hearingDay;
      return this;
    }

    public Builder withIsReshare(final Boolean isReshare) {
      this.isReshare = isReshare;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withSharedTime(final ZonedDateTime sharedTime) {
      this.sharedTime = sharedTime;
      return this;
    }

    public Builder withValuesFrom(final HearingResulted hearingResulted) {
      this.hearing = hearingResulted.getHearing();
      this.hearingDay = hearingResulted.getHearingDay();
      this.isReshare = hearingResulted.getIsReshare();
      this.shadowListedOffences = hearingResulted.getShadowListedOffences();
      this.sharedTime = hearingResulted.getSharedTime();
      return this;
    }

    public HearingResulted build() {
      return new uk.gov.justice.hearing.courts.HearingResulted(hearing, hearingDay, isReshare, shadowListedOffences, sharedTime);
    }
  }
}
