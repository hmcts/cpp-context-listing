package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum ApplicationStatus {
  IN_PROGRESS("IN_PROGRESS"),

  EJECTED("EJECTED"),

  LISTED("LISTED"),

  FINALISED("FINALISED"),

  DRAFT("DRAFT"),

  UN_ALLOCATED("UN_ALLOCATED");

  private final String value;

  ApplicationStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<ApplicationStatus> valueFor(final String value) {
    if(IN_PROGRESS.value.equals(value)) { return Optional.of(IN_PROGRESS); };
    if(EJECTED.value.equals(value)) { return Optional.of(EJECTED); };
    if(LISTED.value.equals(value)) { return Optional.of(LISTED); };
    if(FINALISED.value.equals(value)) { return Optional.of(FINALISED); };
    if(DRAFT.value.equals(value)) { return Optional.of(DRAFT); };
    if(UN_ALLOCATED.value.equals(value)) { return Optional.of(UN_ALLOCATED); };
    return Optional.empty();
  }
}
