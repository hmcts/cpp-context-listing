package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class PoliceOfficerInCase implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Person personDetails;

  private final String policeOfficerRank;

  private final String policeWorkerLocationCode;

  private final String policeWorkerReferenceNumber;

  @JsonCreator
  public PoliceOfficerInCase(final Person personDetails, final String policeOfficerRank, final String policeWorkerLocationCode, final String policeWorkerReferenceNumber) {
    this.personDetails = personDetails;
    this.policeOfficerRank = policeOfficerRank;
    this.policeWorkerLocationCode = policeWorkerLocationCode;
    this.policeWorkerReferenceNumber = policeWorkerReferenceNumber;
  }

  public Person getPersonDetails() {
    return personDetails;
  }

  public String getPoliceOfficerRank() {
    return policeOfficerRank;
  }

  public String getPoliceWorkerLocationCode() {
    return policeWorkerLocationCode;
  }

  public String getPoliceWorkerReferenceNumber() {
    return policeWorkerReferenceNumber;
  }

  public static Builder policeOfficerInCase() {
    return new uk.gov.justice.core.courts.PoliceOfficerInCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.PoliceOfficerInCase that = (uk.gov.justice.core.courts.PoliceOfficerInCase) obj;

    return java.util.Objects.equals(this.personDetails, that.personDetails) &&
    java.util.Objects.equals(this.policeOfficerRank, that.policeOfficerRank) &&
    java.util.Objects.equals(this.policeWorkerLocationCode, that.policeWorkerLocationCode) &&
    java.util.Objects.equals(this.policeWorkerReferenceNumber, that.policeWorkerReferenceNumber);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(personDetails, policeOfficerRank, policeWorkerLocationCode, policeWorkerReferenceNumber);}

  public static class Builder {
    private Person personDetails;

    private String policeOfficerRank;

    private String policeWorkerLocationCode;

    private String policeWorkerReferenceNumber;

    public Builder withPersonDetails(final Person personDetails) {
      this.personDetails = personDetails;
      return this;
    }

    public Builder withPoliceOfficerRank(final String policeOfficerRank) {
      this.policeOfficerRank = policeOfficerRank;
      return this;
    }

    public Builder withPoliceWorkerLocationCode(final String policeWorkerLocationCode) {
      this.policeWorkerLocationCode = policeWorkerLocationCode;
      return this;
    }

    public Builder withPoliceWorkerReferenceNumber(final String policeWorkerReferenceNumber) {
      this.policeWorkerReferenceNumber = policeWorkerReferenceNumber;
      return this;
    }

    public Builder withValuesFrom(final PoliceOfficerInCase policeOfficerInCase) {
      this.personDetails = policeOfficerInCase.getPersonDetails();
      this.policeOfficerRank = policeOfficerInCase.getPoliceOfficerRank();
      this.policeWorkerLocationCode = policeOfficerInCase.getPoliceWorkerLocationCode();
      this.policeWorkerReferenceNumber = policeOfficerInCase.getPoliceWorkerReferenceNumber();
      return this;
    }

    public PoliceOfficerInCase build() {
      return new uk.gov.justice.core.courts.PoliceOfficerInCase(personDetails, policeOfficerRank, policeWorkerLocationCode, policeWorkerReferenceNumber);
    }
  }
}
