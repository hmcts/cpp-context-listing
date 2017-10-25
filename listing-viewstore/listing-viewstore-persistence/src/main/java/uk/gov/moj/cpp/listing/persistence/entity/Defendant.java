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
@Table(name = "defendant")
public class Defendant implements Serializable {

    @Id
    @Column(name = "listing_defendant_id", unique = true, nullable = false)
    private UUID listingDefendantId;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

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
    private Set<Offence> offences = new LinkedHashSet<>();

    @ManyToOne
    @JoinColumn(name = "hearing_id")
    private Hearing hearing;

    public Defendant() {
        // Required by JPA
    }

    public Defendant(final UUID listingDefendantId, final UUID defendantId,
                     final String bailStatus, final String defenceOrganisation,
                     final Set<Offence> offences, final Hearing hearing,
                     final PersonalDetails personalDetails) {
        this.listingDefendantId = listingDefendantId;
        this.defendantId = defendantId;
        this.personId = personalDetails.getPersonId();
        this.firstName = personalDetails.getFirstName();
        this.lastName = personalDetails.getLastName();
        this.dateOfBirth = personalDetails.getDateOfBirth();
        this.bailStatus = bailStatus;
        this.defenceOrganisation = defenceOrganisation;
        this.offences = offences;
        if (offences != null) {
            offences.forEach(offence -> offence.setDefendant(this));
        }
        this.hearing = hearing;
    }

    public UUID getListingDefendantId() {
        return listingDefendantId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public Set<Offence> getOffences() { return offences; }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }

    public String getBailStatus() { return bailStatus; }

    public String getDefenceOrganisation() { return defenceOrganisation; }

    public Hearing getHearing() {
        return hearing;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Defendant defendant = (Defendant) o;

        return listingDefendantId.equals(defendant.listingDefendantId);
    }

    @Override
    public int hashCode() {
        return listingDefendantId.hashCode();
    }


    public static class PersonalDetails {

        private UUID personId;

        private String firstName;

        private String lastName;

        private LocalDate dateOfBirth;

        public PersonalDetails(UUID personId, String firstName, String lastName, LocalDate dateOfBirth) {
            this.personId = personId;
            this.firstName = firstName;
            this.lastName = lastName;
            this.dateOfBirth = dateOfBirth;
        }

        public UUID getPersonId() {
            return personId;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public LocalDate getDateOfBirth() {
            return dateOfBirth;
        }
    }
}

