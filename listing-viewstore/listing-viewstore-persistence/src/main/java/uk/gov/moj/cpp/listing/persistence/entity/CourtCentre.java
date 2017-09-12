package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "court_centre")
public class CourtCentre {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "court_centre_name")
    private String courtCentreName;


    public CourtCentre() {
        // for JPA
    }

    public CourtCentre(final UUID id, final String courtCentreName) {
        this.id = id;
        this.courtCentreName = courtCentreName;
    }

    public UUID getId() {
        return id;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CourtCentre courtCentre = (CourtCentre) o;

        return id.equals(courtCentre.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
