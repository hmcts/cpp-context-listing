package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class DeleteCourtApplicationHearing implements Serializable {
  private static final long serialVersionUID = 8853962013672840378L;

  private final UUID hearingId;

  @JsonCreator
  public DeleteCourtApplicationHearing(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder deleteCourtApplicationHearing() {
    return new uk.gov.justice.listing.commands.DeleteCourtApplicationHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.DeleteCourtApplicationHearing that = (uk.gov.justice.listing.commands.DeleteCourtApplicationHearing) obj;

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

    public Builder withValuesFrom(final DeleteCourtApplicationHearing deleteCourtApplicationHearing) {
      this.hearingId = deleteCourtApplicationHearing.getHearingId();
      return this;
    }

    public DeleteCourtApplicationHearing build() {
      return new uk.gov.justice.listing.commands.DeleteCourtApplicationHearing(hearingId);
    }
  }
}
