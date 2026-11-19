package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class RemovePartiallyMergedOffencesFromOriginalHearing implements Serializable {
  private static final long serialVersionUID = -6227122385704246944L;

  private final UUID hearingIdToBeUpdated;

  private final List<ProsecutionCasesToRemove> prosecutionCasesToRemove;

  @JsonCreator
  public RemovePartiallyMergedOffencesFromOriginalHearing(final UUID hearingIdToBeUpdated, final List<ProsecutionCasesToRemove> prosecutionCasesToRemove) {
    this.hearingIdToBeUpdated = hearingIdToBeUpdated;
    this.prosecutionCasesToRemove = prosecutionCasesToRemove;
  }

  public UUID getHearingIdToBeUpdated() {
    return hearingIdToBeUpdated;
  }

  public List<ProsecutionCasesToRemove> getProsecutionCasesToRemove() {
    return prosecutionCasesToRemove;
  }

  public static Builder removePartiallyMergedOffencesFromOriginalHearing() {
    return new uk.gov.justice.listing.courts.RemovePartiallyMergedOffencesFromOriginalHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.RemovePartiallyMergedOffencesFromOriginalHearing that = (uk.gov.justice.listing.courts.RemovePartiallyMergedOffencesFromOriginalHearing) obj;

    return java.util.Objects.equals(this.hearingIdToBeUpdated, that.hearingIdToBeUpdated) &&
    java.util.Objects.equals(this.prosecutionCasesToRemove, that.prosecutionCasesToRemove);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIdToBeUpdated, prosecutionCasesToRemove);}

  public static class Builder {
    private UUID hearingIdToBeUpdated;

    private List<ProsecutionCasesToRemove> prosecutionCasesToRemove;

    public Builder withHearingIdToBeUpdated(final UUID hearingIdToBeUpdated) {
      this.hearingIdToBeUpdated = hearingIdToBeUpdated;
      return this;
    }

    public Builder withProsecutionCasesToRemove(final List<ProsecutionCasesToRemove> prosecutionCasesToRemove) {
      this.prosecutionCasesToRemove = prosecutionCasesToRemove;
      return this;
    }

    public Builder withValuesFrom(final RemovePartiallyMergedOffencesFromOriginalHearing removePartiallyMergedOffencesFromOriginalHearing) {
      this.hearingIdToBeUpdated = removePartiallyMergedOffencesFromOriginalHearing.getHearingIdToBeUpdated();
      this.prosecutionCasesToRemove = removePartiallyMergedOffencesFromOriginalHearing.getProsecutionCasesToRemove();
      return this;
    }

    public RemovePartiallyMergedOffencesFromOriginalHearing build() {
      return new uk.gov.justice.listing.courts.RemovePartiallyMergedOffencesFromOriginalHearing(hearingIdToBeUpdated, prosecutionCasesToRemove);
    }
  }
}
