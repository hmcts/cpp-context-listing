package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class CourtApplicationPartyListingNeeds implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final UUID courtApplicationId;

  private final UUID courtApplicationPartyId;

  private final HearingLanguage hearingLanguageNeeds;

  @JsonCreator
  public CourtApplicationPartyListingNeeds(final UUID courtApplicationId, final UUID courtApplicationPartyId, final HearingLanguage hearingLanguageNeeds) {
    this.courtApplicationId = courtApplicationId;
    this.courtApplicationPartyId = courtApplicationPartyId;
    this.hearingLanguageNeeds = hearingLanguageNeeds;
  }

  public UUID getCourtApplicationId() {
    return courtApplicationId;
  }

  public UUID getCourtApplicationPartyId() {
    return courtApplicationPartyId;
  }

  public HearingLanguage getHearingLanguageNeeds() {
    return hearingLanguageNeeds;
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
    java.util.Objects.equals(this.hearingLanguageNeeds, that.hearingLanguageNeeds);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplicationId, courtApplicationPartyId, hearingLanguageNeeds);}

  public static class Builder {
    private UUID courtApplicationId;

    private UUID courtApplicationPartyId;

    private HearingLanguage hearingLanguageNeeds;

    public Builder withCourtApplicationId(final UUID courtApplicationId) {
      this.courtApplicationId = courtApplicationId;
      return this;
    }

    public Builder withCourtApplicationPartyId(final UUID courtApplicationPartyId) {
      this.courtApplicationPartyId = courtApplicationPartyId;
      return this;
    }

    public Builder withHearingLanguageNeeds(final HearingLanguage hearingLanguageNeeds) {
      this.hearingLanguageNeeds = hearingLanguageNeeds;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationPartyListingNeeds courtApplicationPartyListingNeeds) {
      this.courtApplicationId = courtApplicationPartyListingNeeds.getCourtApplicationId();
      this.courtApplicationPartyId = courtApplicationPartyListingNeeds.getCourtApplicationPartyId();
      this.hearingLanguageNeeds = courtApplicationPartyListingNeeds.getHearingLanguageNeeds();
      return this;
    }

    public CourtApplicationPartyListingNeeds build() {
      return new uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds(courtApplicationId, courtApplicationPartyId, hearingLanguageNeeds);
    }
  }
}
