package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class ListedCase implements Serializable {
  private static final long serialVersionUID = 641333116650746455L;

  private final CaseIdentifier caseIdentifier;

  private final String caseStatus;

  private final List<Defendant> defendants;

  private final UUID groupId;

  private final UUID id;

  private final Boolean isCivil;

  private final Boolean isEjected;

  private final Boolean isGroupMaster;

  private final Boolean isGroupMember;

  private final List<LinkedCases> linkedCases;

  private final List<Marker> markers;

  private final Prosecutor prosecutor;

  private final Boolean restrictFromCourtList;

  private final Boolean shadowListed;

  private final String trialReceiptType;

  @JsonCreator
  public ListedCase(final CaseIdentifier caseIdentifier, final String caseStatus, final List<Defendant> defendants, final UUID groupId, final UUID id, final Boolean isCivil, final Boolean isEjected, final Boolean isGroupMaster, final Boolean isGroupMember, final List<LinkedCases> linkedCases, final List<Marker> markers, final Prosecutor prosecutor, final Boolean restrictFromCourtList, final Boolean shadowListed, final String trialReceiptType) {
    this.caseIdentifier = caseIdentifier;
    this.caseStatus = caseStatus;
    this.defendants = defendants;
    this.groupId = groupId;
    this.id = id;
    this.isCivil = isCivil;
    this.isEjected = isEjected;
    this.isGroupMaster = isGroupMaster;
    this.isGroupMember = isGroupMember;
    this.linkedCases = linkedCases;
    this.markers = markers;
    this.prosecutor = prosecutor;
    this.restrictFromCourtList = restrictFromCourtList;
    this.shadowListed = shadowListed;
    this.trialReceiptType = trialReceiptType;
  }

  public CaseIdentifier getCaseIdentifier() {
    return caseIdentifier;
  }

  public String getCaseStatus() {
    return caseStatus;
  }

  public List<Defendant> getDefendants() {
    return defendants;
  }

  public UUID getGroupId() {
    return groupId;
  }

  public UUID getId() {
    return id;
  }

  public Boolean getIsCivil() {
    return isCivil;
  }

  public Boolean getIsEjected() {
    return isEjected;
  }

  public Boolean getIsGroupMaster() {
    return isGroupMaster;
  }

  public Boolean getIsGroupMember() {
    return isGroupMember;
  }

  public List<LinkedCases> getLinkedCases() {
    return linkedCases;
  }

  public List<Marker> getMarkers() {
    return markers;
  }

  public Prosecutor getProsecutor() {
    return prosecutor;
  }

  public Boolean getRestrictFromCourtList() {
    return restrictFromCourtList;
  }

  public Boolean getShadowListed() {
    return shadowListed;
  }

  public String getTrialReceiptType() {
    return trialReceiptType;
  }

  public static Builder listedCase() {
    return new uk.gov.justice.listing.events.ListedCase.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.ListedCase that = (uk.gov.justice.listing.events.ListedCase) obj;

    return java.util.Objects.equals(this.caseIdentifier, that.caseIdentifier) &&
    java.util.Objects.equals(this.caseStatus, that.caseStatus) &&
    java.util.Objects.equals(this.defendants, that.defendants) &&
    java.util.Objects.equals(this.groupId, that.groupId) &&
    java.util.Objects.equals(this.id, that.id) &&
    java.util.Objects.equals(this.isCivil, that.isCivil) &&
    java.util.Objects.equals(this.isEjected, that.isEjected) &&
    java.util.Objects.equals(this.isGroupMaster, that.isGroupMaster) &&
    java.util.Objects.equals(this.isGroupMember, that.isGroupMember) &&
    java.util.Objects.equals(this.linkedCases, that.linkedCases) &&
    java.util.Objects.equals(this.markers, that.markers) &&
    java.util.Objects.equals(this.prosecutor, that.prosecutor) &&
    java.util.Objects.equals(this.restrictFromCourtList, that.restrictFromCourtList) &&
    java.util.Objects.equals(this.shadowListed, that.shadowListed) &&
    java.util.Objects.equals(this.trialReceiptType, that.trialReceiptType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(caseIdentifier, caseStatus, defendants, groupId, id, isCivil, isEjected, isGroupMaster, isGroupMember, linkedCases, markers, prosecutor, restrictFromCourtList, shadowListed, trialReceiptType);}

  public static class Builder {
    private CaseIdentifier caseIdentifier;

    private String caseStatus;

    private List<Defendant> defendants;

    private UUID groupId;

    private UUID id;

    private Boolean isCivil;

    private Boolean isEjected;

    private Boolean isGroupMaster;

    private Boolean isGroupMember;

    private List<LinkedCases> linkedCases;

    private List<Marker> markers;

    private Prosecutor prosecutor;

    private Boolean restrictFromCourtList;

    private Boolean shadowListed;

    private String trialReceiptType;

    public Builder withCaseIdentifier(final CaseIdentifier caseIdentifier) {
      this.caseIdentifier = caseIdentifier;
      return this;
    }

    public Builder withCaseStatus(final String caseStatus) {
      this.caseStatus = caseStatus;
      return this;
    }

    public Builder withDefendants(final List<Defendant> defendants) {
      this.defendants = defendants;
      return this;
    }

    public Builder withGroupId(final UUID groupId) {
      this.groupId = groupId;
      return this;
    }

    public Builder withId(final UUID id) {
      this.id = id;
      return this;
    }

    public Builder withIsCivil(final Boolean isCivil) {
      this.isCivil = isCivil;
      return this;
    }

    public Builder withIsEjected(final Boolean isEjected) {
      this.isEjected = isEjected;
      return this;
    }

    public Builder withIsGroupMaster(final Boolean isGroupMaster) {
      this.isGroupMaster = isGroupMaster;
      return this;
    }

    public Builder withIsGroupMember(final Boolean isGroupMember) {
      this.isGroupMember = isGroupMember;
      return this;
    }

    public Builder withLinkedCases(final List<LinkedCases> linkedCases) {
      this.linkedCases = linkedCases;
      return this;
    }

    public Builder withMarkers(final List<Marker> markers) {
      this.markers = markers;
      return this;
    }

    public Builder withProsecutor(final Prosecutor prosecutor) {
      this.prosecutor = prosecutor;
      return this;
    }

    public Builder withRestrictFromCourtList(final Boolean restrictFromCourtList) {
      this.restrictFromCourtList = restrictFromCourtList;
      return this;
    }

    public Builder withShadowListed(final Boolean shadowListed) {
      this.shadowListed = shadowListed;
      return this;
    }

    public Builder withTrialReceiptType(final String trialReceiptType) {
      this.trialReceiptType = trialReceiptType;
      return this;
    }

    public Builder withValuesFrom(final ListedCase listedCase) {
      this.caseIdentifier = listedCase.getCaseIdentifier();
      this.caseStatus = listedCase.getCaseStatus();
      this.defendants = listedCase.getDefendants();
      this.groupId = listedCase.getGroupId();
      this.id = listedCase.getId();
      this.isCivil = listedCase.getIsCivil();
      this.isEjected = listedCase.getIsEjected();
      this.isGroupMaster = listedCase.getIsGroupMaster();
      this.isGroupMember = listedCase.getIsGroupMember();
      this.linkedCases = listedCase.getLinkedCases();
      this.markers = listedCase.getMarkers();
      this.prosecutor = listedCase.getProsecutor();
      this.restrictFromCourtList = listedCase.getRestrictFromCourtList();
      this.shadowListed = listedCase.getShadowListed();
      this.trialReceiptType = listedCase.getTrialReceiptType();
      return this;
    }

    public ListedCase build() {
      return new uk.gov.justice.listing.events.ListedCase(caseIdentifier, caseStatus, defendants, groupId, id, isCivil, isEjected, isGroupMaster, isGroupMember, linkedCases, markers, prosecutor, restrictFromCourtList, shadowListed, trialReceiptType);
    }
  }
}
