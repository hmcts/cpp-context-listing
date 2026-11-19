package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingDay;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;

public class AddCourtApplicationForHearing implements Serializable {
  private static final long serialVersionUID = 4236291654523980784L;

  private final CourtApplication courtApplication;

  private final CourtCentre courtCentre;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  private final JurisdictionType jurisdictionType;

  private final List<ProsecutionCase> prosecutionCases;

  private final List<UUID> shadowListedOffences;

  @JsonCreator
  public AddCourtApplicationForHearing(final CourtApplication courtApplication, final CourtCentre courtCentre, final List<HearingDay> hearingDays, final UUID hearingId, final JurisdictionType jurisdictionType, final List<ProsecutionCase> prosecutionCases, final List<UUID> shadowListedOffences) {
    this.courtApplication = courtApplication;
    this.courtCentre = courtCentre;
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
    this.jurisdictionType = jurisdictionType;
    this.prosecutionCases = prosecutionCases;
    this.shadowListedOffences = shadowListedOffences;
  }

  public CourtApplication getCourtApplication() {
    return courtApplication;
  }

  public CourtCentre getCourtCentre() {
    return courtCentre;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public List<ProsecutionCase> getProsecutionCases() {
    return prosecutionCases;
  }

  public List<UUID> getShadowListedOffences() {
    return shadowListedOffences;
  }

  public static Builder addCourtApplicationForHearing() {
    return new uk.gov.justice.listing.courts.AddCourtApplicationForHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.AddCourtApplicationForHearing that = (uk.gov.justice.listing.courts.AddCourtApplicationForHearing) obj;

    return java.util.Objects.equals(this.courtApplication, that.courtApplication) &&
    java.util.Objects.equals(this.courtCentre, that.courtCentre) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.shadowListedOffences, that.shadowListedOffences);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(courtApplication, courtCentre, hearingDays, hearingId, jurisdictionType, prosecutionCases, shadowListedOffences);}

  public static class Builder {
    private CourtApplication courtApplication;

    private CourtCentre courtCentre;

    private List<HearingDay> hearingDays;

    private UUID hearingId;

    private JurisdictionType jurisdictionType;

    private List<ProsecutionCase> prosecutionCases;

    private List<UUID> shadowListedOffences;

    public Builder withCourtApplication(final CourtApplication courtApplication) {
      this.courtApplication = courtApplication;
      return this;
    }

    public Builder withCourtCentre(final CourtCentre courtCentre) {
      this.courtCentre = courtCentre;
      return this;
    }

    public Builder withHearingDays(final List<HearingDay> hearingDays) {
      this.hearingDays = hearingDays;
      return this;
    }

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
      return this;
    }

    public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
      this.jurisdictionType = jurisdictionType;
      return this;
    }

    public Builder withProsecutionCases(final List<ProsecutionCase> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withShadowListedOffences(final List<UUID> shadowListedOffences) {
      this.shadowListedOffences = shadowListedOffences;
      return this;
    }

    public Builder withValuesFrom(final AddCourtApplicationForHearing addCourtApplicationForHearing) {
      this.courtApplication = addCourtApplicationForHearing.getCourtApplication();
      this.courtCentre = addCourtApplicationForHearing.getCourtCentre();
      this.hearingDays = addCourtApplicationForHearing.getHearingDays();
      this.hearingId = addCourtApplicationForHearing.getHearingId();
      this.jurisdictionType = addCourtApplicationForHearing.getJurisdictionType();
      this.prosecutionCases = addCourtApplicationForHearing.getProsecutionCases();
      this.shadowListedOffences = addCourtApplicationForHearing.getShadowListedOffences();
      return this;
    }

    public AddCourtApplicationForHearing build() {
      return new uk.gov.justice.listing.courts.AddCourtApplicationForHearing(courtApplication, courtCentre, hearingDays, hearingId, jurisdictionType, prosecutionCases, shadowListedOffences);
    }
  }
}
