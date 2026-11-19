package uk.gov.justice.listing.commands;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JudicialRole;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.courts.JudiciaryAssignmentSource;
import uk.gov.justice.listing.courts.SelectedCourtCentre;

public class UpdateHearingForListing implements Serializable {
  private static final long serialVersionUID = 7919779722927081093L;

  private final String bookingType;

  private final UUID courtCentreId;

  private final UUID courtRoomId;

  private final LocalDate endDate;

  private final Boolean hasVideoLink;

  private final List<HearingDay> hearingDays;

  private final UUID hearingId;

  private final HearingLanguage hearingLanguage;

  private final Boolean isJudiciaryFromCourtSchedule;

  private final List<JudicialRole> judiciary;

  private final JudiciaryAssignmentSource judiciaryAssignmentSource;

  private final JurisdictionType jurisdictionType;

  private final List<NonDefaultDay> nonDefaultDays;

  private final List<LocalDate> nonSittingDays;

  private final String panel;

  private final String priority;

  private final String publicListNote;

  private final SelectedCourtCentre selectedCourtCentre;

  private final Boolean sendNotificationToParties;

  private final String source;

  private final List<String> specialRequirements;

  private final String splitHearing;

  private final LocalDate startDate;

  private final Boolean startDateAndWeekCommencingOptional;

  private final HearingType type;

  private final Integer weekCommencingDurationInWeeks;

  private final LocalDate weekCommencingEndDate;

  private final LocalDate weekCommencingStartDate;

