package uk.gov.justice.listing.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum AttendanceType {
  NOT_PRESENT("NOT_PRESENT"),

  BY_VIDEO("BY_VIDEO"),

  IN_PERSON("IN_PERSON");

  private final String value;

  AttendanceType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<AttendanceType> valueFor(final String value) {
    if(NOT_PRESENT.value.equals(value)) { return Optional.of(NOT_PRESENT); };
    if(BY_VIDEO.value.equals(value)) { return Optional.of(BY_VIDEO); };
    if(IN_PERSON.value.equals(value)) { return Optional.of(IN_PERSON); };
    return Optional.empty();
  }
}
