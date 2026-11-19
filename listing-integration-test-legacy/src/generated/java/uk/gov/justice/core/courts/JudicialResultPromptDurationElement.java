package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class JudicialResultPromptDurationElement implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final String durationEndDate;

  private final String durationStartDate;

  private final String primaryDurationUnit;

  private final Integer primaryDurationValue;

  private final String secondaryDurationUnit;

  private final Integer secondaryDurationValue;

  @JsonCreator
  public JudicialResultPromptDurationElement(final String durationEndDate, final String durationStartDate, final String primaryDurationUnit, final Integer primaryDurationValue, final String secondaryDurationUnit, final Integer secondaryDurationValue) {
    this.durationEndDate = durationEndDate;
    this.durationStartDate = durationStartDate;
    this.primaryDurationUnit = primaryDurationUnit;
    this.primaryDurationValue = primaryDurationValue;
    this.secondaryDurationUnit = secondaryDurationUnit;
    this.secondaryDurationValue = secondaryDurationValue;
  }

  public String getDurationEndDate() {
    return durationEndDate;
  }

  public String getDurationStartDate() {
    return durationStartDate;
  }

  public String getPrimaryDurationUnit() {
    return primaryDurationUnit;
  }

  public Integer getPrimaryDurationValue() {
    return primaryDurationValue;
  }

  public String getSecondaryDurationUnit() {
    return secondaryDurationUnit;
  }

  public Integer getSecondaryDurationValue() {
    return secondaryDurationValue;
  }

  public static Builder judicialResultPromptDurationElement() {
    return new uk.gov.justice.core.courts.JudicialResultPromptDurationElement.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.JudicialResultPromptDurationElement that = (uk.gov.justice.core.courts.JudicialResultPromptDurationElement) obj;

    return java.util.Objects.equals(this.durationEndDate, that.durationEndDate) &&
    java.util.Objects.equals(this.durationStartDate, that.durationStartDate) &&
    java.util.Objects.equals(this.primaryDurationUnit, that.primaryDurationUnit) &&
    java.util.Objects.equals(this.primaryDurationValue, that.primaryDurationValue) &&
    java.util.Objects.equals(this.secondaryDurationUnit, that.secondaryDurationUnit) &&
    java.util.Objects.equals(this.secondaryDurationValue, that.secondaryDurationValue);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(durationEndDate, durationStartDate, primaryDurationUnit, primaryDurationValue, secondaryDurationUnit, secondaryDurationValue);}

  public static class Builder {
    private String durationEndDate;

    private String durationStartDate;

    private String primaryDurationUnit;

    private Integer primaryDurationValue;

    private String secondaryDurationUnit;

    private Integer secondaryDurationValue;

    public Builder withDurationEndDate(final String durationEndDate) {
      this.durationEndDate = durationEndDate;
      return this;
    }

    public Builder withDurationStartDate(final String durationStartDate) {
      this.durationStartDate = durationStartDate;
      return this;
    }

    public Builder withPrimaryDurationUnit(final String primaryDurationUnit) {
      this.primaryDurationUnit = primaryDurationUnit;
      return this;
    }

    public Builder withPrimaryDurationValue(final Integer primaryDurationValue) {
      this.primaryDurationValue = primaryDurationValue;
      return this;
    }

    public Builder withSecondaryDurationUnit(final String secondaryDurationUnit) {
      this.secondaryDurationUnit = secondaryDurationUnit;
      return this;
    }

    public Builder withSecondaryDurationValue(final Integer secondaryDurationValue) {
      this.secondaryDurationValue = secondaryDurationValue;
      return this;
    }

    public Builder withValuesFrom(final JudicialResultPromptDurationElement judicialResultPromptDurationElement) {
      this.durationEndDate = judicialResultPromptDurationElement.getDurationEndDate();
      this.durationStartDate = judicialResultPromptDurationElement.getDurationStartDate();
      this.primaryDurationUnit = judicialResultPromptDurationElement.getPrimaryDurationUnit();
      this.primaryDurationValue = judicialResultPromptDurationElement.getPrimaryDurationValue();
      this.secondaryDurationUnit = judicialResultPromptDurationElement.getSecondaryDurationUnit();
      this.secondaryDurationValue = judicialResultPromptDurationElement.getSecondaryDurationValue();
      return this;
    }

    public JudicialResultPromptDurationElement build() {
      return new uk.gov.justice.core.courts.JudicialResultPromptDurationElement(durationEndDate, durationStartDate, primaryDurationUnit, primaryDurationValue, secondaryDurationUnit, secondaryDurationValue);
    }
  }
}
