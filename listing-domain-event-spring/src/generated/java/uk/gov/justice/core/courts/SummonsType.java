package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum SummonsType {
  SJP_REFERRAL("SJP_REFERRAL"),

  BREACH("BREACH"),

  APPLICATION("APPLICATION"),

  YOUTH("YOUTH"),

  FIRST_HEARING("FIRST_HEARING");

  private final String value;

  SummonsType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<SummonsType> valueFor(final String value) {
    if(SJP_REFERRAL.value.equals(value)) { return Optional.of(SJP_REFERRAL); };
    if(BREACH.value.equals(value)) { return Optional.of(BREACH); };
    if(APPLICATION.value.equals(value)) { return Optional.of(APPLICATION); };
    if(YOUTH.value.equals(value)) { return Optional.of(YOUTH); };
    if(FIRST_HEARING.value.equals(value)) { return Optional.of(FIRST_HEARING); };
    return Optional.empty();
  }
}
