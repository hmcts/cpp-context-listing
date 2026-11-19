package uk.gov.justice.hearing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;

public class DefendantDetails implements Serializable {
  private static final long serialVersionUID = 5433828913769985343L;

  @JsonCreator
  public DefendantDetails() {
  }

  public static Builder defendantDetails() {
    return new uk.gov.justice.hearing.courts.DefendantDetails.Builder();
  }

  public static class Builder {
    public Builder withValuesFrom(final DefendantDetails defendantDetails) {
      return this;
    }

    public DefendantDetails build() {
      return new uk.gov.justice.hearing.courts.DefendantDetails();
    }
  }
}
