package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.UUID;

public class DefendantsWithWelshTranslation implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final UUID defendantId;

  private final Boolean welshTranslation;

  @JsonCreator
  public DefendantsWithWelshTranslation(final UUID defendantId, final Boolean welshTranslation) {
    this.defendantId = defendantId;
    this.welshTranslation = welshTranslation;
  }

  public UUID getDefendantId() {
    return defendantId;
  }

  public Boolean getWelshTranslation() {
    return welshTranslation;
  }

  public static Builder defendantsWithWelshTranslation() {
    return new uk.gov.justice.core.courts.DefendantsWithWelshTranslation.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.DefendantsWithWelshTranslation that = (uk.gov.justice.core.courts.DefendantsWithWelshTranslation) obj;

    return java.util.Objects.equals(this.defendantId, that.defendantId) &&
    java.util.Objects.equals(this.welshTranslation, that.welshTranslation);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(defendantId, welshTranslation);}

  public static class Builder {
    private UUID defendantId;

    private Boolean welshTranslation;

    public Builder withDefendantId(final UUID defendantId) {
      this.defendantId = defendantId;
      return this;
    }

    public Builder withWelshTranslation(final Boolean welshTranslation) {
      this.welshTranslation = welshTranslation;
      return this;
    }

    public Builder withValuesFrom(final DefendantsWithWelshTranslation defendantsWithWelshTranslation) {
      this.defendantId = defendantsWithWelshTranslation.getDefendantId();
      this.welshTranslation = defendantsWithWelshTranslation.getWelshTranslation();
      return this;
    }

    public DefendantsWithWelshTranslation build() {
      return new uk.gov.justice.core.courts.DefendantsWithWelshTranslation(defendantId, welshTranslation);
    }
  }
}
