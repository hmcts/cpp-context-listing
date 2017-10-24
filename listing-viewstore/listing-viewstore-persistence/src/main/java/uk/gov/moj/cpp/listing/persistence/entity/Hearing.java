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
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "hearing")
@SuppressWarnings("squid:S00107")
public class Hearing implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "start_date_time")
    private LocalDate startDateTime;

    @Column(name = "estimate_minutes")
    private Integer estimateMinutes;

    @Column(name = "type")
    private String type;

    @Column(name = "court_centre_id")
    private String courtCentreId;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private ListingCase listingCase;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "hearing")
    private Set<Defendant> defendants = new LinkedHashSet<>();

    @Column(name = "allocated")
    private Boolean allocated;

    public Hearing() {
        // for JPA
    }

    public Hearing(final UUID id, final LocalDate startDateTime, final Integer estimateMinutes,
                   final String type, final String courtCentreId, final ListingCase listingCase,
                   final Boolean allocated, Set<Defendant> defendants) {
        this.id = id;
        this.startDateTime = startDateTime;
        this.estimateMinutes = estimateMinutes;
        this.type = type;
        this.courtCentreId = courtCentreId;
        this.listingCase = listingCase;
        this.allocated = allocated;
        this.defendants = defendants;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getStartDateTime() {
        return startDateTime;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public String getType() {
        return type;
    }

    public String getCourtCentreId() {
        return courtCentreId;
    }

    public ListingCase getListingCase() { return listingCase; }

    public Boolean getAllocated() {
        return allocated;
    }

    public Set<Defendant> getDefendants() {
        return defendants;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Hearing hearing = (Hearing) o;

        return id.equals(hearing.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
