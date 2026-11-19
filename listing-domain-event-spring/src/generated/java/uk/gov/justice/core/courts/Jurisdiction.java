package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum Jurisdiction {
  CROWN("CROWN"),

  MAGISTRATES("MAGISTRATES"),

  EITHER("EITHER");

  private final String value;

  Jurisdiction(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<Jurisdiction> valueFor(final String value) {
    if(CROWN.value.equals(value)) { return Optional.of(CROWN); };
    if(MAGISTRATES.value.equals(value)) { return Optional.of(MAGISTRATES); };
    if(EITHER.value.equals(value)) { return Optional.of(EITHER); };
    return Optional.empty();
  }
}
