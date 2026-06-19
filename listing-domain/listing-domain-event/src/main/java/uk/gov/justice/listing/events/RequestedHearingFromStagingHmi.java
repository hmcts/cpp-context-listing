package uk.gov.justice.listing.events;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;

@Deprecated
@Event("listing.events.requested-hearing-from-staging-hmi")
public class RequestedHearingFromStagingHmi implements Serializable {
  private static final long serialVersionUID = 5807578003826852406L;

  private final Hearing hearing;

  @JsonCreator
  public RequestedHearingFromStagingHmi(final Hearing hearing) {
    this.hearing = hearing;
  }

  public Hearing getHearing() {
    return hearing;
  }

  public static Builder requestedHearingFromStagingHmi() {
    return new Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final RequestedHearingFromStagingHmi that = (RequestedHearingFromStagingHmi) obj;

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

    public Builder withValuesFrom(final RequestedHearingFromStagingHmi requestedHearingFromStagingHmi) {
      this.hearing = requestedHearingFromStagingHmi.getHearing();
      return this;
    }

    public RequestedHearingFromStagingHmi build() {
      return new RequestedHearingFromStagingHmi(hearing);
    }
  }
}
