package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;

public class CivilOffence implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Boolean isExParte;

  private final Boolean isRespondent;

  @JsonCreator
  public CivilOffence(final Boolean isExParte, final Boolean isRespondent) {
    this.isExParte = isExParte;
    this.isRespondent = isRespondent;
  }

  public Boolean getIsExParte() {
    return isExParte;
  }

  public Boolean getIsRespondent() {
    return isRespondent;
  }

  public static Builder civilOffence() {
    return new uk.gov.justice.core.courts.CivilOffence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CivilOffence that = (uk.gov.justice.core.courts.CivilOffence) obj;

    return java.util.Objects.equals(this.isExParte, that.isExParte) &&
    java.util.Objects.equals(this.isRespondent, that.isRespondent);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(isExParte, isRespondent);}

  public static class Builder {
    private Boolean isExParte;

    private Boolean isRespondent;

    public Builder withIsExParte(final Boolean isExParte) {
      this.isExParte = isExParte;
      return this;
    }

    public Builder withIsRespondent(final Boolean isRespondent) {
      this.isRespondent = isRespondent;
      return this;
    }

    public Builder withValuesFrom(final CivilOffence civilOffence) {
      this.isExParte = civilOffence.getIsExParte();
      this.isRespondent = civilOffence.getIsRespondent();
      return this;
    }

    public CivilOffence build() {
      return new uk.gov.justice.core.courts.CivilOffence(isExParte, isRespondent);
    }
  }
}
