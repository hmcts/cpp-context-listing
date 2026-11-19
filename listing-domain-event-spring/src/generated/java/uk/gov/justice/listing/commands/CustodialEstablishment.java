package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CustodialEstablishment implements Serializable {
  private static final long serialVersionUID = 5831617285896704541L;

  private final String custody;

  private final UUID id;

  private final String name;

  @JsonCreator
  public CustodialEstablishment(final String custody, final UUID id, final String name) {
    this.custody = custody;
    this.id = id;
    this.name = name;
  }

  public String getCustody() {
    return custody;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public static Builder custodialEstablishment() {
    return new uk.gov.justice.listing.commands.CustodialEstablishment.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.CustodialEstablishment that = (uk.gov.justice.listing.commands.CustodialEstablishment) obj;

    return java.util.Objects.equals(this.custody, that.custody) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.name, that.name);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(custody, id, name);}

  public static class Builder {
    private String custody;

    private UUID id;

    private String name;

    public Builder withCustody(final String custody) {
      this.custody = custody;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withValuesFrom(final CustodialEstablishment custodialEstablishment) {
      this.custody = custodialEstablishment.getCustody();
      this.id = custodialEstablishment.getId();
      this.name = custodialEstablishment.getName();
      return this;
    }

    public CustodialEstablishment build() {
      return new uk.gov.justice.listing.commands.CustodialEstablishment(custody, id, name);
    }
  }
}
