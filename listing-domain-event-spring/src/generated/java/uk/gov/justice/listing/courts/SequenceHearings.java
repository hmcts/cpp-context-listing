package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;

public class SequenceHearings implements Serializable {
  private static final long serialVersionUID = 9086484946466270462L;

  private final List<Hearings> hearings;

  @JsonCreator
  public SequenceHearings(final List<Hearings> hearings) {
    this.hearings = hearings;
  }

  public List<Hearings> getHearings() {
    return hearings;
  }

  public static Builder sequenceHearings() {
    return new uk.gov.justice.listing.courts.SequenceHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.SequenceHearings that = (uk.gov.justice.listing.courts.SequenceHearings) obj;

    return java.util.Objects.equals(this.hearings, that.hearings);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearings);}

  public static class Builder {
    private List<Hearings> hearings;

    public Builder withHearings(final List<Hearings> hearings) {
      this.hearings = hearings;
      return this;
    }

    public Builder withValuesFrom(final SequenceHearings sequenceHearings) {
      this.hearings = sequenceHearings.getHearings();
      return this;
    }

    public SequenceHearings build() {
      return new uk.gov.justice.listing.courts.SequenceHearings(hearings);
    }
  }
}
