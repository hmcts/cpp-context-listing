package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CompositeDefendantId implements Serializable {

    @Column(name = "hearing_id", nullable = false)
    private UUID hearingId;

    @Column(name = "defendant_id", nullable = false)
    private UUID defendantId;

    public CompositeDefendantId() {
    }

    public CompositeDefendantId(UUID hearingId, UUID defendantId) {
        this.hearingId = hearingId;
        this.defendantId = defendantId;
    }

    public UUID getHearingId() {
        return hearingId;
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
        final CompositeDefendantId that = (CompositeDefendantId) o;
        return Objects.equals(hearingId, that.hearingId) &&
                Objects.equals(defendantId, that.defendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hearingId, defendantId);
    }
}
