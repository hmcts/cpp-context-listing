package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.time.ZonedDateTime;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.new-defendant-added-for-court-proceedings")
public class NewDefendantAddedForCourtProceedings implements Serializable {
  private static final long serialVersionUID = 7329580279612096052L;

  private final UUID caseId;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final Defendant defendant;

  private final ZonedDateTime hearingDateTime;

  private final UUID hearingId;

  @JsonCreator
  public NewDefendantAddedForCourtProceedings(final UUID caseId, final UUID courtCentreId, final UUID courtRoomId, final Defendant defendant, final ZonedDateTime hearingDateTime, final UUID hearingId) {
    this.caseId = caseId;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.defendant = defendant;
    this.hearingDateTime = hearingDateTime;
    this.hearingId = hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public Defendant getDefendant() {
    return defendant;
  }

  public ZonedDateTime getHearingDateTime() {
    return hearingDateTime;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder newDefendantAddedForCourtProceedings() {
    return new uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings that = (uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings) obj;

    return java.util.Objects.equals(this.caseId, that.caseId) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.defendant, that.defendant) &&
    java.util.Objects.equals(this.hearingDateTime, that.hearingDateTime) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseId, courtCentreId, courtRoomId, defendant, hearingDateTime, hearingId);}

  public static class Builder {
    private UUID caseId;

    private UUID courtCentreId;

    private UUID courtRoomId;

    private Defendant defendant;

    private ZonedDateTime hearingDateTime;

    private UUID hearingId;

    public Builder withCaseId(final UUID caseId) {
      this.caseId = caseId;
      return this;
    }

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withDefendant(final Defendant defendant) {
      this.defendant = defendant;
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

    public Builder withValuesFrom(final NewDefendantAddedForCourtProceedings newDefendantAddedForCourtProceedings) {
      this.caseId = newDefendantAddedForCourtProceedings.getCaseId();
      this.courtCentreId = newDefendantAddedForCourtProceedings.getCourtCentreId();
      this.courtRoomId = newDefendantAddedForCourtProceedings.getCourtRoomId();
      this.defendant = newDefendantAddedForCourtProceedings.getDefendant();
      this.hearingDateTime = newDefendantAddedForCourtProceedings.getHearingDateTime();
      this.hearingId = newDefendantAddedForCourtProceedings.getHearingId();
      return this;
    }

    public NewDefendantAddedForCourtProceedings build() {
      return new uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings(caseId, courtCentreId, courtRoomId, defendant, hearingDateTime, hearingId);
    }
  }
}
