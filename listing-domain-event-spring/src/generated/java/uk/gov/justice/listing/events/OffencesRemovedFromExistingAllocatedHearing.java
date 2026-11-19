package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class OffencesRemovedFromExistingAllocatedHearing implements Serializable {
  private static final long serialVersionUID = 6549646488666331650L;

  private final UUID hearingId;

  private final Boolean isResultFlow;

  private final List<UUID> offenceIds;

  private final String sourceContext;

  @JsonCreator
  public OffencesRemovedFromExistingAllocatedHearing(final UUID hearingId, final Boolean isResultFlow, final List<UUID> offenceIds, final String sourceContext) {
    this.hearingId = hearingId;
    this.isResultFlow = isResultFlow;
    this.offenceIds = offenceIds;
    this.sourceContext = sourceContext;
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

  public String getSourceContext() {
    return sourceContext;
  }

  public static Builder offencesRemovedFromExistingAllocatedHearing() {
    return new uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing that = (uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.isResultFlow, that.isResultFlow) &&
    java.util.Objects.equals(this.offenceIds, that.offenceIds) &&
    java.util.Objects.equals(this.sourceContext, that.sourceContext);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, isResultFlow, offenceIds, sourceContext);}

  public static class Builder {
    private UUID hearingId;

    private Boolean isResultFlow;

    private List<UUID> offenceIds;

    private String sourceContext;

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

    public Builder withSourceContext(final String sourceContext) {
      this.sourceContext = sourceContext;
      return this;
    }

    public Builder withValuesFrom(final OffencesRemovedFromExistingAllocatedHearing offencesRemovedFromExistingAllocatedHearing) {
      this.hearingId = offencesRemovedFromExistingAllocatedHearing.getHearingId();
      this.isResultFlow = offencesRemovedFromExistingAllocatedHearing.getIsResultFlow();
      this.offenceIds = offencesRemovedFromExistingAllocatedHearing.getOffenceIds();
      this.sourceContext = offencesRemovedFromExistingAllocatedHearing.getSourceContext();
      return this;
    }

    public OffencesRemovedFromExistingAllocatedHearing build() {
      return new uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing(hearingId, isResultFlow, offenceIds, sourceContext);
    }
  }
}
