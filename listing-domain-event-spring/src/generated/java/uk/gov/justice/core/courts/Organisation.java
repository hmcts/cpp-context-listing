package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Organisation implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Address address;

  private final ContactNumber contact;

  private final UUID id;

  private final String incorporationNumber;

  private final Boolean isProbationBreach;

  private final String name;

  private final String registeredCharityNumber;

  @JsonCreator
  public Organisation(final Address address, final ContactNumber contact, final UUID id, final String incorporationNumber, final Boolean isProbationBreach, final String name, final String registeredCharityNumber) {
    this.address = address;
    this.contact = contact;
    this.id = id;
    this.incorporationNumber = incorporationNumber;
    this.isProbationBreach = isProbationBreach;
    this.name = name;
    this.registeredCharityNumber = registeredCharityNumber;
  }

  public Address getAddress() {
    return address;
  }

  public ContactNumber getContact() {
    return contact;
  }

  public UUID getId() {
    return id;
  }

  public String getIncorporationNumber() {
    return incorporationNumber;
  }

  public Boolean getIsProbationBreach() {
    return isProbationBreach;
  }

  public String getName() {
    return name;
  }

  public String getRegisteredCharityNumber() {
    return registeredCharityNumber;
  }

  public static Builder organisation() {
    return new uk.gov.justice.core.courts.Organisation.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Organisation that = (uk.gov.justice.core.courts.Organisation) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.contact, that.contact) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.incorporationNumber, that.incorporationNumber) &&
    java.util.Objects.equals(this.isProbationBreach, that.isProbationBreach) &&
    java.util.Objects.equals(this.name, that.name) &&
    java.util.Objects.equals(this.registeredCharityNumber, that.registeredCharityNumber);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, contact, id, incorporationNumber, isProbationBreach, name, registeredCharityNumber);}

  public static class Builder {
    private Address address;

    private ContactNumber contact;

    private UUID id;

    private String incorporationNumber;

    private Boolean isProbationBreach;

    private String name;

    private String registeredCharityNumber;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withContact(final ContactNumber contact) {
      this.contact = contact;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIncorporationNumber(final String incorporationNumber) {
      this.incorporationNumber = incorporationNumber;
      return this;
    }

    public Builder withIsProbationBreach(final Boolean isProbationBreach) {
      this.isProbationBreach = isProbationBreach;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withRegisteredCharityNumber(final String registeredCharityNumber) {
      this.registeredCharityNumber = registeredCharityNumber;
      return this;
    }

    public Builder withValuesFrom(final Organisation organisation) {
      this.address = organisation.getAddress();
      this.contact = organisation.getContact();
      this.id = organisation.getId();
      this.incorporationNumber = organisation.getIncorporationNumber();
      this.isProbationBreach = organisation.getIsProbationBreach();
      this.name = organisation.getName();
      this.registeredCharityNumber = organisation.getRegisteredCharityNumber();
      return this;
    }

    public Organisation build() {
      return new uk.gov.justice.core.courts.Organisation(address, contact, id, incorporationNumber, isProbationBreach, name, registeredCharityNumber);
    }
  }
}
