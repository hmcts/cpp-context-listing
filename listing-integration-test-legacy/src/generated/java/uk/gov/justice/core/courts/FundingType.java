package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum FundingType {
  PRO_BONO("PRO_BONO"),

  COURT_APPOINTED("COURT_APPOINTED"),

  REPRESENTATION_ORDER("REPRESENTATION_ORDER"),

  REPRESENTATION_ORDER_APPLIED_FOR("REPRESENTATION_ORDER_APPLIED_FOR"),

  PRIVATE("PRIVATE");

  private final String value;

  FundingType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<FundingType> valueFor(final String value) {
    if(PRO_BONO.value.equals(value)) { return Optional.of(PRO_BONO); };
    if(COURT_APPOINTED.value.equals(value)) { return Optional.of(COURT_APPOINTED); };
    if(REPRESENTATION_ORDER.value.equals(value)) { return Optional.of(REPRESENTATION_ORDER); };
    if(REPRESENTATION_ORDER_APPLIED_FOR.value.equals(value)) { return Optional.of(REPRESENTATION_ORDER_APPLIED_FOR); };
    if(PRIVATE.value.equals(value)) { return Optional.of(PRIVATE); };
    return Optional.empty();
  }
}
