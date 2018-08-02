package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public class AbstractDefendant implements Serializable {

    @EmbeddedId
    private CompositeDefendantId id;

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

    @Column(name = "custody_time_limit")
    private LocalDate custodyTimeLimit;

    @Column(name = "defence_organisation")
    private String defenceOrganisation;

    public AbstractDefendant() {
        // Required by JPA
    }

    @SuppressWarnings({"squid:S00107"}) // Constructor is only used by Builders of sub classes
    public AbstractDefendant(
            CompositeDefendantId compositeDefendantId,
            UUID personId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String bailStatus,
            LocalDate custodyTimeLimit,
            String defenceOrganisation) {
        this.id = compositeDefendantId;
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.defenceOrganisation = defenceOrganisation;
    }


    public CompositeDefendantId getId() { return id; }

    public UUID getPersonId() {
        return personId;
    }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }

    public LocalDate getCustodyTimeLimit() { return custodyTimeLimit; }

    public String getBailStatus() { return bailStatus; }

    public String getDefenceOrganisation() { return defenceOrganisation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AbstractDefendant that = (AbstractDefendant) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

