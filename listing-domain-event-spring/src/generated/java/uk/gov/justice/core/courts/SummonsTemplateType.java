package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum SummonsTemplateType {
  NOT_APPLICABLE("NOT_APPLICABLE"),

  GENERIC_APPLICATION("GENERIC_APPLICATION"),

  BREACH("BREACH"),

  PARENT_GENERIC_CASE("PARENT_GENERIC_CASE"),

  FIRST_HEARING("FIRST_HEARING");

  private final String value;

  SummonsTemplateType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<SummonsTemplateType> valueFor(final String value) {
    if(NOT_APPLICABLE.value.equals(value)) { return Optional.of(NOT_APPLICABLE); };
    if(GENERIC_APPLICATION.value.equals(value)) { return Optional.of(GENERIC_APPLICATION); };
    if(BREACH.value.equals(value)) { return Optional.of(BREACH); };
    if(PARENT_GENERIC_CASE.value.equals(value)) { return Optional.of(PARENT_GENERIC_CASE); };
    if(FIRST_HEARING.value.equals(value)) { return Optional.of(FIRST_HEARING); };
    return Optional.empty();
  }
}
