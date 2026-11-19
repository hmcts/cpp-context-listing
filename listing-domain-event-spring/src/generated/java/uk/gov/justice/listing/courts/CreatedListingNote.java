package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CreatedListingNote implements Serializable {
  private static final long serialVersionUID = 9216837750557098719L;

  private final UUID courtRoomId;

  private final String date;

  private final UUID id;

  private final String note;

  @JsonCreator
  public CreatedListingNote(final UUID courtRoomId, final String date, final UUID id, final String note) {
    this.courtRoomId = courtRoomId;
    this.date = date;
    this.id = id;
    this.note = note;
  }

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public String getDate() {
    return date;
  }

  public UUID getId() {
    return id;
  }

  public String getNote() {
    return note;
  }

  public static Builder createdListingNote() {
    return new uk.gov.justice.listing.courts.CreatedListingNote.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.CreatedListingNote that = (uk.gov.justice.listing.courts.CreatedListingNote) obj;

    return java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.date, that.date) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.note, that.note);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtRoomId, date, id, note);}

  public static class Builder {
    private UUID courtRoomId;

    private String date;

    private UUID id;

    private String note;

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withDate(final String date) {
      this.date = date;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withNote(final String note) {
      this.note = note;
      return this;
    }

    public Builder withValuesFrom(final CreatedListingNote createdListingNote) {
      this.courtRoomId = createdListingNote.getCourtRoomId();
      this.date = createdListingNote.getDate();
      this.id = createdListingNote.getId();
      this.note = createdListingNote.getNote();
      return this;
    }

    public CreatedListingNote build() {
      return new uk.gov.justice.listing.courts.CreatedListingNote(courtRoomId, date, id, note);
    }
  }
}
