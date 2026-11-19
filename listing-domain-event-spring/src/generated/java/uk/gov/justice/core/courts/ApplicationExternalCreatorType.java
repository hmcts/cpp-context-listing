package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum ApplicationExternalCreatorType {
  PROSECUTOR("PROSECUTOR");

  private final String value;

  ApplicationExternalCreatorType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<ApplicationExternalCreatorType> valueFor(final String value) {
    if(PROSECUTOR.value.equals(value)) { return Optional.of(PROSECUTOR); };
    return Optional.empty();
  }
}
