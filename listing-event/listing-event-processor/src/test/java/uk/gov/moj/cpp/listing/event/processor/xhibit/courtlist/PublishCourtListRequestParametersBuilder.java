package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishCourtListRequestParametersBuilder {

    private UUID publishCourtListRequestId;
    private UUID courtCentreId;
    private LocalDate startDate;
    private LocalDate endDate;
    private PublishCourtListType publishCourtListType;
    private ZonedDateTime requestedTime;
    private Boolean sendNotificationToParties;

    private PublishCourtListRequestParametersBuilder() {
        publishCourtListRequestId = randomUUID();
        courtCentreId = randomUUID();
        startDate = LocalDate.of(2019, 11, 4);
        endDate = LocalDate.of(2019, 11, 5);
        publishCourtListType = PublishCourtListType.FIRM;
        requestedTime = ZonedDateTime.now();
        sendNotificationToParties = true;
    }

    public static PublishCourtListRequestParametersBuilder withDefaults() {
        return new PublishCourtListRequestParametersBuilder();
    }

    public PublishCourtListRequestParametersBuilder withCourtCentreId(final UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
        return this;
    }

    public PublishCourtListRequestParametersBuilder withStartDate(final LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }

    public PublishCourtListRequestParametersBuilder withEndDate(final LocalDate endDateDate) {
        this.endDate = endDateDate;
        return this;
    }

    public PublishCourtListRequestParametersBuilder publishCourtListType(final PublishCourtListType publishCourtListType) {
        this.publishCourtListType = publishCourtListType;
        return this;
    }

    public PublishCourtListRequestParametersBuilder withRequestedTime(final ZonedDateTime requestedTime) {
        this.requestedTime = requestedTime;
        return this;
    }

    public PublishCourtListRequestParametersBuilder withSendNotificationToParties(final Boolean sendNotificationToParties) {
        this.sendNotificationToParties = sendNotificationToParties;
        return this;
    }

    public PublishCourtListRequestParameters build() {
        return new PublishCourtListRequestParameters(publishCourtListRequestId, courtCentreId, startDate, endDate, publishCourtListType, requestedTime, sendNotificationToParties);
    }
}
