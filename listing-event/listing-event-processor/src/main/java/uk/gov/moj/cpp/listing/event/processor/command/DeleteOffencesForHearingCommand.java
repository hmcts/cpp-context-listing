package uk.gov.moj.cpp.listing.event.processor.command;

import uk.gov.moj.cpp.listing.domain.SimpleOffence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DeleteOffencesForHearingCommand {

  private final List<SimpleOffence> offences;

  private final UUID caseId;

  private final UUID defendantId;

  private final UUID hearingId;

  public DeleteOffencesForHearingCommand(final List<SimpleOffence> offences, final UUID hearingId, final UUID caseId, final UUID defendantId) {
    this.offences = new ArrayList<>(offences);
    this.hearingId = hearingId;
    this.caseId = caseId;
    this.defendantId = defendantId;
  }

  public List<SimpleOffence> getOffences() {
    return new ArrayList<>(offences);
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public UUID getCaseId() {
    return caseId;
  }

  public UUID getDefendantId() {
    return defendantId;
  }
}
