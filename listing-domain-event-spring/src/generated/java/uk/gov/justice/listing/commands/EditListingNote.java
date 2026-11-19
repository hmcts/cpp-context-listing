package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class EditListingNote implements Serializable {
  private static final long serialVersionUID = -3706260843573156738L;

  private final String noteDescription;

  @JsonCreator
  public EditListingNote(final String noteDescription) {
    this.noteDescription = noteDescription;
  }

  public String getNoteDescription() {
    return noteDescription;
  }

  public static Builder editListingNote() {
    return new uk.gov.justice.listing.commands.EditListingNote.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.EditListingNote that = (uk.gov.justice.listing.commands.EditListingNote) obj;

    return java.util.Objects.equals(this.noteDescription, that.noteDescription);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(noteDescription);}

  public static class Builder {
    private String noteDescription;

    public Builder withNoteDescription(final String noteDescription) {
      this.noteDescription = noteDescription;
      return this;
    }

    public Builder withValuesFrom(final EditListingNote editListingNote) {
      this.noteDescription = editListingNote.getNoteDescription();
      return this;
    }

    public EditListingNote build() {
      return new uk.gov.justice.listing.commands.EditListingNote(noteDescription);
    }
  }
}
