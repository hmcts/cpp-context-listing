package uk.gov.moj.cpp.listing.steps.data;


import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class DefendantData {

    private final UUID defendantId;
    private final String firstName;
    private final String lastName;
    private final List<OffenceData> offences;
    private final String bailStatus;
    private final LocalDate dateOfBirth;
    private final LocalDate custodyTimeLimit;
    private final String defenceOrganisation;
    private final Boolean restrictFromCourtList ;
    private final LegalEntityDefendantData legalEntityDefendant;

    public DefendantData(final UUID defendantId, final String firstName,
                         final String lastName, final LocalDate dateOfBirth,
                         final LocalDate custodyTimeLimit, final String bailStatus,
                         final String defenceOrganisation, final List<OffenceData> offences, LegalEntityDefendantData legalEntityDefendant, final Boolean restrictFromCourtList) {
        this.defendantId = defendantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.offences  = offences;
        this.defenceOrganisation = defenceOrganisation;
        this.legalEntityDefendant = legalEntityDefendant;
        this.restrictFromCourtList = restrictFromCourtList;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getChangedFirstName() {
        return firstName + "-Changed";
    }

    public String getLastName() { return lastName; }

    public String getChangedLastName() { return lastName + "-Changed"; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }

    public String getBailStatus() { return bailStatus; }

    public LocalDate getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public Boolean getRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public List<OffenceData> getOffences() { return offences; }

    public String getDefenceOrganisation() { return defenceOrganisation; }

    public LegalEntityDefendantData getLegalEntityDefendant() {
        return legalEntityDefendant;
    }
}
