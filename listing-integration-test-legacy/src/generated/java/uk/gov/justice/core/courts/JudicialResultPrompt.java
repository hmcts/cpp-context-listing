package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class JudicialResultPrompt implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Boolean activeOrderExtended;

  private final Boolean activeOrderNotExtended;

  private final String courtExtract;

  private final JudicialResultPromptDurationElement durationElement;

  private final Integer durationSequence;

  private final Boolean isDurationEndDate;

  private final Boolean isDurationStartDate;

  private final Boolean isFinancialImposition;

  private final UUID judicialResultPromptTypeId;

  private final String label;

  private final String promptReference;

  private final BigDecimal promptSequence;

  private final String qualifier;

  private final BigDecimal totalPenaltyPoints;

  private final String type;

  private final List<String> usergroups;

  private final String value;

  private final String welshLabel;

  private final String welshValue;

  @JsonCreator
  public JudicialResultPrompt(final Boolean activeOrderExtended, final Boolean activeOrderNotExtended, final String courtExtract, final JudicialResultPromptDurationElement durationElement, final Integer durationSequence, final Boolean isDurationEndDate, final Boolean isDurationStartDate, final Boolean isFinancialImposition, final UUID judicialResultPromptTypeId, final String label, final String promptReference, final BigDecimal promptSequence, final String qualifier, final BigDecimal totalPenaltyPoints, final String type, final List<String> usergroups, final String value, final String welshLabel, final String welshValue) {
    this.activeOrderExtended = activeOrderExtended;
    this.activeOrderNotExtended = activeOrderNotExtended;
    this.courtExtract = courtExtract;
    this.durationElement = durationElement;
    this.durationSequence = durationSequence;
    this.isDurationEndDate = isDurationEndDate;
    this.isDurationStartDate = isDurationStartDate;
    this.isFinancialImposition = isFinancialImposition;
    this.judicialResultPromptTypeId = judicialResultPromptTypeId;
    this.label = label;
    this.promptReference = promptReference;
    this.promptSequence = promptSequence;
    this.qualifier = qualifier;
    this.totalPenaltyPoints = totalPenaltyPoints;
    this.type = type;
    this.usergroups = usergroups;
    this.value = value;
    this.welshLabel = welshLabel;
    this.welshValue = welshValue;
  }

  public Boolean getActiveOrderExtended() {
    return activeOrderExtended;
  }

  public Boolean getActiveOrderNotExtended() {
    return activeOrderNotExtended;
  }

  public String getCourtExtract() {
    return courtExtract;
  }

  public JudicialResultPromptDurationElement getDurationElement() {
    return durationElement;
  }

  public Integer getDurationSequence() {
    return durationSequence;
  }

  public Boolean getIsDurationEndDate() {
    return isDurationEndDate;
  }

  public Boolean getIsDurationStartDate() {
    return isDurationStartDate;
  }

  public Boolean getIsFinancialImposition() {
    return isFinancialImposition;
  }

  public UUID getJudicialResultPromptTypeId() {
    return judicialResultPromptTypeId;
  }

  public String getLabel() {
    return label;
  }

  public String getPromptReference() {
    return promptReference;
  }

  public BigDecimal getPromptSequence() {
    return promptSequence;
  }

  public String getQualifier() {
    return qualifier;
  }

  public BigDecimal getTotalPenaltyPoints() {
    return totalPenaltyPoints;
  }

  public String getType() {
    return type;
  }

  public List<String> getUsergroups() {
    return usergroups;
  }

  public String getValue() {
    return value;
  }

  public String getWelshLabel() {
    return welshLabel;
  }

  public String getWelshValue() {
    return welshValue;
  }

  public static Builder judicialResultPrompt() {
    return new uk.gov.justice.core.courts.JudicialResultPrompt.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.JudicialResultPrompt that = (uk.gov.justice.core.courts.JudicialResultPrompt) obj;

    return java.util.Objects.equals(this.activeOrderExtended, that.activeOrderExtended) &&
    java.util.Objects.equals(this.activeOrderNotExtended, that.activeOrderNotExtended) &&
    java.util.Objects.equals(this.courtExtract, that.courtExtract) &&
    java.util.Objects.equals(this.durationElement, that.durationElement) &&
    java.util.Objects.equals(this.durationSequence, that.durationSequence) &&
    java.util.Objects.equals(this.isDurationEndDate, that.isDurationEndDate) &&
    java.util.Objects.equals(this.isDurationStartDate, that.isDurationStartDate) &&
    java.util.Objects.equals(this.isFinancialImposition, that.isFinancialImposition) &&
    java.util.Objects.equals(this.judicialResultPromptTypeId, that.judicialResultPromptTypeId) &&
    java.util.Objects.equals(this.label, that.label) &&
    java.util.Objects.equals(this.promptReference, that.promptReference) &&
    java.util.Objects.equals(this.promptSequence, that.promptSequence) &&
    java.util.Objects.equals(this.qualifier, that.qualifier) &&
    java.util.Objects.equals(this.totalPenaltyPoints, that.totalPenaltyPoints) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.usergroups, that.usergroups) &&
    java.util.Objects.equals(this.value, that.value) &&
    java.util.Objects.equals(this.welshLabel, that.welshLabel) &&
    java.util.Objects.equals(this.welshValue, that.welshValue);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(activeOrderExtended, activeOrderNotExtended, courtExtract, durationElement, durationSequence, isDurationEndDate, isDurationStartDate, isFinancialImposition, judicialResultPromptTypeId, label, promptReference, promptSequence, qualifier, totalPenaltyPoints, type, usergroups, value, welshLabel, welshValue);}

  public static class Builder {
    private Boolean activeOrderExtended;

    private Boolean activeOrderNotExtended;

    private String courtExtract;

    private JudicialResultPromptDurationElement durationElement;

    private Integer durationSequence;

    private Boolean isDurationEndDate;

    private Boolean isDurationStartDate;

    private Boolean isFinancialImposition;

    private UUID judicialResultPromptTypeId;

    private String label;

    private String promptReference;

    private BigDecimal promptSequence;

    private String qualifier;

    private BigDecimal totalPenaltyPoints;

    private String type;

    private List<String> usergroups;

    private String value;

    private String welshLabel;

    private String welshValue;

    public Builder withActiveOrderExtended(final Boolean activeOrderExtended) {
      this.activeOrderExtended = activeOrderExtended;
      return this;
    }

    public Builder withActiveOrderNotExtended(final Boolean activeOrderNotExtended) {
      this.activeOrderNotExtended = activeOrderNotExtended;
      return this;
    }

    public Builder withCourtExtract(final String courtExtract) {
      this.courtExtract = courtExtract;
      return this;
    }

    public Builder withDurationElement(final JudicialResultPromptDurationElement durationElement) {
      this.durationElement = durationElement;
      return this;
    }

    public Builder withDurationSequence(final Integer durationSequence) {
      this.durationSequence = durationSequence;
      return this;
    }

    public Builder withIsDurationEndDate(final Boolean isDurationEndDate) {
      this.isDurationEndDate = isDurationEndDate;
      return this;
    }

    public Builder withIsDurationStartDate(final Boolean isDurationStartDate) {
      this.isDurationStartDate = isDurationStartDate;
      return this;
    }

    public Builder withIsFinancialImposition(final Boolean isFinancialImposition) {
      this.isFinancialImposition = isFinancialImposition;
      return this;
    }

    public Builder withJudicialResultPromptTypeId(final UUID judicialResultPromptTypeId) {
      this.judicialResultPromptTypeId = judicialResultPromptTypeId;
      return this;
    }

    public Builder withLabel(final String label) {
      this.label = label;
      return this;
    }

    public Builder withPromptReference(final String promptReference) {
      this.promptReference = promptReference;
      return this;
    }

    public Builder withPromptSequence(final BigDecimal promptSequence) {
      this.promptSequence = promptSequence;
      return this;
    }

    public Builder withQualifier(final String qualifier) {
      this.qualifier = qualifier;
      return this;
    }

    public Builder withTotalPenaltyPoints(final BigDecimal totalPenaltyPoints) {
      this.totalPenaltyPoints = totalPenaltyPoints;
      return this;
    }

    public Builder withType(final String type) {
      this.type = type;
      return this;
    }

    public Builder withUsergroups(final List<String> usergroups) {
      this.usergroups = usergroups;
      return this;
    }

    public Builder withValue(final String value) {
      this.value = value;
      return this;
    }

    public Builder withWelshLabel(final String welshLabel) {
      this.welshLabel = welshLabel;
      return this;
    }

    public Builder withWelshValue(final String welshValue) {
      this.welshValue = welshValue;
      return this;
    }

    public Builder withValuesFrom(final JudicialResultPrompt judicialResultPrompt) {
      this.activeOrderExtended = judicialResultPrompt.getActiveOrderExtended();
      this.activeOrderNotExtended = judicialResultPrompt.getActiveOrderNotExtended();
      this.courtExtract = judicialResultPrompt.getCourtExtract();
      this.durationElement = judicialResultPrompt.getDurationElement();
      this.durationSequence = judicialResultPrompt.getDurationSequence();
      this.isDurationEndDate = judicialResultPrompt.getIsDurationEndDate();
      this.isDurationStartDate = judicialResultPrompt.getIsDurationStartDate();
      this.isFinancialImposition = judicialResultPrompt.getIsFinancialImposition();
      this.judicialResultPromptTypeId = judicialResultPrompt.getJudicialResultPromptTypeId();
      this.label = judicialResultPrompt.getLabel();
      this.promptReference = judicialResultPrompt.getPromptReference();
      this.promptSequence = judicialResultPrompt.getPromptSequence();
      this.qualifier = judicialResultPrompt.getQualifier();
      this.totalPenaltyPoints = judicialResultPrompt.getTotalPenaltyPoints();
      this.type = judicialResultPrompt.getType();
      this.usergroups = judicialResultPrompt.getUsergroups();
      this.value = judicialResultPrompt.getValue();
      this.welshLabel = judicialResultPrompt.getWelshLabel();
      this.welshValue = judicialResultPrompt.getWelshValue();
      return this;
    }

    public JudicialResultPrompt build() {
      return new uk.gov.justice.core.courts.JudicialResultPrompt(activeOrderExtended, activeOrderNotExtended, courtExtract, durationElement, durationSequence, isDurationEndDate, isDurationStartDate, isFinancialImposition, judicialResultPromptTypeId, label, promptReference, promptSequence, qualifier, totalPenaltyPoints, type, usergroups, value, welshLabel, welshValue);
    }
  }
}
