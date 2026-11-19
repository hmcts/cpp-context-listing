package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.LaaReference;

public class ApplicationOffencesUpdated implements Serializable {
  private static final long serialVersionUID = -6482407368690980538L;

  private final UUID applicationId;

  private final LaaReference laaReference;

  private final UUID offenceId;

  private final UUID subjectId;

  @JsonCreator
  public ApplicationOffencesUpdated(final UUID applicationId, final LaaReference laaReference, final UUID offenceId, final UUID subjectId) {
    this.applicationId = applicationId;
    this.laaReference = laaReference;
    this.offenceId = offenceId;
    this.subjectId = subjectId;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public LaaReference getLaaReference() {
    return laaReference;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public UUID getSubjectId() {
    return subjectId;
  }

  public static Builder applicationOffencesUpdated() {
    return new uk.gov.justice.progression.courts.ApplicationOffencesUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.ApplicationOffencesUpdated that = (uk.gov.justice.progression.courts.ApplicationOffencesUpdated) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.laaReference, that.laaReference) &&
    java.util.Objects.equals(this.offenceId, that.offenceId) &&
    java.util.Objects.equals(this.subjectId, that.subjectId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, laaReference, offenceId, subjectId);}

  public static class Builder {
    private UUID applicationId;

    private LaaReference laaReference;

    private UUID offenceId;

    private UUID subjectId;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withLaaReference(final LaaReference laaReference) {
      this.laaReference = laaReference;
      return this;
    }

    public Builder withOffenceId(final UUID offenceId) {
      this.offenceId = offenceId;
      return this;
    }

    public Builder withSubjectId(final UUID subjectId) {
      this.subjectId = subjectId;
      return this;
    }

    public Builder withValuesFrom(final ApplicationOffencesUpdated applicationOffencesUpdated) {
      this.applicationId = applicationOffencesUpdated.getApplicationId();
      this.laaReference = applicationOffencesUpdated.getLaaReference();
      this.offenceId = applicationOffencesUpdated.getOffenceId();
      this.subjectId = applicationOffencesUpdated.getSubjectId();
      return this;
    }

    public ApplicationOffencesUpdated build() {
      return new uk.gov.justice.progression.courts.ApplicationOffencesUpdated(applicationId, laaReference, offenceId, subjectId);
    }
  }
}
