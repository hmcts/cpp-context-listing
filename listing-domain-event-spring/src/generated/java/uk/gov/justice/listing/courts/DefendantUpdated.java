package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;

public class DefendantUpdated implements Serializable {
  private static final long serialVersionUID = -2466170967895900147L;

  private final Defendant defendant;

  @JsonCreator
  public DefendantUpdated(final Defendant defendant) {
    this.defendant = defendant;
  }

  public Defendant getDefendant() {
    return defendant;
  }

  public static Builder defendantUpdated() {
    return new uk.gov.justice.listing.courts.DefendantUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.DefendantUpdated that = (uk.gov.justice.listing.courts.DefendantUpdated) obj;

    return java.util.Objects.equals(this.defendant, that.defendant);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendant);}

  public static class Builder {
    private Defendant defendant;

    public Builder withDefendant(final Defendant defendant) {
      this.defendant = defendant;
      return this;
    }

    public Builder withValuesFrom(final DefendantUpdated defendantUpdated) {
      this.defendant = defendantUpdated.getDefendant();
      return this;
    }

    public DefendantUpdated build() {
      return new uk.gov.justice.listing.courts.DefendantUpdated(defendant);
    }
  }
}
