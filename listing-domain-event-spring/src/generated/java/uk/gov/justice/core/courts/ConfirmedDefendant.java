package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class ConfirmedDefendant implements Serializable {
  private static final long serialVersionUID = -9219648287822407011L;

  private final UUID id;

  private final List<ConfirmedOffence> offences;

  @JsonCreator
  public ConfirmedDefendant(final UUID id, final List<ConfirmedOffence> offences) {
    this.id = id;
    this.offences = offences;
  }

  public UUID getId() {
    return id;
  }

  public List<ConfirmedOffence> getOffences() {
    return offences;
  }

  public static Builder confirmedDefendant() {
    return new uk.gov.justice.core.courts.ConfirmedDefendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ConfirmedDefendant that = (uk.gov.justice.core.courts.ConfirmedDefendant) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.offences, that.offences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, offences);}

  public static class Builder {
    private UUID id;

    private List<ConfirmedOffence> offences;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withOffences(final List<ConfirmedOffence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withValuesFrom(final ConfirmedDefendant confirmedDefendant) {
      this.id = confirmedDefendant.getId();
      this.offences = confirmedDefendant.getOffences();
      return this;
    }

    public ConfirmedDefendant build() {
      return new uk.gov.justice.core.courts.ConfirmedDefendant(id, offences);
    }
  }
}
