package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Ethnicity implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String observedEthnicityCode;

  private final String observedEthnicityDescription;

  private final UUID observedEthnicityId;

  private final String selfDefinedEthnicityCode;

  private final String selfDefinedEthnicityDescription;

  private final UUID selfDefinedEthnicityId;

  @JsonCreator
  public Ethnicity(final String observedEthnicityCode, final String observedEthnicityDescription, final UUID observedEthnicityId, final String selfDefinedEthnicityCode, final String selfDefinedEthnicityDescription, final UUID selfDefinedEthnicityId) {
    this.observedEthnicityCode = observedEthnicityCode;
    this.observedEthnicityDescription = observedEthnicityDescription;
    this.observedEthnicityId = observedEthnicityId;
    this.selfDefinedEthnicityCode = selfDefinedEthnicityCode;
    this.selfDefinedEthnicityDescription = selfDefinedEthnicityDescription;
    this.selfDefinedEthnicityId = selfDefinedEthnicityId;
  }

  public String getObservedEthnicityCode() {
    return observedEthnicityCode;
  }

  public String getObservedEthnicityDescription() {
    return observedEthnicityDescription;
  }

  public UUID getObservedEthnicityId() {
    return observedEthnicityId;
  }

  public String getSelfDefinedEthnicityCode() {
    return selfDefinedEthnicityCode;
  }

  public String getSelfDefinedEthnicityDescription() {
    return selfDefinedEthnicityDescription;
  }

  public UUID getSelfDefinedEthnicityId() {
    return selfDefinedEthnicityId;
  }

  public static Builder ethnicity() {
    return new uk.gov.justice.core.courts.Ethnicity.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Ethnicity that = (uk.gov.justice.core.courts.Ethnicity) obj;

    return java.util.Objects.equals(this.observedEthnicityCode, that.observedEthnicityCode) &&
    java.util.Objects.equals(this.observedEthnicityDescription, that.observedEthnicityDescription) &&
    java.util.Objects.equals(this.observedEthnicityId, that.observedEthnicityId) &&
    java.util.Objects.equals(this.selfDefinedEthnicityCode, that.selfDefinedEthnicityCode) &&
    java.util.Objects.equals(this.selfDefinedEthnicityDescription, that.selfDefinedEthnicityDescription) &&
    java.util.Objects.equals(this.selfDefinedEthnicityId, that.selfDefinedEthnicityId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(observedEthnicityCode, observedEthnicityDescription, observedEthnicityId, selfDefinedEthnicityCode, selfDefinedEthnicityDescription, selfDefinedEthnicityId);}

  public static class Builder {
    private String observedEthnicityCode;

    private String observedEthnicityDescription;

    private UUID observedEthnicityId;

    private String selfDefinedEthnicityCode;

    private String selfDefinedEthnicityDescription;

    private UUID selfDefinedEthnicityId;

    public Builder withObservedEthnicityCode(final String observedEthnicityCode) {
      this.observedEthnicityCode = observedEthnicityCode;
      return this;
    }

    public Builder withObservedEthnicityDescription(final String observedEthnicityDescription) {
      this.observedEthnicityDescription = observedEthnicityDescription;
      return this;
    }

    public Builder withObservedEthnicityId(final UUID observedEthnicityId) {
      this.observedEthnicityId = observedEthnicityId;
      return this;
    }

    public Builder withSelfDefinedEthnicityCode(final String selfDefinedEthnicityCode) {
      this.selfDefinedEthnicityCode = selfDefinedEthnicityCode;
      return this;
    }

    public Builder withSelfDefinedEthnicityDescription(final String selfDefinedEthnicityDescription) {
      this.selfDefinedEthnicityDescription = selfDefinedEthnicityDescription;
      return this;
    }

    public Builder withSelfDefinedEthnicityId(final UUID selfDefinedEthnicityId) {
      this.selfDefinedEthnicityId = selfDefinedEthnicityId;
      return this;
    }

    public Builder withValuesFrom(final Ethnicity ethnicity) {
      this.observedEthnicityCode = ethnicity.getObservedEthnicityCode();
      this.observedEthnicityDescription = ethnicity.getObservedEthnicityDescription();
      this.observedEthnicityId = ethnicity.getObservedEthnicityId();
      this.selfDefinedEthnicityCode = ethnicity.getSelfDefinedEthnicityCode();
      this.selfDefinedEthnicityDescription = ethnicity.getSelfDefinedEthnicityDescription();
      this.selfDefinedEthnicityId = ethnicity.getSelfDefinedEthnicityId();
      return this;
    }

    public Ethnicity build() {
      return new uk.gov.justice.core.courts.Ethnicity(observedEthnicityCode, observedEthnicityDescription, observedEthnicityId, selfDefinedEthnicityCode, selfDefinedEthnicityDescription, selfDefinedEthnicityId);
    }
  }
}
