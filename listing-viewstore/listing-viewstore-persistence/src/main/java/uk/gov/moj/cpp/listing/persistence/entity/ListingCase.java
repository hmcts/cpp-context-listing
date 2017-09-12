package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "listing_case")
public class ListingCase implements Serializable {

    @Id
    @Column(name = "id", unique = true)
    private UUID id;

    @Column(name = "urn")
    private String urn;

    @Column(name = "sending_committal_date")
    private LocalDate sendingCommittalDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "listingCase")
    private Set<Defendant> defendants = new LinkedHashSet<>();

    public ListingCase() {
        // Required by JPA
    }

    ListingCase(final UUID id, final String urn, final LocalDate sendingCommittalDate,
                final Set<Defendant> defendants) {
        this.id = id;
        this.urn = urn;
        this.sendingCommittalDate = sendingCommittalDate;
        this.defendants = defendants;
        if (defendants != null) {
            defendants.forEach(defendant -> defendant.setListingCase(this));
        }
    }

    public UUID getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public LocalDate getSendingCommittalDate() {
        return sendingCommittalDate;
    }

    public Set<Defendant> getDefendants() {
        return defendants;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListingCase that = (ListingCase) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

