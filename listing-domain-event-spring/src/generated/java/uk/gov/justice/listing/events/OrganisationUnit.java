package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class OrganisationUnit implements Serializable {
  private static final long serialVersionUID = -3690903134111168927L;

  private final String address1;

  private final String address2;

  private final String address3;

  private final String address4;

  private final String address5;

  private final String cpsEmailAddress;

  private final String defaultDurationHrs;

  private final String defaultStartTime;

  private final String email;

  private final String fax;

  private final String id;

  private final Boolean isWelsh;

  private final String lja;

  private final String oucode;

  private final String oucodeEffectiveFromDate;

  private final String oucodeEffectiveToDate;

  private final String oucodeL1Code;

  private final String oucodeL1Name;

  private final String oucodeL2Code;

  private final String oucodeL2Name;

  private final String oucodeL3Code;

  private final String oucodeL3Name;

  private final String oucodeL3WelshName;

  private final String phone;

  private final String postcode;

  private final String region;

  private final String welshAddress1;

  private final String welshAddress2;

  private final String welshAddress3;

  private final String welshAddress4;

  private final String welshAddress5;

  @JsonCreator
  public OrganisationUnit(final String address1, final String address2, final String address3, final String address4, final String address5, final String cpsEmailAddress, final String defaultDurationHrs, final String defaultStartTime, final String email, final String fax, final String id, final Boolean isWelsh, final String lja, final String oucode, final String oucodeEffectiveFromDate, final String oucodeEffectiveToDate, final String oucodeL1Code, final String oucodeL1Name, final String oucodeL2Code, final String oucodeL2Name, final String oucodeL3Code, final String oucodeL3Name, final String oucodeL3WelshName, final String phone, final String postcode, final String region, final String welshAddress1, final String welshAddress2, final String welshAddress3, final String welshAddress4, final String welshAddress5) {
    this.address1 = address1;
    this.address2 = address2;
    this.address3 = address3;
    this.address4 = address4;
    this.address5 = address5;
    this.cpsEmailAddress = cpsEmailAddress;
    this.defaultDurationHrs = defaultDurationHrs;
    this.defaultStartTime = defaultStartTime;
    this.email = email;
    this.fax = fax;
    this.id = id;
    this.isWelsh = isWelsh;
    this.lja = lja;
    this.oucode = oucode;
    this.oucodeEffectiveFromDate = oucodeEffectiveFromDate;
    this.oucodeEffectiveToDate = oucodeEffectiveToDate;
    this.oucodeL1Code = oucodeL1Code;
    this.oucodeL1Name = oucodeL1Name;
    this.oucodeL2Code = oucodeL2Code;
    this.oucodeL2Name = oucodeL2Name;
    this.oucodeL3Code = oucodeL3Code;
    this.oucodeL3Name = oucodeL3Name;
    this.oucodeL3WelshName = oucodeL3WelshName;
    this.phone = phone;
    this.postcode = postcode;
    this.region = region;
    this.welshAddress1 = welshAddress1;
    this.welshAddress2 = welshAddress2;
    this.welshAddress3 = welshAddress3;
    this.welshAddress4 = welshAddress4;
    this.welshAddress5 = welshAddress5;
  }

  public String getAddress1() {
    return address1;
  }

  public String getAddress2() {
    return address2;
  }

  public String getAddress3() {
    return address3;
  }

  public String getAddress4() {
    return address4;
  }

  public String getAddress5() {
    return address5;
  }

  public String getCpsEmailAddress() {
    return cpsEmailAddress;
  }

  public String getDefaultDurationHrs() {
    return defaultDurationHrs;
  }

  public String getDefaultStartTime() {
    return defaultStartTime;
  }

  public String getEmail() {
    return email;
  }

  public String getFax() {
    return fax;
  }

  public String getId() {
    return id;
  }

  public Boolean getIsWelsh() {
    return isWelsh;
  }

  public String getLja() {
    return lja;
  }

  public String getOucode() {
    return oucode;
  }

  public String getOucodeEffectiveFromDate() {
    return oucodeEffectiveFromDate;
  }

  public String getOucodeEffectiveToDate() {
    return oucodeEffectiveToDate;
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

  public String getOucodeL2Name() {
    return oucodeL2Name;
  }

  public String getOucodeL3Code() {
    return oucodeL3Code;
  }

  public String getOucodeL3Name() {
    return oucodeL3Name;
  }

  public String getOucodeL3WelshName() {
    return oucodeL3WelshName;
  }

  public String getPhone() {
    return phone;
  }

  public String getPostcode() {
    return postcode;
  }

  public String getRegion() {
    return region;
  }

  public String getWelshAddress1() {
    return welshAddress1;
  }

  public String getWelshAddress2() {
    return welshAddress2;
  }

  public String getWelshAddress3() {
    return welshAddress3;
  }

  public String getWelshAddress4() {
    return welshAddress4;
  }

  public String getWelshAddress5() {
    return welshAddress5;
  }

  public static Builder organisationUnit() {
    return new uk.gov.justice.listing.events.OrganisationUnit.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.OrganisationUnit that = (uk.gov.justice.listing.events.OrganisationUnit) obj;

    return java.util.Objects.equals(this.address1, that.address1) &&
    java.util.Objects.equals(this.address2, that.address2) &&
    java.util.Objects.equals(this.address3, that.address3) &&
    java.util.Objects.equals(this.address4, that.address4) &&
    java.util.Objects.equals(this.address5, that.address5) &&
    java.util.Objects.equals(this.cpsEmailAddress, that.cpsEmailAddress) &&
    java.util.Objects.equals(this.defaultDurationHrs, that.defaultDurationHrs) &&
    java.util.Objects.equals(this.defaultStartTime, that.defaultStartTime) &&
    java.util.Objects.equals(this.email, that.email) &&
    java.util.Objects.equals(this.fax, that.fax) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isWelsh, that.isWelsh) &&
    java.util.Objects.equals(this.lja, that.lja) &&
    java.util.Objects.equals(this.oucode, that.oucode) &&
    java.util.Objects.equals(this.oucodeEffectiveFromDate, that.oucodeEffectiveFromDate) &&
    java.util.Objects.equals(this.oucodeEffectiveToDate, that.oucodeEffectiveToDate) &&
    java.util.Objects.equals(this.oucodeL1Code, that.oucodeL1Code) &&
    java.util.Objects.equals(this.oucodeL1Name, that.oucodeL1Name) &&
    java.util.Objects.equals(this.oucodeL2Code, that.oucodeL2Code) &&
    java.util.Objects.equals(this.oucodeL2Name, that.oucodeL2Name) &&
    java.util.Objects.equals(this.oucodeL3Code, that.oucodeL3Code) &&
    java.util.Objects.equals(this.oucodeL3Name, that.oucodeL3Name) &&
    java.util.Objects.equals(this.oucodeL3WelshName, that.oucodeL3WelshName) &&
    java.util.Objects.equals(this.phone, that.phone) &&
    java.util.Objects.equals(this.postcode, that.postcode) &&
    java.util.Objects.equals(this.region, that.region) &&
    java.util.Objects.equals(this.welshAddress1, that.welshAddress1) &&
    java.util.Objects.equals(this.welshAddress2, that.welshAddress2) &&
    java.util.Objects.equals(this.welshAddress3, that.welshAddress3) &&
    java.util.Objects.equals(this.welshAddress4, that.welshAddress4) &&
    java.util.Objects.equals(this.welshAddress5, that.welshAddress5);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address1, address2, address3, address4, address5, cpsEmailAddress, defaultDurationHrs, defaultStartTime, email, fax, id, isWelsh, lja, oucode, oucodeEffectiveFromDate, oucodeEffectiveToDate, oucodeL1Code, oucodeL1Name, oucodeL2Code, oucodeL2Name, oucodeL3Code, oucodeL3Name, oucodeL3WelshName, phone, postcode, region, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5);}

  public static class Builder {
    private String address1;

    private String address2;

    private String address3;

    private String address4;

    private String address5;

    private String cpsEmailAddress;

    private String defaultDurationHrs;

    private String defaultStartTime;

    private String email;

    private String fax;

    private String id;

    private Boolean isWelsh;

    private String lja;

    private String oucode;

    private String oucodeEffectiveFromDate;

    private String oucodeEffectiveToDate;

    private String oucodeL1Code;

    private String oucodeL1Name;

    private String oucodeL2Code;

    private String oucodeL2Name;

    private String oucodeL3Code;

    private String oucodeL3Name;

    private String oucodeL3WelshName;

    private String phone;

    private String postcode;

    private String region;

    private String welshAddress1;

    private String welshAddress2;

    private String welshAddress3;

    private String welshAddress4;

    private String welshAddress5;

    public Builder withAddress1(final String address1) {
      this.address1 = address1;
      return this;
    }

    public Builder withAddress2(final String address2) {
      this.address2 = address2;
      return this;
    }

    public Builder withAddress3(final String address3) {
      this.address3 = address3;
      return this;
    }

    public Builder withAddress4(final String address4) {
      this.address4 = address4;
      return this;
    }

    public Builder withAddress5(final String address5) {
      this.address5 = address5;
      return this;
    }

    public Builder withCpsEmailAddress(final String cpsEmailAddress) {
      this.cpsEmailAddress = cpsEmailAddress;
      return this;
    }

    public Builder withDefaultDurationHrs(final String defaultDurationHrs) {
      this.defaultDurationHrs = defaultDurationHrs;
      return this;
    }

    public Builder withDefaultStartTime(final String defaultStartTime) {
      this.defaultStartTime = defaultStartTime;
      return this;
    }

    public Builder withEmail(final String email) {
      this.email = email;
      return this;
    }

    public Builder withFax(final String fax) {
      this.fax = fax;
      return this;
    }

    public Builder withId(final String id) {
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

    public Builder withOucode(final String oucode) {
      this.oucode = oucode;
      return this;
    }

    public Builder withOucodeEffectiveFromDate(final String oucodeEffectiveFromDate) {
      this.oucodeEffectiveFromDate = oucodeEffectiveFromDate;
      return this;
    }

    public Builder withOucodeEffectiveToDate(final String oucodeEffectiveToDate) {
      this.oucodeEffectiveToDate = oucodeEffectiveToDate;
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

    public Builder withOucodeL2Name(final String oucodeL2Name) {
      this.oucodeL2Name = oucodeL2Name;
      return this;
    }

    public Builder withOucodeL3Code(final String oucodeL3Code) {
      this.oucodeL3Code = oucodeL3Code;
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

    public Builder withPhone(final String phone) {
      this.phone = phone;
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

    public Builder withWelshAddress1(final String welshAddress1) {
      this.welshAddress1 = welshAddress1;
      return this;
    }

    public Builder withWelshAddress2(final String welshAddress2) {
      this.welshAddress2 = welshAddress2;
      return this;
    }

    public Builder withWelshAddress3(final String welshAddress3) {
      this.welshAddress3 = welshAddress3;
      return this;
    }

    public Builder withWelshAddress4(final String welshAddress4) {
      this.welshAddress4 = welshAddress4;
      return this;
    }

    public Builder withWelshAddress5(final String welshAddress5) {
      this.welshAddress5 = welshAddress5;
      return this;
    }

    public Builder withValuesFrom(final OrganisationUnit organisationUnit) {
      this.address1 = organisationUnit.getAddress1();
      this.address2 = organisationUnit.getAddress2();
      this.address3 = organisationUnit.getAddress3();
      this.address4 = organisationUnit.getAddress4();
      this.address5 = organisationUnit.getAddress5();
      this.cpsEmailAddress = organisationUnit.getCpsEmailAddress();
      this.defaultDurationHrs = organisationUnit.getDefaultDurationHrs();
      this.defaultStartTime = organisationUnit.getDefaultStartTime();
      this.email = organisationUnit.getEmail();
      this.fax = organisationUnit.getFax();
      this.id = organisationUnit.getId();
      this.isWelsh = organisationUnit.getIsWelsh();
      this.lja = organisationUnit.getLja();
      this.oucode = organisationUnit.getOucode();
      this.oucodeEffectiveFromDate = organisationUnit.getOucodeEffectiveFromDate();
      this.oucodeEffectiveToDate = organisationUnit.getOucodeEffectiveToDate();
      this.oucodeL1Code = organisationUnit.getOucodeL1Code();
      this.oucodeL1Name = organisationUnit.getOucodeL1Name();
      this.oucodeL2Code = organisationUnit.getOucodeL2Code();
      this.oucodeL2Name = organisationUnit.getOucodeL2Name();
      this.oucodeL3Code = organisationUnit.getOucodeL3Code();
      this.oucodeL3Name = organisationUnit.getOucodeL3Name();
      this.oucodeL3WelshName = organisationUnit.getOucodeL3WelshName();
      this.phone = organisationUnit.getPhone();
      this.postcode = organisationUnit.getPostcode();
      this.region = organisationUnit.getRegion();
      this.welshAddress1 = organisationUnit.getWelshAddress1();
      this.welshAddress2 = organisationUnit.getWelshAddress2();
      this.welshAddress3 = organisationUnit.getWelshAddress3();
      this.welshAddress4 = organisationUnit.getWelshAddress4();
      this.welshAddress5 = organisationUnit.getWelshAddress5();
      return this;
    }

    public OrganisationUnit build() {
      return new uk.gov.justice.listing.events.OrganisationUnit(address1, address2, address3, address4, address5, cpsEmailAddress, defaultDurationHrs, defaultStartTime, email, fax, id, isWelsh, lja, oucode, oucodeEffectiveFromDate, oucodeEffectiveToDate, oucodeL1Code, oucodeL1Name, oucodeL2Code, oucodeL2Name, oucodeL3Code, oucodeL3Name, oucodeL3WelshName, phone, postcode, region, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5);
    }
  }
}
