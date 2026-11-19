package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class DefendantListingNeeds implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String datesToAvoid;

  private final UUID defendantId;

  private final HearingLanguage hearingLanguageNeeds;

  private final Boolean isYouth;

  private final String listingReason;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public DefendantListingNeeds(final String datesToAvoid, final UUID defendantId, final HearingLanguage hearingLanguageNeeds, final Boolean isYouth, final String listingReason, final UUID prosecutionCaseId) {
    this.datesToAvoid = datesToAvoid;
    this.defendantId = defendantId;
    this.hearingLanguageNeeds = hearingLanguageNeeds;
    this.isYouth = isYouth;
    this.listingReason = listingReason;
    this.prosecutionCaseId = prosecutionCaseId;
  }

  public String getDatesToAvoid() {
    return datesToAvoid;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public HearingLanguage getHearingLanguageNeeds() {
    return hearingLanguageNeeds;
  }

  public Boolean getIsYouth() {
    return isYouth;
  }

  public String getListingReason() {
    return listingReason;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public static Builder defendantListingNeeds() {
    return new uk.gov.justice.core.courts.DefendantListingNeeds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefendantListingNeeds that = (uk.gov.justice.core.courts.DefendantListingNeeds) obj;

    return java.util.Objects.equals(this.datesToAvoid, that.datesToAvoid) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds) &&
    java.util.Objects.equals(this.isYouth, that.isYouth) &&
    java.util.Objects.equals(this.listingReason, that.listingReason) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(datesToAvoid, defendantId, hearingLanguageNeeds, isYouth, listingReason, prosecutionCaseId);}

  public static class Builder {
    private String datesToAvoid;

    private UUID defendantId;

    private HearingLanguage hearingLanguageNeeds;

    private Boolean isYouth;

    private String listingReason;

    private UUID prosecutionCaseId;

    public Builder withDatesToAvoid(final String datesToAvoid) {
      this.datesToAvoid = datesToAvoid;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withHearingLanguageNeeds(final HearingLanguage hearingLanguageNeeds) {
      this.hearingLanguageNeeds = hearingLanguageNeeds;
      return this;
    }

    public Builder withIsYouth(final Boolean isYouth) {
      this.isYouth = isYouth;
      return this;
    }

    public Builder withListingReason(final String listingReason) {
      this.listingReason = listingReason;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withValuesFrom(final DefendantListingNeeds defendantListingNeeds) {
      this.datesToAvoid = defendantListingNeeds.getDatesToAvoid();
      this.defendantId = defendantListingNeeds.getDefendantId();
      this.hearingLanguageNeeds = defendantListingNeeds.getHearingLanguageNeeds();
      this.isYouth = defendantListingNeeds.getIsYouth();
      this.listingReason = defendantListingNeeds.getListingReason();
      this.prosecutionCaseId = defendantListingNeeds.getProsecutionCaseId();
      return this;
    }

    public DefendantListingNeeds build() {
      return new uk.gov.justice.core.courts.DefendantListingNeeds(datesToAvoid, defendantId, hearingLanguageNeeds, isYouth, listingReason, prosecutionCaseId);
    }
  }
}
