package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class Address implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String address1;

  private final String address2;

  private final String address3;

  private final String address4;

  private final String address5;

  private final String postcode;

  private final String welshAddress1;

  private final String welshAddress2;

  private final String welshAddress3;

  private final String welshAddress4;

  private final String welshAddress5;

  @JsonCreator
  public Address(final String address1, final String address2, final String address3, final String address4, final String address5, final String postcode, final String welshAddress1, final String welshAddress2, final String welshAddress3, final String welshAddress4, final String welshAddress5) {
    this.address1 = address1;
    this.address2 = address2;
    this.address3 = address3;
    this.address4 = address4;
    this.address5 = address5;
    this.postcode = postcode;
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

  public String getPostcode() {
    return postcode;
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

  public static Builder address() {
    return new uk.gov.justice.core.courts.Address.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Address that = (uk.gov.justice.core.courts.Address) obj;

    return java.util.Objects.equals(this.address1, that.address1) &&
    java.util.Objects.equals(this.address2, that.address2) &&
    java.util.Objects.equals(this.address3, that.address3) &&
    java.util.Objects.equals(this.address4, that.address4) &&
    java.util.Objects.equals(this.address5, that.address5) &&
    java.util.Objects.equals(this.postcode, that.postcode) &&
    java.util.Objects.equals(this.welshAddress1, that.welshAddress1) &&
    java.util.Objects.equals(this.welshAddress2, that.welshAddress2) &&
    java.util.Objects.equals(this.welshAddress3, that.welshAddress3) &&
    java.util.Objects.equals(this.welshAddress4, that.welshAddress4) &&
    java.util.Objects.equals(this.welshAddress5, that.welshAddress5);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address1, address2, address3, address4, address5, postcode, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5);}

  public static class Builder {
    private String address1;

    private String address2;

    private String address3;

    private String address4;

    private String address5;

    private String postcode;

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

    public Builder withPostcode(final String postcode) {
      this.postcode = postcode;
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

    public Builder withValuesFrom(final Address address) {
      this.address1 = address.getAddress1();
      this.address2 = address.getAddress2();
      this.address3 = address.getAddress3();
      this.address4 = address.getAddress4();
      this.address5 = address.getAddress5();
      this.postcode = address.getPostcode();
      this.welshAddress1 = address.getWelshAddress1();
      this.welshAddress2 = address.getWelshAddress2();
      this.welshAddress3 = address.getWelshAddress3();
      this.welshAddress4 = address.getWelshAddress4();
      this.welshAddress5 = address.getWelshAddress5();
      return this;
    }

    public Address build() {
      return new uk.gov.justice.core.courts.Address(address1, address2, address3, address4, address5, postcode, welshAddress1, welshAddress2, welshAddress3, welshAddress4, welshAddress5);
    }
  }
}
