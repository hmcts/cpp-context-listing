package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class HearingPartiallyUpdated implements Serializable {
  private static final long serialVersionUID = 8670700731056116778L;

  private final UUID hearingIdToBeUpdated;

  private final List<ProsecutionCases> prosecutionCases;

  private final String splitHearing;

  @JsonCreator
  public HearingPartiallyUpdated(final UUID hearingIdToBeUpdated, final List<ProsecutionCases> prosecutionCases, final String splitHearing) {
    this.hearingIdToBeUpdated = hearingIdToBeUpdated;
    this.prosecutionCases = prosecutionCases;
    this.splitHearing = splitHearing;
  }

  public UUID getHearingIdToBeUpdated() {
    return hearingIdToBeUpdated;
  }

  public List<ProsecutionCases> getProsecutionCases() {
    return prosecutionCases;
  }

  public String getSplitHearing() {
    return splitHearing;
  }

  public static Builder hearingPartiallyUpdated() {
    return new uk.gov.justice.listing.events.HearingPartiallyUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingPartiallyUpdated that = (uk.gov.justice.listing.events.HearingPartiallyUpdated) obj;

    return java.util.Objects.equals(this.hearingIdToBeUpdated, that.hearingIdToBeUpdated) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.splitHearing, that.splitHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIdToBeUpdated, prosecutionCases, splitHearing);}

  public static class Builder {
    private UUID hearingIdToBeUpdated;

    private List<ProsecutionCases> prosecutionCases;

    private String splitHearing;

    public Builder withHearingIdToBeUpdated(final UUID hearingIdToBeUpdated) {
      this.hearingIdToBeUpdated = hearingIdToBeUpdated;
      return this;
    }

    public Builder withProsecutionCases(final List<ProsecutionCases> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withSplitHearing(final String splitHearing) {
      this.splitHearing = splitHearing;
      return this;
    }

    public Builder withValuesFrom(final HearingPartiallyUpdated hearingPartiallyUpdated) {
      this.hearingIdToBeUpdated = hearingPartiallyUpdated.getHearingIdToBeUpdated();
      this.prosecutionCases = hearingPartiallyUpdated.getProsecutionCases();
      this.splitHearing = hearingPartiallyUpdated.getSplitHearing();
      return this;
    }

    public HearingPartiallyUpdated build() {
      return new uk.gov.justice.listing.events.HearingPartiallyUpdated(hearingIdToBeUpdated, prosecutionCases, splitHearing);
    }
  }
}
