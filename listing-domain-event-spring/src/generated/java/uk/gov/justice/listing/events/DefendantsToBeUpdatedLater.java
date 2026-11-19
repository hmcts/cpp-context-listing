package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.defendants-to-be-updated-later")
public class DefendantsToBeUpdatedLater implements Serializable {
  private static final long serialVersionUID = -4730577818413474521L;

  private final UUID caseId;

  private final List<Defendant> defendants;

  @JsonCreator
  public DefendantsToBeUpdatedLater(final UUID caseId, final List<Defendant> defendants) {
    this.caseId = caseId;
    this.defendants = defendants;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public static Builder defendantsToBeUpdatedLater() {
    return new uk.gov.justice.listing.events.DefendantsToBeUpdatedLater.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DefendantsToBeUpdatedLater that = (uk.gov.justice.listing.events.DefendantsToBeUpdatedLater) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendants, that.defendants);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendants);}

  public static class Builder {
    private UUID caseId;

    private List<Defendant> defendants;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withValuesFrom(final DefendantsToBeUpdatedLater defendantsToBeUpdatedLater) {
      this.caseId = defendantsToBeUpdatedLater.getCaseId();
      this.defendants = defendantsToBeUpdatedLater.getDefendants();
      return this;
    }

    public DefendantsToBeUpdatedLater build() {
      return new uk.gov.justice.listing.events.DefendantsToBeUpdatedLater(caseId, defendants);
    }
  }
}
