package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum IndicatedPleaValue {
  INDICATED_GUILTY("INDICATED_GUILTY"),

  INDICATED_NOT_GUILTY("INDICATED_NOT_GUILTY"),

  NO_INDICATION("NO_INDICATION");

  private final String value;

  IndicatedPleaValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<IndicatedPleaValue> valueFor(final String value) {
    if(INDICATED_GUILTY.value.equals(value)) { return Optional.of(INDICATED_GUILTY); };
    if(INDICATED_NOT_GUILTY.value.equals(value)) { return Optional.of(INDICATED_NOT_GUILTY); };
    if(NO_INDICATION.value.equals(value)) { return Optional.of(NO_INDICATION); };
    return Optional.empty();
  }
}
