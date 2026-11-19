package uk.gov.justice.progression.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum LinkActionType {
  LINK("LINK"),

  UNLINK("UNLINK");

  private final String value;

  LinkActionType(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<LinkActionType> valueFor(final String value) {
    if(LINK.value.equals(value)) { return Optional.of(LINK); };
    if(UNLINK.value.equals(value)) { return Optional.of(UNLINK); };
    return Optional.empty();
  }
}
