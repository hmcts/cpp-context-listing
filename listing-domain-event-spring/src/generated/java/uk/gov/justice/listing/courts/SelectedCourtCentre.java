package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class SelectedCourtCentre implements Serializable {
  private static final long serialVersionUID = 7919779722927081093L;

  private final UUID courtRoomId;

  private final UUID id;

  private final String ouCode;

  @JsonCreator
  public SelectedCourtCentre(final UUID courtRoomId, final UUID id, final String ouCode) {
    this.courtRoomId = courtRoomId;
    this.id = id;
    this.ouCode = ouCode;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public UUID getId() {
    return id;
  }

  public String getOuCode() {
    return ouCode;
  }

  public static Builder selectedCourtCentre() {
    return new uk.gov.justice.listing.courts.SelectedCourtCentre.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.SelectedCourtCentre that = (uk.gov.justice.listing.courts.SelectedCourtCentre) obj;

    return java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.ouCode, that.ouCode);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtRoomId, id, ouCode);}

  public static class Builder {
    private UUID courtRoomId;

    private UUID id;

    private String ouCode;

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withOuCode(final String ouCode) {
      this.ouCode = ouCode;
      return this;
    }

    public Builder withValuesFrom(final SelectedCourtCentre selectedCourtCentre) {
      this.courtRoomId = selectedCourtCentre.getCourtRoomId();
      this.id = selectedCourtCentre.getId();
      this.ouCode = selectedCourtCentre.getOuCode();
      return this;
    }

    public SelectedCourtCentre build() {
      return new uk.gov.justice.listing.courts.SelectedCourtCentre(courtRoomId, id, ouCode);
    }
  }
}
