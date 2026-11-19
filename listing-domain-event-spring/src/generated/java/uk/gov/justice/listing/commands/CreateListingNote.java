package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CreateListingNote implements Serializable {
  private static final long serialVersionUID = 6153757033019536360L;

  private final UUID courtRoomId;

  private final String hearingDate;

  private final String noteDescription;

  @JsonCreator
  public CreateListingNote(final UUID courtRoomId, final String hearingDate, final String noteDescription) {
    this.courtRoomId = courtRoomId;
    this.hearingDate = hearingDate;
    this.noteDescription = noteDescription;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public String getHearingDate() {
    return hearingDate;
  }

  public String getNoteDescription() {
    return noteDescription;
  }

  public static Builder createListingNote() {
    return new uk.gov.justice.listing.commands.CreateListingNote.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.CreateListingNote that = (uk.gov.justice.listing.commands.CreateListingNote) obj;

    return java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.hearingDate, that.hearingDate) &&
    java.util.Objects.equals(this.noteDescription, that.noteDescription);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtRoomId, hearingDate, noteDescription);}

  public static class Builder {
    private UUID courtRoomId;

    private String hearingDate;

    private String noteDescription;

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withHearingDate(final String hearingDate) {
      this.hearingDate = hearingDate;
      return this;
    }

    public Builder withNoteDescription(final String noteDescription) {
      this.noteDescription = noteDescription;
      return this;
    }

    public Builder withValuesFrom(final CreateListingNote createListingNote) {
      this.courtRoomId = createListingNote.getCourtRoomId();
      this.hearingDate = createListingNote.getHearingDate();
      this.noteDescription = createListingNote.getNoteDescription();
      return this;
    }

    public CreateListingNote build() {
      return new uk.gov.justice.listing.commands.CreateListingNote(courtRoomId, hearingDate, noteDescription);
    }
  }
}
