package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class MarkUnallocatedHearingAsDuplicate implements Serializable {
  private static final long serialVersionUID = -3114332993650857414L;

  private final UUID hearingId;

  @JsonCreator
  public MarkUnallocatedHearingAsDuplicate(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder markUnallocatedHearingAsDuplicate() {
    return new uk.gov.justice.listing.courts.MarkUnallocatedHearingAsDuplicate.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.MarkUnallocatedHearingAsDuplicate that = (uk.gov.justice.listing.courts.MarkUnallocatedHearingAsDuplicate) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId);}

  public static class Builder {
    private UUID hearingId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final MarkUnallocatedHearingAsDuplicate markUnallocatedHearingAsDuplicate) {
      this.hearingId = markUnallocatedHearingAsDuplicate.getHearingId();
      return this;
    }

    public MarkUnallocatedHearingAsDuplicate build() {
      return new uk.gov.justice.listing.courts.MarkUnallocatedHearingAsDuplicate(hearingId);
    }
  }
}
