package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class ProsecutingAuthority implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String accountCode;

  private final Address address;

  private final ContactNumber contact;

  private final String firstName;

  private final String lastName;

  private final String majorCreditorCode;

  private final String middleName;

  private final String name;

  private final String prosecutionAuthorityCode;

  private final UUID prosecutionAuthorityId;

  private final String prosecutionAuthorityOUCode;

  private final String prosecutorCategory;

  private final Boolean standard;

  private final String welshName;

  @JsonCreator
  public ProsecutingAuthority(final String accountCode, final Address address, final ContactNumber contact, final String firstName, final String lastName, final String majorCreditorCode, final String middleName, final String name, final String prosecutionAuthorityCode, final UUID prosecutionAuthorityId, final String prosecutionAuthorityOUCode, final String prosecutorCategory, final Boolean standard, final String welshName) {
    this.accountCode = accountCode;
    this.address = address;
    this.contact = contact;
    this.firstName = firstName;
    this.lastName = lastName;
    this.majorCreditorCode = majorCreditorCode;
    this.middleName = middleName;
    this.name = name;
    this.prosecutionAuthorityCode = prosecutionAuthorityCode;
    this.prosecutionAuthorityId = prosecutionAuthorityId;
    this.prosecutionAuthorityOUCode = prosecutionAuthorityOUCode;
    this.prosecutorCategory = prosecutorCategory;
    this.standard = standard;
    this.welshName = welshName;
  }

  public String getAccountCode() {
    return accountCode;
  }

  public Address getAddress() {
    return address;
  }

  public ContactNumber getContact() {
    return contact;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public String getMajorCreditorCode() {
    return majorCreditorCode;
  }

  public String getMiddleName() {
    return middleName;
  }

  public String getName() {
    return name;
  }

  public String getProsecutionAuthorityCode() {
    return prosecutionAuthorityCode;
  }

  public UUID getProsecutionAuthorityId() {
    return prosecutionAuthorityId;
  }

  public String getProsecutionAuthorityOUCode() {
    return prosecutionAuthorityOUCode;
  }

  public String getProsecutorCategory() {
    return prosecutorCategory;
  }

  public Boolean getStandard() {
    return standard;
  }

  public String getWelshName() {
    return welshName;
  }

  public static Builder prosecutingAuthority() {
    return new uk.gov.justice.core.courts.ProsecutingAuthority.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ProsecutingAuthority that = (uk.gov.justice.core.courts.ProsecutingAuthority) obj;

    return java.util.Objects.equals(this.accountCode, that.accountCode) &&
    java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.contact, that.contact) &&
    java.util.Objects.equals(this.firstName, that.firstName) &&
    java.util.Objects.equals(this.lastName, that.lastName) &&
    java.util.Objects.equals(this.majorCreditorCode, that.majorCreditorCode) &&
    java.util.Objects.equals(this.middleName, that.middleName) &&
    java.util.Objects.equals(this.name, that.name) &&
    java.util.Objects.equals(this.prosecutionAuthorityCode, that.prosecutionAuthorityCode) &&
    java.util.Objects.equals(this.prosecutionAuthorityId, that.prosecutionAuthorityId) &&
    java.util.Objects.equals(this.prosecutionAuthorityOUCode, that.prosecutionAuthorityOUCode) &&
    java.util.Objects.equals(this.prosecutorCategory, that.prosecutorCategory) &&
    java.util.Objects.equals(this.standard, that.standard) &&
    java.util.Objects.equals(this.welshName, that.welshName);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(accountCode, address, contact, firstName, lastName, majorCreditorCode, middleName, name, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionAuthorityOUCode, prosecutorCategory, standard, welshName);}

  public static class Builder {
    private String accountCode;

    private Address address;

    private ContactNumber contact;

    private String firstName;

    private String lastName;

    private String majorCreditorCode;

    private String middleName;

    private String name;

    private String prosecutionAuthorityCode;

    private UUID prosecutionAuthorityId;

    private String prosecutionAuthorityOUCode;

    private String prosecutorCategory;

    private Boolean standard;

    private String welshName;

    public Builder withAccountCode(final String accountCode) {
      this.accountCode = accountCode;
      return this;
    }

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withContact(final ContactNumber contact) {
      this.contact = contact;
      return this;
    }

    public Builder withFirstName(final String firstName) {
      this.firstName = firstName;
      return this;
    }

    public Builder withLastName(final String lastName) {
      this.lastName = lastName;
      return this;
    }

    public Builder withMajorCreditorCode(final String majorCreditorCode) {
      this.majorCreditorCode = majorCreditorCode;
      return this;
    }

    public Builder withMiddleName(final String middleName) {
      this.middleName = middleName;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withProsecutionAuthorityCode(final String prosecutionAuthorityCode) {
      this.prosecutionAuthorityCode = prosecutionAuthorityCode;
      return this;
    }

    public Builder withProsecutionAuthorityId(final UUID prosecutionAuthorityId) {
      this.prosecutionAuthorityId = prosecutionAuthorityId;
      return this;
    }

    public Builder withProsecutionAuthorityOUCode(final String prosecutionAuthorityOUCode) {
      this.prosecutionAuthorityOUCode = prosecutionAuthorityOUCode;
      return this;
    }

    public Builder withProsecutorCategory(final String prosecutorCategory) {
      this.prosecutorCategory = prosecutorCategory;
      return this;
    }

    public Builder withStandard(final Boolean standard) {
      this.standard = standard;
      return this;
    }

    public Builder withWelshName(final String welshName) {
      this.welshName = welshName;
      return this;
    }

    public Builder withValuesFrom(final ProsecutingAuthority prosecutingAuthority) {
      this.accountCode = prosecutingAuthority.getAccountCode();
      this.address = prosecutingAuthority.getAddress();
      this.contact = prosecutingAuthority.getContact();
      this.firstName = prosecutingAuthority.getFirstName();
      this.lastName = prosecutingAuthority.getLastName();
      this.majorCreditorCode = prosecutingAuthority.getMajorCreditorCode();
      this.middleName = prosecutingAuthority.getMiddleName();
      this.name = prosecutingAuthority.getName();
      this.prosecutionAuthorityCode = prosecutingAuthority.getProsecutionAuthorityCode();
      this.prosecutionAuthorityId = prosecutingAuthority.getProsecutionAuthorityId();
      this.prosecutionAuthorityOUCode = prosecutingAuthority.getProsecutionAuthorityOUCode();
      this.prosecutorCategory = prosecutingAuthority.getProsecutorCategory();
      this.standard = prosecutingAuthority.getStandard();
      this.welshName = prosecutingAuthority.getWelshName();
      return this;
    }

    public ProsecutingAuthority build() {
      return new uk.gov.justice.core.courts.ProsecutingAuthority(accountCode, address, contact, firstName, lastName, majorCreditorCode, middleName, name, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionAuthorityOUCode, prosecutorCategory, standard, welshName);
    }
  }
}
