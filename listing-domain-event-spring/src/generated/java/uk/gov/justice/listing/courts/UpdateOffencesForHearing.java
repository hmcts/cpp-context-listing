package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.commands.Offence;

public class UpdateOffencesForHearing implements Serializable {
  private static final long serialVersionUID = -2619644771746664968L;

  private final UUID caseId;

  private final UUID defendantId;

  private final UUID hearingId;

  private final List<Offence> offences;

  @JsonCreator
  public UpdateOffencesForHearing(final UUID caseId, final UUID defendantId, final UUID hearingId, final List<Offence> offences) {
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

  public List<Offence> getOffences() {
    return offences;
  }

  public static Builder updateOffencesForHearing() {
    return new uk.gov.justice.listing.courts.UpdateOffencesForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateOffencesForHearing that = (uk.gov.justice.listing.courts.UpdateOffencesForHearing) obj;

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

    private List<Offence> offences;

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

    public Builder withOffences(final List<Offence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withValuesFrom(final UpdateOffencesForHearing updateOffencesForHearing) {
      this.caseId = updateOffencesForHearing.getCaseId();
      this.defendantId = updateOffencesForHearing.getDefendantId();
      this.hearingId = updateOffencesForHearing.getHearingId();
      this.offences = updateOffencesForHearing.getOffences();
      return this;
    }

    public UpdateOffencesForHearing build() {
      return new uk.gov.justice.listing.courts.UpdateOffencesForHearing(caseId, defendantId, hearingId, offences);
    }
  }
}
