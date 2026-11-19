package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.commands.Defendant;

public class UpdateDefendantsForHearing implements Serializable {
  private static final long serialVersionUID = 4517403454346401858L;

  private final UUID caseId;

  private final List<Defendant> defendants;

  private final UUID hearingId;

  @JsonCreator
  public UpdateDefendantsForHearing(final UUID caseId, final List<Defendant> defendants, final UUID hearingId) {
    this.caseId = caseId;
    this.defendants = defendants;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder updateDefendantsForHearing() {
    return new uk.gov.justice.listing.courts.UpdateDefendantsForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateDefendantsForHearing that = (uk.gov.justice.listing.courts.UpdateDefendantsForHearing) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendants, hearingId);}

  public static class Builder {
    private UUID caseId;

    private List<Defendant> defendants;

    private UUID hearingId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final UpdateDefendantsForHearing updateDefendantsForHearing) {
      this.caseId = updateDefendantsForHearing.getCaseId();
      this.defendants = updateDefendantsForHearing.getDefendants();
      this.hearingId = updateDefendantsForHearing.getHearingId();
      return this;
    }

    public UpdateDefendantsForHearing build() {
      return new uk.gov.justice.listing.courts.UpdateDefendantsForHearing(caseId, defendants, hearingId);
    }
  }
}
