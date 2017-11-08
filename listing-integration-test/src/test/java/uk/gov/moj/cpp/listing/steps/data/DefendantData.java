package uk.gov.moj.cpp.listing.steps.data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefendantData {

    private final UUID defendantId;
    private final UUID personId;
    private final String firstName;
    private final String lastName;
    private final List<OffenceData> offences;
    private final String bailStatus;
    private final LocalDate dateOfBirth;
    private final LocalDate custodyTimeLimit;
    private final String defenceOrganisation;

    public DefendantData(final UUID defendantId, final UUID personId, final String firstName,
                         final String lastName, final LocalDate dateOfBirth,
                         final LocalDate custodyTimeLimit, final String bailStatus,
                         final String defenceOrganisation, final List<OffenceData> offences) {
        this.defendantId = defendantId;
        this.personId = personId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.offences  = offences;
        this.defenceOrganisation = defenceOrganisation;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getPersonId() {
        return personId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() { return lastName; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }

    public String getBailStatus() { return bailStatus; }

    public LocalDate getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public List<OffenceData> getOffences() { return offences; }

    public String getDefenceOrganisation() { return defenceOrganisation; }
}
