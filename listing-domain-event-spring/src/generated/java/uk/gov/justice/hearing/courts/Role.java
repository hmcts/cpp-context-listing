package uk.gov.justice.hearing.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum Role {
  INTERPRETER("INTERPRETER"),

  INTERMEDIARY("INTERMEDIARY");

  private final String value;

  Role(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<Role> valueFor(final String value) {
    if(INTERPRETER.value.equals(value)) { return Optional.of(INTERPRETER); };
    if(INTERMEDIARY.value.equals(value)) { return Optional.of(INTERMEDIARY); };
    return Optional.empty();
  }
}
