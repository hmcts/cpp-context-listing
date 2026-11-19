package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum LinkType {
  LINKED("LINKED"),

  STANDALONE("STANDALONE"),

  SJP("SJP"),

  FIRST_HEARING("FIRST_HEARING");

  private final String value;

  LinkType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<LinkType> valueFor(final String value) {
    if(LINKED.value.equals(value)) { return Optional.of(LINKED); };
    if(STANDALONE.value.equals(value)) { return Optional.of(STANDALONE); };
    if(SJP.value.equals(value)) { return Optional.of(SJP); };
    if(FIRST_HEARING.value.equals(value)) { return Optional.of(FIRST_HEARING); };
    return Optional.empty();
  }
}
