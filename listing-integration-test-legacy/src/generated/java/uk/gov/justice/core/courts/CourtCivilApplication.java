package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;

public class CourtCivilApplication implements Serializable {
  private static final long serialVersionUID = 8670175116192822584L;

  private final Boolean isCivil;

  private final Boolean isExParte;

  @JsonCreator
  public CourtCivilApplication(final Boolean isCivil, final Boolean isExParte) {
    this.isCivil = isCivil;
    this.isExParte = isExParte;
  }

  public Boolean getIsCivil() {
    return isCivil;
  }

  public Boolean getIsExParte() {
    return isExParte;
  }

  public static Builder courtCivilApplication() {
    return new uk.gov.justice.core.courts.CourtCivilApplication.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtCivilApplication that = (uk.gov.justice.core.courts.CourtCivilApplication) obj;

    return java.util.Objects.equals(this.isCivil, that.isCivil) &&
    java.util.Objects.equals(this.isExParte, that.isExParte);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(isCivil, isExParte);}

  public static class Builder {
    private Boolean isCivil;

    private Boolean isExParte;

    public Builder withIsCivil(final Boolean isCivil) {
      this.isCivil = isCivil;
      return this;
    }

    public Builder withIsExParte(final Boolean isExParte) {
      this.isExParte = isExParte;
      return this;
    }

    public Builder withValuesFrom(final CourtCivilApplication courtCivilApplication) {
      this.isCivil = courtCivilApplication.getIsCivil();
      this.isExParte = courtCivilApplication.getIsExParte();
      return this;
    }

    public CourtCivilApplication build() {
      return new uk.gov.justice.core.courts.CourtCivilApplication(isCivil, isExParte);
    }
  }
}
