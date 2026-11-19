package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Prosecutor implements Serializable {
  private static final long serialVersionUID = 641333116650746455L;

  private final Address address;

  private final String prosecutorCode;

  private final UUID prosecutorId;

  private final String prosecutorName;

  @JsonCreator
  public Prosecutor(final Address address, final String prosecutorCode, final UUID prosecutorId, final String prosecutorName) {
    this.address = address;
    this.prosecutorCode = prosecutorCode;
    this.prosecutorId = prosecutorId;
    this.prosecutorName = prosecutorName;
  }

  public Address getAddress() {
    return address;
  }

  public String getProsecutorCode() {
    return prosecutorCode;
  }

  public UUID getProsecutorId() {
    return prosecutorId;
  }

  public String getProsecutorName() {
    return prosecutorName;
  }

  public static Builder prosecutor() {
    return new uk.gov.justice.listing.events.Prosecutor.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.Prosecutor that = (uk.gov.justice.listing.events.Prosecutor) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.prosecutorCode, that.prosecutorCode) &&
    java.util.Objects.equals(this.prosecutorId, that.prosecutorId) &&
    java.util.Objects.equals(this.prosecutorName, that.prosecutorName);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, prosecutorCode, prosecutorId, prosecutorName);}

  public static class Builder {
    private Address address;

    private String prosecutorCode;

    private UUID prosecutorId;

    private String prosecutorName;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withProsecutorCode(final String prosecutorCode) {
      this.prosecutorCode = prosecutorCode;
      return this;
    }

    public Builder withProsecutorId(final UUID prosecutorId) {
      this.prosecutorId = prosecutorId;
      return this;
    }

    public Builder withProsecutorName(final String prosecutorName) {
      this.prosecutorName = prosecutorName;
      return this;
    }

    public Builder withValuesFrom(final Prosecutor prosecutor) {
      this.address = prosecutor.getAddress();
      this.prosecutorCode = prosecutor.getProsecutorCode();
      this.prosecutorId = prosecutor.getProsecutorId();
      this.prosecutorName = prosecutor.getProsecutorName();
      return this;
    }

    public Prosecutor build() {
      return new uk.gov.justice.listing.events.Prosecutor(address, prosecutorCode, prosecutorId, prosecutorName);
    }
  }
}
