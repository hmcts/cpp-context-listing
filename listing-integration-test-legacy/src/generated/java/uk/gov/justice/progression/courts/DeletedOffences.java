package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class DeletedOffences implements Serializable {
  private static final long serialVersionUID = -3848107891521503840L;

  private final UUID defendantId;

  private final List<UUID> offences;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public DeletedOffences(final UUID defendantId, final List<UUID> offences, final UUID prosecutionCaseId) {
    this.defendantId = defendantId;
    this.offences = offences;
    this.prosecutionCaseId = prosecutionCaseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public List<UUID> getOffences() {
    return offences;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public static Builder deletedOffences() {
    return new uk.gov.justice.progression.courts.DeletedOffences.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.DeletedOffences that = (uk.gov.justice.progression.courts.DeletedOffences) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.offences, that.offences) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, offences, prosecutionCaseId);}

  public static class Builder {
    private UUID defendantId;

    private List<UUID> offences;

    private UUID prosecutionCaseId;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withOffences(final List<UUID> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withValuesFrom(final DeletedOffences deletedOffences) {
      this.defendantId = deletedOffences.getDefendantId();
      this.offences = deletedOffences.getOffences();
      this.prosecutionCaseId = deletedOffences.getProsecutionCaseId();
      return this;
    }

    public DeletedOffences build() {
      return new uk.gov.justice.progression.courts.DeletedOffences(defendantId, offences, prosecutionCaseId);
    }
  }
}
