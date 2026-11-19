package uk.gov.justice.listing.event;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum Action {
  ADD("ADD"),

  REMOVE("REMOVE"),

  UPDATE("UPDATE");

  private final String value;

  Action(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<Action> valueFor(final String value) {
    if(ADD.value.equals(value)) { return Optional.of(ADD); };
    if(REMOVE.value.equals(value)) { return Optional.of(REMOVE); };
    if(UPDATE.value.equals(value)) { return Optional.of(UPDATE); };
    return Optional.empty();
  }
}
