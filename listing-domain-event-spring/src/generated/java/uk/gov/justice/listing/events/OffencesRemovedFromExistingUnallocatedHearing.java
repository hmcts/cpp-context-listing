package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.offences-removed-from-existing-unallocated-hearing")
public class OffencesRemovedFromExistingUnallocatedHearing implements Serializable {
  private static final long serialVersionUID = -3656612045825777381L;

  private final UUID hearingId;

  private final Boolean isResultFlow;

  private final List<UUID> offenceIds;

  @JsonCreator
  public OffencesRemovedFromExistingUnallocatedHearing(final UUID hearingId, final Boolean isResultFlow, final List<UUID> offenceIds) {
    this.hearingId = hearingId;
    this.isResultFlow = isResultFlow;
    this.offenceIds = offenceIds;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public Boolean getIsResultFlow() {
    return isResultFlow;
  }

  public List<UUID> getOffenceIds() {
    return offenceIds;
  }

  public static Builder offencesRemovedFromExistingUnallocatedHearing() {
    return new uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing that = (uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.isResultFlow, that.isResultFlow) &&
    java.util.Objects.equals(this.offenceIds, that.offenceIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, isResultFlow, offenceIds);}

  public static class Builder {
    private UUID hearingId;

    private Boolean isResultFlow;

    private List<UUID> offenceIds;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withIsResultFlow(final Boolean isResultFlow) {
      this.isResultFlow = isResultFlow;
      return this;
    }

    public Builder withOffenceIds(final List<UUID> offenceIds) {
      this.offenceIds = offenceIds;
      return this;
    }

    public Builder withValuesFrom(final OffencesRemovedFromExistingUnallocatedHearing offencesRemovedFromExistingUnallocatedHearing) {
      this.hearingId = offencesRemovedFromExistingUnallocatedHearing.getHearingId();
      this.isResultFlow = offencesRemovedFromExistingUnallocatedHearing.getIsResultFlow();
      this.offenceIds = offencesRemovedFromExistingUnallocatedHearing.getOffenceIds();
      return this;
    }

    public OffencesRemovedFromExistingUnallocatedHearing build() {
      return new uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing(hearingId, isResultFlow, offenceIds);
    }
  }
}
