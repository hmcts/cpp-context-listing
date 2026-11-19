package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.application-ejected")
public class ApplicationEjected implements Serializable {
  private static final long serialVersionUID = 8116724383796635143L;

  private final UUID applicationId;

  private final UUID hearingId;

  private final String removalReason;

  @JsonCreator
  public ApplicationEjected(final UUID applicationId, final UUID hearingId, final String removalReason) {
    this.applicationId = applicationId;
    this.hearingId = hearingId;
    this.removalReason = removalReason;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getRemovalReason() {
    return removalReason;
  }

  public static Builder applicationEjected() {
    return new uk.gov.justice.listing.events.ApplicationEjected.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ApplicationEjected that = (uk.gov.justice.listing.events.ApplicationEjected) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.removalReason, that.removalReason);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, hearingId, removalReason);}

  public static class Builder {
    private UUID applicationId;

    private UUID hearingId;

    private String removalReason;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withRemovalReason(final String removalReason) {
      this.removalReason = removalReason;
      return this;
    }

    public Builder withValuesFrom(final ApplicationEjected applicationEjected) {
      this.applicationId = applicationEjected.getApplicationId();
      this.hearingId = applicationEjected.getHearingId();
      this.removalReason = applicationEjected.getRemovalReason();
      return this;
    }

    public ApplicationEjected build() {
      return new uk.gov.justice.listing.events.ApplicationEjected(applicationId, hearingId, removalReason);
    }
  }
}
