package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.core.courts.LaaReference;

public class ApplicationLaaReferenceUpdatedForApplication implements Serializable {
  private static final long serialVersionUID = -2704140721031147502L;

  private final UUID applicationId;

  private final LaaReference laaReference;

  private final UUID subjectId;

  @JsonCreator
  public ApplicationLaaReferenceUpdatedForApplication(final UUID applicationId, final LaaReference laaReference, final UUID subjectId) {
    this.applicationId = applicationId;
    this.laaReference = laaReference;
    this.subjectId = subjectId;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public LaaReference getLaaReference() {
    return laaReference;
  }

  public UUID getSubjectId() {
    return subjectId;
  }

  public static Builder applicationLaaReferenceUpdatedForApplication() {
    return new uk.gov.justice.progression.courts.ApplicationLaaReferenceUpdatedForApplication.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.ApplicationLaaReferenceUpdatedForApplication that = (uk.gov.justice.progression.courts.ApplicationLaaReferenceUpdatedForApplication) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.laaReference, that.laaReference) &&
    java.util.Objects.equals(this.subjectId, that.subjectId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, laaReference, subjectId);}

  public static class Builder {
    private UUID applicationId;

    private LaaReference laaReference;

    private UUID subjectId;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withLaaReference(final LaaReference laaReference) {
      this.laaReference = laaReference;
      return this;
    }

    public Builder withSubjectId(final UUID subjectId) {
      this.subjectId = subjectId;
      return this;
    }

    public Builder withValuesFrom(final ApplicationLaaReferenceUpdatedForApplication applicationLaaReferenceUpdatedForApplication) {
      this.applicationId = applicationLaaReferenceUpdatedForApplication.getApplicationId();
      this.laaReference = applicationLaaReferenceUpdatedForApplication.getLaaReference();
      this.subjectId = applicationLaaReferenceUpdatedForApplication.getSubjectId();
      return this;
    }

    public ApplicationLaaReferenceUpdatedForApplication build() {
      return new uk.gov.justice.progression.courts.ApplicationLaaReferenceUpdatedForApplication(applicationId, laaReference, subjectId);
    }
  }
}