  @JsonCreator
  public UpdateHearingForListing(final String bookingType, final UUID courtCentreId, final UUID courtRoomId, final LocalDate endDate, final Boolean hasVideoLink, final List<HearingDay> hearingDays, final UUID hearingId, final HearingLanguage hearingLanguage, final Boolean isJudiciaryFromCourtSchedule, final List<JudicialRole> judiciary, final JudiciaryAssignmentSource judiciaryAssignmentSource, final JurisdictionType jurisdictionType, final List<NonDefaultDay> nonDefaultDays, final List<LocalDate> nonSittingDays, final String panel, final String priority, final String publicListNote, final SelectedCourtCentre selectedCourtCentre, final Boolean sendNotificationToParties, final String source, final List<String> specialRequirements, final String splitHearing, final LocalDate startDate, final Boolean startDateAndWeekCommencingOptional, final HearingType type, final Integer weekCommencingDurationInWeeks, final LocalDate weekCommencingEndDate, final LocalDate weekCommencingStartDate) {
    this.bookingType = bookingType;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.endDate = endDate;
    this.hasVideoLink = hasVideoLink;
    this.hearingDays = hearingDays;
    this.hearingId = hearingId;
    this.hearingLanguage = hearingLanguage;
    this.isJudiciaryFromCourtSchedule = isJudiciaryFromCourtSchedule;
    this.judiciary = judiciary;
    this.judiciaryAssignmentSource = judiciaryAssignmentSource;
    this.jurisdictionType = jurisdictionType;
    this.nonDefaultDays = nonDefaultDays;
    this.nonSittingDays = nonSittingDays;
    this.panel = panel;
    this.priority = priority;
    this.publicListNote = publicListNote;
    this.selectedCourtCentre = selectedCourtCentre;
    this.sendNotificationToParties = sendNotificationToParties;
    this.source = source;
    this.specialRequirements = specialRequirements;
    this.splitHearing = splitHearing;
    this.startDate = startDate;
    this.startDateAndWeekCommencingOptional = startDateAndWeekCommencingOptional;
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

  public UUID getCourtRoomId() {
    return courtRoomId;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public Boolean getHasVideoLink() {
    return hasVideoLink;
  }

  public List<HearingDay> getHearingDays() {
    return hearingDays;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public HearingLanguage getHearingLanguage() {
    return hearingLanguage;
  }

  public Boolean getIsJudiciaryFromCourtSchedule() {
    return isJudiciaryFromCourtSchedule;
  }

  public List<JudicialRole> getJudiciary() {
    return judiciary;
  }

  public JudiciaryAssignmentSource getJudiciaryAssignmentSource() {
    return judiciaryAssignmentSource;
  }

  public JurisdictionType getJurisdictionType() {
    return jurisdictionType;
  }

  public List<NonDefaultDay> getNonDefaultDays() {
    return nonDefaultDays;
  }

  public List<LocalDate> getNonSittingDays() {
    return nonSittingDays;
  }

  public String getPanel() {
    return panel;
  }

  public String getPriority() {
    return priority;
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

  public LocalDate getStartDate() {
    return startDate;
  }

  public Boolean getStartDateAndWeekCommencingOptional() {
    return startDateAndWeekCommencingOptional;
  }

  public HearingType getType() {
    return type;
  }

  public Integer getWeekCommencingDurationInWeeks() {
    return weekCommencingDurationInWeeks;
  }

  public LocalDate getWeekCommencingEndDate() {
    return weekCommencingEndDate;
  }

  public LocalDate getWeekCommencingStartDate() {
    return weekCommencingStartDate;
  }

  public static Builder updateHearingForListing() {
    return new uk.gov.justice.listing.commands.UpdateHearingForListing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.commands.UpdateHearingForListing that = (uk.gov.justice.listing.commands.UpdateHearingForListing) obj;

    return java.util.Objects.equals(this.bookingType, that.bookingType) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.hasVideoLink, that.hasVideoLink) &&
    java.util.Objects.equals(this.hearingDays, that.hearingDays) &&
    java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.isJudiciaryFromCourtSchedule, that.isJudiciaryFromCourtSchedule) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.judiciaryAssignmentSource, that.judiciaryAssignmentSource) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.nonSittingDays, that.nonSittingDays) &&
    java.util.Objects.equals(this.panel, that.panel) &&
    java.util.Objects.equals(this.priority, that.priority) &&
    java.util.Objects.equals(this.publicListNote, that.publicListNote) &&
    java.util.Objects.equals(this.selectedCourtCentre, that.selectedCourtCentre) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.source, that.source) &&
    java.util.Objects.equals(this.specialRequirements, that.specialRequirements) &&
    java.util.Objects.equals(this.splitHearing, that.splitHearing) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.startDateAndWeekCommencingOptional, that.startDateAndWeekCommencingOptional) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.weekCommencingDurationInWeeks, that.weekCommencingDurationInWeeks) &&
    java.util.Objects.equals(this.weekCommencingEndDate, that.weekCommencingEndDate) &&
    java.util.Objects.equals(this.weekCommencingStartDate, that.weekCommencingStartDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookingType, courtCentreId, courtRoomId, endDate, hasVideoLink, hearingDays, hearingId, hearingLanguage, isJudiciaryFromCourtSchedule, judiciary, judiciaryAssignmentSource, jurisdictionType, nonDefaultDays, nonSittingDays, panel, priority, publicListNote, selectedCourtCentre, sendNotificationToParties, source, specialRequirements, splitHearing, startDate, startDateAndWeekCommencingOptional, type, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);}

  public static class Builder {
    private String bookingType;

    private UUID courtCentreId;

    private UUID courtRoomId;

    private LocalDate endDate;

    private Boolean hasVideoLink;

    private List<HearingDay> hearingDays;

    private UUID hearingId;

    private HearingLanguage hearingLanguage;

    private Boolean isJudiciaryFromCourtSchedule;

    private List<JudicialRole> judiciary;

    private JudiciaryAssignmentSource judiciaryAssignmentSource;

    private JurisdictionType jurisdictionType;

    private List<NonDefaultDay> nonDefaultDays;

    private List<LocalDate> nonSittingDays;

    private String panel;

    private String priority;

    private String publicListNote;

    private SelectedCourtCentre selectedCourtCentre;

    private Boolean sendNotificationToParties;

    private String source;

    private List<String> specialRequirements;

    private String splitHearing;

    private LocalDate startDate;

    private Boolean startDateAndWeekCommencingOptional;

    private HearingType type;

    private Integer weekCommencingDurationInWeeks;

    private LocalDate weekCommencingEndDate;

    private LocalDate weekCommencingStartDate;

    public Builder withBookingType(final String bookingType) {
      this.bookingType = bookingType;
      return this;
    }

    public Builder withCourtCentreId(final UUID courtCentreId) {
      this.courtCentreId = courtCentreId;
      return this;
    }

    public Builder withCourtRoomId(final UUID courtRoomId) {
      this.courtRoomId = courtRoomId;
      return this;
    }

    public Builder withEndDate(final LocalDate endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder withHasVideoLink(final Boolean hasVideoLink) {
      this.hasVideoLink = hasVideoLink;
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

    public Builder withHearingLanguage(final HearingLanguage hearingLanguage) {
      this.hearingLanguage = hearingLanguage;
      return this;
    }

    public Builder withIsJudiciaryFromCourtSchedule(final Boolean isJudiciaryFromCourtSchedule) {
      this.isJudiciaryFromCourtSchedule = isJudiciaryFromCourtSchedule;
      return this;
    }

    public Builder withJudiciary(final List<JudicialRole> judiciary) {
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

    public Builder withNonDefaultDays(final List<NonDefaultDay> nonDefaultDays) {
      this.nonDefaultDays = nonDefaultDays;
      return this;
    }

    public Builder withNonSittingDays(final List<LocalDate> nonSittingDays) {
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

    public Builder withStartDate(final LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder withStartDateAndWeekCommencingOptional(final Boolean startDateAndWeekCommencingOptional) {
      this.startDateAndWeekCommencingOptional = startDateAndWeekCommencingOptional;
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

    public Builder withWeekCommencingEndDate(final LocalDate weekCommencingEndDate) {
      this.weekCommencingEndDate = weekCommencingEndDate;
      return this;
    }

    public Builder withWeekCommencingStartDate(final LocalDate weekCommencingStartDate) {
      this.weekCommencingStartDate = weekCommencingStartDate;
      return this;
    }

    public Builder withValuesFrom(final UpdateHearingForListing updateHearingForListing) {
      this.bookingType = updateHearingForListing.getBookingType();
      this.courtCentreId = updateHearingForListing.getCourtCentreId();
      this.courtRoomId = updateHearingForListing.getCourtRoomId();
      this.endDate = updateHearingForListing.getEndDate();
      this.hasVideoLink = updateHearingForListing.getHasVideoLink();
      this.hearingDays = updateHearingForListing.getHearingDays();
      this.hearingId = updateHearingForListing.getHearingId();
      this.hearingLanguage = updateHearingForListing.getHearingLanguage();
      this.isJudiciaryFromCourtSchedule = updateHearingForListing.getIsJudiciaryFromCourtSchedule();
      this.judiciary = updateHearingForListing.getJudiciary();
      this.judiciaryAssignmentSource = updateHearingForListing.getJudiciaryAssignmentSource();
      this.jurisdictionType = updateHearingForListing.getJurisdictionType();
      this.nonDefaultDays = updateHearingForListing.getNonDefaultDays();
      this.nonSittingDays = updateHearingForListing.getNonSittingDays();
      this.panel = updateHearingForListing.getPanel();
      this.priority = updateHearingForListing.getPriority();
      this.publicListNote = updateHearingForListing.getPublicListNote();
      this.selectedCourtCentre = updateHearingForListing.getSelectedCourtCentre();
      this.sendNotificationToParties = updateHearingForListing.getSendNotificationToParties();
      this.source = updateHearingForListing.getSource();
      this.specialRequirements = updateHearingForListing.getSpecialRequirements();
      this.splitHearing = updateHearingForListing.getSplitHearing();
      this.startDate = updateHearingForListing.getStartDate();
      this.startDateAndWeekCommencingOptional = updateHearingForListing.getStartDateAndWeekCommencingOptional();
      this.type = updateHearingForListing.getType();
      this.weekCommencingDurationInWeeks = updateHearingForListing.getWeekCommencingDurationInWeeks();
      this.weekCommencingEndDate = updateHearingForListing.getWeekCommencingEndDate();
      this.weekCommencingStartDate = updateHearingForListing.getWeekCommencingStartDate();
      return this;
    }

    public UpdateHearingForListing build() {
      return new uk.gov.justice.listing.commands.UpdateHearingForListing(bookingType, courtCentreId, courtRoomId, endDate, hasVideoLink, hearingDays, hearingId, hearingLanguage, isJudiciaryFromCourtSchedule, judiciary, judiciaryAssignmentSource, jurisdictionType, nonDefaultDays, nonSittingDays, panel, priority, publicListNote, selectedCourtCentre, sendNotificationToParties, source, specialRequirements, splitHearing, startDate, startDateAndWeekCommencingOptional, type, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);
    }
  }
}
