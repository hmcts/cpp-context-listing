package uk.gov.moj.cpp.listing.query.view.hearing;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class DefendantSummary  implements Serializable {

    private final UUID defendantId;

    private final UUID hearingId;

    private final String firstName;

    private final String lastName;

    private final String bailStatus;

    private final LocalDate custodyTimeLimit;

    private final Set<OffenceSummary> offences;

    public DefendantSummary(final UUID hearingId, final UUID defendantId, final String firstName, final String lastName,
                            final String bailStatus, final LocalDate custodyTimeLimit,
                            final Set<OffenceSummary> offences) {
        this.defendantId = defendantId;
        this.hearingId = hearingId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.bailStatus = bailStatus;
        this.custodyTimeLimit = custodyTimeLimit;
        this.offences = offences;
    }

    public String getFirstName() { return firstName; }

    public String getLastName() { return lastName;  }

    public String getBailStatus() { return bailStatus; }

    public LocalDate getCustodyTimeLimit() {
        return custodyTimeLimit;
    }

    public Set<OffenceSummary> getOffences() { return offences; }

    public UUID getDefendantId() { return defendantId; }

    public UUID getHearingId() { return hearingId; }
}
