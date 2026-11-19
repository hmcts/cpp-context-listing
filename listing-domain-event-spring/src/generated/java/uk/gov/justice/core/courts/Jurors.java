package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;

public class Jurors implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Integer numberOfJurors;

  private final Integer numberOfSplitJurors;

  private final Boolean unanimous;

  @JsonCreator
  public Jurors(final Integer numberOfJurors, final Integer numberOfSplitJurors, final Boolean unanimous) {
    this.numberOfJurors = numberOfJurors;
    this.numberOfSplitJurors = numberOfSplitJurors;
    this.unanimous = unanimous;
  }

  public Integer getNumberOfJurors() {
    return numberOfJurors;
  }

  public Integer getNumberOfSplitJurors() {
    return numberOfSplitJurors;
  }

  public Boolean getUnanimous() {
    return unanimous;
  }

  public static Builder jurors() {
    return new uk.gov.justice.core.courts.Jurors.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.Jurors that = (uk.gov.justice.core.courts.Jurors) obj;

    return java.util.Objects.equals(this.numberOfJurors, that.numberOfJurors) &&
    java.util.Objects.equals(this.numberOfSplitJurors, that.numberOfSplitJurors) &&
    java.util.Objects.equals(this.unanimous, that.unanimous);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(numberOfJurors, numberOfSplitJurors, unanimous);}

  public static class Builder {
    private Integer numberOfJurors;

    private Integer numberOfSplitJurors;

    private Boolean unanimous;

    public Builder withNumberOfJurors(final Integer numberOfJurors) {
      this.numberOfJurors = numberOfJurors;
      return this;
    }

    public Builder withNumberOfSplitJurors(final Integer numberOfSplitJurors) {
      this.numberOfSplitJurors = numberOfSplitJurors;
      return this;
    }

    public Builder withUnanimous(final Boolean unanimous) {
      this.unanimous = unanimous;
      return this;
    }

    public Builder withValuesFrom(final Jurors jurors) {
      this.numberOfJurors = jurors.getNumberOfJurors();
      this.numberOfSplitJurors = jurors.getNumberOfSplitJurors();
      this.unanimous = jurors.getUnanimous();
      return this;
    }

    public Jurors build() {
      return new uk.gov.justice.core.courts.Jurors(numberOfJurors, numberOfSplitJurors, unanimous);
    }
  }
}
