package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Judiciaries {
    private final String judicialType;
    private final UUID judicialId;

    public Judiciaries(final UUID judicialId, final String judicialType) {
        this.judicialType = judicialType;
        this.judicialId = judicialId;
    }

    public String getJudicialType() {
        return judicialType;
    }

    public UUID getJudicialId() {
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

        final Judiciaries that = (Judiciaries) o;

        return new EqualsBuilder()
                .append(judicialType, that.judicialType)
                .append(judicialId, that.judicialId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(judicialType)
                .append(judicialId)
                .toHashCode();
    }
}
