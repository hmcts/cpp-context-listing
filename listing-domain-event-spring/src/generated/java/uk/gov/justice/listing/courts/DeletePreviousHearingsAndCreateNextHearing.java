package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;

public class DeletePreviousHearingsAndCreateNextHearing implements Serializable {
  private static final long serialVersionUID = 5216642992639585539L;

  private final CreateNextHearing createNextHearing;

  private final DeletePreviousHearings deletePreviousHearings;

  @JsonCreator
  public DeletePreviousHearingsAndCreateNextHearing(final CreateNextHearing createNextHearing, final DeletePreviousHearings deletePreviousHearings) {
    this.createNextHearing = createNextHearing;
    this.deletePreviousHearings = deletePreviousHearings;
  }

  public CreateNextHearing getCreateNextHearing() {
    return createNextHearing;
  }

  public DeletePreviousHearings getDeletePreviousHearings() {
    return deletePreviousHearings;
  }

  public static Builder deletePreviousHearingsAndCreateNextHearing() {
    return new uk.gov.justice.listing.courts.DeletePreviousHearingsAndCreateNextHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.DeletePreviousHearingsAndCreateNextHearing that = (uk.gov.justice.listing.courts.DeletePreviousHearingsAndCreateNextHearing) obj;

    return java.util.Objects.equals(this.createNextHearing, that.createNextHearing) &&
    java.util.Objects.equals(this.deletePreviousHearings, that.deletePreviousHearings);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(createNextHearing, deletePreviousHearings);}

  public static class Builder {
    private CreateNextHearing createNextHearing;

    private DeletePreviousHearings deletePreviousHearings;

    public Builder withCreateNextHearing(final CreateNextHearing createNextHearing) {
      this.createNextHearing = createNextHearing;
      return this;
    }

    public Builder withDeletePreviousHearings(final DeletePreviousHearings deletePreviousHearings) {
      this.deletePreviousHearings = deletePreviousHearings;
      return this;
    }

    public Builder withValuesFrom(final DeletePreviousHearingsAndCreateNextHearing deletePreviousHearingsAndCreateNextHearing) {
      this.createNextHearing = deletePreviousHearingsAndCreateNextHearing.getCreateNextHearing();
      this.deletePreviousHearings = deletePreviousHearingsAndCreateNextHearing.getDeletePreviousHearings();
      return this;
    }

    public DeletePreviousHearingsAndCreateNextHearing build() {
      return new uk.gov.justice.listing.courts.DeletePreviousHearingsAndCreateNextHearing(createNextHearing, deletePreviousHearings);
    }
  }
}
