package uk.gov.justice.core.courts;

import java.lang.Override;
import java.lang.String;
import java.util.Optional;

public enum VehicleCode {
  OTHER("OTHER"),

  LARGE_GOODS_VEHICLE("LARGE_GOODS_VEHICLE"),

  PASSENGER_CARRYING_VEHICLE("PASSENGER_CARRYING_VEHICLE");

  private final String value;

  VehicleCode(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  public static Optional<VehicleCode> valueFor(final String value) {
    if(OTHER.value.equals(value)) { return Optional.of(OTHER); };
    if(LARGE_GOODS_VEHICLE.value.equals(value)) { return Optional.of(LARGE_GOODS_VEHICLE); };
    if(PASSENGER_CARRYING_VEHICLE.value.equals(value)) { return Optional.of(PASSENGER_CARRYING_VEHICLE); };
    return Optional.empty();
  }
}
