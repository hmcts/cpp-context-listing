package uk.gov.justice.hearing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class MarkedAsDuplicate implements Serializable {
  private static final long serialVersionUID = -6476446444887276729L;

  private final List<UUID> defendantIds;

  private final UUID hearingId;

  private final List<UUID> offenceIds;

  private final List<UUID> prosecutionCaseIds;

  @JsonCreator
  public MarkedAsDuplicate(final List<UUID> defendantIds, final UUID hearingId, final List<UUID> offenceIds, final List<UUID> prosecutionCaseIds) {
    this.defendantIds = defendantIds;
    this.hearingId = hearingId;
    this.offenceIds = offenceIds;
    this.prosecutionCaseIds = prosecutionCaseIds;
  }

  public List<UUID> getDefendantIds() {
    return defendantIds;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<UUID> getOffenceIds() {
    return offenceIds;
  }

  public List<UUID> getProsecutionCaseIds() {
    return prosecutionCaseIds;
  }

  public static Builder markedAsDuplicate() {
    return new uk.gov.justice.hearing.courts.MarkedAsDuplicate.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.hearing.courts.MarkedAsDuplicate that = (uk.gov.justice.hearing.courts.MarkedAsDuplicate) obj;

    return java.util.Objects.equals(this.defendantIds, that.defendantIds) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.offenceIds, that.offenceIds) &&
    java.util.Objects.equals(this.prosecutionCaseIds, that.prosecutionCaseIds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantIds, hearingId, offenceIds, prosecutionCaseIds);}

  public static class Builder {
    private List<UUID> defendantIds;

    private UUID hearingId;

    private List<UUID> offenceIds;

    private List<UUID> prosecutionCaseIds;

    public Builder withDefendantIds(final List<UUID> defendantIds) {
      this.defendantIds = defendantIds;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withOffenceIds(final List<UUID> offenceIds) {
      this.offenceIds = offenceIds;
      return this;
    }

    public Builder withProsecutionCaseIds(final List<UUID> prosecutionCaseIds) {
      this.prosecutionCaseIds = prosecutionCaseIds;
      return this;
    }

    public Builder withValuesFrom(final MarkedAsDuplicate markedAsDuplicate) {
      this.defendantIds = markedAsDuplicate.getDefendantIds();
      this.hearingId = markedAsDuplicate.getHearingId();
      this.offenceIds = markedAsDuplicate.getOffenceIds();
      this.prosecutionCaseIds = markedAsDuplicate.getProsecutionCaseIds();
      return this;
    }

    public MarkedAsDuplicate build() {
      return new uk.gov.justice.hearing.courts.MarkedAsDuplicate(defendantIds, hearingId, offenceIds, prosecutionCaseIds);
    }
  }
}
