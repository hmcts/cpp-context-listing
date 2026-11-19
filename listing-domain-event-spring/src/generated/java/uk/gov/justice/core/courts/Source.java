package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum Source {
  IN_COURT("IN_COURT"),

  ONLINE("ONLINE");

  private final String value;

  Source(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<Source> valueFor(final String value) {
    if(IN_COURT.value.equals(value)) { return Optional.of(IN_COURT); };
    if(ONLINE.value.equals(value)) { return Optional.of(ONLINE); };
    return Optional.empty();
  }
}
