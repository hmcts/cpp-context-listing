package uk.gov.moj.cpp.listing.domain;

import java.util.Optional;

public enum BailStatus {
  CONDITIONAL("CONDITIONAL"),

  IN_CUSTODY("IN_CUSTODY"),

  UNCONDITIONAL("UNCONDITIONAL");

  private final String value;

  BailStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<BailStatus> valueFor(final String value) {
    if(CONDITIONAL.value.equals(value)) { return Optional.of(CONDITIONAL); };
    if(IN_CUSTODY.value.equals(value)) { return Optional.of(IN_CUSTODY); };
    if(UNCONDITIONAL.value.equals(value)) { return Optional.of(UNCONDITIONAL); };
    return Optional.empty();
  }
}
