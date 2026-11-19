package uk.gov.justice.core.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.time.ZonedDateTime;
import java.util.UUID;

public class CourtApplicationType implements Serializable {
  private static final long serialVersionUID = -7512644503274841725L;

  private final Boolean appealFlag;

  private final Boolean applicantAppellantFlag;

  private final String applicationSynonym;

  private final String applicationWording;

  private final String applicationWordingWelsh;

  private final String boxworkNotifTemplate;

  private final BreachType breachType;

  private final String categoryCode;

  private final String changeDate;

  private final String code;

  private final Boolean commrOfOathFlag;

  private final Boolean contestedFeeApplicable;

  private final Boolean courtExtractAvlFlag;

  private final Boolean courtOfAppealFlag;

  private final Boolean exParte;

  private final String hearingCode;

  private final UUID id;

  private final Boolean initialFeeApplicable;

  private final Jurisdiction jurisdiction;

  private final ZonedDateTime lastModified;

  private final String legislation;

  private final String legislationWelsh;

  private final LinkType linkType;

  private final String listingNotifTemplate;

  private final OffenceActiveOrder offenceActiveOrder;

  private final Boolean pleaApplicableFlag;

  private final String prefix;

  private final Boolean prosecutorThirdPartyFlag;

  private final Integer pssId;

  private final String resentencingActivationCode;

  private final String sowRef;

  private final Boolean spiOutApplicableFlag;

  private final SummonsTemplateType summonsTemplateType;

  private final String terminalEntry;

  private final String type;

  private final String typeWelsh;

  private final String validFrom;

  private final String validTo;

  @JsonCreator
  public CourtApplicationType(final Boolean appealFlag, final Boolean applicantAppellantFlag, final String applicationSynonym, final String applicationWording, final String applicationWordingWelsh, final String boxworkNotifTemplate, final BreachType breachType, final String categoryCode, final String changeDate, final String code, final Boolean commrOfOathFlag, final Boolean contestedFeeApplicable, final Boolean courtExtractAvlFlag, final Boolean courtOfAppealFlag, final Boolean exParte, final String hearingCode, final UUID id, final Boolean initialFeeApplicable, final Jurisdiction jurisdiction, final ZonedDateTime lastModified, final String legislation, final String legislationWelsh, final LinkType linkType, final String listingNotifTemplate, final OffenceActiveOrder offenceActiveOrder, final Boolean pleaApplicableFlag, final String prefix, final Boolean prosecutorThirdPartyFlag, final Integer pssId, final String resentencingActivationCode, final String sowRef, final Boolean spiOutApplicableFlag, final SummonsTemplateType summonsTemplateType, final String terminalEntry, final String type, final String typeWelsh, final String validFrom, final String validTo) {
    this.appealFlag = appealFlag;
    this.applicantAppellantFlag = applicantAppellantFlag;
    this.applicationSynonym = applicationSynonym;
    this.applicationWording = applicationWording;
    this.applicationWordingWelsh = applicationWordingWelsh;
    this.boxworkNotifTemplate = boxworkNotifTemplate;
    this.breachType = breachType;
    this.categoryCode = categoryCode;
    this.changeDate = changeDate;
    this.code = code;
    this.commrOfOathFlag = commrOfOathFlag;
    this.contestedFeeApplicable = contestedFeeApplicable;
    this.courtExtractAvlFlag = courtExtractAvlFlag;
    this.courtOfAppealFlag = courtOfAppealFlag;
    this.exParte = exParte;
    this.hearingCode = hearingCode;
    this.id = id;
    this.initialFeeApplicable = initialFeeApplicable;
    this.jurisdiction = jurisdiction;
    this.lastModified = lastModified;
    this.legislation = legislation;
    this.legislationWelsh = legislationWelsh;
    this.linkType = linkType;
    this.listingNotifTemplate = listingNotifTemplate;
    this.offenceActiveOrder = offenceActiveOrder;
    this.pleaApplicableFlag = pleaApplicableFlag;
    this.prefix = prefix;
    this.prosecutorThirdPartyFlag = prosecutorThirdPartyFlag;
    this.pssId = pssId;
    this.resentencingActivationCode = resentencingActivationCode;
    this.sowRef = sowRef;
    this.spiOutApplicableFlag = spiOutApplicableFlag;
    this.summonsTemplateType = summonsTemplateType;
    this.terminalEntry = terminalEntry;
    this.type = type;
    this.typeWelsh = typeWelsh;
    this.validFrom = validFrom;
    this.validTo = validTo;
  }

