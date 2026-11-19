package uk.gov.justice.listing.event;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum CounselType {
  PROSECUTION("PROSECUTION"),

  DEFENCE("DEFENCE");

  private final String value;

  CounselType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<CounselType> valueFor(final String value) {
    if(PROSECUTION.value.equals(value)) { return Optional.of(PROSECUTION); };
    if(DEFENCE.value.equals(value)) { return Optional.of(DEFENCE); };
    return Optional.empty();
  }
}
