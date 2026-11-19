package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class PtphDetail implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String keyReason;

  private final String listType;

  private final String tier;

  @JsonCreator
  public PtphDetail(final String keyReason, final String listType, final String tier) {
    this.keyReason = keyReason;
    this.listType = listType;
    this.tier = tier;
  }

  public String getKeyReason() {
    return keyReason;
  }

  public String getListType() {
    return listType;
  }

  public String getTier() {
    return tier;
  }

  public static Builder ptphDetail() {
    return new uk.gov.justice.listing.events.PtphDetail.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.PtphDetail that = (uk.gov.justice.listing.events.PtphDetail) obj;

    return java.util.Objects.equals(this.keyReason, that.keyReason) &&
    java.util.Objects.equals(this.listType, that.listType) &&
    java.util.Objects.equals(this.tier, that.tier);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(keyReason, listType, tier);}

  public static class Builder {
    private String keyReason;

    private String listType;

    private String tier;

    public Builder withKeyReason(final String keyReason) {
      this.keyReason = keyReason;
      return this;
    }

    public Builder withListType(final String listType) {
      this.listType = listType;
      return this;
    }

    public Builder withTier(final String tier) {
      this.tier = tier;
      return this;
    }

    public Builder withValuesFrom(final PtphDetail ptphDetail) {
      this.keyReason = ptphDetail.getKeyReason();
      this.listType = ptphDetail.getListType();
      this.tier = ptphDetail.getTier();
      return this;
    }

    public PtphDetail build() {
      return new uk.gov.justice.listing.events.PtphDetail(keyReason, listType, tier);
    }
  }
}
