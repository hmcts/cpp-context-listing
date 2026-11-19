package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum NotifiedPleaValue {
  NOTIFIED_NOT_GUILTY("NOTIFIED_NOT_GUILTY"),

  NOTIFIED_GUILTY("NOTIFIED_GUILTY"),

  NO_NOTIFICATION("NO_NOTIFICATION");

  private final String value;

  NotifiedPleaValue(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<NotifiedPleaValue> valueFor(final String value) {
    if(NOTIFIED_NOT_GUILTY.value.equals(value)) { return Optional.of(NOTIFIED_NOT_GUILTY); };
    if(NOTIFIED_GUILTY.value.equals(value)) { return Optional.of(NOTIFIED_GUILTY); };
    if(NO_NOTIFICATION.value.equals(value)) { return Optional.of(NO_NOTIFICATION); };
    return Optional.empty();
  }
}
