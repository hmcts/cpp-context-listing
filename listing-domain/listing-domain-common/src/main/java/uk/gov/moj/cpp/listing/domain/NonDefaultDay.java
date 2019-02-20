package uk.gov.moj.cpp.listing.domain;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Optional;

@SuppressWarnings({"squid:S00107", "squid:S00121"})
public class NonDefaultDay implements Serializable {
  private static final long serialVersionUID = 1L;

  private final Integer duration;

  private final ZonedDateTime startTime;

  public NonDefaultDay(final Optional<Integer> duration, final ZonedDateTime startTime) {
    this.duration = duration.orElse(null);
    this.startTime = startTime;
  }

  public Optional<Integer> getDuration() {
    return duration!=null ? of(duration) : empty();
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public static Builder nonDefaultDay() {
    return new NonDefaultDay.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final NonDefaultDay that = (NonDefaultDay) obj;

    return java.util.Objects.equals(this.duration, that.duration) &&
    java.util.Objects.equals(this.startTime, that.startTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(duration, startTime);}

  @Override
  public String toString() {
    return "NonDefaultDay{" +
    	"duration='" + duration + "'," +
    	"startTime='" + startTime + "'" +
    "}";
  }

  public static class Builder {
    private Optional<Integer> duration;

    private ZonedDateTime startTime;

    public Builder withDuration(final Optional<Integer> duration) {
     this.duration = duration;
      return this;
    }

    public Builder withStartTime(final ZonedDateTime startTime) {
      this.startTime = startTime;
      return this;
    }

    public NonDefaultDay build() {
      return new NonDefaultDay(duration, startTime);
    }
  }
}
