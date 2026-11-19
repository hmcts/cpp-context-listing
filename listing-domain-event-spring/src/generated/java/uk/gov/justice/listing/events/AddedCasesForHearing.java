package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.event.added-cases-for-hearing")
public class AddedCasesForHearing implements Serializable {
  private static final long serialVersionUID = -48959413494803141L;

  private final UUID hearingId;

  private final List<ListedCase> unAllocatedListedCases;

  @JsonCreator
  public AddedCasesForHearing(final UUID hearingId, final List<ListedCase> unAllocatedListedCases) {
    this.hearingId = hearingId;
    this.unAllocatedListedCases = unAllocatedListedCases;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public List<ListedCase> getUnAllocatedListedCases() {
    return unAllocatedListedCases;
  }

  public static Builder addedCasesForHearing() {
    return new uk.gov.justice.listing.events.AddedCasesForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.AddedCasesForHearing that = (uk.gov.justice.listing.events.AddedCasesForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.unAllocatedListedCases, that.unAllocatedListedCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, unAllocatedListedCases);}

  public static class Builder {
    private UUID hearingId;

    private List<ListedCase> unAllocatedListedCases;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withUnAllocatedListedCases(final List<ListedCase> unAllocatedListedCases) {
      this.unAllocatedListedCases = unAllocatedListedCases;
      return this;
    }

    public Builder withValuesFrom(final AddedCasesForHearing addedCasesForHearing) {
      this.hearingId = addedCasesForHearing.getHearingId();
      this.unAllocatedListedCases = addedCasesForHearing.getUnAllocatedListedCases();
      return this;
    }

    public AddedCasesForHearing build() {
      return new uk.gov.justice.listing.events.AddedCasesForHearing(hearingId, unAllocatedListedCases);
    }
  }
}
