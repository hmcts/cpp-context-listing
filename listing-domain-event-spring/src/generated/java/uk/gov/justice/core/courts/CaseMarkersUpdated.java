package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class CaseMarkersUpdated implements Serializable {
  private static final long serialVersionUID = 5448211026520752259L;

  private final List<Marker> caseMarkers;

  private final UUID hearingId;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public CaseMarkersUpdated(final List<Marker> caseMarkers, final UUID hearingId, final UUID prosecutionCaseId) {
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

  public static Builder caseMarkersUpdated() {
    return new uk.gov.justice.core.courts.CaseMarkersUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CaseMarkersUpdated that = (uk.gov.justice.core.courts.CaseMarkersUpdated) obj;

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

    public Builder withValuesFrom(final CaseMarkersUpdated caseMarkersUpdated) {
      this.caseMarkers = caseMarkersUpdated.getCaseMarkers();
      this.hearingId = caseMarkersUpdated.getHearingId();
      this.prosecutionCaseId = caseMarkersUpdated.getProsecutionCaseId();
      return this;
    }

    public CaseMarkersUpdated build() {
      return new uk.gov.justice.core.courts.CaseMarkersUpdated(caseMarkers, hearingId, prosecutionCaseId);
    }
  }
}
