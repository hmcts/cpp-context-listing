package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.unmodifiableSet;

@Entity
@Table(name = "defendant")
public class Defendant implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "person_id")
    private UUID personId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "bail_status")
    private String bailStatus;

    @Column(name = "defence_organisation")
    private String defenceOrganisation;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defendant")
    private Set<Offence> offences = new LinkedHashSet<>();;

    @ManyToOne
    @JoinColumn(name = "case_id")
    private ListingCase listingCase;

    public Defendant() {
        // Required by JPA
    }

    public Defendant(final UUID id, final UUID personId, final String firstName,
                     final String lastName, final String bailStatus, final String defenceOrganisation,
                     final LocalDate dateOfBirth, final Set<Offence> offences,
                     final ListingCase listingCase) {
        this.id = id;
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.bailStatus = bailStatus;
        this.defenceOrganisation = defenceOrganisation;
        this.offences = offences;
        if (offences != null) {
            offences.forEach(offence -> offence.setDefendant(this));
        }
        this.listingCase = listingCase;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPersonId() {
        return personId;
    }

    public Set<Offence> getOffences() { return offences; }

    public ListingCase getListingCase() {
        return listingCase;
    }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }

    public String getBailStatus() { return bailStatus; }

    public String getDefenceOrganisation() { return defenceOrganisation; }

    public void setListingCase(final ListingCase listingCase) {
        this.listingCase = listingCase;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Defendant defendant = (Defendant) o;

        return id.equals(defendant.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

