package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum OffenceActiveOrder {
  OFFENCE("OFFENCE"),

  NOT_APPLICABLE("NOT_APPLICABLE"),

  COURT_ORDER("COURT_ORDER");

  private final String value;

  OffenceActiveOrder(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<OffenceActiveOrder> valueFor(final String value) {
    if(OFFENCE.value.equals(value)) { return Optional.of(OFFENCE); };
    if(NOT_APPLICABLE.value.equals(value)) { return Optional.of(NOT_APPLICABLE); };
    if(COURT_ORDER.value.equals(value)) { return Optional.of(COURT_ORDER); };
    return Optional.empty();
  }
}
