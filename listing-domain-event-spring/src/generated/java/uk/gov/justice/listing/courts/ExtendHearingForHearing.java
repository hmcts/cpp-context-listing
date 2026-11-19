package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.CourtApplication;

public class ExtendHearingForHearing implements Serializable {
  private static final long serialVersionUID = -9048581471045514656L;

  private final UUID allocatedHearingId;

  private final List<CourtApplication> courtApplications;

  private final List<ProsecutionCases> prosecutionCases;

  private final Boolean sendNotificationToParties;

  @JsonCreator
  public ExtendHearingForHearing(final UUID allocatedHearingId, final List<CourtApplication> courtApplications, final List<ProsecutionCases> prosecutionCases, final Boolean sendNotificationToParties) {
    this.allocatedHearingId = allocatedHearingId;
    this.courtApplications = courtApplications;
    this.prosecutionCases = prosecutionCases;
    this.sendNotificationToParties = sendNotificationToParties;
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

  public static Builder extendHearingForHearing() {
    return new uk.gov.justice.listing.courts.ExtendHearingForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.ExtendHearingForHearing that = (uk.gov.justice.listing.courts.ExtendHearingForHearing) obj;

    return java.util.Objects.equals(this.allocatedHearingId, that.allocatedHearingId) &&
    java.util.Objects.equals(this.courtApplications, that.courtApplications) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(allocatedHearingId, courtApplications, prosecutionCases, sendNotificationToParties);}

  public static class Builder {
    private UUID allocatedHearingId;

    private List<CourtApplication> courtApplications;

    private List<ProsecutionCases> prosecutionCases;

    private Boolean sendNotificationToParties;

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

    public Builder withValuesFrom(final ExtendHearingForHearing extendHearingForHearing) {
      this.allocatedHearingId = extendHearingForHearing.getAllocatedHearingId();
      this.courtApplications = extendHearingForHearing.getCourtApplications();
      this.prosecutionCases = extendHearingForHearing.getProsecutionCases();
      this.sendNotificationToParties = extendHearingForHearing.getSendNotificationToParties();
      return this;
    }

    public ExtendHearingForHearing build() {
      return new uk.gov.justice.listing.courts.ExtendHearingForHearing(allocatedHearingId, courtApplications, prosecutionCases, sendNotificationToParties);
    }
  }
}
