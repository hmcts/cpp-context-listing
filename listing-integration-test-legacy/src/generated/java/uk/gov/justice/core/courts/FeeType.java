package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum FeeType {
  CONTESTED("CONTESTED"),

  INITIAL("INITIAL");

  private final String value;

  FeeType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<FeeType> valueFor(final String value) {
    if(CONTESTED.value.equals(value)) { return Optional.of(CONTESTED); };
    if(INITIAL.value.equals(value)) { return Optional.of(INITIAL); };
    return Optional.empty();
  }
}
