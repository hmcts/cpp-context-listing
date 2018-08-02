package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CompositeOffenceId implements Serializable {

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    @Column(name = "offence_id", nullable = false)
    private UUID offenceId;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

    public CompositeOffenceId() {
    }

    public CompositeOffenceId(UUID hearingId, UUID offenceId, UUID defendantId) {
        this.hearingId = hearingId;
        this.offenceId = offenceId;
        this.defendantId = defendantId;
    }

    public UUID getHearingId() {
        return hearingId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CompositeOffenceId that = (CompositeOffenceId) o;
        return Objects.equals(hearingId, that.hearingId) &&
                Objects.equals(offenceId, that.offenceId) &&
                Objects.equals(defendantId, that.defendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingId, offenceId, defendantId);
    }
}
