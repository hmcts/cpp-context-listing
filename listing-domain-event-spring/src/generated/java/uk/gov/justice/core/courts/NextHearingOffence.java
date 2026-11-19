package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class NextHearingOffence implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final UUID id;

  @JsonCreator
  public NextHearingOffence(final UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
  }

  public static Builder nextHearingOffence() {
    return new uk.gov.justice.core.courts.NextHearingOffence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.NextHearingOffence that = (uk.gov.justice.core.courts.NextHearingOffence) obj;

    return java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id);}

  public static class Builder {
    private UUID id;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withValuesFrom(final NextHearingOffence nextHearingOffence) {
      this.id = nextHearingOffence.getId();
      return this;
    }

    public NextHearingOffence build() {
      return new uk.gov.justice.core.courts.NextHearingOffence(id);
    }
  }
}
