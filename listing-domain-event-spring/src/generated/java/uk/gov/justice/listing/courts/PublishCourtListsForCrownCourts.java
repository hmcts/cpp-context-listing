package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;

public class PublishCourtListsForCrownCourts implements Serializable {
  private static final long serialVersionUID = 6724344042512873842L;

  @JsonCreator
  public PublishCourtListsForCrownCourts() {
  }

  public static Builder publishCourtListsForCrownCourts() {
    return new uk.gov.justice.listing.courts.PublishCourtListsForCrownCourts.Builder();
  }

  public static class Builder {
    public Builder withValuesFrom(final PublishCourtListsForCrownCourts publishCourtListsForCrownCourts) {
      return this;
    }

    public PublishCourtListsForCrownCourts build() {
      return new uk.gov.justice.listing.courts.PublishCourtListsForCrownCourts();
    }
  }
}
