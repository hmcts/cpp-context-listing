package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class OrganisationData {

    private final UUID id;
    private final String name;

    public OrganisationData(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public UUID getId() {
        return id;
    }
}
