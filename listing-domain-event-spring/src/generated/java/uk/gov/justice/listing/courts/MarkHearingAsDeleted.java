package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class MarkHearingAsDeleted implements Serializable {
  private static final long serialVersionUID = -2173907796583845333L;

  private final UUID hearingIdToMarkAsDeleted;

  @JsonCreator
  public MarkHearingAsDeleted(final UUID hearingIdToMarkAsDeleted) {
    this.hearingIdToMarkAsDeleted = hearingIdToMarkAsDeleted;
  }

  public UUID getHearingIdToMarkAsDeleted() {
    return hearingIdToMarkAsDeleted;
  }

  public static Builder markHearingAsDeleted() {
    return new uk.gov.justice.listing.courts.MarkHearingAsDeleted.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.MarkHearingAsDeleted that = (uk.gov.justice.listing.courts.MarkHearingAsDeleted) obj;

    return java.util.Objects.equals(this.hearingIdToMarkAsDeleted, that.hearingIdToMarkAsDeleted);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIdToMarkAsDeleted);}

  public static class Builder {
    private UUID hearingIdToMarkAsDeleted;

    public Builder withHearingIdToMarkAsDeleted(final UUID hearingIdToMarkAsDeleted) {
      this.hearingIdToMarkAsDeleted = hearingIdToMarkAsDeleted;
      return this;
    }

    public Builder withValuesFrom(final MarkHearingAsDeleted markHearingAsDeleted) {
      this.hearingIdToMarkAsDeleted = markHearingAsDeleted.getHearingIdToMarkAsDeleted();
      return this;
    }

    public MarkHearingAsDeleted build() {
      return new uk.gov.justice.listing.courts.MarkHearingAsDeleted(hearingIdToMarkAsDeleted);
    }
  }
}
