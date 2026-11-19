package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.LaaReference;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.laa-reference-for-application-updated")
public class LaaReferenceForApplicationUpdated implements Serializable {
  private static final long serialVersionUID = 7597620069471456771L;

  private final UUID applicationId;

  private final List<UUID> hearingIds;

  private final LaaReference laaReference;

  private final UUID offenceId;

  private final UUID subjectId;

  @JsonCreator
  public LaaReferenceForApplicationUpdated(final UUID applicationId, final List<UUID> hearingIds, final LaaReference laaReference, final UUID offenceId, final UUID subjectId) {
    this.applicationId = applicationId;
    this.hearingIds = hearingIds;
    this.laaReference = laaReference;
    this.offenceId = offenceId;
    this.subjectId = subjectId;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
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

  public static Builder laaReferenceForApplicationUpdated() {
    return new uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated that = (uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.laaReference, that.laaReference) &&
    java.util.Objects.equals(this.offenceId, that.offenceId) &&
    java.util.Objects.equals(this.subjectId, that.subjectId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, hearingIds, laaReference, offenceId, subjectId);}

  public static class Builder {
    private UUID applicationId;

    private List<UUID> hearingIds;

    private LaaReference laaReference;

    private UUID offenceId;

    private UUID subjectId;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
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

    public Builder withValuesFrom(final LaaReferenceForApplicationUpdated laaReferenceForApplicationUpdated) {
      this.applicationId = laaReferenceForApplicationUpdated.getApplicationId();
      this.hearingIds = laaReferenceForApplicationUpdated.getHearingIds();
      this.laaReference = laaReferenceForApplicationUpdated.getLaaReference();
      this.offenceId = laaReferenceForApplicationUpdated.getOffenceId();
      this.subjectId = laaReferenceForApplicationUpdated.getSubjectId();
      return this;
    }

    public LaaReferenceForApplicationUpdated build() {
      return new uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated(applicationId, hearingIds, laaReference, offenceId, subjectId);
    }
  }
}
