package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class NextHearingDefendant implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final UUID id;

  private final List<NextHearingOffence> offences;

  @JsonCreator
  public NextHearingDefendant(final UUID id, final List<NextHearingOffence> offences) {
    this.id = id;
    this.offences = offences;
  }

  public UUID getId() {
    return id;
  }

  public List<NextHearingOffence> getOffences() {
    return offences;
  }

  public static Builder nextHearingDefendant() {
    return new uk.gov.justice.core.courts.NextHearingDefendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.NextHearingDefendant that = (uk.gov.justice.core.courts.NextHearingDefendant) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.offences, that.offences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, offences);}

  public static class Builder {
    private UUID id;

    private List<NextHearingOffence> offences;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withOffences(final List<NextHearingOffence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withValuesFrom(final NextHearingDefendant nextHearingDefendant) {
      this.id = nextHearingDefendant.getId();
      this.offences = nextHearingDefendant.getOffences();
      return this;
    }

    public NextHearingDefendant build() {
      return new uk.gov.justice.core.courts.NextHearingDefendant(id, offences);
    }
  }
}
