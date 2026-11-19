package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum JudicialResultCategory {
  ANCILLARY("ANCILLARY"),

  FINAL("FINAL"),

  INTERMEDIARY("INTERMEDIARY");

  private final String value;

  JudicialResultCategory(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<JudicialResultCategory> valueFor(final String value) {
    if(ANCILLARY.value.equals(value)) { return Optional.of(ANCILLARY); };
    if(FINAL.value.equals(value)) { return Optional.of(FINAL); };
    if(INTERMEDIARY.value.equals(value)) { return Optional.of(INTERMEDIARY); };
    return Optional.empty();
  }
}
