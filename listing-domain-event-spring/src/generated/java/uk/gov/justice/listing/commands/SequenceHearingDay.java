package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.time.LocalDate;

public class SequenceHearingDay implements Serializable {
  private static final long serialVersionUID = 9086484946466270462L;

  private final LocalDate hearingDate;

  private final Integer sequence;

  @JsonCreator
  public SequenceHearingDay(final LocalDate hearingDate, final Integer sequence) {
    this.hearingDate = hearingDate;
    this.sequence = sequence;
  }

  public LocalDate getHearingDate() {
    return hearingDate;
  }

  public Integer getSequence() {
    return sequence;
  }

  public static Builder sequenceHearingDay() {
    return new uk.gov.justice.listing.commands.SequenceHearingDay.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.SequenceHearingDay that = (uk.gov.justice.listing.commands.SequenceHearingDay) obj;

    return java.util.Objects.equals(this.hearingDate, that.hearingDate) &&
    java.util.Objects.equals(this.sequence, that.sequence);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingDate, sequence);}

  public static class Builder {
    private LocalDate hearingDate;

    private Integer sequence;

    public Builder withHearingDate(final LocalDate hearingDate) {
      this.hearingDate = hearingDate;
      return this;
    }

    public Builder withSequence(final Integer sequence) {
      this.sequence = sequence;
      return this;
    }

    public Builder withValuesFrom(final SequenceHearingDay sequenceHearingDay) {
      this.hearingDate = sequenceHearingDay.getHearingDate();
      this.sequence = sequenceHearingDay.getSequence();
      return this;
    }

    public SequenceHearingDay build() {
      return new uk.gov.justice.listing.commands.SequenceHearingDay(hearingDate, sequence);
    }
  }
}
