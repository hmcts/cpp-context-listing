package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum NoteType {
  LEGAL("LEGAL"),

  HMCTS("HMCTS");

  private final String value;

  NoteType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<NoteType> valueFor(final String value) {
    if(LEGAL.value.equals(value)) { return Optional.of(LEGAL); };
    if(HMCTS.value.equals(value)) { return Optional.of(HMCTS); };
    return Optional.empty();
  }
}
