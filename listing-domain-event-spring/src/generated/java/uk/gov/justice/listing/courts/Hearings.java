package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.listing.commands.SequenceHearingDay;

public class Hearings implements Serializable {
  private static final long serialVersionUID = 9086484946466270462L;

  private final UUID id;

  private final List<SequenceHearingDay> sequenceHearingDays;

  @JsonCreator
  public Hearings(final UUID id, final List<SequenceHearingDay> sequenceHearingDays) {
    this.id = id;
    this.sequenceHearingDays = sequenceHearingDays;
  }

  public UUID getId() {
    return id;
  }

  public List<SequenceHearingDay> getSequenceHearingDays() {
    return sequenceHearingDays;
  }

  public static Builder hearings() {
    return new uk.gov.justice.listing.courts.Hearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.Hearings that = (uk.gov.justice.listing.courts.Hearings) obj;

    return java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.sequenceHearingDays, that.sequenceHearingDays);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id, sequenceHearingDays);}

  public static class Builder {
    private UUID id;

    private List<SequenceHearingDay> sequenceHearingDays;

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withSequenceHearingDays(final List<SequenceHearingDay> sequenceHearingDays) {
      this.sequenceHearingDays = sequenceHearingDays;
      return this;
    }

    public Builder withValuesFrom(final Hearings hearings) {
      this.id = hearings.getId();
      this.sequenceHearingDays = hearings.getSequenceHearingDays();
      return this;
    }

    public Hearings build() {
      return new uk.gov.justice.listing.courts.Hearings(id, sequenceHearingDays);
    }
  }
}
