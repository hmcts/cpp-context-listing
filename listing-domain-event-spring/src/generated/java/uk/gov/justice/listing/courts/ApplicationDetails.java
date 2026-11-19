package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class ApplicationDetails implements Serializable {
  private static final long serialVersionUID = -4659675027108169311L;

  private final UUID applicationId;

  private final String applicationReference;

  private final Subject subject;

  @JsonCreator
  public ApplicationDetails(final UUID applicationId, final String applicationReference, final Subject subject) {
    this.applicationId = applicationId;
    this.applicationReference = applicationReference;
    this.subject = subject;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public String getApplicationReference() {
    return applicationReference;
  }

  public Subject getSubject() {
    return subject;
  }

  public static Builder applicationDetails() {
    return new uk.gov.justice.listing.courts.ApplicationDetails.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ApplicationDetails that = (uk.gov.justice.listing.courts.ApplicationDetails) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.applicationReference, that.applicationReference) &&
    java.util.Objects.equals(this.subject, that.subject);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, applicationReference, subject);}

  public static class Builder {
    private UUID applicationId;

    private String applicationReference;

    private Subject subject;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withApplicationReference(final String applicationReference) {
      this.applicationReference = applicationReference;
      return this;
    }

    public Builder withSubject(final Subject subject) {
      this.subject = subject;
      return this;
    }

    public Builder withValuesFrom(final ApplicationDetails applicationDetails) {
      this.applicationId = applicationDetails.getApplicationId();
      this.applicationReference = applicationDetails.getApplicationReference();
      this.subject = applicationDetails.getSubject();
      return this;
    }

    public ApplicationDetails build() {
      return new uk.gov.justice.listing.courts.ApplicationDetails(applicationId, applicationReference, subject);
    }
  }
}
