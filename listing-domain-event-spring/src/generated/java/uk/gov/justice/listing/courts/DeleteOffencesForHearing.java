package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.commands.SimpleOffence;

public class DeleteOffencesForHearing implements Serializable {
  private static final long serialVersionUID = -2477885670629621568L;

  private final UUID caseId;

  private final UUID defendantId;

  private final UUID hearingId;

  private final List<SimpleOffence> offences;

  @JsonCreator
  public DeleteOffencesForHearing(final UUID caseId, final UUID defendantId, final UUID hearingId, final List<SimpleOffence> offences) {
    this.caseId = caseId;
    this.defendantId = defendantId;
    this.hearingId = hearingId;
    this.offences = offences;
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

  public List<SimpleOffence> getOffences() {
    return offences;
  }

  public static Builder deleteOffencesForHearing() {
    return new uk.gov.justice.listing.courts.DeleteOffencesForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.DeleteOffencesForHearing that = (uk.gov.justice.listing.courts.DeleteOffencesForHearing) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.offences, that.offences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendantId, hearingId, offences);}

  public static class Builder {
    private UUID caseId;

    private UUID defendantId;

    private UUID hearingId;

    private List<SimpleOffence> offences;

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

    public Builder withOffences(final List<SimpleOffence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withValuesFrom(final DeleteOffencesForHearing deleteOffencesForHearing) {
      this.caseId = deleteOffencesForHearing.getCaseId();
      this.defendantId = deleteOffencesForHearing.getDefendantId();
      this.hearingId = deleteOffencesForHearing.getHearingId();
      this.offences = deleteOffencesForHearing.getOffences();
      return this;
    }

    public DeleteOffencesForHearing build() {
      return new uk.gov.justice.listing.courts.DeleteOffencesForHearing(caseId, defendantId, hearingId, offences);
    }
  }
}
