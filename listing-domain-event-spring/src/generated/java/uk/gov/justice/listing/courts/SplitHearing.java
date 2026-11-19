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

public class SplitHearing implements Serializable {
  private static final long serialVersionUID = -5013253869262251816L;

  private final String bookingType;

  private final UUID courtCentreId;

  private final CourtRoomId courtRoomId;

  private final EndDate endDate;

  private final Boolean hasVideoLink;

  private final HearingLanguage hearingLanguage;

  private final List<Judiciary> judiciary;

  private final JurisdictionType jurisdictionType;

  private final List<NonDefaultDays> nonDefaultDays;

  private final List<String> nonSittingDays;

  private final String panel;

  private final String priority;

  private final List<ProsecutionCases> prosecutionCases;

  private final String publicListNote;

  private final Boolean sendNotificationToParties;

  private final List<String> specialRequirements;

  private final String startDate;

  private final HearingType type;

  private final Integer weekCommencingDurationInWeeks;

  private final String weekCommencingEndDate;

  private final String weekCommencingStartDate;

  @JsonCreator
  public SplitHearing(final String bookingType, final UUID courtCentreId, final CourtRoomId courtRoomId, final EndDate endDate, final Boolean hasVideoLink, final HearingLanguage hearingLanguage, final List<Judiciary> judiciary, final JurisdictionType jurisdictionType, final List<NonDefaultDays> nonDefaultDays, final List<String> nonSittingDays, final String panel, final String priority, final List<ProsecutionCases> prosecutionCases, final String publicListNote, final Boolean sendNotificationToParties, final List<String> specialRequirements, final String startDate, final HearingType type, final Integer weekCommencingDurationInWeeks, final String weekCommencingEndDate, final String weekCommencingStartDate) {
    this.bookingType = bookingType;
    this.courtCentreId = courtCentreId;
    this.courtRoomId = courtRoomId;
    this.endDate = endDate;
    this.hasVideoLink = hasVideoLink;
    this.hearingLanguage = hearingLanguage;
    this.judiciary = judiciary;
    this.jurisdictionType = jurisdictionType;
    this.nonDefaultDays = nonDefaultDays;
    this.nonSittingDays = nonSittingDays;
    this.panel = panel;
    this.priority = priority;
    this.prosecutionCases = prosecutionCases;
    this.publicListNote = publicListNote;
    this.sendNotificationToParties = sendNotificationToParties;
    this.specialRequirements = specialRequirements;
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

  public Boolean getSendNotificationToParties() {
    return sendNotificationToParties;
  }

  public List<String> getSpecialRequirements() {
    return specialRequirements;
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

  public static Builder splitHearing() {
    return new uk.gov.justice.listing.courts.SplitHearing.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.SplitHearing that = (uk.gov.justice.listing.courts.SplitHearing) obj;

    return java.util.Objects.equals(this.bookingType, that.bookingType) &&
    java.util.Objects.equals(this.courtCentreId, that.courtCentreId) &&
    java.util.Objects.equals(this.courtRoomId, that.courtRoomId) &&
    java.util.Objects.equals(this.endDate, that.endDate) &&
    java.util.Objects.equals(this.hasVideoLink, that.hasVideoLink) &&
    java.util.Objects.equals(this.hearingLanguage, that.hearingLanguage) &&
    java.util.Objects.equals(this.judiciary, that.judiciary) &&
    java.util.Objects.equals(this.jurisdictionType, that.jurisdictionType) &&
    java.util.Objects.equals(this.nonDefaultDays, that.nonDefaultDays) &&
    java.util.Objects.equals(this.nonSittingDays, that.nonSittingDays) &&
    java.util.Objects.equals(this.panel, that.panel) &&
    java.util.Objects.equals(this.priority, that.priority) &&
    java.util.Objects.equals(this.prosecutionCases, that.prosecutionCases) &&
    java.util.Objects.equals(this.publicListNote, that.publicListNote) &&
    java.util.Objects.equals(this.sendNotificationToParties, that.sendNotificationToParties) &&
    java.util.Objects.equals(this.specialRequirements, that.specialRequirements) &&
    java.util.Objects.equals(this.startDate, that.startDate) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.weekCommencingDurationInWeeks, that.weekCommencingDurationInWeeks) &&
    java.util.Objects.equals(this.weekCommencingEndDate, that.weekCommencingEndDate) &&
    java.util.Objects.equals(this.weekCommencingStartDate, that.weekCommencingStartDate);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(bookingType, courtCentreId, courtRoomId, endDate, hasVideoLink, hearingLanguage, judiciary, jurisdictionType, nonDefaultDays, nonSittingDays, panel, priority, prosecutionCases, publicListNote, sendNotificationToParties, specialRequirements, startDate, type, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);}

  public static class Builder {
    private String bookingType;

    private UUID courtCentreId;

    private CourtRoomId courtRoomId;

    private EndDate endDate;

    private Boolean hasVideoLink;

    private HearingLanguage hearingLanguage;

    private List<Judiciary> judiciary;

    private JurisdictionType jurisdictionType;

    private List<NonDefaultDays> nonDefaultDays;

    private List<String> nonSittingDays;

    private String panel;

    private String priority;

    private List<ProsecutionCases> prosecutionCases;

    private String publicListNote;

    private Boolean sendNotificationToParties;

    private List<String> specialRequirements;

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

    public Builder withSendNotificationToParties(final Boolean sendNotificationToParties) {
      this.sendNotificationToParties = sendNotificationToParties;
      return this;
    }

    public Builder withSpecialRequirements(final List<String> specialRequirements) {
      this.specialRequirements = specialRequirements;
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

    public Builder withValuesFrom(final SplitHearing splitHearing) {
      this.bookingType = splitHearing.getBookingType();
      this.courtCentreId = splitHearing.getCourtCentreId();
      this.courtRoomId = splitHearing.getCourtRoomId();
      this.endDate = splitHearing.getEndDate();
      this.hasVideoLink = splitHearing.getHasVideoLink();
      this.hearingLanguage = splitHearing.getHearingLanguage();
      this.judiciary = splitHearing.getJudiciary();
      this.jurisdictionType = splitHearing.getJurisdictionType();
      this.nonDefaultDays = splitHearing.getNonDefaultDays();
      this.nonSittingDays = splitHearing.getNonSittingDays();
      this.panel = splitHearing.getPanel();
      this.priority = splitHearing.getPriority();
      this.prosecutionCases = splitHearing.getProsecutionCases();
      this.publicListNote = splitHearing.getPublicListNote();
      this.sendNotificationToParties = splitHearing.getSendNotificationToParties();
      this.specialRequirements = splitHearing.getSpecialRequirements();
      this.startDate = splitHearing.getStartDate();
      this.type = splitHearing.getType();
      this.weekCommencingDurationInWeeks = splitHearing.getWeekCommencingDurationInWeeks();
      this.weekCommencingEndDate = splitHearing.getWeekCommencingEndDate();
      this.weekCommencingStartDate = splitHearing.getWeekCommencingStartDate();
      return this;
    }

    public SplitHearing build() {
      return new uk.gov.justice.listing.courts.SplitHearing(bookingType, courtCentreId, courtRoomId, endDate, hasVideoLink, hearingLanguage, judiciary, jurisdictionType, nonDefaultDays, nonSittingDays, panel, priority, prosecutionCases, publicListNote, sendNotificationToParties, specialRequirements, startDate, type, weekCommencingDurationInWeeks, weekCommencingEndDate, weekCommencingStartDate);
    }
  }
}
