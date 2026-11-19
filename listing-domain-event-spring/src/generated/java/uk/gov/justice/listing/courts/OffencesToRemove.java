package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class OffencesToRemove implements Serializable {
  private static final long serialVersionUID = -6227122385704246944L;

  private final UUID offenceId;

  @JsonCreator
  public OffencesToRemove(final UUID offenceId) {
    this.offenceId = offenceId;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public static Builder offencesToRemove() {
    return new uk.gov.justice.listing.courts.OffencesToRemove.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.OffencesToRemove that = (uk.gov.justice.listing.courts.OffencesToRemove) obj;

    return java.util.Objects.equals(this.offenceId, that.offenceId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(offenceId);}

  public static class Builder {
    private UUID offenceId;

    public Builder withOffenceId(final UUID offenceId) {
      this.offenceId = offenceId;
      return this;
    }

    public Builder withValuesFrom(final OffencesToRemove offencesToRemove) {
      this.offenceId = offencesToRemove.getOffenceId();
      return this;
    }

    public OffencesToRemove build() {
      return new uk.gov.justice.listing.courts.OffencesToRemove(offenceId);
    }
  }
}
