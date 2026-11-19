package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum BreachType {
  COMMISSION_OF_NEW_OFFENCE_BREACH("COMMISSION_OF_NEW_OFFENCE_BREACH"),

  NOT_APPLICABLE("NOT_APPLICABLE"),

  GENERIC_BREACH("GENERIC_BREACH");

  private final String value;

  BreachType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<BreachType> valueFor(final String value) {
    if(COMMISSION_OF_NEW_OFFENCE_BREACH.value.equals(value)) { return Optional.of(COMMISSION_OF_NEW_OFFENCE_BREACH); };
    if(NOT_APPLICABLE.value.equals(value)) { return Optional.of(NOT_APPLICABLE); };
    if(GENERIC_BREACH.value.equals(value)) { return Optional.of(GENERIC_BREACH); };
    return Optional.empty();
  }
}
