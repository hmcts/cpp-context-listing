package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.master-case-updated-for-group")
public class MasterCaseUpdatedForGroup implements Serializable {
  private static final long serialVersionUID = 5892172425964306603L;

  private final UUID caseId;

  private final List<UUID> hearingIds;

  @JsonCreator
  public MasterCaseUpdatedForGroup(final UUID caseId, final List<UUID> hearingIds) {
    this.caseId = caseId;
    this.hearingIds = hearingIds;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public static Builder masterCaseUpdatedForGroup() {
    return new uk.gov.justice.listing.events.MasterCaseUpdatedForGroup.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.MasterCaseUpdatedForGroup that = (uk.gov.justice.listing.events.MasterCaseUpdatedForGroup) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.hearingIds, that.hearingIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, hearingIds);}

  public static class Builder {
    private UUID caseId;

    private List<UUID> hearingIds;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
      return this;
    }

    public Builder withValuesFrom(final MasterCaseUpdatedForGroup masterCaseUpdatedForGroup) {
      this.caseId = masterCaseUpdatedForGroup.getCaseId();
      this.hearingIds = masterCaseUpdatedForGroup.getHearingIds();
      return this;
    }

    public MasterCaseUpdatedForGroup build() {
      return new uk.gov.justice.listing.events.MasterCaseUpdatedForGroup(caseId, hearingIds);
    }
  }
}
