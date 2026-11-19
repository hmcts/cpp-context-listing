package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class MigrationSourceSystem implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final List<DefendantFineAccountNumber> defendantFineAccountNumbers;

  private final MigrationCaseStatus migrationCaseStatus;

  private final String migrationSourceSystemCaseIdentifier;

  private final String migrationSourceSystemName;

  @JsonCreator
  public MigrationSourceSystem(final List<DefendantFineAccountNumber> defendantFineAccountNumbers, final MigrationCaseStatus migrationCaseStatus, final String migrationSourceSystemCaseIdentifier, final String migrationSourceSystemName) {
    this.defendantFineAccountNumbers = defendantFineAccountNumbers;
    this.migrationCaseStatus = migrationCaseStatus;
    this.migrationSourceSystemCaseIdentifier = migrationSourceSystemCaseIdentifier;
    this.migrationSourceSystemName = migrationSourceSystemName;
  }

  public List<DefendantFineAccountNumber> getDefendantFineAccountNumbers() {
    return defendantFineAccountNumbers;
  }

  public MigrationCaseStatus getMigrationCaseStatus() {
    return migrationCaseStatus;
  }

  public String getMigrationSourceSystemCaseIdentifier() {
    return migrationSourceSystemCaseIdentifier;
  }

  public String getMigrationSourceSystemName() {
    return migrationSourceSystemName;
  }

  public static Builder migrationSourceSystem() {
    return new uk.gov.justice.core.courts.MigrationSourceSystem.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.MigrationSourceSystem that = (uk.gov.justice.core.courts.MigrationSourceSystem) obj;

    return java.util.Objects.equals(this.defendantFineAccountNumbers, that.defendantFineAccountNumbers) &&
    java.util.Objects.equals(this.migrationCaseStatus, that.migrationCaseStatus) &&
    java.util.Objects.equals(this.migrationSourceSystemCaseIdentifier, that.migrationSourceSystemCaseIdentifier) &&
    java.util.Objects.equals(this.migrationSourceSystemName, that.migrationSourceSystemName);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantFineAccountNumbers, migrationCaseStatus, migrationSourceSystemCaseIdentifier, migrationSourceSystemName);}

  public static class Builder {
    private List<DefendantFineAccountNumber> defendantFineAccountNumbers;

    private MigrationCaseStatus migrationCaseStatus;

    private String migrationSourceSystemCaseIdentifier;

    private String migrationSourceSystemName;

    public Builder withDefendantFineAccountNumbers(final List<DefendantFineAccountNumber> defendantFineAccountNumbers) {
      this.defendantFineAccountNumbers = defendantFineAccountNumbers;
      return this;
    }

    public Builder withMigrationCaseStatus(final MigrationCaseStatus migrationCaseStatus) {
      this.migrationCaseStatus = migrationCaseStatus;
      return this;
    }

    public Builder withMigrationSourceSystemCaseIdentifier(final String migrationSourceSystemCaseIdentifier) {
      this.migrationSourceSystemCaseIdentifier = migrationSourceSystemCaseIdentifier;
      return this;
    }

    public Builder withMigrationSourceSystemName(final String migrationSourceSystemName) {
      this.migrationSourceSystemName = migrationSourceSystemName;
      return this;
    }

    public Builder withValuesFrom(final MigrationSourceSystem migrationSourceSystem) {
      this.defendantFineAccountNumbers = migrationSourceSystem.getDefendantFineAccountNumbers();
      this.migrationCaseStatus = migrationSourceSystem.getMigrationCaseStatus();
      this.migrationSourceSystemCaseIdentifier = migrationSourceSystem.getMigrationSourceSystemCaseIdentifier();
      this.migrationSourceSystemName = migrationSourceSystem.getMigrationSourceSystemName();
      return this;
    }

    public MigrationSourceSystem build() {
      return new uk.gov.justice.core.courts.MigrationSourceSystem(defendantFineAccountNumbers, migrationCaseStatus, migrationSourceSystemCaseIdentifier, migrationSourceSystemName);
    }
  }
}
