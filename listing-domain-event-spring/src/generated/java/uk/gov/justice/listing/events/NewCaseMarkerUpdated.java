package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;

public class NewCaseMarkerUpdated implements Serializable {
  private static final long serialVersionUID = 7398997435644601421L;

  private final UUID caseId;

  private final List<Marker> caseMarkers;

  private final UUID hearingId;

  @JsonCreator
  public NewCaseMarkerUpdated(final UUID caseId, final List<Marker> caseMarkers, final UUID hearingId) {
    this.caseId = caseId;
    this.caseMarkers = caseMarkers;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public List<Marker> getCaseMarkers() {
    return caseMarkers;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder newCaseMarkerUpdated() {
    return new uk.gov.justice.listing.events.NewCaseMarkerUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NewCaseMarkerUpdated that = (uk.gov.justice.listing.events.NewCaseMarkerUpdated) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.caseMarkers, that.caseMarkers) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, caseMarkers, hearingId);}

  public static class Builder {
    private UUID caseId;

    private List<Marker> caseMarkers;

    private UUID hearingId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCaseMarkers(final List<Marker> caseMarkers) {
      this.caseMarkers = caseMarkers;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final NewCaseMarkerUpdated newCaseMarkerUpdated) {
      this.caseId = newCaseMarkerUpdated.getCaseId();
      this.caseMarkers = newCaseMarkerUpdated.getCaseMarkers();
      this.hearingId = newCaseMarkerUpdated.getHearingId();
      return this;
    }

    public NewCaseMarkerUpdated build() {
      return new uk.gov.justice.listing.events.NewCaseMarkerUpdated(caseId, caseMarkers, hearingId);
    }
  }
}
