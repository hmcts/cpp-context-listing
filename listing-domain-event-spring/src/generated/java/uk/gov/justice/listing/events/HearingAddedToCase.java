package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class HearingAddedToCase implements Serializable {
  private static final long serialVersionUID = -2976461574965711541L;

  private final UUID caseId;

  private final UUID hearingId;

  @JsonCreator
  public HearingAddedToCase(final UUID caseId, final UUID hearingId) {
    this.caseId = caseId;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingAddedToCase() {
    return new uk.gov.justice.listing.events.HearingAddedToCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingAddedToCase that = (uk.gov.justice.listing.events.HearingAddedToCase) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, hearingId);}

  public static class Builder {
    private UUID caseId;

    private UUID hearingId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final HearingAddedToCase hearingAddedToCase) {
      this.caseId = hearingAddedToCase.getCaseId();
      this.hearingId = hearingAddedToCase.getHearingId();
      return this;
    }

    public HearingAddedToCase build() {
      return new uk.gov.justice.listing.events.HearingAddedToCase(caseId, hearingId);
    }
  }
}
