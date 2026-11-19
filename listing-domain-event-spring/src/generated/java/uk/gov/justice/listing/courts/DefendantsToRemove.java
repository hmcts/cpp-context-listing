package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class DefendantsToRemove implements Serializable {
  private static final long serialVersionUID = -6227122385704246944L;

  private final UUID defendantId;

  private final List<OffencesToRemove> offencesToRemove;

  @JsonCreator
  public DefendantsToRemove(final UUID defendantId, final List<OffencesToRemove> offencesToRemove) {
    this.defendantId = defendantId;
    this.offencesToRemove = offencesToRemove;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public List<OffencesToRemove> getOffencesToRemove() {
    return offencesToRemove;
  }

  public static Builder defendantsToRemove() {
    return new uk.gov.justice.listing.courts.DefendantsToRemove.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.DefendantsToRemove that = (uk.gov.justice.listing.courts.DefendantsToRemove) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.offencesToRemove, that.offencesToRemove);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, offencesToRemove);}

  public static class Builder {
    private UUID defendantId;

    private List<OffencesToRemove> offencesToRemove;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withOffencesToRemove(final List<OffencesToRemove> offencesToRemove) {
      this.offencesToRemove = offencesToRemove;
      return this;
    }

    public Builder withValuesFrom(final DefendantsToRemove defendantsToRemove) {
      this.defendantId = defendantsToRemove.getDefendantId();
      this.offencesToRemove = defendantsToRemove.getOffencesToRemove();
      return this;
    }

    public DefendantsToRemove build() {
      return new uk.gov.justice.listing.courts.DefendantsToRemove(defendantId, offencesToRemove);
    }
  }
}
