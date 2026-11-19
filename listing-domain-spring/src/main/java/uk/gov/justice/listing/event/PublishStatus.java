package uk.gov.justice.listing.event;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum PublishStatus {
  INVALID_STATUS("INVALID_STATUS"),

  EXPORT_FAILED("EXPORT_FAILED"),

  EXPORT_SUCCESSFUL("EXPORT_SUCCESSFUL"),

  COURT_LIST_PRODUCED("COURT_LIST_PRODUCED"),

  COURT_LIST_REQUESTED("COURT_LIST_REQUESTED");

  private final String value;

  PublishStatus(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<PublishStatus> valueFor(final String value) {
    if(INVALID_STATUS.value.equals(value)) { return Optional.of(INVALID_STATUS); };
    if(EXPORT_FAILED.value.equals(value)) { return Optional.of(EXPORT_FAILED); };
    if(EXPORT_SUCCESSFUL.value.equals(value)) { return Optional.of(EXPORT_SUCCESSFUL); };
    if(COURT_LIST_PRODUCED.value.equals(value)) { return Optional.of(COURT_LIST_PRODUCED); };
    if(COURT_LIST_REQUESTED.value.equals(value)) { return Optional.of(COURT_LIST_REQUESTED); };
    return Optional.empty();
  }
}
