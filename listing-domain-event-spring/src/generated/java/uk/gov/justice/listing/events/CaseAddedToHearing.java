package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.ConfirmedProsecutionCase;

public class CaseAddedToHearing implements Serializable {
  private static final long serialVersionUID = -9219648287822407011L;

  private final List<ConfirmedProsecutionCase> confirmedProsecutionCase;

  private final UUID existingHearingId;

  private final UUID hearingId;

  @JsonCreator
  public CaseAddedToHearing(final List<ConfirmedProsecutionCase> confirmedProsecutionCase, final UUID existingHearingId, final UUID hearingId) {
    this.confirmedProsecutionCase = confirmedProsecutionCase;
    this.existingHearingId = existingHearingId;
    this.hearingId = hearingId;
  }

  public List<ConfirmedProsecutionCase> getConfirmedProsecutionCase() {
    return confirmedProsecutionCase;
  }

  public UUID getExistingHearingId() {
    return existingHearingId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public static Builder caseAddedToHearing() {
    return new uk.gov.justice.listing.events.CaseAddedToHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CaseAddedToHearing that = (uk.gov.justice.listing.events.CaseAddedToHearing) obj;

    return java.util.Objects.equals(this.confirmedProsecutionCase, that.confirmedProsecutionCase) &&
    java.util.Objects.equals(this.existingHearingId, that.existingHearingId) &&
    java.util.Objects.equals(this.hearingId, that.hearingId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(confirmedProsecutionCase, existingHearingId, hearingId);}

  public static class Builder {
    private List<ConfirmedProsecutionCase> confirmedProsecutionCase;

    private UUID existingHearingId;

    private UUID hearingId;

    public Builder withConfirmedProsecutionCase(final List<ConfirmedProsecutionCase> confirmedProsecutionCase) {
      this.confirmedProsecutionCase = confirmedProsecutionCase;
      return this;
    }

    public Builder withExistingHearingId(final UUID existingHearingId) {
      this.existingHearingId = existingHearingId;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withValuesFrom(final CaseAddedToHearing caseAddedToHearing) {
      this.confirmedProsecutionCase = caseAddedToHearing.getConfirmedProsecutionCase();
      this.existingHearingId = caseAddedToHearing.getExistingHearingId();
      this.hearingId = caseAddedToHearing.getHearingId();
      return this;
    }

    public CaseAddedToHearing build() {
      return new uk.gov.justice.listing.events.CaseAddedToHearing(confirmedProsecutionCase, existingHearingId, hearingId);
    }
  }
}
