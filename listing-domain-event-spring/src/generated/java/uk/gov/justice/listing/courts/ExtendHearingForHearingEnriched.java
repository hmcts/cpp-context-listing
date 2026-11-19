package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.CourtApplication;

public class ExtendHearingForHearingEnriched implements Serializable {
  private static final long serialVersionUID = 1047907063408303953L;

  private final UUID allocatedHearingId;

  private final List<CourtApplication> courtApplications;

  private final List<ProsecutionCases> prosecutionCases;

  private final Boolean sendNotificationToParties;

  private final UUID unAllocatedHearingId;

  @JsonCreator
  public ExtendHearingForHearingEnriched(final UUID allocatedHearingId, final List<CourtApplication> courtApplications, final List<ProsecutionCases> prosecutionCases, final Boolean sendNotificationToParties, final UUID unAllocatedHearingId) {
    this.allocatedHearingId = allocatedHearingId;
    this.courtApplications = courtApplications;
    this.prosecutionCases = prosecutionCases;
    this.sendNotificationToParties = sendNotificationToParties;
    this.unAllocatedHearingId = unAllocatedHearingId;
  }

  public UUID getAllocatedHearingId() {
    return allocatedHearingId;
  }

  public List<CourtApplication> getCourtApplications() {
    return courtApplications;
  }

  public List<ProsecutionCases> getProsecutionCases() {
    return prosecutionCases;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public UUID getUnAllocatedHearingId() {
    return unAllocatedHearingId;
  }

  public static Builder extendHearingForHearingEnriched() {
    return new uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched that = (uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched) obj;

    return java.util.Objects.equals(this.allocatedHearingId, that.allocatedHearingId) &&
    java.util.Objects.equals(this.courtApplications, that.courtApplications) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.unAllocatedHearingId, that.unAllocatedHearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allocatedHearingId, courtApplications, prosecutionCases, sendNotificationToParties, unAllocatedHearingId);}

  public static class Builder {
    private UUID allocatedHearingId;

    private List<CourtApplication> courtApplications;

    private List<ProsecutionCases> prosecutionCases;

    private Boolean sendNotificationToParties;

    private UUID unAllocatedHearingId;

    public Builder withAllocatedHearingId(final UUID allocatedHearingId) {
      this.allocatedHearingId = allocatedHearingId;
      return this;
    }

    public Builder withCourtApplications(final List<CourtApplication> courtApplications) {
      this.courtApplications = courtApplications;
      return this;
    }

    public Builder withProsecutionCases(final List<ProsecutionCases> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withUnAllocatedHearingId(final UUID unAllocatedHearingId) {
      this.unAllocatedHearingId = unAllocatedHearingId;
      return this;
    }

    public Builder withValuesFrom(final ExtendHearingForHearingEnriched extendHearingForHearingEnriched) {
      this.allocatedHearingId = extendHearingForHearingEnriched.getAllocatedHearingId();
      this.courtApplications = extendHearingForHearingEnriched.getCourtApplications();
      this.prosecutionCases = extendHearingForHearingEnriched.getProsecutionCases();
      this.sendNotificationToParties = extendHearingForHearingEnriched.getSendNotificationToParties();
      this.unAllocatedHearingId = extendHearingForHearingEnriched.getUnAllocatedHearingId();
      return this;
    }

    public ExtendHearingForHearingEnriched build() {
      return new uk.gov.justice.listing.courts.ExtendHearingForHearingEnriched(allocatedHearingId, courtApplications, prosecutionCases, sendNotificationToParties, unAllocatedHearingId);
    }
  }
}
