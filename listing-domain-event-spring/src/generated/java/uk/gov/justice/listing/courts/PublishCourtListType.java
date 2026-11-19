package uk.gov.justice.listing.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum PublishCourtListType {
  DRAFT("DRAFT"),

  FINAL("FINAL");

  private final String value;

  PublishCourtListType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<PublishCourtListType> valueFor(final String value) {
    if(DRAFT.value.equals(value)) { return Optional.of(DRAFT); };
    if(FINAL.value.equals(value)) { return Optional.of(FINAL); };
    return Optional.empty();
  }
}
