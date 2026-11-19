package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum FeeStatus {
  NOT_APPLICABLE("NOT_APPLICABLE"),

  REFUNDED("REFUNDED"),

  OUTSTANDING("OUTSTANDING"),

  WAIVED("WAIVED"),

  REDUCED("REDUCED"),

  SATISFIED("SATISFIED"),

  PAYMENT_UNDERTAKING_MADE("PAYMENT_UNDERTAKING_MADE");

  private final String value;

  FeeStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<FeeStatus> valueFor(final String value) {
    if(NOT_APPLICABLE.value.equals(value)) { return Optional.of(NOT_APPLICABLE); };
    if(REFUNDED.value.equals(value)) { return Optional.of(REFUNDED); };
    if(OUTSTANDING.value.equals(value)) { return Optional.of(OUTSTANDING); };
    if(WAIVED.value.equals(value)) { return Optional.of(WAIVED); };
    if(REDUCED.value.equals(value)) { return Optional.of(REDUCED); };
    if(SATISFIED.value.equals(value)) { return Optional.of(SATISFIED); };
    if(PAYMENT_UNDERTAKING_MADE.value.equals(value)) { return Optional.of(PAYMENT_UNDERTAKING_MADE); };
    return Optional.empty();
  }
}
