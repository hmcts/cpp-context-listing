package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Verdict implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final UUID applicationId;

  private final Boolean isDeleted;

  private final Jurors jurors;

  private final LesserOrAlternativeOffence lesserOrAlternativeOffence;

  private final UUID offenceId;

  private final UUID originatingHearingId;

  private final String verdictDate;

  private final VerdictType verdictType;

  @JsonCreator
  public Verdict(final UUID applicationId, final Boolean isDeleted, final Jurors jurors, final LesserOrAlternativeOffence lesserOrAlternativeOffence, final UUID offenceId, final UUID originatingHearingId, final String verdictDate, final VerdictType verdictType) {
    this.applicationId = applicationId;
    this.isDeleted = isDeleted;
    this.jurors = jurors;
    this.lesserOrAlternativeOffence = lesserOrAlternativeOffence;
    this.offenceId = offenceId;
    this.originatingHearingId = originatingHearingId;
    this.verdictDate = verdictDate;
    this.verdictType = verdictType;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public Boolean getIsDeleted() {
    return isDeleted;
  }

  public Jurors getJurors() {
    return jurors;
  }

  public LesserOrAlternativeOffence getLesserOrAlternativeOffence() {
    return lesserOrAlternativeOffence;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public UUID getOriginatingHearingId() {
    return originatingHearingId;
  }

  public String getVerdictDate() {
    return verdictDate;
  }

  public VerdictType getVerdictType() {
    return verdictType;
  }

  public static Builder verdict() {
    return new uk.gov.justice.core.courts.Verdict.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Verdict that = (uk.gov.justice.core.courts.Verdict) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.isDeleted, that.isDeleted) &&
    java.util.Objects.equals(this.jurors, that.jurors) &&
    java.util.Objects.equals(this.lesserOrAlternativeOffence, that.lesserOrAlternativeOffence) &&
    java.util.Objects.equals(this.offenceId, that.offenceId) &&
    java.util.Objects.equals(this.originatingHearingId, that.originatingHearingId) &&
    java.util.Objects.equals(this.verdictDate, that.verdictDate) &&
    java.util.Objects.equals(this.verdictType, that.verdictType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, isDeleted, jurors, lesserOrAlternativeOffence, offenceId, originatingHearingId, verdictDate, verdictType);}

  public static class Builder {
    private UUID applicationId;

    private Boolean isDeleted;

    private Jurors jurors;

    private LesserOrAlternativeOffence lesserOrAlternativeOffence;

    private UUID offenceId;

    private UUID originatingHearingId;

    private String verdictDate;

    private VerdictType verdictType;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withIsDeleted(final Boolean isDeleted) {
      this.isDeleted = isDeleted;
      return this;
    }

    public Builder withJurors(final Jurors jurors) {
      this.jurors = jurors;
      return this;
    }

    public Builder withLesserOrAlternativeOffence(final LesserOrAlternativeOffence lesserOrAlternativeOffence) {
      this.lesserOrAlternativeOffence = lesserOrAlternativeOffence;
      return this;
    }

    public Builder withOffenceId(final UUID offenceId) {
      this.offenceId = offenceId;
      return this;
    }

    public Builder withOriginatingHearingId(final UUID originatingHearingId) {
      this.originatingHearingId = originatingHearingId;
      return this;
    }

    public Builder withVerdictDate(final String verdictDate) {
      this.verdictDate = verdictDate;
      return this;
    }

    public Builder withVerdictType(final VerdictType verdictType) {
      this.verdictType = verdictType;
      return this;
    }

    public Builder withValuesFrom(final Verdict verdict) {
      this.applicationId = verdict.getApplicationId();
      this.isDeleted = verdict.getIsDeleted();
      this.jurors = verdict.getJurors();
      this.lesserOrAlternativeOffence = verdict.getLesserOrAlternativeOffence();
      this.offenceId = verdict.getOffenceId();
      this.originatingHearingId = verdict.getOriginatingHearingId();
      this.verdictDate = verdict.getVerdictDate();
      this.verdictType = verdict.getVerdictType();
      return this;
    }

    public Verdict build() {
      return new uk.gov.justice.core.courts.Verdict(applicationId, isDeleted, jurors, lesserOrAlternativeOffence, offenceId, originatingHearingId, verdictDate, verdictType);
    }
  }
}
