package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class HearingTypeData {

    private final UUID typeId;
    private final String typeDescription;

    public HearingTypeData(final UUID typeId, final String typeDescription) {
        this.typeDescription = typeDescription;
        this.typeId = typeId;
    }

    public UUID getTypeId() { return typeId; }

    public String getTypeDescription() { return typeDescription; }
}
