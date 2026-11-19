package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class DefendantsToBeUpdated implements Serializable {
  private static final long serialVersionUID = 9061038558079300910L;

  private final UUID caseId;

  private final List<Defendant> defendants;

  private final List<UUID> hearings;

  @JsonCreator
  public DefendantsToBeUpdated(final UUID caseId, final List<Defendant> defendants, final List<UUID> hearings) {
    this.caseId = caseId;
    this.defendants = defendants;
    this.hearings = hearings;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public List<UUID> getHearings() {
    return hearings;
  }

  public static Builder defendantsToBeUpdated() {
    return new uk.gov.justice.listing.events.DefendantsToBeUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DefendantsToBeUpdated that = (uk.gov.justice.listing.events.DefendantsToBeUpdated) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.hearings, that.hearings);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendants, hearings);}

  public static class Builder {
    private UUID caseId;

    private List<Defendant> defendants;

    private List<UUID> hearings;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withHearings(final List<UUID> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withValuesFrom(final DefendantsToBeUpdated defendantsToBeUpdated) {
      this.caseId = defendantsToBeUpdated.getCaseId();
      this.defendants = defendantsToBeUpdated.getDefendants();
      this.hearings = defendantsToBeUpdated.getHearings();
      return this;
    }

    public DefendantsToBeUpdated build() {
      return new uk.gov.justice.listing.events.DefendantsToBeUpdated(caseId, defendants, hearings);
    }
  }
}
