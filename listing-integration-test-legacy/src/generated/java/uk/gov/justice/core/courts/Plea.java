package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class Plea implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final UUID applicationId;

  private final DelegatedPowers delegatedPowers;

  private final LesserOrAlternativeOffence lesserOrAlternativeOffence;

  private final UUID offenceId;

  private final UUID originatingHearingId;

  private final String pleaDate;

  private final String pleaValue;

  @JsonCreator
  public Plea(final UUID applicationId, final DelegatedPowers delegatedPowers, final LesserOrAlternativeOffence lesserOrAlternativeOffence, final UUID offenceId, final UUID originatingHearingId, final String pleaDate, final String pleaValue) {
    this.applicationId = applicationId;
    this.delegatedPowers = delegatedPowers;
    this.lesserOrAlternativeOffence = lesserOrAlternativeOffence;
    this.offenceId = offenceId;
    this.originatingHearingId = originatingHearingId;
    this.pleaDate = pleaDate;
    this.pleaValue = pleaValue;
  }

  public UUID getApplicationId() {
    return applicationId;
  }

  public DelegatedPowers getDelegatedPowers() {
    return delegatedPowers;
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

  public String getPleaDate() {
    return pleaDate;
  }

  public String getPleaValue() {
    return pleaValue;
  }

  public static Builder plea() {
    return new uk.gov.justice.core.courts.Plea.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Plea that = (uk.gov.justice.core.courts.Plea) obj;

    return java.util.Objects.equals(this.applicationId, that.applicationId) &&
    java.util.Objects.equals(this.delegatedPowers, that.delegatedPowers) &&
    java.util.Objects.equals(this.lesserOrAlternativeOffence, that.lesserOrAlternativeOffence) &&
    java.util.Objects.equals(this.offenceId, that.offenceId) &&
    java.util.Objects.equals(this.originatingHearingId, that.originatingHearingId) &&
    java.util.Objects.equals(this.pleaDate, that.pleaDate) &&
    java.util.Objects.equals(this.pleaValue, that.pleaValue);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(applicationId, delegatedPowers, lesserOrAlternativeOffence, offenceId, originatingHearingId, pleaDate, pleaValue);}

  public static class Builder {
    private UUID applicationId;

    private DelegatedPowers delegatedPowers;

    private LesserOrAlternativeOffence lesserOrAlternativeOffence;

    private UUID offenceId;

    private UUID originatingHearingId;

    private String pleaDate;

    private String pleaValue;

    public Builder withApplicationId(final UUID applicationId) {
      this.applicationId = applicationId;
      return this;
    }

    public Builder withDelegatedPowers(final DelegatedPowers delegatedPowers) {
      this.delegatedPowers = delegatedPowers;
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

    public Builder withPleaDate(final String pleaDate) {
      this.pleaDate = pleaDate;
      return this;
    }

    public Builder withPleaValue(final String pleaValue) {
      this.pleaValue = pleaValue;
      return this;
    }

    public Builder withValuesFrom(final Plea plea) {
      this.applicationId = plea.getApplicationId();
      this.delegatedPowers = plea.getDelegatedPowers();
      this.lesserOrAlternativeOffence = plea.getLesserOrAlternativeOffence();
      this.offenceId = plea.getOffenceId();
      this.originatingHearingId = plea.getOriginatingHearingId();
      this.pleaDate = plea.getPleaDate();
      this.pleaValue = plea.getPleaValue();
      return this;
    }

    public Plea build() {
      return new uk.gov.justice.core.courts.Plea(applicationId, delegatedPowers, lesserOrAlternativeOffence, offenceId, originatingHearingId, pleaDate, pleaValue);
    }
  }
}
