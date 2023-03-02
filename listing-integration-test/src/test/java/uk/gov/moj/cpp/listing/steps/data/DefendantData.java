package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.core.courts.BailStatus;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public class DefendantData {

    private final UUID defendantId;
    private final UUID masterDefendantId;
    private final ZonedDateTime courtProceedingsInitiated;
    private final String firstName;
    private final String lastName;
    private final List<OffenceData> offences;
    private final BailStatus bailStatus;
    private final LocalDate dateOfBirth;
    private final LocalDate custodyTimeLimit;
    private final String defenceOrganisation;
    private final Boolean restrictFromCourtList;
    private final LegalEntityDefendantData legalEntityDefendant;
    private final Boolean isYouth;
    private final Boolean proceedingsConcluded;
    private final String listingReason;

    public DefendantData(final UUID defendantId, final String firstName,
                         final String lastName, final LocalDate dateOfBirth,
                         final LocalDate custodyTimeLimit, final BailStatus bailStatus,
                         final String defenceOrganisation, final List<OffenceData> offences, final LegalEntityDefendantData legalEntityDefendant,
                         final Boolean restrictFromCourtList, final Boolean isYouth, final Boolean proceedingsConcluded,
                         final UUID masterDefendantId, final ZonedDateTime courtProceedingsInitiated, final String listingReason) {
        this.defendantId = defendantId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.offences = offences;
        this.defenceOrganisation = defenceOrganisation;
        this.legalEntityDefendant = legalEntityDefendant;
        this.restrictFromCourtList = restrictFromCourtList;
        this.isYouth = isYouth;
        this.proceedingsConcluded = proceedingsConcluded;
        this.masterDefendantId = masterDefendantId;
        this.courtProceedingsInitiated = courtProceedingsInitiated;
        this.listingReason = listingReason;
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

    public String getLastName() {
        return lastName;
    }

    public String getChangedLastName() {
        return lastName + "-Changed";
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public BailStatus getBailStatus() {
        return bailStatus;
    }

    public LocalDate getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public Boolean getRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public List<OffenceData> getOffences() {
        return offences;
    }

    public String getDefenceOrganisation() {
        return defenceOrganisation;
    }

    public LegalEntityDefendantData getLegalEntityDefendant() {
        return legalEntityDefendant;
    }

    public Boolean getIsYouth() {
        return isYouth;
    }

    public Boolean getProceedingsConcluded() {
        return proceedingsConcluded;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public ZonedDateTime getCourtProceedingsInitiated() {
        return courtProceedingsInitiated;
    }

    public String getListingReason() {
        return listingReason;
    }
}
