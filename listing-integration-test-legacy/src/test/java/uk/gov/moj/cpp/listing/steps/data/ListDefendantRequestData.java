package uk.gov.moj.cpp.listing.steps.data;

import java.util.List;
import java.util.UUID;

public class ListDefendantRequestData {

    private final List<UUID> defendantOffences;
    private final UUID prosecutionCaseId;

    public ListDefendantRequestData(final List<UUID> defendantOffences, final UUID prosecutionCaseId) {
        this.defendantOffences = defendantOffences;
        this.prosecutionCaseId = prosecutionCaseId;
    }

    public List<UUID> getDefendantOffences() {
        return defendantOffences;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }
}
