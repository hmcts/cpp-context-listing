package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class DeleteHearingCommand implements Serializable {
  private static final long serialVersionUID = 6826587594307913934L;

  private final UUID hearingId;

  private final UUID seedingHearingId;

  @JsonCreator
  public DeleteHearingCommand(final UUID hearingId, final UUID seedingHearingId) {
    this.hearingId = hearingId;
    this.seedingHearingId = seedingHearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getSeedingHearingId() {
    return seedingHearingId;
  }

  public static Builder deleteHearingCommand() {
    return new uk.gov.justice.listing.courts.DeleteHearingCommand.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.DeleteHearingCommand that = (uk.gov.justice.listing.courts.DeleteHearingCommand) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.seedingHearingId, that.seedingHearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, seedingHearingId);}

  public static class Builder {
    private UUID hearingId;

    private UUID seedingHearingId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withSeedingHearingId(final UUID seedingHearingId) {
      this.seedingHearingId = seedingHearingId;
      return this;
    }

    public Builder withValuesFrom(final DeleteHearingCommand deleteHearingCommand) {
      this.hearingId = deleteHearingCommand.getHearingId();
      this.seedingHearingId = deleteHearingCommand.getSeedingHearingId();
      return this;
    }

    public DeleteHearingCommand build() {
      return new uk.gov.justice.listing.courts.DeleteHearingCommand(hearingId, seedingHearingId);
    }
  }
}
