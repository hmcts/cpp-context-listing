package uk.gov.moj.cpp.hearing.json.schema.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;
import uk.gov.justice.core.courts.Address;

public class CpsProsecutorUpdated implements Serializable {
  private static final long serialVersionUID = -3130059973790005618L;

  private final Address address;

  private final String caseURN;

  private final List<UUID> hearingIds;

  private final String oldCpsProsecutor;

  private final String prosecutionAuthorityCode;

  private final UUID prosecutionAuthorityId;

  private final String prosecutionAuthorityName;

  private final String prosecutionAuthorityReference;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public CpsProsecutorUpdated(final Address address, final String caseURN, final List<UUID> hearingIds, final String oldCpsProsecutor, final String prosecutionAuthorityCode, final UUID prosecutionAuthorityId, final String prosecutionAuthorityName, final String prosecutionAuthorityReference, final UUID prosecutionCaseId) {
    this.address = address;
    this.caseURN = caseURN;
    this.hearingIds = hearingIds;
    this.oldCpsProsecutor = oldCpsProsecutor;
    this.prosecutionAuthorityCode = prosecutionAuthorityCode;
    this.prosecutionAuthorityId = prosecutionAuthorityId;
    this.prosecutionAuthorityName = prosecutionAuthorityName;
    this.prosecutionAuthorityReference = prosecutionAuthorityReference;
    this.prosecutionCaseId = prosecutionCaseId;
  }

  public Address getAddress() {
    return address;
  }

  public String getCaseURN() {
    return caseURN;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
  }

  public String getOldCpsProsecutor() {
    return oldCpsProsecutor;
  }

  public String getProsecutionAuthorityCode() {
    return prosecutionAuthorityCode;
  }

  public UUID getProsecutionAuthorityId() {
    return prosecutionAuthorityId;
  }

  public String getProsecutionAuthorityName() {
    return prosecutionAuthorityName;
  }

  public String getProsecutionAuthorityReference() {
    return prosecutionAuthorityReference;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public static Builder cpsProsecutorUpdated() {
    return new uk.gov.moj.cpp.hearing.json.schema.event.CpsProsecutorUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.moj.cpp.hearing.json.schema.event.CpsProsecutorUpdated that = (uk.gov.moj.cpp.hearing.json.schema.event.CpsProsecutorUpdated) obj;

    return java.util.Objects.equals(this.address, that.address) &&
    java.util.Objects.equals(this.caseURN, that.caseURN) &&
    java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.oldCpsProsecutor, that.oldCpsProsecutor) &&
    java.util.Objects.equals(this.prosecutionAuthorityCode, that.prosecutionAuthorityCode) &&
    java.util.Objects.equals(this.prosecutionAuthorityId, that.prosecutionAuthorityId) &&
    java.util.Objects.equals(this.prosecutionAuthorityName, that.prosecutionAuthorityName) &&
    java.util.Objects.equals(this.prosecutionAuthorityReference, that.prosecutionAuthorityReference) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(address, caseURN, hearingIds, oldCpsProsecutor, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionAuthorityName, prosecutionAuthorityReference, prosecutionCaseId);}

  public static class Builder {
    private Address address;

    private String caseURN;

    private List<UUID> hearingIds;

    private String oldCpsProsecutor;

    private String prosecutionAuthorityCode;

    private UUID prosecutionAuthorityId;

    private String prosecutionAuthorityName;

    private String prosecutionAuthorityReference;

    private UUID prosecutionCaseId;

    public Builder withAddress(final Address address) {
      this.address = address;
      return this;
    }

    public Builder withCaseURN(final String caseURN) {
      this.caseURN = caseURN;
      return this;
    }

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
      return this;
    }

    public Builder withOldCpsProsecutor(final String oldCpsProsecutor) {
      this.oldCpsProsecutor = oldCpsProsecutor;
      return this;
    }

    public Builder withProsecutionAuthorityCode(final String prosecutionAuthorityCode) {
      this.prosecutionAuthorityCode = prosecutionAuthorityCode;
      return this;
    }

    public Builder withProsecutionAuthorityId(final UUID prosecutionAuthorityId) {
      this.prosecutionAuthorityId = prosecutionAuthorityId;
      return this;
    }

    public Builder withProsecutionAuthorityName(final String prosecutionAuthorityName) {
      this.prosecutionAuthorityName = prosecutionAuthorityName;
      return this;
    }

    public Builder withProsecutionAuthorityReference(final String prosecutionAuthorityReference) {
      this.prosecutionAuthorityReference = prosecutionAuthorityReference;
      return this;
    }

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withValuesFrom(final CpsProsecutorUpdated cpsProsecutorUpdated) {
      this.address = cpsProsecutorUpdated.getAddress();
      this.caseURN = cpsProsecutorUpdated.getCaseURN();
      this.hearingIds = cpsProsecutorUpdated.getHearingIds();
      this.oldCpsProsecutor = cpsProsecutorUpdated.getOldCpsProsecutor();
      this.prosecutionAuthorityCode = cpsProsecutorUpdated.getProsecutionAuthorityCode();
      this.prosecutionAuthorityId = cpsProsecutorUpdated.getProsecutionAuthorityId();
      this.prosecutionAuthorityName = cpsProsecutorUpdated.getProsecutionAuthorityName();
      this.prosecutionAuthorityReference = cpsProsecutorUpdated.getProsecutionAuthorityReference();
      this.prosecutionCaseId = cpsProsecutorUpdated.getProsecutionCaseId();
      return this;
    }

    public CpsProsecutorUpdated build() {
      return new uk.gov.moj.cpp.hearing.json.schema.event.CpsProsecutorUpdated(address, caseURN, hearingIds, oldCpsProsecutor, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionAuthorityName, prosecutionAuthorityReference, prosecutionCaseId);
    }
  }
}
