package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "hearing")
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

    public Hearing() {
        // for JPA
    }

    public Hearing(final UUID id, final LocalDate startDateTime, final Integer estimateMinutes,
                   final String type, final String courtCentreId, final ListingCase listingCase) {
        this.id = id;
        this.startDateTime = startDateTime;
        this.estimateMinutes = estimateMinutes;
        this.type = type;
        this.courtCentreId = courtCentreId;
        this.listingCase = listingCase;
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
