package uk.gov.moj.cpp.listing.steps.data;

import java.util.Optional;
import java.util.UUID;

public class JudicialRoleData {

    private final Optional<Boolean> isBenchChairman;

    private final Optional<Boolean> isDeputy;

    private final UUID judicialId;

    private final JudicialRoleTypeData judicialRoleType;

    public JudicialRoleData(Optional<Boolean> isBenchChairman, Optional<Boolean> isDeputy, UUID judicialId, JudicialRoleTypeData judicialRoleType) {
        this.isBenchChairman = isBenchChairman;
        this.isDeputy = isDeputy;
        this.judicialId = judicialId;
        this.judicialRoleType = judicialRoleType;
    }

    public Optional<Boolean> getIsBenchChairman() {
        return isBenchChairman;
    }

    public Optional<Boolean> getIsDeputy() {
        return isDeputy;
    }

    public UUID getJudicialId() {
        return judicialId;
    }

    public JudicialRoleTypeData getJudicialRoleType() {
        return judicialRoleType;
    }
}
