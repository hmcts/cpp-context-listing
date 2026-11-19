package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;

public class UpdateHearingForListing implements Serializable {
  private static final long serialVersionUID = -6189780249334107910L;

  private final String bookingType;

  private final UUID courtCentreId;

  private final CourtRoomId courtRoomId;

  private final EndDate endDate;

  private final Boolean hasVideoLink;

  private final HearingLanguage hearingLanguage;

  private final List<Judiciary> judiciary;

  private final JudiciaryAssignmentSource judiciaryAssignmentSource;

  private final JurisdictionType jurisdictionType;

  private final List<NonDefaultDays> nonDefaultDays;

  private final List<String> nonSittingDays;

  private final String panel;

  private final String priority;

  private final List<ProsecutionCases> prosecutionCases;

  private final String publicListNote;

  private final SelectedCourtCentre selectedCourtCentre;

  private final Boolean sendNotificationToParties;

  private final String source;

  private final List<String> specialRequirements;

  private final String splitHearing;

  private final String startDate;

  private final HearingType type;

  private final Integer weekCommencingDurationInWeeks;

  private final String weekCommencingEndDate;

  private final String weekCommencingStartDate;

  @JsonCreator
  public UpdateHearingForListing(final String bookingType, final UUID courtCentreId, final CourtRoomId courtRoomId, final EndDate endDate, final Boolean hasVideoLink, final HearingLanguage hearingLanguage, final List<Judiciary> judiciary, final JudiciaryAssignmentSource judiciaryAssignmentSource, final JurisdictionType jurisdictionType, final List<NonDefaultDays> nonDefaultDays, final List<String> nonSittingDays, final String panel, final String priority, final List<ProsecutionCases> prosecutionCases, final String publicListNote, final SelectedCourtCentre selectedCourtCentre, final Boolean sendNotificationToParties, final String source, final List<String> specialRequirements, final String splitHearing, final String startDate, final HearingType type, final Integer weekCommencingDurationInWeeks, final String weekCommencingEndDate, final String weekCommencingStartDate) {
    this.bookingType = bookingType;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.endDate = endDate;
    this.hasVideoLink = hasVideoLink;
    this.hearingLanguage = hearingLanguage;
    this.judiciary = judiciary;
    this.judiciaryAssignmentSource = judiciaryAssignmentSource;
    this.jurisdictionType = jurisdictionType;
    this.nonDefaultDays = nonDefaultDays;
    this.nonSittingDays = nonSittingDays;
    this.panel = panel;
    this.priority = priority;
    this.prosecutionCases = prosecutionCases;
    this.publicListNote = publicListNote;
    this.selectedCourtCentre = selectedCourtCentre;
    this.sendNotificationToParties = sendNotificationToParties;
    this.source = source;
    this.specialRequirements = specialRequirements;
    this.splitHearing = splitHearing;
    this.startDate = startDate;
    this.type = type;
    this.weekCommencingDurationInWeeks = weekCommencingDurationInWeeks;
    this.weekCommencingEndDate = weekCommencingEndDate;
    this.weekCommencingStartDate = weekCommencingStartDate;
  }

  public String getBookingType() {
    return bookingType;
  }

  public UUID getCourtCentreId() {
    return courtCentreId;
  }

  public CourtRoomId getCourtRoomId() {
    return courtRoomId;
  }

  public EndDate getEndDate() {
    return endDate;
  }

  public Boolean getHasVideoLink() {
    return hasVideoLink;
  }

  public HearingLanguage getHearingLanguage() {
    return hearingLanguage;
  }

  public List<Judiciary> getJudiciary() {
    return judiciary;
  }

