package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import uk.gov.moj.cpp.listing.domain.xhibit.XhibitCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishCourtListRequestParameters {

    private UUID courtCentreId;
    private LocalDate startDate;
    private LocalDate endDate;
    private XhibitCourtListType xhibitCourtListType;
    private ZonedDateTime requestedTime;

    public PublishCourtListRequestParameters(final UUID courtCentreId,
                                             final LocalDate startDate,
                                             final LocalDate endDate,
                                             final XhibitCourtListType xhibitCourtListType,
                                             final ZonedDateTime requestedTime) {

        this.courtCentreId = courtCentreId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.xhibitCourtListType = xhibitCourtListType;
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

    public XhibitCourtListType getXhibitCourtListType() {
        return xhibitCourtListType;
    }

    public ZonedDateTime getRequestedTime() {
        return requestedTime;
    }
}
