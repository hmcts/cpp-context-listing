package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.Offence;

public class UpdatedOffences implements Serializable {
  private static final long serialVersionUID = -3848107891521503840L;

  private final UUID defendantId;

  private final List<Offence> offences;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public UpdatedOffences(final UUID defendantId, final List<Offence> offences, final UUID prosecutionCaseId) {
    this.defendantId = defendantId;
    this.offences = offences;
    this.prosecutionCaseId = prosecutionCaseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public List<Offence> getOffences() {
    return offences;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public static Builder updatedOffences() {
    return new uk.gov.justice.progression.courts.UpdatedOffences.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.UpdatedOffences that = (uk.gov.justice.progression.courts.UpdatedOffences) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.offences, that.offences) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, offences, prosecutionCaseId);}

  public static class Builder {
    private UUID defendantId;

    private List<Offence> offences;

    private UUID prosecutionCaseId;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withOffences(final List<Offence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withValuesFrom(final UpdatedOffences updatedOffences) {
      this.defendantId = updatedOffences.getDefendantId();
      this.offences = updatedOffences.getOffences();
      this.prosecutionCaseId = updatedOffences.getProsecutionCaseId();
      return this;
    }

    public UpdatedOffences build() {
      return new uk.gov.justice.progression.courts.UpdatedOffences(defendantId, offences, prosecutionCaseId);
    }
  }
}