  public Boolean getAppealFlag() {
    return appealFlag;
  }

  public Boolean getApplicantAppellantFlag() {
    return applicantAppellantFlag;
  }

  public String getApplicationSynonym() {
    return applicationSynonym;
  }

  public String getApplicationWording() {
    return applicationWording;
  }

  public String getApplicationWordingWelsh() {
    return applicationWordingWelsh;
  }

  public String getBoxworkNotifTemplate() {
    return boxworkNotifTemplate;
  }

  public BreachType getBreachType() {
    return breachType;
  }

  public String getCategoryCode() {
    return categoryCode;
  }

  public String getChangeDate() {
    return changeDate;
  }

  public String getCode() {
    return code;
  }

  public Boolean getCommrOfOathFlag() {
    return commrOfOathFlag;
  }

  public Boolean getContestedFeeApplicable() {
    return contestedFeeApplicable;
  }

  public Boolean getCourtExtractAvlFlag() {
    return courtExtractAvlFlag;
  }

  public Boolean getCourtOfAppealFlag() {
    return courtOfAppealFlag;
  }

  public Boolean getExParte() {
    return exParte;
  }

  public String getHearingCode() {
    return hearingCode;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getInitialFeeApplicable() {
    return initialFeeApplicable;
  }

  public Jurisdiction getJurisdiction() {
    return jurisdiction;
  }

  public ZonedDateTime getLastModified() {
    return lastModified;
  }

  public String getLegislation() {
    return legislation;
  }

  public String getLegislationWelsh() {
    return legislationWelsh;
  }

  public LinkType getLinkType() {
    return linkType;
  }

  public String getListingNotifTemplate() {
    return listingNotifTemplate;
  }

  public OffenceActiveOrder getOffenceActiveOrder() {
    return offenceActiveOrder;
  }

  public Boolean getPleaApplicableFlag() {
    return pleaApplicableFlag;
  }

  public String getPrefix() {
    return prefix;
  }

  public Boolean getProsecutorThirdPartyFlag() {
    return prosecutorThirdPartyFlag;
  }

  public Integer getPssId() {
    return pssId;
  }

  public String getResentencingActivationCode() {
    return resentencingActivationCode;
  }

  public String getSowRef() {
    return sowRef;
  }

  public Boolean getSpiOutApplicableFlag() {
    return spiOutApplicableFlag;
  }

  public SummonsTemplateType getSummonsTemplateType() {
    return summonsTemplateType;
  }

  public String getTerminalEntry() {
    return terminalEntry;
  }

  public String getType() {
    return type;
  }

  public String getTypeWelsh() {
    return typeWelsh;
  }

  public String getValidFrom() {
    return validFrom;
  }

  public String getValidTo() {
    return validTo;
  }

  public static Builder courtApplicationType() {
    return new uk.gov.justice.core.courts.CourtApplicationType.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.core.courts.CourtApplicationType that = (uk.gov.justice.core.courts.CourtApplicationType) obj;

    return java.util.Objects.equals(this.appealFlag, that.appealFlag) &&
    java.util.Objects.equals(this.applicantAppellantFlag, that.applicantAppellantFlag) &&
    java.util.Objects.equals(this.applicationSynonym, that.applicationSynonym) &&
    java.util.Objects.equals(this.applicationWording, that.applicationWording) &&
    java.util.Objects.equals(this.applicationWordingWelsh, that.applicationWordingWelsh) &&
    java.util.Objects.equals(this.boxworkNotifTemplate, that.boxworkNotifTemplate) &&
    java.util.Objects.equals(this.breachType, that.breachType) &&
    java.util.Objects.equals(this.categoryCode, that.categoryCode) &&
    java.util.Objects.equals(this.changeDate, that.changeDate) &&
    java.util.Objects.equals(this.code, that.code) &&
    java.util.Objects.equals(this.commrOfOathFlag, that.commrOfOathFlag) &&
    java.util.Objects.equals(this.contestedFeeApplicable, that.contestedFeeApplicable) &&
    java.util.Objects.equals(this.courtExtractAvlFlag, that.courtExtractAvlFlag) &&
    java.util.Objects.equals(this.courtOfAppealFlag, that.courtOfAppealFlag) &&
    java.util.Objects.equals(this.exParte, that.exParte) &&
    java.util.Objects.equals(this.hearingCode, that.hearingCode) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.initialFeeApplicable, that.initialFeeApplicable) &&
    java.util.Objects.equals(this.jurisdiction, that.jurisdiction) &&
    java.util.Objects.equals(this.lastModified, that.lastModified) &&
    java.util.Objects.equals(this.legislation, that.legislation) &&
    java.util.Objects.equals(this.legislationWelsh, that.legislationWelsh) &&
    java.util.Objects.equals(this.linkType, that.linkType) &&
    java.util.Objects.equals(this.listingNotifTemplate, that.listingNotifTemplate) &&
    java.util.Objects.equals(this.offenceActiveOrder, that.offenceActiveOrder) &&
    java.util.Objects.equals(this.pleaApplicableFlag, that.pleaApplicableFlag) &&
    java.util.Objects.equals(this.prefix, that.prefix) &&
    java.util.Objects.equals(this.prosecutorThirdPartyFlag, that.prosecutorThirdPartyFlag) &&
    java.util.Objects.equals(this.pssId, that.pssId) &&
    java.util.Objects.equals(this.resentencingActivationCode, that.resentencingActivationCode) &&
    java.util.Objects.equals(this.sowRef, that.sowRef) &&
    java.util.Objects.equals(this.spiOutApplicableFlag, that.spiOutApplicableFlag) &&
    java.util.Objects.equals(this.summonsTemplateType, that.summonsTemplateType) &&
    java.util.Objects.equals(this.terminalEntry, that.terminalEntry) &&
    java.util.Objects.equals(this.type, that.type) &&
    java.util.Objects.equals(this.typeWelsh, that.typeWelsh) &&
    java.util.Objects.equals(this.validFrom, that.validFrom) &&
    java.util.Objects.equals(this.validTo, that.validTo);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(appealFlag, applicantAppellantFlag, applicationSynonym, applicationWording, applicationWordingWelsh, boxworkNotifTemplate, breachType, categoryCode, changeDate, code, commrOfOathFlag, contestedFeeApplicable, courtExtractAvlFlag, courtOfAppealFlag, exParte, hearingCode, id, initialFeeApplicable, jurisdiction, lastModified, legislation, legislationWelsh, linkType, listingNotifTemplate, offenceActiveOrder, pleaApplicableFlag, prefix, prosecutorThirdPartyFlag, pssId, resentencingActivationCode, sowRef, spiOutApplicableFlag, summonsTemplateType, terminalEntry, type, typeWelsh, validFrom, validTo);}

