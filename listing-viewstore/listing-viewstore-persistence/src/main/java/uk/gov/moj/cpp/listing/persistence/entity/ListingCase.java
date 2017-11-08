package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "listing_case")
public class ListingCase implements Serializable {

    @Id
    @Column(name = "id", unique = true)
    private UUID caseProgressionId;

    @Column(name = "urn")
    private String urn;

    public ListingCase() {
        // Required by JPA
    }

    ListingCase(final UUID caseProgressionId, final String urn) {
        this.caseProgressionId = caseProgressionId;
        this.urn = urn;
    }

    public UUID getCaseProgressionId() {
        return caseProgressionId;
    }

    public String getUrn() {
        return urn;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListingCase that = (ListingCase) o;

        return caseProgressionId != null ? caseProgressionId.equals(that.caseProgressionId) : that.caseProgressionId == null;
    }

    @Override
    public int hashCode() {
        return caseProgressionId != null ? caseProgressionId.hashCode() : 0;
    }
}

