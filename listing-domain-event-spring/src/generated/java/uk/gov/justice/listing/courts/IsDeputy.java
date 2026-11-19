package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;

public class IsDeputy implements Serializable {
  private static final long serialVersionUID = -2582361536581928800L;

  private final Boolean isDeputy;

  @JsonCreator
  public IsDeputy(final Boolean isDeputy) {
    this.isDeputy = isDeputy;
  }

  public Boolean getIsDeputy() {
    return isDeputy;
  }

  public static Builder isDeputy() {
    return new uk.gov.justice.listing.courts.IsDeputy.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.IsDeputy that = (uk.gov.justice.listing.courts.IsDeputy) obj;

    return java.util.Objects.equals(this.isDeputy, that.isDeputy);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(isDeputy);}

  public static class Builder {
    private Boolean isDeputy;

    public Builder withIsDeputy(final Boolean isDeputy) {
      this.isDeputy = isDeputy;
      return this;
    }

    public Builder withValuesFrom(final IsDeputy isDeputy) {
      this.isDeputy = isDeputy.getIsDeputy();
      return this;
    }

    public IsDeputy build() {
      return new uk.gov.justice.listing.courts.IsDeputy(isDeputy);
    }
  }
}