  public static class Builder {
    private Boolean appealFlag;

    private Boolean applicantAppellantFlag;

    private String applicationSynonym;

    private String applicationWording;

    private String applicationWordingWelsh;

    private String boxworkNotifTemplate;

    private BreachType breachType;

    private String categoryCode;

    private String changeDate;

    private String code;

    private Boolean commrOfOathFlag;

    private Boolean contestedFeeApplicable;

    private Boolean courtExtractAvlFlag;

    private Boolean courtOfAppealFlag;

    private Boolean exParte;

    private String hearingCode;

    private UUID id;

    private Boolean initialFeeApplicable;

    private Jurisdiction jurisdiction;

    private ZonedDateTime lastModified;

    private String legislation;

    private String legislationWelsh;

    private LinkType linkType;

    private String listingNotifTemplate;

    private OffenceActiveOrder offenceActiveOrder;

    private Boolean pleaApplicableFlag;

    private String prefix;

    private Boolean prosecutorThirdPartyFlag;

    private Integer pssId;

    private String resentencingActivationCode;

    private String sowRef;

    private Boolean spiOutApplicableFlag;

    private SummonsTemplateType summonsTemplateType;

    private String terminalEntry;

    private String type;

    private String typeWelsh;

    private String validFrom;

    private String validTo;

    public Builder withAppealFlag(final Boolean appealFlag) {
      this.appealFlag = appealFlag;
      return this;
    }

