package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class PublicListNoteChangedForHearing implements Serializable {
  private static final long serialVersionUID = -1869730059292059420L;

  private final UUID hearingId;

  private final String publicListNote;

  @JsonCreator
  public PublicListNoteChangedForHearing(final UUID hearingId, final String publicListNote) {
    this.hearingId = hearingId;
    this.publicListNote = publicListNote;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getPublicListNote() {
    return publicListNote;
  }

  public static Builder publicListNoteChangedForHearing() {
    return new uk.gov.justice.listing.events.PublicListNoteChangedForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.PublicListNoteChangedForHearing that = (uk.gov.justice.listing.events.PublicListNoteChangedForHearing) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.publicListNote, that.publicListNote);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, publicListNote);}

  public static class Builder {
    private UUID hearingId;

    private String publicListNote;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withPublicListNote(final String publicListNote) {
      this.publicListNote = publicListNote;
      return this;
    }

    public Builder withValuesFrom(final PublicListNoteChangedForHearing publicListNoteChangedForHearing) {
      this.hearingId = publicListNoteChangedForHearing.getHearingId();
      this.publicListNote = publicListNoteChangedForHearing.getPublicListNote();
      return this;
    }

    public PublicListNoteChangedForHearing build() {
      return new uk.gov.justice.listing.events.PublicListNoteChangedForHearing(hearingId, publicListNote);
    }
  }
}
