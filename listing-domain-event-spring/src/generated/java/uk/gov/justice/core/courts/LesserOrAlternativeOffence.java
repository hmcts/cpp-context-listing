package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class LesserOrAlternativeOffence implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String offenceCode;

  private final UUID offenceDefinitionId;

  private final String offenceLegislation;

  private final String offenceLegislationWelsh;

  private final String offenceTitle;

  private final String offenceTitleWelsh;

  @JsonCreator
  public LesserOrAlternativeOffence(final String offenceCode, final UUID offenceDefinitionId, final String offenceLegislation, final String offenceLegislationWelsh, final String offenceTitle, final String offenceTitleWelsh) {
    this.offenceCode = offenceCode;
    this.offenceDefinitionId = offenceDefinitionId;
    this.offenceLegislation = offenceLegislation;
    this.offenceLegislationWelsh = offenceLegislationWelsh;
    this.offenceTitle = offenceTitle;
    this.offenceTitleWelsh = offenceTitleWelsh;
  }

  public String getOffenceCode() {
    return offenceCode;
  }

  public UUID getOffenceDefinitionId() {
    return offenceDefinitionId;
  }

  public String getOffenceLegislation() {
    return offenceLegislation;
  }

  public String getOffenceLegislationWelsh() {
    return offenceLegislationWelsh;
  }

  public String getOffenceTitle() {
    return offenceTitle;
  }

  public String getOffenceTitleWelsh() {
    return offenceTitleWelsh;
  }

  public static Builder lesserOrAlternativeOffence() {
    return new uk.gov.justice.core.courts.LesserOrAlternativeOffence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.LesserOrAlternativeOffence that = (uk.gov.justice.core.courts.LesserOrAlternativeOffence) obj;

    return java.util.Objects.equals(this.offenceCode, that.offenceCode) &&
    java.util.Objects.equals(this.offenceDefinitionId, that.offenceDefinitionId) &&
    java.util.Objects.equals(this.offenceLegislation, that.offenceLegislation) &&
    java.util.Objects.equals(this.offenceLegislationWelsh, that.offenceLegislationWelsh) &&
    java.util.Objects.equals(this.offenceTitle, that.offenceTitle) &&
    java.util.Objects.equals(this.offenceTitleWelsh, that.offenceTitleWelsh);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(offenceCode, offenceDefinitionId, offenceLegislation, offenceLegislationWelsh, offenceTitle, offenceTitleWelsh);}

  public static class Builder {
    private String offenceCode;

    private UUID offenceDefinitionId;

    private String offenceLegislation;

    private String offenceLegislationWelsh;

    private String offenceTitle;

    private String offenceTitleWelsh;

    public Builder withOffenceCode(final String offenceCode) {
      this.offenceCode = offenceCode;
      return this;
    }

    public Builder withOffenceDefinitionId(final UUID offenceDefinitionId) {
      this.offenceDefinitionId = offenceDefinitionId;
      return this;
    }

    public Builder withOffenceLegislation(final String offenceLegislation) {
      this.offenceLegislation = offenceLegislation;
      return this;
    }

    public Builder withOffenceLegislationWelsh(final String offenceLegislationWelsh) {
      this.offenceLegislationWelsh = offenceLegislationWelsh;
      return this;
    }

    public Builder withOffenceTitle(final String offenceTitle) {
      this.offenceTitle = offenceTitle;
      return this;
    }

    public Builder withOffenceTitleWelsh(final String offenceTitleWelsh) {
      this.offenceTitleWelsh = offenceTitleWelsh;
      return this;
    }

    public Builder withValuesFrom(final LesserOrAlternativeOffence lesserOrAlternativeOffence) {
      this.offenceCode = lesserOrAlternativeOffence.getOffenceCode();
      this.offenceDefinitionId = lesserOrAlternativeOffence.getOffenceDefinitionId();
      this.offenceLegislation = lesserOrAlternativeOffence.getOffenceLegislation();
      this.offenceLegislationWelsh = lesserOrAlternativeOffence.getOffenceLegislationWelsh();
      this.offenceTitle = lesserOrAlternativeOffence.getOffenceTitle();
      this.offenceTitleWelsh = lesserOrAlternativeOffence.getOffenceTitleWelsh();
      return this;
    }

    public LesserOrAlternativeOffence build() {
      return new uk.gov.justice.core.courts.LesserOrAlternativeOffence(offenceCode, offenceDefinitionId, offenceLegislation, offenceLegislationWelsh, offenceTitle, offenceTitleWelsh);
    }
  }
}