    public Builder withApplicantAppellantFlag(final Boolean applicantAppellantFlag) {
      this.applicantAppellantFlag = applicantAppellantFlag;
      return this;
    }

    public Builder withApplicationSynonym(final String applicationSynonym) {
      this.applicationSynonym = applicationSynonym;
      return this;
    }

    public Builder withApplicationWording(final String applicationWording) {
      this.applicationWording = applicationWording;
      return this;
    }

    public Builder withApplicationWordingWelsh(final String applicationWordingWelsh) {
      this.applicationWordingWelsh = applicationWordingWelsh;
      return this;
    }

    public Builder withBoxworkNotifTemplate(final String boxworkNotifTemplate) {
      this.boxworkNotifTemplate = boxworkNotifTemplate;
      return this;
    }

    public Builder withBreachType(final BreachType breachType) {
      this.breachType = breachType;
      return this;
    }

    public Builder withCategoryCode(final String categoryCode) {
      this.categoryCode = categoryCode;
      return this;
    }

    public Builder withChangeDate(final String changeDate) {
      this.changeDate = changeDate;
      return this;
    }

    public Builder withCode(final String code) {
      this.code = code;
      return this;
    }

    public Builder withCommrOfOathFlag(final Boolean commrOfOathFlag) {
      this.commrOfOathFlag = commrOfOathFlag;
      return this;
    }

    public Builder withContestedFeeApplicable(final Boolean contestedFeeApplicable) {
      this.contestedFeeApplicable = contestedFeeApplicable;
      return this;
    }

    public Builder withCourtExtractAvlFlag(final Boolean courtExtractAvlFlag) {
      this.courtExtractAvlFlag = courtExtractAvlFlag;
      return this;
    }

    public Builder withCourtOfAppealFlag(final Boolean courtOfAppealFlag) {
      this.courtOfAppealFlag = courtOfAppealFlag;
      return this;
    }

    public Builder withExParte(final Boolean exParte) {
      this.exParte = exParte;
      return this;
    }

