package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings({"squid:S2160"}) // Super class uses unique ID to test for equality
@Entity
@Table(name = "defendant")
public class Defendant extends AbstractDefendant {


    @ManyToOne
    @JoinColumn(name = "mapped_hearing_id")
    private Hearing hearing;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "defendant")
    private Set<Offence> offences = new LinkedHashSet<>();

    public Defendant() {
        // Required by JPA
    }

    @SuppressWarnings({"squid:S00107"}) // Constructor is only used by Builders of sub classes
    public Defendant(
            CompositeDefendantId compositeDefendantId,
            UUID personId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String bailStatus,
            LocalDate custodyTimeLimit,
            String defenceOrganisation,
            Hearing hearing,
            Set<Offence> offences) {
        super(
                compositeDefendantId,
                personId,
                firstName,
                lastName,
                dateOfBirth,
                bailStatus,
                custodyTimeLimit,
                defenceOrganisation);
        this.hearing = hearing;
        this.offences = offences;
        if (offences != null) {
            offences.forEach(offence -> offence.setDefendant(this));
        }
    }

    public Set<Offence> getOffences() { return offences; }

    public Hearing getHearing() { return hearing; }
}

