package uk.gov.justice.listing.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;
import uk.gov.justice.domain.annotation.Event;

@Event("listing.events.case-identifier-updated")
public class CaseIdentifierUpdated implements Serializable {
  private static final long serialVersionUID = -6020213560380514824L;

  private final UUID hearingId;

  private final String prosecutionAuthorityCode;

  private final UUID prosecutionAuthorityId;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public CaseIdentifierUpdated(final UUID hearingId, final String prosecutionAuthorityCode, final UUID prosecutionAuthorityId, final UUID prosecutionCaseId) {
    this.hearingId = hearingId;
    this.prosecutionAuthorityCode = prosecutionAuthorityCode;
    this.prosecutionAuthorityId = prosecutionAuthorityId;
    this.prosecutionCaseId = prosecutionCaseId;
  }

  public UUID getHearingId() {
    return hearingId;
  }

  public String getProsecutionAuthorityCode() {
    return prosecutionAuthorityCode;
  }

  public UUID getProsecutionAuthorityId() {
    return prosecutionAuthorityId;
  }

  public UUID getProsecutionCaseId() {
    return prosecutionCaseId;
  }

  public static Builder caseIdentifierUpdated() {
    return new uk.gov.justice.listing.events.CaseIdentifierUpdated.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.events.CaseIdentifierUpdated that = (uk.gov.justice.listing.events.CaseIdentifierUpdated) obj;

    return java.util.Objects.equals(this.hearingId, that.hearingId) &&
    java.util.Objects.equals(this.prosecutionAuthorityCode, that.prosecutionAuthorityCode) &&
    java.util.Objects.equals(this.prosecutionAuthorityId, that.prosecutionAuthorityId) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingId, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionCaseId);}

  public static class Builder {
    private UUID hearingId;

    private String prosecutionAuthorityCode;

    private UUID prosecutionAuthorityId;

    private UUID prosecutionCaseId;

    public Builder withHearingId(final UUID hearingId) {
      this.hearingId = hearingId;
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

    public Builder withProsecutionCaseId(final UUID prosecutionCaseId) {
      this.prosecutionCaseId = prosecutionCaseId;
      return this;
    }

    public Builder withValuesFrom(final CaseIdentifierUpdated caseIdentifierUpdated) {
      this.hearingId = caseIdentifierUpdated.getHearingId();
      this.prosecutionAuthorityCode = caseIdentifierUpdated.getProsecutionAuthorityCode();
      this.prosecutionAuthorityId = caseIdentifierUpdated.getProsecutionAuthorityId();
      this.prosecutionCaseId = caseIdentifierUpdated.getProsecutionCaseId();
      return this;
    }

    public CaseIdentifierUpdated build() {
      return new uk.gov.justice.listing.events.CaseIdentifierUpdated(hearingId, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionCaseId);
    }
  }
}
