package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalTime;
import java.util.UUID;

public class CourtCentreDetails implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Integer defaultDuration;

  private final LocalTime defaultStartTime;

  private final UUID id;

  @JsonCreator
  public CourtCentreDetails(final Integer defaultDuration, final LocalTime defaultStartTime, final UUID id) {
    this.defaultDuration = defaultDuration;
    this.defaultStartTime = defaultStartTime;
    this.id = id;
  }

  public Integer getDefaultDuration() {
    return defaultDuration;
  }

  public LocalTime getDefaultStartTime() {
    return defaultStartTime;
  }

  public UUID getId() {
    return id;
  }

  public static Builder courtCentreDetails() {
    return new uk.gov.justice.listing.events.CourtCentreDetails.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CourtCentreDetails that = (uk.gov.justice.listing.events.CourtCentreDetails) obj;

    return java.util.Objects.equals(this.defaultDuration, that.defaultDuration) &&
    java.util.Objects.equals(this.defaultStartTime, that.defaultStartTime) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defaultDuration, defaultStartTime, id);}

  public static class Builder {
    private Integer defaultDuration;

    private LocalTime defaultStartTime;

    private UUID id;

    public Builder withDefaultDuration(final Integer defaultDuration) {
      this.defaultDuration = defaultDuration;
      return this;
    }

    public Builder withDefaultStartTime(final LocalTime defaultStartTime) {
      this.defaultStartTime = defaultStartTime;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withValuesFrom(final CourtCentreDetails courtCentreDetails) {
      this.defaultDuration = courtCentreDetails.getDefaultDuration();
      this.defaultStartTime = courtCentreDetails.getDefaultStartTime();
      this.id = courtCentreDetails.getId();
      return this;
    }

    public CourtCentreDetails build() {
      return new uk.gov.justice.listing.events.CourtCentreDetails(defaultDuration, defaultStartTime, id);
    }
  }
}
