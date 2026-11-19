package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;

public class IsBenchChairman implements Serializable {
  private static final long serialVersionUID = -2582361536581928800L;

  private final Boolean isBenchChairman;

  @JsonCreator
  public IsBenchChairman(final Boolean isBenchChairman) {
    this.isBenchChairman = isBenchChairman;
  }

  public Boolean getIsBenchChairman() {
    return isBenchChairman;
  }

  public static Builder isBenchChairman() {
    return new uk.gov.justice.listing.courts.IsBenchChairman.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.IsBenchChairman that = (uk.gov.justice.listing.courts.IsBenchChairman) obj;

    return java.util.Objects.equals(this.isBenchChairman, that.isBenchChairman);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(isBenchChairman);}

  public static class Builder {
    private Boolean isBenchChairman;

    public Builder withIsBenchChairman(final Boolean isBenchChairman) {
      this.isBenchChairman = isBenchChairman;
      return this;
    }

    public Builder withValuesFrom(final IsBenchChairman isBenchChairman) {
      this.isBenchChairman = isBenchChairman.getIsBenchChairman();
      return this;
    }

    public IsBenchChairman build() {
      return new uk.gov.justice.listing.courts.IsBenchChairman(isBenchChairman);
    }
  }
}
