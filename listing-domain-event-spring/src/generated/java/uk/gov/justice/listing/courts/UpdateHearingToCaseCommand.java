package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class UpdateHearingToCaseCommand implements Serializable {
  private static final long serialVersionUID = -5916417040024391763L;

  private final UUID caseId;

  private final UUID existingHearingId;

  private final UUID id;

  @JsonCreator
  public UpdateHearingToCaseCommand(final UUID caseId, final UUID existingHearingId, final UUID id) {
    this.caseId = caseId;
    this.existingHearingId = existingHearingId;
    this.id = id;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getExistingHearingId() {
    return existingHearingId;
  }

  public UUID getId() {
    return id;
  }

  public static Builder updateHearingToCaseCommand() {
    return new uk.gov.justice.listing.courts.UpdateHearingToCaseCommand.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateHearingToCaseCommand that = (uk.gov.justice.listing.courts.UpdateHearingToCaseCommand) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.existingHearingId, that.existingHearingId) &&
    java.util.Objects.equals(this.id, that.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, existingHearingId, id);}

  public static class Builder {
    private UUID caseId;

    private UUID existingHearingId;

    private UUID id;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withExistingHearingId(final UUID existingHearingId) {
      this.existingHearingId = existingHearingId;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withValuesFrom(final UpdateHearingToCaseCommand updateHearingToCaseCommand) {
      this.caseId = updateHearingToCaseCommand.getCaseId();
      this.existingHearingId = updateHearingToCaseCommand.getExistingHearingId();
      this.id = updateHearingToCaseCommand.getId();
      return this;
    }

    public UpdateHearingToCaseCommand build() {
      return new uk.gov.justice.listing.courts.UpdateHearingToCaseCommand(caseId, existingHearingId, id);
    }
  }
}
