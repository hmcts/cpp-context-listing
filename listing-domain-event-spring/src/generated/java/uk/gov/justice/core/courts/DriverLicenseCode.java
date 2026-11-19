package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum DriverLicenseCode {
  PROVISIONAL("PROVISIONAL"),

  FULL("FULL");

  private final String value;

  DriverLicenseCode(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<DriverLicenseCode> valueFor(final String value) {
    if(PROVISIONAL.value.equals(value)) { return Optional.of(PROVISIONAL); };
    if(FULL.value.equals(value)) { return Optional.of(FULL); };
    return Optional.empty();
  }
}
