package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class CourtRoomId implements Serializable {
  private static final long serialVersionUID = -2582361536581928800L;

  private final String courtRoomId;

  @JsonCreator
  public CourtRoomId(final String courtRoomId) {
    this.courtRoomId = courtRoomId;
  }

  public String getCourtRoomId() {
    return courtRoomId;
  }

  public static Builder courtRoomId() {
    return new uk.gov.justice.listing.courts.CourtRoomId.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CourtRoomId that = (uk.gov.justice.listing.courts.CourtRoomId) obj;

    return java.util.Objects.equals(this.courtRoomId, that.courtRoomId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtRoomId);}

  public static class Builder {
    private String courtRoomId;

    public Builder withCourtRoomId(final String courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withValuesFrom(final CourtRoomId courtRoomId) {
      this.courtRoomId = courtRoomId.getCourtRoomId();
      return this;
    }

    public CourtRoomId build() {
      return new uk.gov.justice.listing.courts.CourtRoomId(courtRoomId);
    }
  }
}
