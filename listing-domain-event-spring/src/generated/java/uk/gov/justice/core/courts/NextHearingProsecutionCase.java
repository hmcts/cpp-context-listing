package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class NextHearingProsecutionCase implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final List<NextHearingDefendant> defendants;

  private final UUID id;

  @JsonCreator
  public NextHearingProsecutionCase(final List<NextHearingDefendant> defendants, final UUID id) {
    this.defendants = defendants;
    this.id = id;
  }

  public List<NextHearingDefendant> getDefendants() {
    return defendants;
  }

  public UUID getId() {
    return id;
  }

  public static Builder nextHearingProsecutionCase() {
    return new uk.gov.justice.core.courts.NextHearingProsecutionCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.NextHearingProsecutionCase that = (uk.gov.justice.core.courts.NextHearingProsecutionCase) obj;

    return java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendants, id);}

  public static class Builder {
    private List<NextHearingDefendant> defendants;

    private UUID id;

    public Builder withDefendants(final List<NextHearingDefendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withValuesFrom(final NextHearingProsecutionCase nextHearingProsecutionCase) {
      this.defendants = nextHearingProsecutionCase.getDefendants();
      this.id = nextHearingProsecutionCase.getId();
      return this;
    }

    public NextHearingProsecutionCase build() {
      return new uk.gov.justice.core.courts.NextHearingProsecutionCase(defendants, id);
    }
  }
}
