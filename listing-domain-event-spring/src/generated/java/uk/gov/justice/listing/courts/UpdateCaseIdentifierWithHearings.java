package uk.gov.justice.listing.courts;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.List;
import java.util.UUID;

public class UpdateCaseIdentifierWithHearings implements Serializable {
  private static final long serialVersionUID = 5643723163813076591L;

  private final List<UUID> hearingIds;

  private final String prosecutionAuthorityCode;

  private final UUID prosecutionAuthorityId;

  private final UUID prosecutionCaseId;

  @JsonCreator
  public UpdateCaseIdentifierWithHearings(final List<UUID> hearingIds, final String prosecutionAuthorityCode, final UUID prosecutionAuthorityId, final UUID prosecutionCaseId) {
    this.hearingIds = hearingIds;
    this.prosecutionAuthorityCode = prosecutionAuthorityCode;
    this.prosecutionAuthorityId = prosecutionAuthorityId;
    this.prosecutionCaseId = prosecutionCaseId;
  }

  public List<UUID> getHearingIds() {
    return hearingIds;
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

  public static Builder updateCaseIdentifierWithHearings() {
    return new uk.gov.justice.listing.courts.UpdateCaseIdentifierWithHearings.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.UpdateCaseIdentifierWithHearings that = (uk.gov.justice.listing.courts.UpdateCaseIdentifierWithHearings) obj;

    return java.util.Objects.equals(this.hearingIds, that.hearingIds) &&
    java.util.Objects.equals(this.prosecutionAuthorityCode, that.prosecutionAuthorityCode) &&
    java.util.Objects.equals(this.prosecutionAuthorityId, that.prosecutionAuthorityId) &&
    java.util.Objects.equals(this.prosecutionCaseId, that.prosecutionCaseId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(hearingIds, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionCaseId);}

  public static class Builder {
    private List<UUID> hearingIds;

    private String prosecutionAuthorityCode;

    private UUID prosecutionAuthorityId;

    private UUID prosecutionCaseId;

    public Builder withHearingIds(final List<UUID> hearingIds) {
      this.hearingIds = hearingIds;
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

    public Builder withValuesFrom(final UpdateCaseIdentifierWithHearings updateCaseIdentifierWithHearings) {
      this.hearingIds = updateCaseIdentifierWithHearings.getHearingIds();
      this.prosecutionAuthorityCode = updateCaseIdentifierWithHearings.getProsecutionAuthorityCode();
      this.prosecutionAuthorityId = updateCaseIdentifierWithHearings.getProsecutionAuthorityId();
      this.prosecutionCaseId = updateCaseIdentifierWithHearings.getProsecutionCaseId();
      return this;
    }

    public UpdateCaseIdentifierWithHearings build() {
      return new uk.gov.justice.listing.courts.UpdateCaseIdentifierWithHearings(hearingIds, prosecutionAuthorityCode, prosecutionAuthorityId, prosecutionCaseId);
    }
  }
}
