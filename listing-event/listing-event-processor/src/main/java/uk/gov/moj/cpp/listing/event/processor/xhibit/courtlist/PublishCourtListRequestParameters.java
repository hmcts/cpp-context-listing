package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class PublishCourtListRequestParameters {

    private UUID courtCentreId;
    private LocalDate startDate;
    private LocalDate endDate;
    private PublishCourtListType publishCourtListType;
    private ZonedDateTime requestedTime;
    private boolean weekCommencing;
    public PublishCourtListRequestParameters(final UUID courtCentreId,
                                             final LocalDate startDate,
                                             final LocalDate endDate,
                                             final PublishCourtListType publishCourtListType,
                                             final ZonedDateTime requestedTime) {

        this.courtCentreId = courtCentreId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.publishCourtListType = publishCourtListType;
        this.requestedTime = requestedTime;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public PublishCourtListType getPublishCourtListType() {
        return publishCourtListType;
    }

    public ZonedDateTime getRequestedTime() {
        return requestedTime;
    }

    public boolean isWeekCommencing() {
        return weekCommencing;
    }

    public void setWeekCommencing(boolean weekCommencing) {
        this.weekCommencing = weekCommencing;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }
}
