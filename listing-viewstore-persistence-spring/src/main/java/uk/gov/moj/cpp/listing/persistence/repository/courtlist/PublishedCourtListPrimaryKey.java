package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class PublishedCourtListPrimaryKey implements Serializable {

    private static final long serialVersionUID = -7564100497622006770L;

    private UUID courtCentreId;
    private PublishCourtListType publishCourtListType;
    private LocalDate startDate;

    public PublishedCourtListPrimaryKey() {
    }

    public PublishedCourtListPrimaryKey(final UUID courtCentreId,
                                        final PublishCourtListType publishCourtListType,
                                        final LocalDate startDate) {
        this.courtCentreId = courtCentreId;
        this.publishCourtListType = publishCourtListType;
        this.startDate = startDate;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public PublishCourtListType getPublishCourtListType() {
        return publishCourtListType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courtCentreId, publishCourtListType, startDate);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
