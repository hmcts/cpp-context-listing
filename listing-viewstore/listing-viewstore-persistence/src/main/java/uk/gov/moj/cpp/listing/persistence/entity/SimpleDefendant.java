package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "defendant")
public class SimpleDefendant extends AbstractDefendant {


    public SimpleDefendant() {
        // Required by JPA
    }

    public SimpleDefendant(
            CompositeDefendantId compositeDefendantId,
            UUID personId,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String bailStatus,
            LocalDate custodyTimeLimit,
            String defenceOrganisation) {
        super(
                compositeDefendantId,
                personId, firstName,
                lastName, dateOfBirth,
                bailStatus,
                custodyTimeLimit,
                defenceOrganisation);
    }
}

