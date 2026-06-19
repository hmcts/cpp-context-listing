package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@SuppressWarnings("squid:S1067")
@Embeddable
public class CaseIdentifier implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "authority_id")
    private UUID authorityId;

    @Column(name = "authority_code")
    private String authorityCode;

    @Column(name = "case_reference")
    private String caseReference;

    public UUID getAuthorityId() {
        return authorityId;
    }

    public void setAuthorityId(final UUID authorityId) {
        this.authorityId = authorityId;
    }

    public String getAuthorityCode() {
        return authorityCode;
    }

    public void setAuthorityCode(final String authorityCode) {
        this.authorityCode = authorityCode;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public void setCaseReference(final String caseReference) {
        this.caseReference = caseReference;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseIdentifier that = (CaseIdentifier) o;
        return Objects.equals(authorityId, that.authorityId) &&
                Objects.equals(authorityCode, that.authorityCode) &&
                Objects.equals(caseReference, that.caseReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorityId, authorityCode, caseReference);
    }
}