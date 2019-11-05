package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishCourtListRequestParameters {

    private UUID courtCentreId;
    private LocalDate startDate;
    private LocalDate endDate;
    private PublishCourtListType publishCourtListType;
    private ZonedDateTime requestedTime;

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
}
