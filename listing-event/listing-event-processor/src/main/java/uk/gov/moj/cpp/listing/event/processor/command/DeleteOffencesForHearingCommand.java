package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.moj.cpp.listing.domain.SimpleOffence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeleteOffencesForHearingCommand {

  private final List<SimpleOffence> offences;

  private final UUID hearingId;

  public DeleteOffencesForHearingCommand(final List<SimpleOffence> offences, final UUID hearingId) {
    this.offences = new ArrayList<>(offences);
    this.hearingId = hearingId;
  }

  public List<SimpleOffence> getOffences() {
    return new ArrayList<>(offences);
  }

  public UUID getHearingId() {
    return hearingId;
  }
}
