package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;

public class DeleteListingNote implements Serializable {
  private static final long serialVersionUID = 6675622357791141707L;

  @JsonCreator
  public DeleteListingNote() {
  }

  public static Builder deleteListingNote() {
    return new uk.gov.justice.listing.commands.DeleteListingNote.Builder();
  }

  public static class Builder {
    public Builder withValuesFrom(final DeleteListingNote deleteListingNote) {
      return this;
    }

    public DeleteListingNote build() {
      return new uk.gov.justice.listing.commands.DeleteListingNote();
    }
  }
}
