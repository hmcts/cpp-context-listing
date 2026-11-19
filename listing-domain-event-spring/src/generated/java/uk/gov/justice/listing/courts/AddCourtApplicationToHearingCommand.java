package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class AddCourtApplicationToHearingCommand implements Serializable {
  private static final long serialVersionUID = -6401640461702178305L;

  private final UUID applicationId;

  private final UUID hearingId;

  @JsonCreator
  public AddCourtApplicationToHearingCommand(final UUID applicationId, final UUID hearingId) {
    this.applicationId = applicationId;
    this.hearingId = hearingId;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder addCourtApplicationToHearingCommand() {
    return new uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand that = (uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, hearingId);}

  public static class Builder {
    private UUID applicationId;

    private UUID hearingId;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final AddCourtApplicationToHearingCommand addCourtApplicationToHearingCommand) {
      this.applicationId = addCourtApplicationToHearingCommand.getApplicationId();
      this.hearingId = addCourtApplicationToHearingCommand.getHearingId();
      return this;
    }

    public AddCourtApplicationToHearingCommand build() {
      return new uk.gov.justice.listing.courts.AddCourtApplicationToHearingCommand(applicationId, hearingId);
    }
  }
}
