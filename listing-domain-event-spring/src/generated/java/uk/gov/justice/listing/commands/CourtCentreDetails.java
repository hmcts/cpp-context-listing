package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.courts.Courtrooms;

public class CourtCentreDetails implements Serializable {
  private static final long serialVersionUID = 6716282330939640861L;

  private final String address1;

  private final String address2;

  private final String courtId;

  private final List<Courtrooms> courtrooms;

  private final Integer defaultDuration;

  private final LocalTime defaultStartTime;

  private final UUID id;

  private final Boolean isWelsh;

  private final String lja;

  private final String name;

  private final String oucode;

  private final String oucodeL1Code;

  private final String oucodeL1Name;

  private final String oucodeL2Code;

  private final String oucodeL3Name;

  private final String oucodeL3WelshName;

  private final String postcode;

  private final String region;

  @JsonCreator
  public CourtCentreDetails(final String address1, final String address2, final String courtId, final List<Courtrooms> courtrooms, final Integer defaultDuration, final LocalTime defaultStartTime, final UUID id, final Boolean isWelsh, final String lja, final String name, final String oucode, final String oucodeL1Code, final String oucodeL1Name, final String oucodeL2Code, final String oucodeL3Name, final String oucodeL3WelshName, final String postcode, final String region) {
    this.address1 = address1;
    this.address2 = address2;
    this.courtId = courtId;
    this.courtrooms = courtrooms;
    this.defaultDuration = defaultDuration;
    this.defaultStartTime = defaultStartTime;
    this.id = id;
    this.isWelsh = isWelsh;
    this.lja = lja;
    this.name = name;
    this.oucode = oucode;
    this.oucodeL1Code = oucodeL1Code;
    this.oucodeL1Name = oucodeL1Name;
    this.oucodeL2Code = oucodeL2Code;
    this.oucodeL3Name = oucodeL3Name;
    this.oucodeL3WelshName = oucodeL3WelshName;
    this.postcode = postcode;
    this.region = region;
  }

  public String getAddress1() {
    return address1;
  }

  public String getAddress2() {
    return address2;
  }

  public String getCourtId() {
    return courtId;
  }

  public List<Courtrooms> getCourtrooms() {
    return courtrooms;
  }

  public Integer getDefaultDuration() {
    return defaultDuration;
  }

