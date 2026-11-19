package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class DeleteHearing implements Serializable {
  private static final long serialVersionUID = 7252999289976591996L;

  private final UUID hearingId;

  @JsonCreator
  public DeleteHearing(final UUID hearingId) {
    this.hearingId = hearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder deleteHearing() {
    return new uk.gov.justice.listing.commands.DeleteHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.DeleteHearing that = (uk.gov.justice.listing.commands.DeleteHearing) obj;

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

    public Builder withValuesFrom(final DeleteHearing deleteHearing) {
      this.hearingId = deleteHearing.getHearingId();
      return this;
    }

    public DeleteHearing build() {
      return new uk.gov.justice.listing.commands.DeleteHearing(hearingId);
    }
  }
}
