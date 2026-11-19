package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.time.ZonedDateTime;
import java.util.UUID;

public class HearingDay implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final Boolean hasSharedResults;

  private final Boolean isCancelled;

  private final Integer listedDurationMinutes;

  private final Integer listingSequence;

  private final ZonedDateTime sittingDay;

  @JsonCreator
  public HearingDay(final UUID courtCentreId, final UUID courtRoomId, final Boolean hasSharedResults, final Boolean isCancelled, final Integer listedDurationMinutes, final Integer listingSequence, final ZonedDateTime sittingDay) {
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.hasSharedResults = hasSharedResults;
    this.isCancelled = isCancelled;
    this.listedDurationMinutes = listedDurationMinutes;
    this.listingSequence = listingSequence;
    this.sittingDay = sittingDay;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public Boolean getHasSharedResults() {
    return hasSharedResults;
  }

  public Boolean getIsCancelled() {
    return isCancelled;
  }

  public Integer getListedDurationMinutes() {
    return listedDurationMinutes;
  }

  public Integer getListingSequence() {
    return listingSequence;
  }

  public ZonedDateTime getSittingDay() {
    return sittingDay;
  }

  public static Builder hearingDay() {
    return new uk.gov.justice.core.courts.HearingDay.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.HearingDay that = (uk.gov.justice.core.courts.HearingDay) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.hasSharedResults, that.hasSharedResults) &&
    java.util.Objects.equals(this.isCancelled, that.isCancelled) &&
    java.util.Objects.equals(this.listedDurationMinutes, that.listedDurationMinutes) &&
    java.util.Objects.equals(this.listingSequence, that.listingSequence) &&
    java.util.Objects.equals(this.sittingDay, that.sittingDay);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtRoomId, hasSharedResults, isCancelled, listedDurationMinutes, listingSequence, sittingDay);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtRoomId;

    private Boolean hasSharedResults;

    private Boolean isCancelled;

    private Integer listedDurationMinutes;

    private Integer listingSequence;

    private ZonedDateTime sittingDay;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withHasSharedResults(final Boolean hasSharedResults) {
      this.hasSharedResults = hasSharedResults;
      return this;
    }

    public Builder withIsCancelled(final Boolean isCancelled) {
      this.isCancelled = isCancelled;
      return this;
    }

    public Builder withListedDurationMinutes(final Integer listedDurationMinutes) {
      this.listedDurationMinutes = listedDurationMinutes;
      return this;
    }

    public Builder withListingSequence(final Integer listingSequence) {
      this.listingSequence = listingSequence;
      return this;
    }

    public Builder withSittingDay(final ZonedDateTime sittingDay) {
      this.sittingDay = sittingDay;
      return this;
    }

    public Builder withValuesFrom(final HearingDay hearingDay) {
      this.courtCentreId = hearingDay.getCourtCentreId();
      this.courtRoomId = hearingDay.getCourtRoomId();
      this.hasSharedResults = hearingDay.getHasSharedResults();
      this.isCancelled = hearingDay.getIsCancelled();
      this.listedDurationMinutes = hearingDay.getListedDurationMinutes();
      this.listingSequence = hearingDay.getListingSequence();
      this.sittingDay = hearingDay.getSittingDay();
      return this;
    }

    public HearingDay build() {
      return new uk.gov.justice.core.courts.HearingDay(courtCentreId, courtRoomId, hasSharedResults, isCancelled, listedDurationMinutes, listingSequence, sittingDay);
    }
  }
}
