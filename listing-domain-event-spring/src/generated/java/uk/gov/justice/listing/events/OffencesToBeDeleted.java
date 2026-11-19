package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.offences-to-be-deleted")
public class OffencesToBeDeleted implements Serializable {
  private static final long serialVersionUID = 5714527378216727770L;

  private final UUID caseId;

  private final UUID defendantId;

  private final List<UUID> hearings;

  private final List<SimpleOffence> offences;

  @JsonCreator
  public OffencesToBeDeleted(final UUID caseId, final UUID defendantId, final List<UUID> hearings, final List<SimpleOffence> offences) {
    this.caseId = caseId;
    this.defendantId = defendantId;
    this.hearings = hearings;
    this.offences = offences;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public List<UUID> getHearings() {
    return hearings;
  }

  public List<SimpleOffence> getOffences() {
    return offences;
  }

  public static Builder offencesToBeDeleted() {
    return new uk.gov.justice.listing.events.OffencesToBeDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OffencesToBeDeleted that = (uk.gov.justice.listing.events.OffencesToBeDeleted) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.hearings, that.hearings) &&
    java.util.Objects.equals(this.offences, that.offences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, defendantId, hearings, offences);}

  public static class Builder {
    private UUID caseId;

    private UUID defendantId;

    private List<UUID> hearings;

    private List<SimpleOffence> offences;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withHearings(final List<UUID> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withOffences(final List<SimpleOffence> offences) {
      this.offences = offences;
      return this;
    }

    public Builder withValuesFrom(final OffencesToBeDeleted offencesToBeDeleted) {
      this.caseId = offencesToBeDeleted.getCaseId();
      this.defendantId = offencesToBeDeleted.getDefendantId();
      this.hearings = offencesToBeDeleted.getHearings();
      this.offences = offencesToBeDeleted.getOffences();
      return this;
    }

    public OffencesToBeDeleted build() {
      return new uk.gov.justice.listing.events.OffencesToBeDeleted(caseId, defendantId, hearings, offences);
    }
  }
}
