package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class HearingListedCaseUpdated implements Serializable {
  private static final long serialVersionUID = 641333116650746455L;

  private final Hearing hearing;

  private final UUID hearingIdToBeDeleted;

  private final List<ListedCase> unAllocatedListedCases;

  @JsonCreator
  public HearingListedCaseUpdated(final Hearing hearing, final UUID hearingIdToBeDeleted, final List<ListedCase> unAllocatedListedCases) {
    this.hearing = hearing;
    this.hearingIdToBeDeleted = hearingIdToBeDeleted;
    this.unAllocatedListedCases = unAllocatedListedCases;
  }

  public Hearing getHearing() {
    return hearing;
  }

  public UUID getHearingIdToBeDeleted() {
    return hearingIdToBeDeleted;
  }

  public List<ListedCase> getUnAllocatedListedCases() {
    return unAllocatedListedCases;
  }

  public static Builder hearingListedCaseUpdated() {
    return new uk.gov.justice.listing.events.HearingListedCaseUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingListedCaseUpdated that = (uk.gov.justice.listing.events.HearingListedCaseUpdated) obj;

    return java.util.Objects.equals(this.hearing, that.hearing) &&
    java.util.Objects.equals(this.hearingIdToBeDeleted, that.hearingIdToBeDeleted) &&
    java.util.Objects.equals(this.unAllocatedListedCases, that.unAllocatedListedCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearing, hearingIdToBeDeleted, unAllocatedListedCases);}

  public static class Builder {
    private Hearing hearing;

    private UUID hearingIdToBeDeleted;

    private List<ListedCase> unAllocatedListedCases;

    public Builder withHearing(final Hearing hearing) {
      this.hearing = hearing;
      return this;
    }

    public Builder withHearingIdToBeDeleted(final UUID hearingIdToBeDeleted) {
      this.hearingIdToBeDeleted = hearingIdToBeDeleted;
      return this;
    }

    public Builder withUnAllocatedListedCases(final List<ListedCase> unAllocatedListedCases) {
      this.unAllocatedListedCases = unAllocatedListedCases;
      return this;
    }

    public Builder withValuesFrom(final HearingListedCaseUpdated hearingListedCaseUpdated) {
      this.hearing = hearingListedCaseUpdated.getHearing();
      this.hearingIdToBeDeleted = hearingListedCaseUpdated.getHearingIdToBeDeleted();
      this.unAllocatedListedCases = hearingListedCaseUpdated.getUnAllocatedListedCases();
      return this;
    }

    public HearingListedCaseUpdated build() {
      return new uk.gov.justice.listing.events.HearingListedCaseUpdated(hearing, hearingIdToBeDeleted, unAllocatedListedCases);
    }
  }
}
