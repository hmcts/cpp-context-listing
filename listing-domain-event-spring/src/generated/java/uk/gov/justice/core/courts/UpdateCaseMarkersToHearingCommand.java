package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class UpdateCaseMarkersToHearingCommand implements Serializable {
  private static final long serialVersionUID = -8667892929741573353L;

  private final List<Marker> caseMarkers;

  private final UUID hearingId;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public UpdateCaseMarkersToHearingCommand(final List<Marker> caseMarkers, final UUID hearingId, final UUID prosecutionCaseId) {
    this.caseMarkers = caseMarkers;
    this.hearingId = hearingId;
    this.prosecutionCaseId = prosecutionCaseId;
  }

  public List<Marker> getCaseMarkers() {
    return caseMarkers;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public static Builder updateCaseMarkersToHearingCommand() {
    return new uk.gov.justice.core.courts.UpdateCaseMarkersToHearingCommand.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.UpdateCaseMarkersToHearingCommand that = (uk.gov.justice.core.courts.UpdateCaseMarkersToHearingCommand) obj;

    return java.util.Objects.equals(this.caseMarkers, that.caseMarkers) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseMarkers, hearingId, prosecutionCaseId);}

  public static class Builder {
    private List<Marker> caseMarkers;

    private UUID hearingId;

    private UUID prosecutionCaseId;

    public Builder withCaseMarkers(final List<Marker> caseMarkers) {
      this.caseMarkers = caseMarkers;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withValuesFrom(final UpdateCaseMarkersToHearingCommand updateCaseMarkersToHearingCommand) {
      this.caseMarkers = updateCaseMarkersToHearingCommand.getCaseMarkers();
      this.hearingId = updateCaseMarkersToHearingCommand.getHearingId();
      this.prosecutionCaseId = updateCaseMarkersToHearingCommand.getProsecutionCaseId();
      return this;
    }

    public UpdateCaseMarkersToHearingCommand build() {
      return new uk.gov.justice.core.courts.UpdateCaseMarkersToHearingCommand(caseMarkers, hearingId, prosecutionCaseId);
    }
  }
}
