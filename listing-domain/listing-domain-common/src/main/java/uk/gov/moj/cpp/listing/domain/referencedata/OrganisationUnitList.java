package uk.gov.moj.cpp.listing.domain.referencedata;

import java.util.List;

public class OrganisationUnitList {

    private List<OrganisationUnit> organisationunits;

    public OrganisationUnitList(final List<OrganisationUnit> organisationunits) {
        this.organisationunits = organisationunits;
    }

    public List<OrganisationUnit> getOrganisationunits() {
        return organisationunits;
    }

    public void setOrganisationunits(final List<OrganisationUnit> organisationunits) {
        this.organisationunits = organisationunits;
    }
}