package uk.gov.justice.listing.events;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum AttendantType {
  DEFENDANTS("DEFENDANTS"),

  WITNESS("WITNESS");

  private final String value;

  AttendantType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<AttendantType> valueFor(final String value) {
    if(DEFENDANTS.value.equals(value)) { return Optional.of(DEFENDANTS); };
    if(WITNESS.value.equals(value)) { return Optional.of(WITNESS); };
    return Optional.empty();
  }
}
