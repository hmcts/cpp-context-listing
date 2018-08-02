package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.moj.cpp.listing.domain.Defendant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UpdateDefendantsForHearingCommand {

  private final List<Defendant> defendants;

  private final UUID hearingId;

  public UpdateDefendantsForHearingCommand(final List<Defendant> defendants, final UUID hearingId) {
    this.defendants = new ArrayList<>(defendants);
    this.hearingId = hearingId;
  }

  public List<Defendant> getDefendants() {
    return new ArrayList<>(defendants);
  }

  public UUID getHearingId() {
    return hearingId;
  }
}
