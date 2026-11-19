package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.created-listing-note")
public class CreatedListingNote implements Serializable {
  private static final long serialVersionUID = -1261983022156587431L;

  private final UUID courtRoomId;

  private final String hearingDate;

  private final UUID id;

  private final String noteDescription;

  @JsonCreator
  public CreatedListingNote(final UUID courtRoomId, final String hearingDate, final UUID id, final String noteDescription) {
    this.courtRoomId = courtRoomId;
    this.hearingDate = hearingDate;
    this.id = id;
    this.noteDescription = noteDescription;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public String getHearingDate() {
    return hearingDate;
  }

  public UUID getId() {
    return id;
  }

  public String getNoteDescription() {
    return noteDescription;
  }

  public static Builder createdListingNote() {
    return new uk.gov.justice.listing.events.CreatedListingNote.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CreatedListingNote that = (uk.gov.justice.listing.events.CreatedListingNote) obj;

    return java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.hearingDate, that.hearingDate) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.noteDescription, that.noteDescription);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtRoomId, hearingDate, id, noteDescription);}

  public static class Builder {
    private UUID courtRoomId;

    private String hearingDate;

    private UUID id;

    private String noteDescription;

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withHearingDate(final String hearingDate) {
      this.hearingDate = hearingDate;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withNoteDescription(final String noteDescription) {
      this.noteDescription = noteDescription;
      return this;
    }

    public Builder withValuesFrom(final CreatedListingNote createdListingNote) {
      this.courtRoomId = createdListingNote.getCourtRoomId();
      this.hearingDate = createdListingNote.getHearingDate();
      this.id = createdListingNote.getId();
      this.noteDescription = createdListingNote.getNoteDescription();
      return this;
    }

    public CreatedListingNote build() {
      return new uk.gov.justice.listing.events.CreatedListingNote(courtRoomId, hearingDate, id, noteDescription);
    }
  }
}
