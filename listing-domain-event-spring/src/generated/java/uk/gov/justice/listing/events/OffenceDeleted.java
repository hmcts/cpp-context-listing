package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.offence-deleted")
public class OffenceDeleted implements Serializable {
  private static final long serialVersionUID = 6215577240355177149L;

  private final UUID caseId;

  private final UUID defendantId;

  private final UUID hearingId;

  private final UUID offenceId;

  @JsonCreator
  public OffenceDeleted(final UUID caseId, final UUID defendantId, final UUID hearingId, final UUID offenceId) {
    this.caseId = caseId;
    this.defendantId = defendantId;
    this.hearingId = hearingId;
    this.offenceId = offenceId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public static Builder offenceDeleted() {
    return new uk.gov.justice.listing.events.OffenceDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OffenceDeleted that = (uk.gov.justice.listing.events.OffenceDeleted) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.offenceId, that.offenceId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendantId, hearingId, offenceId);}

  public static class Builder {
    private UUID caseId;

    private UUID defendantId;

    private UUID hearingId;

    private UUID offenceId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withOffenceId(final UUID offenceId) {
      this.offenceId = offenceId;
      return this;
    }

    public Builder withValuesFrom(final OffenceDeleted offenceDeleted) {
      this.caseId = offenceDeleted.getCaseId();
      this.defendantId = offenceDeleted.getDefendantId();
      this.hearingId = offenceDeleted.getHearingId();
      this.offenceId = offenceDeleted.getOffenceId();
      return this;
    }

    public OffenceDeleted build() {
      return new uk.gov.justice.listing.events.OffenceDeleted(caseId, defendantId, hearingId, offenceId);
    }
  }
}
