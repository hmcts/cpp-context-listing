package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class CaseMarkersToBeUpdated implements Serializable {
  private static final long serialVersionUID = 6890083467116640440L;

  private final List<UUID> hearings;

  private final List<Marker> markers;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public CaseMarkersToBeUpdated(final List<UUID> hearings, final List<Marker> markers, final UUID prosecutionCaseId) {
    this.hearings = hearings;
    this.markers = markers;
    this.prosecutionCaseId = prosecutionCaseId;
  }

  public List<UUID> getHearings() {
    return hearings;
  }

  public List<Marker> getMarkers() {
    return markers;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public static Builder caseMarkersToBeUpdated() {
    return new uk.gov.justice.listing.events.CaseMarkersToBeUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CaseMarkersToBeUpdated that = (uk.gov.justice.listing.events.CaseMarkersToBeUpdated) obj;

    return java.util.Objects.equals(this.hearings, that.hearings) &&
    java.util.Objects.equals(this.markers, that.markers) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearings, markers, prosecutionCaseId);}

  public static class Builder {
    private List<UUID> hearings;

    private List<Marker> markers;

    private UUID prosecutionCaseId;

    public Builder withHearings(final List<UUID> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withMarkers(final List<Marker> markers) {
      this.markers = markers;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withValuesFrom(final CaseMarkersToBeUpdated caseMarkersToBeUpdated) {
      this.hearings = caseMarkersToBeUpdated.getHearings();
      this.markers = caseMarkersToBeUpdated.getMarkers();
      this.prosecutionCaseId = caseMarkersToBeUpdated.getProsecutionCaseId();
      return this;
    }

    public CaseMarkersToBeUpdated build() {
      return new uk.gov.justice.listing.events.CaseMarkersToBeUpdated(hearings, markers, prosecutionCaseId);
    }
  }
}
