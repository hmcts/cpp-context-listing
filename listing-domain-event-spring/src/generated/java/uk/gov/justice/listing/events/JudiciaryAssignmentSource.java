package uk.gov.justice.listing.events;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum JudiciaryAssignmentSource {
  AUTO("AUTO"),

  MANUAL("MANUAL");

  private final String value;

  JudiciaryAssignmentSource(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<JudiciaryAssignmentSource> valueFor(final String value) {
    if(AUTO.value.equals(value)) { return Optional.of(AUTO); };
    if(MANUAL.value.equals(value)) { return Optional.of(MANUAL); };
    return Optional.empty();
  }
}
