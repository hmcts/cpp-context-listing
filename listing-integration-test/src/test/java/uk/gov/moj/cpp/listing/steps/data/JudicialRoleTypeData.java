package uk.gov.moj.cpp.listing.steps.data;

import java.util.Optional;
import java.util.UUID;

public class JudicialRoleTypeData {

    private final Optional<UUID> judicialRoleTypeId;

    private final String judiciaryType;

    public JudicialRoleTypeData(Optional<UUID> judicialRoleTypeId, String judiciaryType) {
        this.judicialRoleTypeId = judicialRoleTypeId;
        this.judiciaryType = judiciaryType;
    }


    public Optional<UUID> getJudicialRoleTypeId() {
        return judicialRoleTypeId;
    }

    public String getJudiciaryType() {
        return judiciaryType;
    }
}
