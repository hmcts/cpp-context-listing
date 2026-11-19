package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class Offences implements Serializable {
  private static final long serialVersionUID = 7919779722927081093L;

  private final UUID offenceId;

  @JsonCreator
  public Offences(final UUID offenceId) {
    this.offenceId = offenceId;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public static Builder offences() {
    return new uk.gov.justice.listing.courts.Offences.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.Offences that = (uk.gov.justice.listing.courts.Offences) obj;

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

    public Builder withValuesFrom(final Offences offences) {
      this.offenceId = offences.getOffenceId();
      return this;
    }

    public Offences build() {
      return new uk.gov.justice.listing.courts.Offences(offenceId);
    }
  }
}
