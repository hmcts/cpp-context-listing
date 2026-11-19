package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CourtApplicationPartyListingNeeds implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final UUID courtApplicationId;

  private final UUID courtApplicationPartyId;

  private final String courtApplicationPartyType;

  private final HearingLanguage hearingLanguageNeeds;

  private final SummonsApprovedOutcome summonsApprovedOutcome;

  private final SummonsType summonsRequired;

  @JsonCreator
  public CourtApplicationPartyListingNeeds(final UUID courtApplicationId, final UUID courtApplicationPartyId, final String courtApplicationPartyType, final HearingLanguage hearingLanguageNeeds, final SummonsApprovedOutcome summonsApprovedOutcome, final SummonsType summonsRequired) {
    this.courtApplicationId = courtApplicationId;
    this.courtApplicationPartyId = courtApplicationPartyId;
    this.courtApplicationPartyType = courtApplicationPartyType;
    this.hearingLanguageNeeds = hearingLanguageNeeds;
    this.summonsApprovedOutcome = summonsApprovedOutcome;
    this.summonsRequired = summonsRequired;
  }

  public UUID getCourtApplicationId() {
    return courtApplicationId;
  }

  public UUID getCourtApplicationPartyId() {
    return courtApplicationPartyId;
  }

  public String getCourtApplicationPartyType() {
    return courtApplicationPartyType;
  }

  public HearingLanguage getHearingLanguageNeeds() {
    return hearingLanguageNeeds;
  }

  public SummonsApprovedOutcome getSummonsApprovedOutcome() {
    return summonsApprovedOutcome;
  }

  public SummonsType getSummonsRequired() {
    return summonsRequired;
  }

  public static Builder courtApplicationPartyListingNeeds() {
    return new uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds that = (uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds) obj;

    return java.util.Objects.equals(this.courtApplicationId, that.courtApplicationId) &&
    java.util.Objects.equals(this.courtApplicationPartyId, that.courtApplicationPartyId) &&
    java.util.Objects.equals(this.courtApplicationPartyType, that.courtApplicationPartyType) &&
    java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds) &&
    java.util.Objects.equals(this.summonsApprovedOutcome, that.summonsApprovedOutcome) &&
    java.util.Objects.equals(this.summonsRequired, that.summonsRequired);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplicationId, courtApplicationPartyId, courtApplicationPartyType, hearingLanguageNeeds, summonsApprovedOutcome, summonsRequired);}

  public static class Builder {
    private UUID courtApplicationId;

    private UUID courtApplicationPartyId;

    private String courtApplicationPartyType;

    private HearingLanguage hearingLanguageNeeds;

    private SummonsApprovedOutcome summonsApprovedOutcome;

    private SummonsType summonsRequired;

    public Builder withCourtApplicationId(final UUID courtApplicationId) {
      this.courtApplicationId = courtApplicationId;
      return this;
    }

    public Builder withCourtApplicationPartyId(final UUID courtApplicationPartyId) {
      this.courtApplicationPartyId = courtApplicationPartyId;
      return this;
    }

    public Builder withCourtApplicationPartyType(final String courtApplicationPartyType) {
      this.courtApplicationPartyType = courtApplicationPartyType;
      return this;
    }

    public Builder withHearingLanguageNeeds(final HearingLanguage hearingLanguageNeeds) {
      this.hearingLanguageNeeds = hearingLanguageNeeds;
      return this;
    }

    public Builder withSummonsApprovedOutcome(final SummonsApprovedOutcome summonsApprovedOutcome) {
      this.summonsApprovedOutcome = summonsApprovedOutcome;
      return this;
    }

    public Builder withSummonsRequired(final SummonsType summonsRequired) {
      this.summonsRequired = summonsRequired;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds) {
      this.courtApplicationId = courtApplicationPartyListingNeeds.getCourtApplicationId();
      this.courtApplicationPartyId = courtApplicationPartyListingNeeds.getCourtApplicationPartyId();
      this.courtApplicationPartyType = courtApplicationPartyListingNeeds.getCourtApplicationPartyType();
      this.hearingLanguageNeeds = courtApplicationPartyListingNeeds.getHearingLanguageNeeds();
      this.summonsApprovedOutcome = courtApplicationPartyListingNeeds.getSummonsApprovedOutcome();
      this.summonsRequired = courtApplicationPartyListingNeeds.getSummonsRequired();
      return this;
    }

    public CourtApplicationPartyListingNeeds build() {
      return new uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds(courtApplicationId, courtApplicationPartyId, courtApplicationPartyType, hearingLanguageNeeds, summonsApprovedOutcome, summonsRequired);
    }
  }
}
