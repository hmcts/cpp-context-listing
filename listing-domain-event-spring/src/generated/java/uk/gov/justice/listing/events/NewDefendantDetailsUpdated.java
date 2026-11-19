package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class NewDefendantDetailsUpdated implements Serializable {
  private static final long serialVersionUID = -5509604120113450525L;

  private final UUID caseId;

  private final NewBaseDefendant defendant;

  private final UUID hearingId;

  @JsonCreator
  public NewDefendantDetailsUpdated(final UUID caseId, final NewBaseDefendant defendant, final UUID hearingId) {
    this.caseId = caseId;
    this.defendant = defendant;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public NewBaseDefendant getDefendant() {
    return defendant;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder newDefendantDetailsUpdated() {
    return new uk.gov.justice.listing.events.NewDefendantDetailsUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NewDefendantDetailsUpdated that = (uk.gov.justice.listing.events.NewDefendantDetailsUpdated) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendant, that.defendant) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendant, hearingId);}

  public static class Builder {
    private UUID caseId;

    private NewBaseDefendant defendant;

    private UUID hearingId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendant(final NewBaseDefendant defendant) {
      this.defendant = defendant;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final NewDefendantDetailsUpdated newDefendantDetailsUpdated) {
      this.caseId = newDefendantDetailsUpdated.getCaseId();
      this.defendant = newDefendantDetailsUpdated.getDefendant();
      this.hearingId = newDefendantDetailsUpdated.getHearingId();
      return this;
    }

    public NewDefendantDetailsUpdated build() {
      return new uk.gov.justice.listing.events.NewDefendantDetailsUpdated(caseId, defendant, hearingId);
    }
  }
}
