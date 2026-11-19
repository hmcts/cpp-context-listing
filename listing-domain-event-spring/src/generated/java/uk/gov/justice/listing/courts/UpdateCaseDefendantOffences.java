package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;

public class UpdateCaseDefendantOffences implements Serializable {
  private static final long serialVersionUID = 3670486588993476867L;

  private final List<AddedOffences> addedOffences;

  private final List<DeletedOffences> deletedOffences;

  private final String modifiedDate;

  private final List<UpdatedOffences> updatedOffences;

  @JsonCreator
  public UpdateCaseDefendantOffences(final List<AddedOffences> addedOffences, final List<DeletedOffences> deletedOffences, final String modifiedDate, final List<UpdatedOffences> updatedOffences) {
    this.addedOffences = addedOffences;
    this.deletedOffences = deletedOffences;
    this.modifiedDate = modifiedDate;
    this.updatedOffences = updatedOffences;
  }

  public List<AddedOffences> getAddedOffences() {
    return addedOffences;
  }

  public List<DeletedOffences> getDeletedOffences() {
    return deletedOffences;
  }

  public String getModifiedDate() {
    return modifiedDate;
  }

  public List<UpdatedOffences> getUpdatedOffences() {
    return updatedOffences;
  }

  public static Builder updateCaseDefendantOffences() {
    return new uk.gov.justice.listing.courts.UpdateCaseDefendantOffences.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateCaseDefendantOffences that = (uk.gov.justice.listing.courts.UpdateCaseDefendantOffences) obj;

    return java.util.Objects.equals(this.addedOffences, that.addedOffences) &&
    java.util.Objects.equals(this.deletedOffences, that.deletedOffences) &&
    java.util.Objects.equals(this.modifiedDate, that.modifiedDate) &&
    java.util.Objects.equals(this.updatedOffences, that.updatedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(addedOffences, deletedOffences, modifiedDate, updatedOffences);}

  public static class Builder {
    private List<AddedOffences> addedOffences;

    private List<DeletedOffences> deletedOffences;

    private String modifiedDate;

    private List<UpdatedOffences> updatedOffences;

    public Builder withAddedOffences(final List<AddedOffences> addedOffences) {
      this.addedOffences = addedOffences;
      return this;
    }

    public Builder withDeletedOffences(final List<DeletedOffences> deletedOffences) {
      this.deletedOffences = deletedOffences;
      return this;
    }

    public Builder withModifiedDate(final String modifiedDate) {
      this.modifiedDate = modifiedDate;
      return this;
    }

    public Builder withUpdatedOffences(final List<UpdatedOffences> updatedOffences) {
      this.updatedOffences = updatedOffences;
      return this;
    }

    public Builder withValuesFrom(final UpdateCaseDefendantOffences updateCaseDefendantOffences) {
      this.addedOffences = updateCaseDefendantOffences.getAddedOffences();
      this.deletedOffences = updateCaseDefendantOffences.getDeletedOffences();
      this.modifiedDate = updateCaseDefendantOffences.getModifiedDate();
      this.updatedOffences = updateCaseDefendantOffences.getUpdatedOffences();
      return this;
    }

    public UpdateCaseDefendantOffences build() {
      return new uk.gov.justice.listing.courts.UpdateCaseDefendantOffences(addedOffences, deletedOffences, modifiedDate, updatedOffences);
    }
  }
}
