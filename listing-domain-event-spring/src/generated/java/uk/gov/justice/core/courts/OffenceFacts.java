package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class OffenceFacts implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Integer alcoholReadingAmount;

  private final String alcoholReadingMethodCode;

  private final String alcoholReadingMethodDescription;

  private final VehicleCode vehicleCode;

  private final String vehicleMake;

  private final String vehicleRegistration;

  @JsonCreator
  public OffenceFacts(final Integer alcoholReadingAmount, final String alcoholReadingMethodCode, final String alcoholReadingMethodDescription, final VehicleCode vehicleCode, final String vehicleMake, final String vehicleRegistration) {
    this.alcoholReadingAmount = alcoholReadingAmount;
    this.alcoholReadingMethodCode = alcoholReadingMethodCode;
    this.alcoholReadingMethodDescription = alcoholReadingMethodDescription;
    this.vehicleCode = vehicleCode;
    this.vehicleMake = vehicleMake;
    this.vehicleRegistration = vehicleRegistration;
  }

  public Integer getAlcoholReadingAmount() {
    return alcoholReadingAmount;
  }

  public String getAlcoholReadingMethodCode() {
    return alcoholReadingMethodCode;
  }

  public String getAlcoholReadingMethodDescription() {
    return alcoholReadingMethodDescription;
  }

  public VehicleCode getVehicleCode() {
    return vehicleCode;
  }

  public String getVehicleMake() {
    return vehicleMake;
  }

  public String getVehicleRegistration() {
    return vehicleRegistration;
  }

  public static Builder offenceFacts() {
    return new uk.gov.justice.core.courts.OffenceFacts.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.OffenceFacts that = (uk.gov.justice.core.courts.OffenceFacts) obj;

    return java.util.Objects.equals(this.alcoholReadingAmount, that.alcoholReadingAmount) &&
    java.util.Objects.equals(this.alcoholReadingMethodCode, that.alcoholReadingMethodCode) &&
    java.util.Objects.equals(this.alcoholReadingMethodDescription, that.alcoholReadingMethodDescription) &&
    java.util.Objects.equals(this.vehicleCode, that.vehicleCode) &&
    java.util.Objects.equals(this.vehicleMake, that.vehicleMake) &&
    java.util.Objects.equals(this.vehicleRegistration, that.vehicleRegistration);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(alcoholReadingAmount, alcoholReadingMethodCode, alcoholReadingMethodDescription, vehicleCode, vehicleMake, vehicleRegistration);}

  public static class Builder {
    private Integer alcoholReadingAmount;

    private String alcoholReadingMethodCode;

    private String alcoholReadingMethodDescription;

    private VehicleCode vehicleCode;

    private String vehicleMake;

    private String vehicleRegistration;

    public Builder withAlcoholReadingAmount(final Integer alcoholReadingAmount) {
      this.alcoholReadingAmount = alcoholReadingAmount;
      return this;
    }

    public Builder withAlcoholReadingMethodCode(final String alcoholReadingMethodCode) {
      this.alcoholReadingMethodCode = alcoholReadingMethodCode;
      return this;
    }

    public Builder withAlcoholReadingMethodDescription(final String alcoholReadingMethodDescription) {
      this.alcoholReadingMethodDescription = alcoholReadingMethodDescription;
      return this;
    }

    public Builder withVehicleCode(final VehicleCode vehicleCode) {
      this.vehicleCode = vehicleCode;
      return this;
    }

    public Builder withVehicleMake(final String vehicleMake) {
      this.vehicleMake = vehicleMake;
      return this;
    }

    public Builder withVehicleRegistration(final String vehicleRegistration) {
      this.vehicleRegistration = vehicleRegistration;
      return this;
    }

    public Builder withValuesFrom(final OffenceFacts offenceFacts) {
      this.alcoholReadingAmount = offenceFacts.getAlcoholReadingAmount();
      this.alcoholReadingMethodCode = offenceFacts.getAlcoholReadingMethodCode();
      this.alcoholReadingMethodDescription = offenceFacts.getAlcoholReadingMethodDescription();
      this.vehicleCode = offenceFacts.getVehicleCode();
      this.vehicleMake = offenceFacts.getVehicleMake();
      this.vehicleRegistration = offenceFacts.getVehicleRegistration();
      return this;
    }

    public OffenceFacts build() {
      return new uk.gov.justice.core.courts.OffenceFacts(alcoholReadingAmount, alcoholReadingMethodCode, alcoholReadingMethodDescription, vehicleCode, vehicleMake, vehicleRegistration);
    }
  }
}
