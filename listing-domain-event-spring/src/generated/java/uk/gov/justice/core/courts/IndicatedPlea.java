package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class IndicatedPlea implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String indicatedPleaDate;

  private final IndicatedPleaValue indicatedPleaValue;

  private final UUID offenceId;

  private final UUID originatingHearingId;

  private final Source source;

  @JsonCreator
  public IndicatedPlea(final String indicatedPleaDate, final IndicatedPleaValue indicatedPleaValue, final UUID offenceId, final UUID originatingHearingId, final Source source) {
    this.indicatedPleaDate = indicatedPleaDate;
    this.indicatedPleaValue = indicatedPleaValue;
    this.offenceId = offenceId;
    this.originatingHearingId = originatingHearingId;
    this.source = source;
  }

  public String getIndicatedPleaDate() {
    return indicatedPleaDate;
  }

  public IndicatedPleaValue getIndicatedPleaValue() {
    return indicatedPleaValue;
  }

  public UUID getOffenceId() {
    return offenceId;
  }

  public UUID getOriginatingHearingId() {
    return originatingHearingId;
  }

  public Source getSource() {
    return source;
  }

  public static Builder indicatedPlea() {
    return new uk.gov.justice.core.courts.IndicatedPlea.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.IndicatedPlea that = (uk.gov.justice.core.courts.IndicatedPlea) obj;

    return java.util.Objects.equals(this.indicatedPleaDate, that.indicatedPleaDate) &&
    java.util.Objects.equals(this.indicatedPleaValue, that.indicatedPleaValue) &&
    java.util.Objects.equals(this.offenceId, that.offenceId) &&
    java.util.Objects.equals(this.originatingHearingId, that.originatingHearingId) &&
    java.util.Objects.equals(this.source, that.source);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(indicatedPleaDate, indicatedPleaValue, offenceId, originatingHearingId, source);}

  public static class Builder {
    private String indicatedPleaDate;

    private IndicatedPleaValue indicatedPleaValue;

    private UUID offenceId;

    private UUID originatingHearingId;

    private Source source;

    public Builder withIndicatedPleaDate(final String indicatedPleaDate) {
      this.indicatedPleaDate = indicatedPleaDate;
      return this;
    }

    public Builder withIndicatedPleaValue(final IndicatedPleaValue indicatedPleaValue) {
      this.indicatedPleaValue = indicatedPleaValue;
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

    public Builder withSource(final Source source) {
      this.source = source;
      return this;
    }

    public Builder withValuesFrom(final IndicatedPlea indicatedPlea) {
      this.indicatedPleaDate = indicatedPlea.getIndicatedPleaDate();
      this.indicatedPleaValue = indicatedPlea.getIndicatedPleaValue();
      this.offenceId = indicatedPlea.getOffenceId();
      this.originatingHearingId = indicatedPlea.getOriginatingHearingId();
      this.source = indicatedPlea.getSource();
      return this;
    }

    public IndicatedPlea build() {
      return new uk.gov.justice.core.courts.IndicatedPlea(indicatedPleaDate, indicatedPleaValue, offenceId, originatingHearingId, source);
    }
  }
}
