package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.time.ZonedDateTime;
import java.util.UUID;
import uk.gov.justice.core.courts.CourtCentre;

public class PublicListingNewDefendantAddedForCourtProceedings implements Serializable {
  private static final long serialVersionUID = -756907417797555844L;

  private final UUID caseId;

  private final CourtCentre courtCentre;

  private final UUID defendantId;

  private final ZonedDateTime hearingDateTime;

  private final UUID hearingId;

  @JsonCreator
  public PublicListingNewDefendantAddedForCourtProceedings(final UUID caseId, final CourtCentre courtCentre, final UUID defendantId, final ZonedDateTime hearingDateTime, final UUID hearingId) {
    this.caseId = caseId;
    this.courtCentre = courtCentre;
    this.defendantId = defendantId;
    this.hearingDateTime = hearingDateTime;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public CourtCentre getCourtCentre() {
    return courtCentre;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public ZonedDateTime getHearingDateTime() {
    return hearingDateTime;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder publicListingNewDefendantAddedForCourtProceedings() {
    return new uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings that = (uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.hearingDateTime, that.hearingDateTime) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, courtCentre, defendantId, hearingDateTime, hearingId);}

  public static class Builder {
    private UUID caseId;

    private CourtCentre courtCentre;

    private UUID defendantId;

    private ZonedDateTime hearingDateTime;

    private UUID hearingId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCourtCentre(final CourtCentre courtCentre) {
      this.courtCentre = courtCentre;
      return this;
    }

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withHearingDateTime(final ZonedDateTime hearingDateTime) {
      this.hearingDateTime = hearingDateTime;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final PublicListingNewDefendantAddedForCourtProceedings publicListingNewDefendantAddedForCourtProceedings) {
      this.caseId = publicListingNewDefendantAddedForCourtProceedings.getCaseId();
      this.courtCentre = publicListingNewDefendantAddedForCourtProceedings.getCourtCentre();
      this.defendantId = publicListingNewDefendantAddedForCourtProceedings.getDefendantId();
      this.hearingDateTime = publicListingNewDefendantAddedForCourtProceedings.getHearingDateTime();
      this.hearingId = publicListingNewDefendantAddedForCourtProceedings.getHearingId();
      return this;
    }

    public PublicListingNewDefendantAddedForCourtProceedings build() {
      return new uk.gov.justice.listing.events.PublicListingNewDefendantAddedForCourtProceedings(caseId, courtCentre, defendantId, hearingDateTime, hearingId);
    }
  }
}
