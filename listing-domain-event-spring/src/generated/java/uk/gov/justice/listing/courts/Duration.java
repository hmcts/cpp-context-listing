package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;

public class Duration implements Serializable {
  private static final long serialVersionUID = -2582361536581928800L;

  private final Integer duration;

  @JsonCreator
  public Duration(final Integer duration) {
    this.duration = duration;
  }

  public Integer getDuration() {
    return duration;
  }

  public static Builder duration() {
    return new uk.gov.justice.listing.courts.Duration.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.Duration that = (uk.gov.justice.listing.courts.Duration) obj;

    return java.util.Objects.equals(this.duration, that.duration);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(duration);}

  public static class Builder {
    private Integer duration;

    public Builder withDuration(final Integer duration) {
      this.duration = duration;
      return this;
    }

    public Builder withValuesFrom(final Duration duration) {
      this.duration = duration.getDuration();
      return this;
    }

    public Duration build() {
      return new uk.gov.justice.listing.courts.Duration(duration);
    }
  }
}
