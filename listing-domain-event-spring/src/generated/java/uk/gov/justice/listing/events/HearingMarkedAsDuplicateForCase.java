package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class HearingMarkedAsDuplicateForCase implements Serializable {
  private static final long serialVersionUID = 5521334097589566090L;

  private final UUID caseId;

  private final UUID hearingId;

  @JsonCreator
  public HearingMarkedAsDuplicateForCase(final UUID caseId, final UUID hearingId) {
    this.caseId = caseId;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder hearingMarkedAsDuplicateForCase() {
    return new uk.gov.justice.listing.events.HearingMarkedAsDuplicateForCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingMarkedAsDuplicateForCase that = (uk.gov.justice.listing.events.HearingMarkedAsDuplicateForCase) obj;

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

    public Builder withValuesFrom(final HearingMarkedAsDuplicateForCase hearingMarkedAsDuplicateForCase) {
      this.caseId = hearingMarkedAsDuplicateForCase.getCaseId();
      this.hearingId = hearingMarkedAsDuplicateForCase.getHearingId();
      return this;
    }

    public HearingMarkedAsDuplicateForCase build() {
      return new uk.gov.justice.listing.events.HearingMarkedAsDuplicateForCase(caseId, hearingId);
    }
  }
}
