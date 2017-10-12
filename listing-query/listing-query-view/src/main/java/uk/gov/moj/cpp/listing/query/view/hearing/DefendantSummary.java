package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

public class DefendantSummary  implements Serializable {

    private final UUID id;

    private final String name;

    private final String bailStatus;

    private final Set<OffenceSummary> offences;

    public DefendantSummary(final UUID id, final String firstName, final String lastName, final String bailStatus, final Set<OffenceSummary> offences) {
        this.id = id;
        this.name = firstName+" "+lastName;
        this.bailStatus = bailStatus;
        this.offences = offences;
    }

    public String getName() {
        return name;
    }

    public String getBailStatus() {
        return bailStatus;
    }

    public Set<OffenceSummary> getOffences() {
        return offences;
    }

    public UUID getId() {

        return id;
    }
}
