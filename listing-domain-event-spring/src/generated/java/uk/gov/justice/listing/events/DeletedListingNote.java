package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class DeletedListingNote implements Serializable {
  private static final long serialVersionUID = -1858310735056555884L;

  private final UUID noteId;

  @JsonCreator
  public DeletedListingNote(final UUID noteId) {
    this.noteId = noteId;
  }

  public UUID getNoteId() {
    return noteId;
  }

  public static Builder deletedListingNote() {
    return new uk.gov.justice.listing.events.DeletedListingNote.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.DeletedListingNote that = (uk.gov.justice.listing.events.DeletedListingNote) obj;

    return java.util.Objects.equals(this.noteId, that.noteId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(noteId);}

  public static class Builder {
    private UUID noteId;

    public Builder withNoteId(final UUID noteId) {
      this.noteId = noteId;
      return this;
    }

    public Builder withValuesFrom(final DeletedListingNote deletedListingNote) {
      this.noteId = deletedListingNote.getNoteId();
      return this;
    }

    public DeletedListingNote build() {
      return new uk.gov.justice.listing.events.DeletedListingNote(noteId);
    }
  }
}
