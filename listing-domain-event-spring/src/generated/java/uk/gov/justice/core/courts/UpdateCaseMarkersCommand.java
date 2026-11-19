package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class UpdateCaseMarkersCommand implements Serializable {
  private static final long serialVersionUID = -5554059416925317252L;

  private final List<Marker> caseMarkers;

  private final UUID hearingId;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public UpdateCaseMarkersCommand(final List<Marker> caseMarkers, final UUID hearingId, final UUID prosecutionCaseId) {
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

  public static Builder updateCaseMarkersCommand() {
    return new uk.gov.justice.core.courts.UpdateCaseMarkersCommand.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.UpdateCaseMarkersCommand that = (uk.gov.justice.core.courts.UpdateCaseMarkersCommand) obj;

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

    public Builder withValuesFrom(final UpdateCaseMarkersCommand updateCaseMarkersCommand) {
      this.caseMarkers = updateCaseMarkersCommand.getCaseMarkers();
      this.hearingId = updateCaseMarkersCommand.getHearingId();
      this.prosecutionCaseId = updateCaseMarkersCommand.getProsecutionCaseId();
      return this;
    }

    public UpdateCaseMarkersCommand build() {
      return new uk.gov.justice.core.courts.UpdateCaseMarkersCommand(caseMarkers, hearingId, prosecutionCaseId);
    }
  }
}
