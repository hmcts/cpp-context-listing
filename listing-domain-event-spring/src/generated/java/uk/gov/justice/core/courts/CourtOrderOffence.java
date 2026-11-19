package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class CourtOrderOffence implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Offence offence;

  private final UUID prosecutionCaseId;

  private final ProsecutionCaseIdentifier prosecutionCaseIdentifier;

  @JsonCreator
  public CourtOrderOffence(final Offence offence, final UUID prosecutionCaseId, final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
    this.offence = offence;
    this.prosecutionCaseId = prosecutionCaseId;
    this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
  }

  public Offence getOffence() {
    return offence;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public ProsecutionCaseIdentifier getProsecutionCaseIdentifier() {
    return prosecutionCaseIdentifier;
  }

  public static Builder courtOrderOffence() {
    return new uk.gov.justice.core.courts.CourtOrderOffence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtOrderOffence that = (uk.gov.justice.core.courts.CourtOrderOffence) obj;

    return java.util.Objects.equals(this.offence, that.offence) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId) &&
    java.util.Objects.equals(this.prosecutionCaseIdentifier, that.prosecutionCaseIdentifier);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(offence, prosecutionCaseId, prosecutionCaseIdentifier);}

  public static class Builder {
    private Offence offence;

    private UUID prosecutionCaseId;

    private ProsecutionCaseIdentifier prosecutionCaseIdentifier;

    public Builder withOffence(final Offence offence) {
      this.offence = offence;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withProsecutionCaseIdentifier(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
      this.prosecutionCaseIdentifier = prosecutionCaseIdentifier;
      return this;
    }

    public Builder withValuesFrom(final CourtOrderOffence courtOrderOffence) {
      this.offence = courtOrderOffence.getOffence();
      this.prosecutionCaseId = courtOrderOffence.getProsecutionCaseId();
      this.prosecutionCaseIdentifier = courtOrderOffence.getProsecutionCaseIdentifier();
      return this;
    }

    public CourtOrderOffence build() {
      return new uk.gov.justice.core.courts.CourtOrderOffence(offence, prosecutionCaseId, prosecutionCaseIdentifier);
    }
  }
}
