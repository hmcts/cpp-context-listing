package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class HearingPartiallyUpdated implements Serializable {
  private static final long serialVersionUID = 2018606168314334186L;

  private final UUID hearingIdToBeUpdated;

  private final List<ProsecutionCases> prosecutionCases;

  @JsonCreator
  public HearingPartiallyUpdated(final UUID hearingIdToBeUpdated, final List<ProsecutionCases> prosecutionCases) {
    this.hearingIdToBeUpdated = hearingIdToBeUpdated;
    this.prosecutionCases = prosecutionCases;
  }

  public UUID getHearingIdToBeUpdated() {
    return hearingIdToBeUpdated;
  }

  public List<ProsecutionCases> getProsecutionCases() {
    return prosecutionCases;
  }

  public static Builder hearingPartiallyUpdated() {
    return new uk.gov.justice.listing.courts.HearingPartiallyUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.HearingPartiallyUpdated that = (uk.gov.justice.listing.courts.HearingPartiallyUpdated) obj;

    return java.util.Objects.equals(this.hearingIdToBeUpdated, that.hearingIdToBeUpdated) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIdToBeUpdated, prosecutionCases);}

  public static class Builder {
    private UUID hearingIdToBeUpdated;

    private List<ProsecutionCases> prosecutionCases;

    public Builder withHearingIdToBeUpdated(final UUID hearingIdToBeUpdated) {
      this.hearingIdToBeUpdated = hearingIdToBeUpdated;
      return this;
    }

    public Builder withProsecutionCases(final List<ProsecutionCases> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withValuesFrom(final HearingPartiallyUpdated hearingPartiallyUpdated) {
      this.hearingIdToBeUpdated = hearingPartiallyUpdated.getHearingIdToBeUpdated();
      this.prosecutionCases = hearingPartiallyUpdated.getProsecutionCases();
      return this;
    }

    public HearingPartiallyUpdated build() {
      return new uk.gov.justice.listing.courts.HearingPartiallyUpdated(hearingIdToBeUpdated, prosecutionCases);
    }
  }
}
