package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.offence-added")
public class OffenceAdded implements Serializable {
  private static final long serialVersionUID = -1685985291250586305L;

  private final UUID caseId;

  private final UUID defendantId;

  private final UUID hearingId;

  private final Offence offence;

  @JsonCreator
  public OffenceAdded(final UUID caseId, final UUID defendantId, final UUID hearingId, final Offence offence) {
    this.caseId = caseId;
    this.defendantId = defendantId;
    this.hearingId = hearingId;
    this.offence = offence;
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

  public Offence getOffence() {
    return offence;
  }

  public static Builder offenceAdded() {
    return new uk.gov.justice.listing.events.OffenceAdded.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OffenceAdded that = (uk.gov.justice.listing.events.OffenceAdded) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.offence, that.offence);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendantId, hearingId, offence);}

  public static class Builder {
    private UUID caseId;

    private UUID defendantId;

    private UUID hearingId;

    private Offence offence;

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

    public Builder withOffence(final Offence offence) {
      this.offence = offence;
      return this;
    }

    public Builder withValuesFrom(final OffenceAdded offenceAdded) {
      this.caseId = offenceAdded.getCaseId();
      this.defendantId = offenceAdded.getDefendantId();
      this.hearingId = offenceAdded.getHearingId();
      this.offence = offenceAdded.getOffence();
      return this;
    }

    public OffenceAdded build() {
      return new uk.gov.justice.listing.events.OffenceAdded(caseId, defendantId, hearingId, offence);
    }
  }
}
