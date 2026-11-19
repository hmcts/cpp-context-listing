package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class MoveHearingToPastDate implements Serializable {
  private static final long serialVersionUID = 1818913930783743818L;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final String endDateTime;

  private final String startDateTime;

  @JsonCreator
  public MoveHearingToPastDate(final UUID courtCentreId, final UUID courtRoomId, final String endDateTime, final String startDateTime) {
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.endDateTime = endDateTime;
    this.startDateTime = startDateTime;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public String getEndDateTime() {
    return endDateTime;
  }

  public String getStartDateTime() {
    return startDateTime;
  }

  public static Builder moveHearingToPastDate() {
    return new uk.gov.justice.listing.courts.MoveHearingToPastDate.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.MoveHearingToPastDate that = (uk.gov.justice.listing.courts.MoveHearingToPastDate) obj;

    return java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.endDateTime, that.endDateTime) &&
    java.util.Objects.equals(this.startDateTime, that.startDateTime);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtCentreId, courtRoomId, endDateTime, startDateTime);}

  public static class Builder {
    private UUID courtCentreId;

    private UUID courtRoomId;

    private String endDateTime;

    private String startDateTime;

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withEndDateTime(final String endDateTime) {
      this.endDateTime = endDateTime;
      return this;
    }

    public Builder withStartDateTime(final String startDateTime) {
      this.startDateTime = startDateTime;
      return this;
    }

    public Builder withValuesFrom(final MoveHearingToPastDate moveHearingToPastDate) {
      this.courtCentreId = moveHearingToPastDate.getCourtCentreId();
      this.courtRoomId = moveHearingToPastDate.getCourtRoomId();
      this.endDateTime = moveHearingToPastDate.getEndDateTime();
      this.startDateTime = moveHearingToPastDate.getStartDateTime();
      return this;
    }

    public MoveHearingToPastDate build() {
      return new uk.gov.justice.listing.courts.MoveHearingToPastDate(courtCentreId, courtRoomId, endDateTime, startDateTime);
    }
  }
}
