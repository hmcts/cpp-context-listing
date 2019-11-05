package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.ZonedDateTime.now;
import static uk.gov.justice.listing.event.PublishCourtListType.FIRM;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class PublishCourtListRequestParametersBuilder {

    private UUID courtCentreId;
    private LocalDate startDate;
    private LocalDate endDate;
    private PublishCourtListType publishCourtListType;
    private ZonedDateTime requestedTime;

    private PublishCourtListRequestParametersBuilder() {
        courtCentreId = UUID.randomUUID();
        startDate = LocalDate.of(2019, 11, 4);
        endDate = LocalDate.of(2019, 11, 5);
        publishCourtListType = FIRM;
        requestedTime = now();
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

    public PublishCourtListRequestParametersBuilder withPublishCourtListType(final PublishCourtListType publishCourtListType) {
        this.publishCourtListType = publishCourtListType;
        return this;
    }

    public PublishCourtListRequestParametersBuilder withRequestedTime(final ZonedDateTime requestedTime) {
        this.requestedTime = requestedTime;
        return this;
    }


    public PublishCourtListRequestParameters build() {
        return new PublishCourtListRequestParameters(courtCentreId, startDate, endDate, publishCourtListType, requestedTime);
    }
}
