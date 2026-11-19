package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Courtrooms implements Serializable {
  private static final long serialVersionUID = 6716282330939640861L;

  private final Integer courtroomId;

  private final String courtroomName;

  private final UUID id;

  private final Integer venueId;

  private final String venueName;

  @JsonCreator
  public Courtrooms(final Integer courtroomId, final String courtroomName, final UUID id, final Integer venueId, final String venueName) {
    this.courtroomId = courtroomId;
    this.courtroomName = courtroomName;
    this.id = id;
    this.venueId = venueId;
    this.venueName = venueName;
  }

  public Integer getCourtroomId() {
    return courtroomId;
  }

  public String getCourtroomName() {
    return courtroomName;
  }

  public UUID getId() {
    return id;
  }

  public Integer getVenueId() {
    return venueId;
  }

  public String getVenueName() {
    return venueName;
  }

  public static Builder courtrooms() {
    return new uk.gov.justice.listing.courts.Courtrooms.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.Courtrooms that = (uk.gov.justice.listing.courts.Courtrooms) obj;

    return java.util.Objects.equals(this.courtroomId, that.courtroomId) &&
    java.util.Objects.equals(this.courtroomName, that.courtroomName) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.venueId, that.venueId) &&
    java.util.Objects.equals(this.venueName, that.venueName);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtroomId, courtroomName, id, venueId, venueName);}

  public static class Builder {
    private Integer courtroomId;

    private String courtroomName;

    private UUID id;

    private Integer venueId;

    private String venueName;

    public Builder withCourtroomId(final Integer courtroomId) {
      this.courtroomId = courtroomId;
      return this;
    }

    public Builder withCourtroomName(final String courtroomName) {
      this.courtroomName = courtroomName;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withVenueId(final Integer venueId) {
      this.venueId = venueId;
      return this;
    }

    public Builder withVenueName(final String venueName) {
      this.venueName = venueName;
      return this;
    }

    public Builder withValuesFrom(final Courtrooms courtrooms) {
      this.courtroomId = courtrooms.getCourtroomId();
      this.courtroomName = courtrooms.getCourtroomName();
      this.id = courtrooms.getId();
      this.venueId = courtrooms.getVenueId();
      this.venueName = courtrooms.getVenueName();
      return this;
    }

    public Courtrooms build() {
      return new uk.gov.justice.listing.courts.Courtrooms(courtroomId, courtroomName, id, venueId, venueName);
    }
  }
}
