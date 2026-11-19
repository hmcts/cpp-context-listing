package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

public class JudiciaryChangedForHearingsStatus implements Serializable {
  private static final long serialVersionUID = -7473541048861580631L;

  private final String status;

  @JsonCreator
  public JudiciaryChangedForHearingsStatus(final String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public static Builder judiciaryChangedForHearingsStatus() {
    return new uk.gov.justice.listing.courts.JudiciaryChangedForHearingsStatus.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.JudiciaryChangedForHearingsStatus that = (uk.gov.justice.listing.courts.JudiciaryChangedForHearingsStatus) obj;

    return java.util.Objects.equals(this.status, that.status);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(status);}

  public static class Builder {
    private String status;

    public Builder withStatus(final String status) {
      this.status = status;
      return this;
    }

    public Builder withValuesFrom(final JudiciaryChangedForHearingsStatus judiciaryChangedForHearingsStatus) {
      this.status = judiciaryChangedForHearingsStatus.getStatus();
      return this;
    }

    public JudiciaryChangedForHearingsStatus build() {
      return new uk.gov.justice.listing.courts.JudiciaryChangedForHearingsStatus(status);
    }
  }
}
