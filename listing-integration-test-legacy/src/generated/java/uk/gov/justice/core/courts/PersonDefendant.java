package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import uk.gov.justice.listing.courts.CustodialEstablishment;

public class PersonDefendant implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String arrestSummonsNumber;

  private final String bailConditions;

  private final String bailReasons;

  private final BailStatus bailStatus;

  private final CustodialEstablishment custodialEstablishment;

  private final String custodyTimeLimit;

  private final DriverLicenseCode driverLicenceCode;

  private final String driverLicenseIssue;

  private final String driverNumber;

  private final Organisation employerOrganisation;

  private final String employerPayrollReference;

  private final Integer perceivedBirthYear;

  private final Person personDetails;

  private final String policeBailConditions;

  private final BailStatus policeBailStatus;

  private final String vehicleOperatorLicenceNumber;

  @JsonCreator
  public PersonDefendant(final String arrestSummonsNumber, final String bailConditions, final String bailReasons, final BailStatus bailStatus, final CustodialEstablishment custodialEstablishment, final String custodyTimeLimit, final DriverLicenseCode driverLicenceCode, final String driverLicenseIssue, final String driverNumber, final Organisation employerOrganisation, final String employerPayrollReference, final Integer perceivedBirthYear, final Person personDetails, final String policeBailConditions, final BailStatus policeBailStatus, final String vehicleOperatorLicenceNumber) {
    this.arrestSummonsNumber = arrestSummonsNumber;
    this.bailConditions = bailConditions;
    this.bailReasons = bailReasons;
    this.bailStatus = bailStatus;
    this.custodialEstablishment = custodialEstablishment;
    this.custodyTimeLimit = custodyTimeLimit;
    this.driverLicenceCode = driverLicenceCode;
    this.driverLicenseIssue = driverLicenseIssue;
    this.driverNumber = driverNumber;
    this.employerOrganisation = employerOrganisation;
    this.employerPayrollReference = employerPayrollReference;
    this.perceivedBirthYear = perceivedBirthYear;
    this.personDetails = personDetails;
    this.policeBailConditions = policeBailConditions;
    this.policeBailStatus = policeBailStatus;
    this.vehicleOperatorLicenceNumber = vehicleOperatorLicenceNumber;
  }

  public String getArrestSummonsNumber() {
    return arrestSummonsNumber;
  }

  public String getBailConditions() {
    return bailConditions;
  }

  public String getBailReasons() {
    return bailReasons;
  }

  public BailStatus getBailStatus() {
    return bailStatus;
  }

  public CustodialEstablishment getCustodialEstablishment() {
    return custodialEstablishment;
  }

  public String getCustodyTimeLimit() {
    return custodyTimeLimit;
  }

  public DriverLicenseCode getDriverLicenceCode() {
    return driverLicenceCode;
  }

  public String getDriverLicenseIssue() {
    return driverLicenseIssue;
  }

  public String getDriverNumber() {
    return driverNumber;
  }

  public Organisation getEmployerOrganisation() {
    return employerOrganisation;
  }

  public String getEmployerPayrollReference() {
    return employerPayrollReference;
  }

  public Integer getPerceivedBirthYear() {
    return perceivedBirthYear;
  }

  public Person getPersonDetails() {
    return personDetails;
  }

  public String getPoliceBailConditions() {
    return policeBailConditions;
  }

  public BailStatus getPoliceBailStatus() {
    return policeBailStatus;
  }

  public String getVehicleOperatorLicenceNumber() {
    return vehicleOperatorLicenceNumber;
  }

  public static Builder personDefendant() {
    return new uk.gov.justice.core.courts.PersonDefendant.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.PersonDefendant that = (uk.gov.justice.core.courts.PersonDefendant) obj;

    return java.util.Objects.equals(this.arrestSummonsNumber, that.arrestSummonsNumber) &&
    java.util.Objects.equals(this.bailConditions, that.bailConditions) &&
    java.util.Objects.equals(this.bailReasons, that.bailReasons) &&
    java.util.Objects.equals(this.bailStatus, that.bailStatus) &&
    java.util.Objects.equals(this.custodialEstablishment, that.custodialEstablishment) &&
    java.util.Objects.equals(this.custodyTimeLimit, that.custodyTimeLimit) &&
    java.util.Objects.equals(this.driverLicenceCode, that.driverLicenceCode) &&
    java.util.Objects.equals(this.driverLicenseIssue, that.driverLicenseIssue) &&
    java.util.Objects.equals(this.driverNumber, that.driverNumber) &&
    java.util.Objects.equals(this.employerOrganisation, that.employerOrganisation) &&
    java.util.Objects.equals(this.employerPayrollReference, that.employerPayrollReference) &&
    java.util.Objects.equals(this.perceivedBirthYear, that.perceivedBirthYear) &&
    java.util.Objects.equals(this.personDetails, that.personDetails) &&
    java.util.Objects.equals(this.policeBailConditions, that.policeBailConditions) &&
    java.util.Objects.equals(this.policeBailStatus, that.policeBailStatus) &&
    java.util.Objects.equals(this.vehicleOperatorLicenceNumber, that.vehicleOperatorLicenceNumber);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(arrestSummonsNumber, bailConditions, bailReasons, bailStatus, custodialEstablishment, custodyTimeLimit, driverLicenceCode, driverLicenseIssue, driverNumber, employerOrganisation, employerPayrollReference, perceivedBirthYear, personDetails, policeBailConditions, policeBailStatus, vehicleOperatorLicenceNumber);}

  public static class Builder {
    private String arrestSummonsNumber;

    private String bailConditions;

    private String bailReasons;

    private BailStatus bailStatus;

    private CustodialEstablishment custodialEstablishment;

    private String custodyTimeLimit;

    private DriverLicenseCode driverLicenceCode;

    private String driverLicenseIssue;

    private String driverNumber;

    private Organisation employerOrganisation;

    private String employerPayrollReference;

    private Integer perceivedBirthYear;

    private Person personDetails;

    private String policeBailConditions;

    private BailStatus policeBailStatus;

    private String vehicleOperatorLicenceNumber;

    public Builder withArrestSummonsNumber(final String arrestSummonsNumber) {
      this.arrestSummonsNumber = arrestSummonsNumber;
      return this;
    }

    public Builder withBailConditions(final String bailConditions) {
      this.bailConditions = bailConditions;
      return this;
    }

    public Builder withBailReasons(final String bailReasons) {
      this.bailReasons = bailReasons;
      return this;
    }

    public Builder withBailStatus(final BailStatus bailStatus) {
      this.bailStatus = bailStatus;
      return this;
    }

    public Builder withCustodialEstablishment(final CustodialEstablishment custodialEstablishment) {
      this.custodialEstablishment = custodialEstablishment;
      return this;
    }

    public Builder withCustodyTimeLimit(final String custodyTimeLimit) {
      this.custodyTimeLimit = custodyTimeLimit;
      return this;
    }

    public Builder withDriverLicenceCode(final DriverLicenseCode driverLicenceCode) {
      this.driverLicenceCode = driverLicenceCode;
      return this;
    }

    public Builder withDriverLicenseIssue(final String driverLicenseIssue) {
      this.driverLicenseIssue = driverLicenseIssue;
      return this;
    }

    public Builder withDriverNumber(final String driverNumber) {
      this.driverNumber = driverNumber;
      return this;
    }

    public Builder withEmployerOrganisation(final Organisation employerOrganisation) {
      this.employerOrganisation = employerOrganisation;
      return this;
    }

    public Builder withEmployerPayrollReference(final String employerPayrollReference) {
      this.employerPayrollReference = employerPayrollReference;
      return this;
    }

    public Builder withPerceivedBirthYear(final Integer perceivedBirthYear) {
      this.perceivedBirthYear = perceivedBirthYear;
      return this;
    }

    public Builder withPersonDetails(final Person personDetails) {
      this.personDetails = personDetails;
      return this;
    }

    public Builder withPoliceBailConditions(final String policeBailConditions) {
      this.policeBailConditions = policeBailConditions;
      return this;
    }

    public Builder withPoliceBailStatus(final BailStatus policeBailStatus) {
      this.policeBailStatus = policeBailStatus;
      return this;
    }

    public Builder withVehicleOperatorLicenceNumber(final String vehicleOperatorLicenceNumber) {
      this.vehicleOperatorLicenceNumber = vehicleOperatorLicenceNumber;
      return this;
    }

    public Builder withValuesFrom(final PersonDefendant personDefendant) {
      this.arrestSummonsNumber = personDefendant.getArrestSummonsNumber();
      this.bailConditions = personDefendant.getBailConditions();
      this.bailReasons = personDefendant.getBailReasons();
      this.bailStatus = personDefendant.getBailStatus();
      this.custodialEstablishment = personDefendant.getCustodialEstablishment();
      this.custodyTimeLimit = personDefendant.getCustodyTimeLimit();
      this.driverLicenceCode = personDefendant.getDriverLicenceCode();
      this.driverLicenseIssue = personDefendant.getDriverLicenseIssue();
      this.driverNumber = personDefendant.getDriverNumber();
      this.employerOrganisation = personDefendant.getEmployerOrganisation();
      this.employerPayrollReference = personDefendant.getEmployerPayrollReference();
      this.perceivedBirthYear = personDefendant.getPerceivedBirthYear();
      this.personDetails = personDefendant.getPersonDetails();
      this.policeBailConditions = personDefendant.getPoliceBailConditions();
      this.policeBailStatus = personDefendant.getPoliceBailStatus();
      this.vehicleOperatorLicenceNumber = personDefendant.getVehicleOperatorLicenceNumber();
      return this;
    }

    public PersonDefendant build() {
      return new uk.gov.justice.core.courts.PersonDefendant(arrestSummonsNumber, bailConditions, bailReasons, bailStatus, custodialEstablishment, custodyTimeLimit, driverLicenceCode, driverLicenseIssue, driverNumber, employerOrganisation, employerPayrollReference, perceivedBirthYear, personDetails, policeBailConditions, policeBailStatus, vehicleOperatorLicenceNumber);
    }
  }
}
