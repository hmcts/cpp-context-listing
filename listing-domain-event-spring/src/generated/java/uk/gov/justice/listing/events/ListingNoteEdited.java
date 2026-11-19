package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class ListingNoteEdited implements Serializable {
  private static final long serialVersionUID = 5953567920598413160L;

  private final String noteDescription;

  private final UUID noteId;

  @JsonCreator
  public ListingNoteEdited(final String noteDescription, final UUID noteId) {
    this.noteDescription = noteDescription;
    this.noteId = noteId;
  }

  public String getNoteDescription() {
    return noteDescription;
  }

  public UUID getNoteId() {
    return noteId;
  }

  public static Builder listingNoteEdited() {
    return new uk.gov.justice.listing.events.ListingNoteEdited.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ListingNoteEdited that = (uk.gov.justice.listing.events.ListingNoteEdited) obj;

    return java.util.Objects.equals(this.noteDescription, that.noteDescription) &&
    java.util.Objects.equals(this.noteId, that.noteId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(noteDescription, noteId);}

  public static class Builder {
    private String noteDescription;

    private UUID noteId;

    public Builder withNoteDescription(final String noteDescription) {
      this.noteDescription = noteDescription;
      return this;
    }

    public Builder withNoteId(final UUID noteId) {
      this.noteId = noteId;
      return this;
    }

    public Builder withValuesFrom(final ListingNoteEdited listingNoteEdited) {
      this.noteDescription = listingNoteEdited.getNoteDescription();
      this.noteId = listingNoteEdited.getNoteId();
      return this;
    }

    public ListingNoteEdited build() {
      return new uk.gov.justice.listing.events.ListingNoteEdited(noteDescription, noteId);
    }
  }
}
