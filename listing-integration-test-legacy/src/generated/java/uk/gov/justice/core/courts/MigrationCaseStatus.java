package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum MigrationCaseStatus {
  ACTIVE("ACTIVE"),

  INACTIVE("INACTIVE");

  private final String value;

  MigrationCaseStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<MigrationCaseStatus> valueFor(final String value) {
    if(ACTIVE.value.equals(value)) { return Optional.of(ACTIVE); };
    if(INACTIVE.value.equals(value)) { return Optional.of(INACTIVE); };
    return Optional.empty();
  }
}
