package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.moj.cpp.listing.domain.Offence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AddOffencesForHearingCommand {

  private final List<Offence> offences;

  private final UUID hearingId;

  public AddOffencesForHearingCommand(final List<Offence> offences, final UUID hearingId) {
    this.offences = new ArrayList<>(offences);
    this.hearingId = hearingId;
  }

  public List<Offence> getOffences() {
    return new ArrayList<>(offences);
  }

  public UUID getHearingId() {
    return hearingId;
  }
}
