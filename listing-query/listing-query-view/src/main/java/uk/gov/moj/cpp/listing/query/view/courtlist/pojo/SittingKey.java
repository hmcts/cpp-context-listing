package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class SittingKey implements Comparable<SittingKey> {

    private LocalDate sittingDate;
    private Optional<UUID> courtRoomId;
    private Optional<UUID> judicialId;

    public SittingKey(final LocalDate sittingDate, final Optional<UUID> courtRoomId, final Optional<UUID> judicialId) {
        this.sittingDate = sittingDate;
        this.courtRoomId = courtRoomId;
        this.judicialId = judicialId;
    }

    public LocalDate getSittingDate() {
        return sittingDate;
    }

    public Optional<UUID> getCourtRoomId() {
        return courtRoomId;
    }

    public Optional<UUID> getJudicialId() {
        return judicialId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final SittingKey that = (SittingKey) o;

        return new EqualsBuilder()
                .append(sittingDate, that.sittingDate)
                .append(courtRoomId, that.courtRoomId)
                .append(judicialId, that.judicialId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(sittingDate)
                .append(courtRoomId)
                .append(judicialId)
                .toHashCode();
    }

    @Override
    public int compareTo(final SittingKey otherSitting) {
        return sittingDate.compareTo(otherSitting.sittingDate);
    }
}