  public JudiciaryAssignmentSource getJudiciaryAssignmentSource() {
    return judiciaryAssignmentSource;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public List<NonDefaultDays> getNonDefaultDays() {
    return nonDefaultDays;
  }

  public List<String> getNonSittingDays() {
    return nonSittingDays;
  }

  public String getPanel() {
    return panel;
  }

  public String getPriority() {
    return priority;
  }

  public List<ProsecutionCases> getProsecutionCases() {
    return prosecutionCases;
  }

  public String getPublicListNote() {
    return publicListNote;
  }

  public SelectedCourtCentre getSelectedCourtCentre() {
    return selectedCourtCentre;
  }

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public String getSource() {
    return source;
  }

  public List<String> getSpecialRequirements() {
    return specialRequirements;
  }

  public String getSplitHearing() {
    return splitHearing;
  }

  public String getStartDate() {
    return startDate;
  }

  public HearingType getType() {
    return type;
  }

  public Integer getWeekCommencingDurationInWeeks() {
    return weekCommencingDurationInWeeks;
  }

  public String getWeekCommencingEndDate() {
    return weekCommencingEndDate;
  }

  public String getWeekCommencingStartDate() {
    return weekCommencingStartDate;
  }

  public static Builder updateHearingForListing() {
    return new uk.gov.justice.listing.courts.UpdateHearingForListing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateHearingForListing that = (uk.gov.justice.listing.courts.UpdateHearingForListing) obj;

    return java.util.Objects.equals(this.bookingType, that.bookingType) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.hasVideoLink, that.hasVideoLink) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.judiciaryAssignmentSource, that.judiciaryAssignmentSource) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.nonSittingDays, that.nonSittingDays) &&
    java.util.Objects.equals(this.panel, that.panel) &&
    java.util.Objects.equals(this.priority, that.priority) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.publicListNote, that.publicListNote) &&
    java.util.Objects.equals(this.selectedCourtCentre, that.selectedCourtCentre) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.source, that.source) &&
    java.util.Objects.equals(this.specialRequirements, that.specialRequirements) &&
    java.util.Objects.equals(this.splitHearing, that.splitHearing) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.weekCommencingDurationInWeeks, that.weekCommencingDurationInWeeks) &&
    java.util.Objects.equals(this.weekCommencingEndDate, that.weekCommencingEndDate) &&
    java.util.Objects.equals(this.weekCommencingStartDate, that.weekCommencingStartDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookingType, courtCentreId, courtRoomId, endDate, hasVideoLink, hearingLanguage, judiciary, judiciaryAssignmentSource, jurisdictionType, nonDefaultDays, nonSittingDays, panel, priority, prosecutionCases, publicListNote, selectedCourtCentre, sendNotificationToParties, source, specialRequirements, splitHearing, startDate, type, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);}

  public static class Builder {
    private String bookingType;

    private UUID courtCentreId;

    private CourtRoomId courtRoomId;

    private EndDate endDate;

    private Boolean hasVideoLink;

    private HearingLanguage hearingLanguage;

    private List<Judiciary> judiciary;

    private JudiciaryAssignmentSource judiciaryAssignmentSource;

    private JurisdictionType jurisdictionType;

    private List<NonDefaultDays> nonDefaultDays;

    private List<String> nonSittingDays;

    private String panel;

    private String priority;

    private List<ProsecutionCases> prosecutionCases;

    private String publicListNote;

    private SelectedCourtCentre selectedCourtCentre;

    private Boolean sendNotificationToParties;

    private String source;

    private List<String> specialRequirements;

    private String splitHearing;

    private String startDate;

    private HearingType type;

    private Integer weekCommencingDurationInWeeks;

    private String weekCommencingEndDate;

    private String weekCommencingStartDate;

    public Builder withBookingType(final String bookingType) {
      this.bookingType = bookingType;
      return this;
    }

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final CourtRoomId courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withEndDate(final EndDate endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withHasVideoLink(final Boolean hasVideoLink) {
      this.hasVideoLink = hasVideoLink;
      return this;
    }

    public Builder withHearingLanguage(final HearingLanguage hearingLanguage) {
      this.hearingLanguage = hearingLanguage;
      return this;
    }

    public Builder withJudiciary(final List<Judiciary> judiciary) {
      this.judiciary = judiciary;
      return this;
    }

    public Builder withJudiciaryAssignmentSource(final JudiciaryAssignmentSource judiciaryAssignmentSource) {
      this.judiciaryAssignmentSource = judiciaryAssignmentSource;
      return this;
    }

    public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
      this.jurisdictionType = jurisdictionType;
      return this;
    }

    public Builder withNonDefaultDays(final List<NonDefaultDays> nonDefaultDays) {
      this.nonDefaultDays = nonDefaultDays;
      return this;
    }

    public Builder withNonSittingDays(final List<String> nonSittingDays) {
      this.nonSittingDays = nonSittingDays;
      return this;
    }

    public Builder withPanel(final String panel) {
      this.panel = panel;
      return this;
    }

    public Builder withPriority(final String priority) {
      this.priority = priority;
      return this;
    }

    public Builder withProsecutionCases(final List<ProsecutionCases> prosecutionCases) {
      this.prosecutionCases = prosecutionCases;
      return this;
    }

    public Builder withPublicListNote(final String publicListNote) {
      this.publicListNote = publicListNote;
      return this;
    }

    public Builder withSelectedCourtCentre(final SelectedCourtCentre selectedCourtCentre) {
      this.selectedCourtCentre = selectedCourtCentre;
      return this;
    }

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withSource(final String source) {
      this.source = source;
      return this;
    }

    public Builder withSpecialRequirements(final List<String> specialRequirements) {
      this.specialRequirements = specialRequirements;
      return this;
    }

    public Builder withSplitHearing(final String splitHearing) {
      this.splitHearing = splitHearing;
      return this;
    }

    public Builder withStartDate(final String startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withType(final HearingType type) {
      this.type = type;
      return this;
    }

    public Builder withWeekCommencingDurationInWeeks(final Integer weekCommencingDurationInWeeks) {
      this.weekCommencingDurationInWeeks = weekCommencingDurationInWeeks;
      return this;
    }

    public Builder withWeekCommencingEndDate(final String weekCommencingEndDate) {
      this.weekCommencingEndDate = weekCommencingEndDate;
      return this;
    }

    public Builder withWeekCommencingStartDate(final String weekCommencingStartDate) {
      this.weekCommencingStartDate = weekCommencingStartDate;
      return this;
    }

    public Builder withValuesFrom(final UpdateHearingForListing updateHearingForListing) {
      this.bookingType = updateHearingForListing.getBookingType();
      this.courtCentreId = updateHearingForListing.getCourtCentreId();
      this.courtRoomId = updateHearingForListing.getCourtRoomId();
      this.endDate = updateHearingForListing.getEndDate();
      this.hasVideoLink = updateHearingForListing.getHasVideoLink();
      this.hearingLanguage = updateHearingForListing.getHearingLanguage();
      this.judiciary = updateHearingForListing.getJudiciary();
      this.judiciaryAssignmentSource = updateHearingForListing.getJudiciaryAssignmentSource();
      this.jurisdictionType = updateHearingForListing.getJurisdictionType();
      this.nonDefaultDays = updateHearingForListing.getNonDefaultDays();
      this.nonSittingDays = updateHearingForListing.getNonSittingDays();
      this.panel = updateHearingForListing.getPanel();
      this.priority = updateHearingForListing.getPriority();
      this.prosecutionCases = updateHearingForListing.getProsecutionCases();
      this.publicListNote = updateHearingForListing.getPublicListNote();
      this.selectedCourtCentre = updateHearingForListing.getSelectedCourtCentre();
      this.sendNotificationToParties = updateHearingForListing.getSendNotificationToParties();
      this.source = updateHearingForListing.getSource();
      this.specialRequirements = updateHearingForListing.getSpecialRequirements();
      this.splitHearing = updateHearingForListing.getSplitHearing();
      this.startDate = updateHearingForListing.getStartDate();
      this.type = updateHearingForListing.getType();
      this.weekCommencingDurationInWeeks = updateHearingForListing.getWeekCommencingDurationInWeeks();
      this.weekCommencingEndDate = updateHearingForListing.getWeekCommencingEndDate();
      this.weekCommencingStartDate = updateHearingForListing.getWeekCommencingStartDate();
      return this;
    }

    public UpdateHearingForListing build() {
      return new uk.gov.justice.listing.courts.UpdateHearingForListing(bookingType, courtCentreId, courtRoomId, endDate, hasVideoLink, hearingLanguage, judiciary, judiciaryAssignmentSource, jurisdictionType, nonDefaultDays, nonSittingDays, panel, priority, prosecutionCases, publicListNote, selectedCourtCentre, sendNotificationToParties, source, specialRequirements, splitHearing, startDate, type, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);
    }
  }
}
