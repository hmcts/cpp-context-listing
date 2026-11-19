package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;

public class CreateNextHearingRequested implements Serializable {
  private static final long serialVersionUID = 5569119786335285770L;

  private final CreateNextHearing createNextHearing;

  @JsonCreator
  public CreateNextHearingRequested(final CreateNextHearing createNextHearing) {
    this.createNextHearing = createNextHearing;
  }

  public CreateNextHearing getCreateNextHearing() {
    return createNextHearing;
  }

  public static Builder createNextHearingRequested() {
    return new uk.gov.justice.listing.events.CreateNextHearingRequested.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CreateNextHearingRequested that = (uk.gov.justice.listing.events.CreateNextHearingRequested) obj;

    return java.util.Objects.equals(this.createNextHearing, that.createNextHearing);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(createNextHearing);}

  public static class Builder {
    private CreateNextHearing createNextHearing;

    public Builder withCreateNextHearing(final CreateNextHearing createNextHearing) {
      this.createNextHearing = createNextHearing;
      return this;
    }

    public Builder withValuesFrom(final CreateNextHearingRequested createNextHearingRequested) {
      this.createNextHearing = createNextHearingRequested.getCreateNextHearing();
      return this;
    }

    public CreateNextHearingRequested build() {
      return new uk.gov.justice.listing.events.CreateNextHearingRequested(createNextHearing);
    }
  }
}
