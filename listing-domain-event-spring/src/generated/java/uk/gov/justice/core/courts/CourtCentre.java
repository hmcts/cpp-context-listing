package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CourtCentre implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Address address;

  private final String code;

  private final String courtHearingLocation;

  private final String courtLocationCode;

  private final UUID id;

  private final LjaDetails lja;

  private final String name;

  private final Integer psaCode;

  private final UUID roomId;

  private final String roomName;

  private final Address welshAddress;

  private final Boolean welshCourtCentre;

  private final String welshName;

  private final String welshRoomName;

  @JsonCreator
  public CourtCentre(final Address address, final String code, final String courtHearingLocation, final String courtLocationCode, final UUID id, final LjaDetails lja, final String name, final Integer psaCode, final UUID roomId, final String roomName, final Address welshAddress, final Boolean welshCourtCentre, final String welshName, final String welshRoomName) {
    this.address = address;
    this.code = code;
    this.courtHearingLocation = courtHearingLocation;
    this.courtLocationCode = courtLocationCode;
    this.id = id;
    this.lja = lja;
    this.name = name;
    this.psaCode = psaCode;
    this.roomId = roomId;
    this.roomName = roomName;
    this.welshAddress = welshAddress;
    this.welshCourtCentre = welshCourtCentre;
    this.welshName = welshName;
    this.welshRoomName = welshRoomName;
  }

  public Address getAddress() {
    return address;
  }

  public String getCode() {
    return code;
  }

  public String getCourtHearingLocation() {
    return courtHearingLocation;
  }

  public String getCourtLocationCode() {
    return courtLocationCode;
  }

  public UUID getId() {
    return id;
  }

  public LjaDetails getLja() {
    return lja;
  }

  public String getName() {
    return name;
  }

  public Integer getPsaCode() {
    return psaCode;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public String getRoomName() {
    return roomName;
  }

  public Address getWelshAddress() {
    return welshAddress;
  }

  public Boolean getWelshCourtCentre() {
    return welshCourtCentre;
  }

  public String getWelshName() {
    return welshName;
  }

  public String getWelshRoomName() {
    return welshRoomName;
  }

  public static Builder courtCentre() {
    return new uk.gov.justice.core.courts.CourtCentre.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtCentre that = (uk.gov.justice.core.courts.CourtCentre) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.code, that.code) &&
    java.util.Objects.equals(this.courtHearingLocation, that.courtHearingLocation) &&
    java.util.Objects.equals(this.courtLocationCode, that.courtLocationCode) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.lja, that.lja) &&
    java.util.Objects.equals(this.name, that.name) &&
    java.util.Objects.equals(this.psaCode, that.psaCode) &&
    java.util.Objects.equals(this.roomId, that.roomId) &&
    java.util.Objects.equals(this.roomName, that.roomName) &&
    java.util.Objects.equals(this.welshAddress, that.welshAddress) &&
    java.util.Objects.equals(this.welshCourtCentre, that.welshCourtCentre) &&
    java.util.Objects.equals(this.welshName, that.welshName) &&
    java.util.Objects.equals(this.welshRoomName, that.welshRoomName);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, code, courtHearingLocation, courtLocationCode, id, lja, name, psaCode, roomId, roomName, welshAddress, welshCourtCentre, welshName, welshRoomName);}

  public static class Builder {
    private Address address;

    private String code;

    private String courtHearingLocation;

    private String courtLocationCode;

    private UUID id;

    private LjaDetails lja;

    private String name;

    private Integer psaCode;

    private UUID roomId;

    private String roomName;

    private Address welshAddress;

    private Boolean welshCourtCentre;

    private String welshName;

    private String welshRoomName;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withCode(final String code) {
      this.code = code;
      return this;
    }

    public Builder withCourtHearingLocation(final String courtHearingLocation) {
      this.courtHearingLocation = courtHearingLocation;
      return this;
    }

    public Builder withCourtLocationCode(final String courtLocationCode) {
      this.courtLocationCode = courtLocationCode;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withLja(final LjaDetails lja) {
      this.lja = lja;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withPsaCode(final Integer psaCode) {
      this.psaCode = psaCode;
      return this;
    }

    public Builder withRoomId(final UUID roomId) {
      this.roomId = roomId;
      return this;
    }

    public Builder withRoomName(final String roomName) {
      this.roomName = roomName;
      return this;
    }

    public Builder withWelshAddress(final Address welshAddress) {
      this.welshAddress = welshAddress;
      return this;
    }

    public Builder withWelshCourtCentre(final Boolean welshCourtCentre) {
      this.welshCourtCentre = welshCourtCentre;
      return this;
    }

    public Builder withWelshName(final String welshName) {
      this.welshName = welshName;
      return this;
    }

    public Builder withWelshRoomName(final String welshRoomName) {
      this.welshRoomName = welshRoomName;
      return this;
    }

    public Builder withValuesFrom(final CourtCentre courtCentre) {
      this.address = courtCentre.getAddress();
      this.code = courtCentre.getCode();
      this.courtHearingLocation = courtCentre.getCourtHearingLocation();
      this.courtLocationCode = courtCentre.getCourtLocationCode();
      this.id = courtCentre.getId();
      this.lja = courtCentre.getLja();
      this.name = courtCentre.getName();
      this.psaCode = courtCentre.getPsaCode();
      this.roomId = courtCentre.getRoomId();
      this.roomName = courtCentre.getRoomName();
      this.welshAddress = courtCentre.getWelshAddress();
      this.welshCourtCentre = courtCentre.getWelshCourtCentre();
      this.welshName = courtCentre.getWelshName();
      this.welshRoomName = courtCentre.getWelshRoomName();
      return this;
    }

    public CourtCentre build() {
      return new uk.gov.justice.core.courts.CourtCentre(address, code, courtHearingLocation, courtLocationCode, id, lja, name, psaCode, roomId, roomName, welshAddress, welshCourtCentre, welshName, welshRoomName);
    }
  }
}
