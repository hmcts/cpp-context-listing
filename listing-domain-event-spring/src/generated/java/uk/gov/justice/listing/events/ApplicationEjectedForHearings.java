package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class ApplicationEjectedForHearings implements Serializable {
  private static final long serialVersionUID = -738782570548566148L;

  private final UUID applicationId;

  private final List<UUID> hearingIds;

  private final String removalReason;

  @JsonCreator
  public ApplicationEjectedForHearings(final UUID applicationId, final List<UUID> hearingIds, final String removalReason) {
    this.applicationId = applicationId;
    this.hearingIds = hearingIds;
    this.removalReason = removalReason;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public String getRemovalReason() {
    return removalReason;
  }

  public static Builder applicationEjectedForHearings() {
    return new uk.gov.justice.listing.events.ApplicationEjectedForHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ApplicationEjectedForHearings that = (uk.gov.justice.listing.events.ApplicationEjectedForHearings) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.removalReason, that.removalReason);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, hearingIds, removalReason);}

  public static class Builder {
    private UUID applicationId;

    private List<UUID> hearingIds;

    private String removalReason;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
      return this;
    }

    public Builder withRemovalReason(final String removalReason) {
      this.removalReason = removalReason;
      return this;
    }

    public Builder withValuesFrom(final ApplicationEjectedForHearings applicationEjectedForHearings) {
      this.applicationId = applicationEjectedForHearings.getApplicationId();
      this.hearingIds = applicationEjectedForHearings.getHearingIds();
      this.removalReason = applicationEjectedForHearings.getRemovalReason();
      return this;
    }

    public ApplicationEjectedForHearings build() {
      return new uk.gov.justice.listing.events.ApplicationEjectedForHearings(applicationId, hearingIds, removalReason);
    }
  }
}
