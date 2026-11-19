package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Marker implements Serializable {
  private static final long serialVersionUID = 6890083467116640440L;

  private final UUID id;

  private final String markerTypeCode;

  private final String markerTypeDescription;

  private final UUID markerTypeid;

  @JsonCreator
  public Marker(final UUID id, final String markerTypeCode, final String markerTypeDescription, final UUID markerTypeid) {
    this.id = id;
    this.markerTypeCode = markerTypeCode;
    this.markerTypeDescription = markerTypeDescription;
    this.markerTypeid = markerTypeid;
  }

  public UUID getId() {
    return id;
  }

  public String getMarkerTypeCode() {
    return markerTypeCode;
  }

  public String getMarkerTypeDescription() {
    return markerTypeDescription;
  }

  public UUID getMarkerTypeid() {
    return markerTypeid;
  }

  public static Builder marker() {
    return new uk.gov.justice.listing.events.Marker.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.Marker that = (uk.gov.justice.listing.events.Marker) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.markerTypeCode, that.markerTypeCode) &&
    java.util.Objects.equals(this.markerTypeDescription, that.markerTypeDescription) &&
    java.util.Objects.equals(this.markerTypeid, that.markerTypeid);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, markerTypeCode, markerTypeDescription, markerTypeid);}

  public static class Builder {
    private UUID id;

    private String markerTypeCode;

    private String markerTypeDescription;

    private UUID markerTypeid;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withMarkerTypeCode(final String markerTypeCode) {
      this.markerTypeCode = markerTypeCode;
      return this;
    }

    public Builder withMarkerTypeDescription(final String markerTypeDescription) {
      this.markerTypeDescription = markerTypeDescription;
      return this;
    }

    public Builder withMarkerTypeid(final UUID markerTypeid) {
      this.markerTypeid = markerTypeid;
      return this;
    }

    public Builder withValuesFrom(final Marker marker) {
      this.id = marker.getId();
      this.markerTypeCode = marker.getMarkerTypeCode();
      this.markerTypeDescription = marker.getMarkerTypeDescription();
      this.markerTypeid = marker.getMarkerTypeid();
      return this;
    }

    public Marker build() {
      return new uk.gov.justice.listing.events.Marker(id, markerTypeCode, markerTypeDescription, markerTypeid);
    }
  }
}
