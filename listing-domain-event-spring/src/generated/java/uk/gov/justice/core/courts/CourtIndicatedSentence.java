package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CourtIndicatedSentence implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final String courtIndicatedSentenceDescription;

  private final UUID courtIndicatedSentenceTypeId;

  @JsonCreator
  public CourtIndicatedSentence(final String courtIndicatedSentenceDescription, final UUID courtIndicatedSentenceTypeId) {
    this.courtIndicatedSentenceDescription = courtIndicatedSentenceDescription;
    this.courtIndicatedSentenceTypeId = courtIndicatedSentenceTypeId;
  }

  public String getCourtIndicatedSentenceDescription() {
    return courtIndicatedSentenceDescription;
  }

  public UUID getCourtIndicatedSentenceTypeId() {
    return courtIndicatedSentenceTypeId;
  }

  public static Builder courtIndicatedSentence() {
    return new uk.gov.justice.core.courts.CourtIndicatedSentence.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtIndicatedSentence that = (uk.gov.justice.core.courts.CourtIndicatedSentence) obj;

    return java.util.Objects.equals(this.courtIndicatedSentenceDescription, that.courtIndicatedSentenceDescription) &&
    java.util.Objects.equals(this.courtIndicatedSentenceTypeId, that.courtIndicatedSentenceTypeId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtIndicatedSentenceDescription, courtIndicatedSentenceTypeId);}

  public static class Builder {
    private String courtIndicatedSentenceDescription;

    private UUID courtIndicatedSentenceTypeId;

    public Builder withCourtIndicatedSentenceDescription(final String courtIndicatedSentenceDescription) {
      this.courtIndicatedSentenceDescription = courtIndicatedSentenceDescription;
      return this;
    }

    public Builder withCourtIndicatedSentenceTypeId(final UUID courtIndicatedSentenceTypeId) {
      this.courtIndicatedSentenceTypeId = courtIndicatedSentenceTypeId;
      return this;
    }

    public Builder withValuesFrom(final CourtIndicatedSentence courtIndicatedSentence) {
      this.courtIndicatedSentenceDescription = courtIndicatedSentence.getCourtIndicatedSentenceDescription();
      this.courtIndicatedSentenceTypeId = courtIndicatedSentence.getCourtIndicatedSentenceTypeId();
      return this;
    }

    public CourtIndicatedSentence build() {
      return new uk.gov.justice.core.courts.CourtIndicatedSentence(courtIndicatedSentenceDescription, courtIndicatedSentenceTypeId);
    }
  }
}
