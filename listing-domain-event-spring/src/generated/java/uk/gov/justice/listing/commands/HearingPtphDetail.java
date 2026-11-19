package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class HearingPtphDetail implements Serializable {
  private static final long serialVersionUID = 7247812267194165235L;

  private final UUID hearingId;

  private final String keyReason;

  private final String listType;

  private final String tier;

  @JsonCreator
  public HearingPtphDetail(final UUID hearingId, final String keyReason, final String listType, final String tier) {
    this.hearingId = hearingId;
    this.keyReason = keyReason;
    this.listType = listType;
    this.tier = tier;
  }

  public UUID getHearingId() {
    return hearingId;
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

  public static Builder hearingPtphDetail() {
    return new uk.gov.justice.listing.commands.HearingPtphDetail.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.HearingPtphDetail that = (uk.gov.justice.listing.commands.HearingPtphDetail) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.keyReason, that.keyReason) &&
    java.util.Objects.equals(this.listType, that.listType) &&
    java.util.Objects.equals(this.tier, that.tier);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, keyReason, listType, tier);}

  public static class Builder {
    private UUID hearingId;

    private String keyReason;

    private String listType;

    private String tier;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

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

    public Builder withValuesFrom(final HearingPtphDetail hearingPtphDetail) {
      this.hearingId = hearingPtphDetail.getHearingId();
      this.keyReason = hearingPtphDetail.getKeyReason();
      this.listType = hearingPtphDetail.getListType();
      this.tier = hearingPtphDetail.getTier();
      return this;
    }

    public HearingPtphDetail build() {
      return new uk.gov.justice.listing.commands.HearingPtphDetail(hearingId, keyReason, listType, tier);
    }
  }
}
