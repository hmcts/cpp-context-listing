package uk.gov.justice.listing.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum ApprovalType {
  APPROVAL("APPROVAL"),

  CHANGE("CHANGE");

  private final String value;

  ApprovalType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<ApprovalType> valueFor(final String value) {
    if(APPROVAL.value.equals(value)) { return Optional.of(APPROVAL); };
    if(CHANGE.value.equals(value)) { return Optional.of(CHANGE); };
    return Optional.empty();
  }
}
