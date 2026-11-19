package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class ProsecutionCaseIdentifier implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Address address;

  private final ContactNumber contact;

  private final String majorCreditorCode;

  private final String prosecutionAuthorityCode;

  private final UUID prosecutionAuthorityId;

  private final String prosecutionAuthorityName;

  private final String prosecutionAuthorityOUCode;

  private final String prosecutorCategory;

  private final String caseURN;

  private final String prosecutionAuthorityReference;

  @JsonCreator
  public ProsecutionCaseIdentifier(final Address address, final ContactNumber contact, final String majorCreditorCode, final String prosecutionAuthorityCode, final UUID prosecutionAuthorityId, final String prosecutionAuthorityName, final String prosecutionAuthorityOUCode, final String prosecutorCategory, final String caseURN, final String prosecutionAuthorityReference) {
    this.address = address;
    this.contact = contact;
    this.majorCreditorCode = majorCreditorCode;
    this.prosecutionAuthorityCode = prosecutionAuthorityCode;
    this.prosecutionAuthorityId = prosecutionAuthorityId;
    this.prosecutionAuthorityName = prosecutionAuthorityName;
    this.prosecutionAuthorityOUCode = prosecutionAuthorityOUCode;
    this.prosecutorCategory = prosecutorCategory;
    this.caseURN = caseURN;
    this.prosecutionAuthorityReference = prosecutionAuthorityReference;
  }

  public Address getAddress() {
    return address;
  }

  public ContactNumber getContact() {
    return contact;
  }

  public String getMajorCreditorCode() {
    return majorCreditorCode;
  }

  public String getProsecutionAuthorityCode() {
    return prosecutionAuthorityCode;
  }

  public UUID getProsecutionAuthorityId() {
    return prosecutionAuthorityId;
  }

  public String getProsecutionAuthorityName() {
    return prosecutionAuthorityName;
  }

  public String getProsecutionAuthorityOUCode() {
    return prosecutionAuthorityOUCode;
  }

  public String getProsecutorCategory() {
    return prosecutorCategory;
  }

  public String getCaseURN() {
    return caseURN;
  }

  public String getProsecutionAuthorityReference() {
    return prosecutionAuthorityReference;
  }

  public static Builder prosecutionCaseIdentifier() {
    return new uk.gov.justice.core.courts.ProsecutionCaseIdentifier.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.ProsecutionCaseIdentifier that = (uk.gov.justice.core.courts.ProsecutionCaseIdentifier) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.contact, that.contact) &&
    java.util.Objects.equals(this.majorCreditorCode, that.majorCreditorCode) &&
    java.util.Objects.equals(this.prosecutionAuthorityCode, that.prosecutionAuthorityCode) &&
    java.util.Objects.equals(this.prosecutionAuthorityId, that.prosecutionAuthorityId) &&
    java.util.Objects.equals(this.prosecutionAuthorityName, that.prosecutionAuthorityName) &&
    java.util.Objects.equals(this.prosecutionAuthorityOUCode, that.prosecutionAuthorityOUCode) &&
    java.util.Objects.equals(this.prosecutorCategory, that.prosecutorCategory) &&
    java.util.Objects.equals(this.caseURN, that.caseURN) &&
    java.util.Objects.equals(this.prosecutionAuthorityReference, that.prosecutionAuthorityReference);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, contact, majorCreditorCode, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionAuthorityName, prosecutionAuthorityOUCode, prosecutorCategory, caseURN, prosecutionAuthorityReference);}

  public static class Builder {
    private Address address;

    private ContactNumber contact;

    private String majorCreditorCode;

    private String prosecutionAuthorityCode;

    private UUID prosecutionAuthorityId;

    private String prosecutionAuthorityName;

    private String prosecutionAuthorityOUCode;

    private String prosecutorCategory;

    private String caseURN;

    private String prosecutionAuthorityReference;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withContact(final ContactNumber contact) {
      this.contact = contact;
      return this;
    }

    public Builder withMajorCreditorCode(final String majorCreditorCode) {
      this.majorCreditorCode = majorCreditorCode;
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

    public Builder withProsecutionAuthorityName(final String prosecutionAuthorityName) {
      this.prosecutionAuthorityName = prosecutionAuthorityName;
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

    public Builder withCaseURN(final String caseURN) {
      this.caseURN = caseURN;
      return this;
    }

    public Builder withProsecutionAuthorityReference(final String prosecutionAuthorityReference) {
      this.prosecutionAuthorityReference = prosecutionAuthorityReference;
      return this;
    }

    public Builder withValuesFrom(final ProsecutionCaseIdentifier prosecutionCaseIdentifier) {
      this.address = prosecutionCaseIdentifier.getAddress();
      this.contact = prosecutionCaseIdentifier.getContact();
      this.majorCreditorCode = prosecutionCaseIdentifier.getMajorCreditorCode();
      this.prosecutionAuthorityCode = prosecutionCaseIdentifier.getProsecutionAuthorityCode();
      this.prosecutionAuthorityId = prosecutionCaseIdentifier.getProsecutionAuthorityId();
      this.prosecutionAuthorityName = prosecutionCaseIdentifier.getProsecutionAuthorityName();
      this.prosecutionAuthorityOUCode = prosecutionCaseIdentifier.getProsecutionAuthorityOUCode();
      this.prosecutorCategory = prosecutionCaseIdentifier.getProsecutorCategory();
      this.caseURN = prosecutionCaseIdentifier.getCaseURN();
      this.prosecutionAuthorityReference = prosecutionCaseIdentifier.getProsecutionAuthorityReference();
      return this;
    }

    public ProsecutionCaseIdentifier build() {
      return new uk.gov.justice.core.courts.ProsecutionCaseIdentifier(address, contact, majorCreditorCode, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionAuthorityName, prosecutionAuthorityOUCode, prosecutorCategory, caseURN, prosecutionAuthorityReference);
    }
  }
}
