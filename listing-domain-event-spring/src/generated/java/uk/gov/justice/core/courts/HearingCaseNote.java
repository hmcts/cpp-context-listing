package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class HearingCaseNote implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final DelegatedPowers courtClerk;

  private final UUID id;

  private final String note;

  private final ZonedDateTime noteDateTime;

  private final NoteType noteType;

  private final UUID originatingHearingId;

  private final List<UUID> prosecutionCases;

  @JsonCreator
  public HearingCaseNote(final DelegatedPowers courtClerk, final UUID id, final String note, final ZonedDateTime noteDateTime, final NoteType noteType, final UUID originatingHearingId, final List<UUID> prosecutionCases) {
    this.courtClerk = courtClerk;
    this.id = id;
    this.note = note;
    this.noteDateTime = noteDateTime;
    this.noteType = noteType;
    this.originatingHearingId = originatingHearingId;
    this.prosecutionCases = prosecutionCases;
  }

  public DelegatedPowers getCourtClerk() {
    return courtClerk;
  }

  public UUID getId() {
    return id;
  }

  public String getNote() {
    return note;
  }

  public ZonedDateTime getNoteDateTime() {
    return noteDateTime;
  }

  public NoteType getNoteType() {
    return noteType;
  }

  public UUID getOriginatingHearingId() {
    return originatingHearingId;
  }

  public List<UUID> getProsecutionCases() {
    return prosecutionCases;
  }

  public static Builder hearingCaseNote() {
    return new uk.gov.justice.core.courts.HearingCaseNote.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.HearingCaseNote that = (uk.gov.justice.core.courts.HearingCaseNote) obj;

    return java.util.Objects.equals(this.courtClerk, that.courtClerk) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.note, that.note) &&
    java.util.Objects.equals(this.noteDateTime, that.noteDateTime) &&
    java.util.Objects.equals(this.noteType, that.noteType) &&
    java.util.Objects.equals(this.originatingHearingId, that.originatingHearingId) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtClerk, id, note, noteDateTime, noteType, originatingHearingId, prosecutionCases);}

  public static class Builder {
    private DelegatedPowers courtClerk;

    private UUID id;

    private String note;

    private ZonedDateTime noteDateTime;

    private NoteType noteType;

    private UUID originatingHearingId;

    private List<UUID> prosecutionCases;

    public Builder withCourtClerk(final DelegatedPowers courtClerk) {
      this.courtClerk = courtClerk;
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

    public Builder withNoteDateTime(final ZonedDateTime noteDateTime) {
      this.noteDateTime = noteDateTime;
      return this;
    }

    public Builder withNoteType(final NoteType noteType) {
      this.noteType = noteType;
      return this;
    }

    public Builder withOriginatingHearingId(final UUID originatingHearingId) {
      this.originatingHearingId = originatingHearingId;
      return this;
    }

    public Builder withProsecutionCases(final List<UUID> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withValuesFrom(final HearingCaseNote hearingCaseNote) {
      this.courtClerk = hearingCaseNote.getCourtClerk();
      this.id = hearingCaseNote.getId();
      this.note = hearingCaseNote.getNote();
      this.noteDateTime = hearingCaseNote.getNoteDateTime();
      this.noteType = hearingCaseNote.getNoteType();
      this.originatingHearingId = hearingCaseNote.getOriginatingHearingId();
      this.prosecutionCases = hearingCaseNote.getProsecutionCases();
      return this;
    }

    public HearingCaseNote build() {
      return new uk.gov.justice.core.courts.HearingCaseNote(courtClerk, id, note, noteDateTime, noteType, originatingHearingId, prosecutionCases);
    }
  }
}
