package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class LegalEntityDefendantData {

    private final UUID id;
    private final OrganisationData organisation;


    public LegalEntityDefendantData(UUID id, OrganisationData organisation) {
        this.id = id;
        this.organisation = organisation;
    }

    public OrganisationData getOrganisation() {
        return organisation;
    }

    public UUID getId() {
        return id;
    }
}
