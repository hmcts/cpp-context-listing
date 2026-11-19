package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class MarkHearingAsDuplicateForCase implements Serializable {
  private static final long serialVersionUID = -4181931228145118366L;

  private final UUID caseId;

  private final UUID hearingId;

  @JsonCreator
  public MarkHearingAsDuplicateForCase(final UUID caseId, final UUID hearingId) {
    this.caseId = caseId;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder markHearingAsDuplicateForCase() {
    return new uk.gov.justice.listing.courts.MarkHearingAsDuplicateForCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.MarkHearingAsDuplicateForCase that = (uk.gov.justice.listing.courts.MarkHearingAsDuplicateForCase) obj;

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

    public Builder withValuesFrom(final MarkHearingAsDuplicateForCase markHearingAsDuplicateForCase) {
      this.caseId = markHearingAsDuplicateForCase.getCaseId();
      this.hearingId = markHearingAsDuplicateForCase.getHearingId();
      return this;
    }

    public MarkHearingAsDuplicateForCase build() {
      return new uk.gov.justice.listing.courts.MarkHearingAsDuplicateForCase(caseId, hearingId);
    }
  }
}
