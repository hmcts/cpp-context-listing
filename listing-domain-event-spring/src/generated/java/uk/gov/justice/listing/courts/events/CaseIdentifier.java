package uk.gov.justice.listing.courts.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.io.Serializable;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.util.UUID;

public class CaseIdentifier implements Serializable {
  private static final long serialVersionUID = 3881700311939688169L;

  private final String authorityCode;

  private final UUID authorityId;

  private final String caseReference;

  @JsonCreator
  public CaseIdentifier(final String authorityCode, final UUID authorityId, final String caseReference) {
    this.authorityCode = authorityCode;
    this.authorityId = authorityId;
    this.caseReference = caseReference;
  }

  public String getAuthorityCode() {
    return authorityCode;
  }

  public UUID getAuthorityId() {
    return authorityId;
  }

  public String getCaseReference() {
    return caseReference;
  }

  public static Builder caseIdentifier() {
    return new uk.gov.justice.listing.courts.events.CaseIdentifier.Builder();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final uk.gov.justice.listing.courts.events.CaseIdentifier that = (uk.gov.justice.listing.courts.events.CaseIdentifier) obj;

    return java.util.Objects.equals(this.authorityCode, that.authorityCode) &&
    java.util.Objects.equals(this.authorityId, that.authorityId) &&
    java.util.Objects.equals(this.caseReference, that.caseReference);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(authorityCode, authorityId, caseReference);}

  public static class Builder {
    private String authorityCode;

    private UUID authorityId;

    private String caseReference;

    public Builder withAuthorityCode(final String authorityCode) {
      this.authorityCode = authorityCode;
      return this;
    }

    public Builder withAuthorityId(final UUID authorityId) {
      this.authorityId = authorityId;
      return this;
    }

    public Builder withCaseReference(final String caseReference) {
      this.caseReference = caseReference;
      return this;
    }

    public Builder withValuesFrom(final CaseIdentifier caseIdentifier) {
      this.authorityCode = caseIdentifier.getAuthorityCode();
      this.authorityId = caseIdentifier.getAuthorityId();
      this.caseReference = caseIdentifier.getCaseReference();
      return this;
    }

    public CaseIdentifier build() {
      return new uk.gov.justice.listing.courts.events.CaseIdentifier(authorityCode, authorityId, caseReference);
    }
  }
}
