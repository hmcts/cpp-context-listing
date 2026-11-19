package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.hearing-listed")
public class HearingListed implements Serializable {
  private static final long serialVersionUID = -1310724711683493705L;

  private final Hearing hearing;

  @JsonCreator
  public HearingListed(final Hearing hearing) {
    this.hearing = hearing;
  }

  public Hearing getHearing() {
    return hearing;
  }

  public static Builder hearingListed() {
    return new uk.gov.justice.listing.events.HearingListed.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.HearingListed that = (uk.gov.justice.listing.events.HearingListed) obj;

    return java.util.Objects.equals(this.hearing, that.hearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearing);}

  public static class Builder {
    private Hearing hearing;

    public Builder withHearing(final Hearing hearing) {
      this.hearing = hearing;
      return this;
    }

    public Builder withValuesFrom(final HearingListed hearingListed) {
      this.hearing = hearingListed.getHearing();
      return this;
    }

    public HearingListed build() {
      return new uk.gov.justice.listing.events.HearingListed(hearing);
    }
  }
}