  public LocalTime getDefaultStartTime() {
    return defaultStartTime;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsWelsh() {
    return isWelsh;
  }

  public String getLja() {
    return lja;
  }

  public String getName() {
    return name;
  }

  public String getOucode() {
    return oucode;
  }

  public String getOucodeL1Code() {
    return oucodeL1Code;
  }

  public String getOucodeL1Name() {
    return oucodeL1Name;
  }

  public String getOucodeL2Code() {
    return oucodeL2Code;
  }

  public String getOucodeL3Name() {
    return oucodeL3Name;
  }

  public String getOucodeL3WelshName() {
    return oucodeL3WelshName;
  }

  public String getPostcode() {
    return postcode;
  }

  public String getRegion() {
    return region;
  }

  public static Builder courtCentreDetails() {
    return new uk.gov.justice.listing.commands.CourtCentreDetails.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.CourtCentreDetails that = (uk.gov.justice.listing.commands.CourtCentreDetails) obj;

    return java.util.Objects.equals(this.address1, that.address1) &&
    java.util.Objects.equals(this.address2, that.address2) &&
    java.util.Objects.equals(this.courtId, that.courtId) &&
    java.util.Objects.equals(this.courtrooms, that.courtrooms) &&
    java.util.Objects.equals(this.defaultDuration, that.defaultDuration) &&
    java.util.Objects.equals(this.defaultStartTime, that.defaultStartTime) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isWelsh, that.isWelsh) &&
    java.util.Objects.equals(this.lja, that.lja) &&
    java.util.Objects.equals(this.name, that.name) &&
    java.util.Objects.equals(this.oucode, that.oucode) &&
    java.util.Objects.equals(this.oucodeL1Code, that.oucodeL1Code) &&
    java.util.Objects.equals(this.oucodeL1Name, that.oucodeL1Name) &&
    java.util.Objects.equals(this.oucodeL2Code, that.oucodeL2Code) &&
    java.util.Objects.equals(this.oucodeL3Name, that.oucodeL3Name) &&
    java.util.Objects.equals(this.oucodeL3WelshName, that.oucodeL3WelshName) &&
    java.util.Objects.equals(this.postcode, that.postcode) &&
    java.util.Objects.equals(this.region, that.region);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address1, address2, courtId, courtrooms, defaultDuration, defaultStartTime, id, isWelsh, lja, name, oucode, oucodeL1Code, oucodeL1Name, oucodeL2Code, oucodeL3Name, oucodeL3WelshName, postcode, region);}

  public static class Builder {
    private String address1;

    private String address2;

    private String courtId;

    private List<Courtrooms> courtrooms;

    private Integer defaultDuration;

    private LocalTime defaultStartTime;

    private UUID id;

    private Boolean isWelsh;

    private String lja;

    private String name;

    private String oucode;

    private String oucodeL1Code;

    private String oucodeL1Name;

    private String oucodeL2Code;

    private String oucodeL3Name;

    private String oucodeL3WelshName;

    private String postcode;

    private String region;

    public Builder withAddress1(final String address1) {
      this.address1 = address1;
      return this;
    }

    public Builder withAddress2(final String address2) {
      this.address2 = address2;
      return this;
    }

    public Builder withCourtId(final String courtId) {
      this.courtId = courtId;
      return this;
    }

    public Builder withCourtrooms(final List<Courtrooms> courtrooms) {
      this.courtrooms = courtrooms;
      return this;
    }

    public Builder withDefaultDuration(final Integer defaultDuration) {
      this.defaultDuration = defaultDuration;
      return this;
    }

    public Builder withDefaultStartTime(final LocalTime defaultStartTime) {
      this.defaultStartTime = defaultStartTime;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIsWelsh(final Boolean isWelsh) {
      this.isWelsh = isWelsh;
      return this;
    }

    public Builder withLja(final String lja) {
      this.lja = lja;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withOucode(final String oucode) {
      this.oucode = oucode;
      return this;
    }

    public Builder withOucodeL1Code(final String oucodeL1Code) {
      this.oucodeL1Code = oucodeL1Code;
      return this;
    }

    public Builder withOucodeL1Name(final String oucodeL1Name) {
      this.oucodeL1Name = oucodeL1Name;
      return this;
    }

    public Builder withOucodeL2Code(final String oucodeL2Code) {
      this.oucodeL2Code = oucodeL2Code;
      return this;
    }

    public Builder withOucodeL3Name(final String oucodeL3Name) {
      this.oucodeL3Name = oucodeL3Name;
      return this;
    }

    public Builder withOucodeL3WelshName(final String oucodeL3WelshName) {
      this.oucodeL3WelshName = oucodeL3WelshName;
      return this;
    }

    public Builder withPostcode(final String postcode) {
      this.postcode = postcode;
      return this;
    }

    public Builder withRegion(final String region) {
      this.region = region;
      return this;
    }

    public Builder withValuesFrom(final CourtCentreDetails courtCentreDetails) {
      this.address1 = courtCentreDetails.getAddress1();
      this.address2 = courtCentreDetails.getAddress2();
      this.courtId = courtCentreDetails.getCourtId();
      this.courtrooms = courtCentreDetails.getCourtrooms();
      this.defaultDuration = courtCentreDetails.getDefaultDuration();
      this.defaultStartTime = courtCentreDetails.getDefaultStartTime();
      this.id = courtCentreDetails.getId();
      this.isWelsh = courtCentreDetails.getIsWelsh();
      this.lja = courtCentreDetails.getLja();
      this.name = courtCentreDetails.getName();
      this.oucode = courtCentreDetails.getOucode();
      this.oucodeL1Code = courtCentreDetails.getOucodeL1Code();
      this.oucodeL1Name = courtCentreDetails.getOucodeL1Name();
      this.oucodeL2Code = courtCentreDetails.getOucodeL2Code();
      this.oucodeL3Name = courtCentreDetails.getOucodeL3Name();
      this.oucodeL3WelshName = courtCentreDetails.getOucodeL3WelshName();
      this.postcode = courtCentreDetails.getPostcode();
      this.region = courtCentreDetails.getRegion();
      return this;
    }

    public CourtCentreDetails build() {
      return new uk.gov.justice.listing.commands.CourtCentreDetails(address1, address2, courtId, courtrooms, defaultDuration, defaultStartTime, id, isWelsh, lja, name, oucode, oucodeL1Code, oucodeL1Name, oucodeL2Code, oucodeL3Name, oucodeL3WelshName, postcode, region);
    }
  }
}
