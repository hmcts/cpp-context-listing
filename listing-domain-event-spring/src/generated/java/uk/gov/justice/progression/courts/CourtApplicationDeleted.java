package uk.gov.justice.progression.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class CourtApplicationDeleted implements Serializable {
  private static final long serialVersionUID = -4984962423869770091L;

  private final UUID applicationId;

  private final UUID hearingId;

  @JsonCreator
  public CourtApplicationDeleted(final UUID applicationId, final UUID hearingId) {
    this.applicationId = applicationId;
    this.hearingId = hearingId;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder courtApplicationDeleted() {
    return new uk.gov.justice.progression.courts.CourtApplicationDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.progression.courts.CourtApplicationDeleted that = (uk.gov.justice.progression.courts.CourtApplicationDeleted) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, hearingId);}

  public static class Builder {
    private UUID applicationId;

    private UUID hearingId;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationDeleted courtApplicationDeleted) {
      this.applicationId = courtApplicationDeleted.getApplicationId();
      this.hearingId = courtApplicationDeleted.getHearingId();
      return this;
    }

    public CourtApplicationDeleted build() {
      return new uk.gov.justice.progression.courts.CourtApplicationDeleted(applicationId, hearingId);
    }
  }
}
