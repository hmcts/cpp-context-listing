package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class AddHearingToCaseCommand implements Serializable {
  private static final long serialVersionUID = -3651786490153555329L;

  private final UUID caseId;

  private final UUID hearingId;

  @JsonCreator
  public AddHearingToCaseCommand(final UUID caseId, final UUID hearingId) {
    this.caseId = caseId;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder addHearingToCaseCommand() {
    return new uk.gov.justice.listing.commands.AddHearingToCaseCommand.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.AddHearingToCaseCommand that = (uk.gov.justice.listing.commands.AddHearingToCaseCommand) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, hearingId);}

  public static class Builder {
    private UUID caseId;

    private UUID hearingId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final AddHearingToCaseCommand addHearingToCaseCommand) {
      this.caseId = addHearingToCaseCommand.getCaseId();
      this.hearingId = addHearingToCaseCommand.getHearingId();
      return this;
    }

    public AddHearingToCaseCommand build() {
      return new uk.gov.justice.listing.commands.AddHearingToCaseCommand(caseId, hearingId);
    }
  }
}
