package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class HearingTypeData {

    private final UUID typeId;
    private final String typeDescription;
    private final String welshDescription;

    public HearingTypeData(final UUID typeId, final String typeDescription, final String welshDescription) {
        this.typeDescription = typeDescription;
        this.typeId = typeId;
        this.welshDescription = welshDescription;
    }

    public UUID getTypeId() { return typeId; }

    public String getTypeDescription() { return typeDescription; }

    public String getWelshDescription() {
        return welshDescription;
    }
}