    public Builder withHearingCode(final String hearingCode) {
      this.hearingCode = hearingCode;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withInitialFeeApplicable(final Boolean initialFeeApplicable) {
      this.initialFeeApplicable = initialFeeApplicable;
      return this;
    }

    public Builder withJurisdiction(final Jurisdiction jurisdiction) {
      this.jurisdiction = jurisdiction;
      return this;
    }

    public Builder withLastModified(final ZonedDateTime lastModified) {
      this.lastModified = lastModified;
      return this;
    }

    public Builder withLegislation(final String legislation) {
      this.legislation = legislation;
      return this;
    }

    public Builder withLegislationWelsh(final String legislationWelsh) {
      this.legislationWelsh = legislationWelsh;
      return this;
    }

    public Builder withLinkType(final LinkType linkType) {
      this.linkType = linkType;
      return this;
    }

    public Builder withListingNotifTemplate(final String listingNotifTemplate) {
      this.listingNotifTemplate = listingNotifTemplate;
      return this;
    }

    public Builder withOffenceActiveOrder(final OffenceActiveOrder offenceActiveOrder) {
      this.offenceActiveOrder = offenceActiveOrder;
      return this;
    }

    public Builder withPleaApplicableFlag(final Boolean pleaApplicableFlag) {
      this.pleaApplicableFlag = pleaApplicableFlag;
      return this;
    }

    public Builder withPrefix(final String prefix) {
      this.prefix = prefix;
      return this;
    }

    public Builder withProsecutorThirdPartyFlag(final Boolean prosecutorThirdPartyFlag) {
      this.prosecutorThirdPartyFlag = prosecutorThirdPartyFlag;
      return this;
    }

    public Builder withPssId(final Integer pssId) {
      this.pssId = pssId;
      return this;
    }

    public Builder withResentencingActivationCode(final String resentencingActivationCode) {
      this.resentencingActivationCode = resentencingActivationCode;
      return this;
    }

    public Builder withSowRef(final String sowRef) {
      this.sowRef = sowRef;
      return this;
    }

    public Builder withSpiOutApplicableFlag(final Boolean spiOutApplicableFlag) {
      this.spiOutApplicableFlag = spiOutApplicableFlag;
      return this;
    }

    public Builder withSummonsTemplateType(final SummonsTemplateType summonsTemplateType) {
      this.summonsTemplateType = summonsTemplateType;
      return this;
    }

    public Builder withTerminalEntry(final String terminalEntry) {
      this.terminalEntry = terminalEntry;
      return this;
    }

    public Builder withType(final String type) {
      this.type = type;
      return this;
    }

    public Builder withTypeWelsh(final String typeWelsh) {
      this.typeWelsh = typeWelsh;
      return this;
    }

    public Builder withValidFrom(final String validFrom) {
      this.validFrom = validFrom;
      return this;
    }

    public Builder withValidTo(final String validTo) {
      this.validTo = validTo;
      return this;
    }

    public Builder withValuesFrom(final CourtApplicationType courtApplicationType) {
      this.appealFlag = courtApplicationType.getAppealFlag();
      this.applicantAppellantFlag = courtApplicationType.getApplicantAppellantFlag();
      this.applicationSynonym = courtApplicationType.getApplicationSynonym();
      this.applicationWording = courtApplicationType.getApplicationWording();
      this.applicationWordingWelsh = courtApplicationType.getApplicationWordingWelsh();
      this.boxworkNotifTemplate = courtApplicationType.getBoxworkNotifTemplate();
      this.breachType = courtApplicationType.getBreachType();
      this.categoryCode = courtApplicationType.getCategoryCode();
      this.changeDate = courtApplicationType.getChangeDate();
      this.code = courtApplicationType.getCode();
      this.commrOfOathFlag = courtApplicationType.getCommrOfOathFlag();
      this.contestedFeeApplicable = courtApplicationType.getContestedFeeApplicable();
      this.courtExtractAvlFlag = courtApplicationType.getCourtExtractAvlFlag();
      this.courtOfAppealFlag = courtApplicationType.getCourtOfAppealFlag();
      this.exParte = courtApplicationType.getExParte();
      this.hearingCode = courtApplicationType.getHearingCode();
      this.id = courtApplicationType.getId();
      this.initialFeeApplicable = courtApplicationType.getInitialFeeApplicable();
      this.jurisdiction = courtApplicationType.getJurisdiction();
      this.lastModified = courtApplicationType.getLastModified();
      this.legislation = courtApplicationType.getLegislation();
      this.legislationWelsh = courtApplicationType.getLegislationWelsh();
      this.linkType = courtApplicationType.getLinkType();
      this.listingNotifTemplate = courtApplicationType.getListingNotifTemplate();
      this.offenceActiveOrder = courtApplicationType.getOffenceActiveOrder();
      this.pleaApplicableFlag = courtApplicationType.getPleaApplicableFlag();
      this.prefix = courtApplicationType.getPrefix();
      this.prosecutorThirdPartyFlag = courtApplicationType.getProsecutorThirdPartyFlag();
      this.pssId = courtApplicationType.getPssId();
      this.resentencingActivationCode = courtApplicationType.getResentencingActivationCode();
      this.sowRef = courtApplicationType.getSowRef();
      this.spiOutApplicableFlag = courtApplicationType.getSpiOutApplicableFlag();
      this.summonsTemplateType = courtApplicationType.getSummonsTemplateType();
      this.terminalEntry = courtApplicationType.getTerminalEntry();
      this.type = courtApplicationType.getType();
      this.typeWelsh = courtApplicationType.getTypeWelsh();
      this.validFrom = courtApplicationType.getValidFrom();
      this.validTo = courtApplicationType.getValidTo();
      return this;
    }

    public CourtApplicationType build() {
      return new uk.gov.justice.core.courts.CourtApplicationType(appealFlag, applicantAppellantFlag, applicationSynonym, applicationWording, applicationWordingWelsh, boxworkNotifTemplate, breachType, categoryCode, changeDate, code, commrOfOathFlag, contestedFeeApplicable, courtExtractAvlFlag, courtOfAppealFlag, exParte, hearingCode, id, initialFeeApplicable, jurisdiction, lastModified, legislation, legislationWelsh, linkType, listingNotifTemplate, offenceActiveOrder, pleaApplicableFlag, prefix, prosecutorThirdPartyFlag, pssId, resentencingActivationCode, sowRef, spiOutApplicableFlag, summonsTemplateType, terminalEntry, type, typeWelsh, validFrom, validTo);
    }
  }
}
