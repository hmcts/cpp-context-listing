package uk.gov.moj.cpp.listing.persistence.repository;


import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CourtListPublishStatus {

    private UUID courtCentreId;
    private PublishCourtListType publishCourtListType;
    private ZonedDateTime lastUpdated;
    private PublishStatus publishStatus;
    private String failureMessage;

    public CourtListPublishStatus(final UUID courtCentreId, final PublishCourtListType publishCourtListType,
                                  final ZonedDateTime lastUpdated, final PublishStatus publishStatus) {
        this.courtCentreId = courtCentreId;
        this.publishCourtListType = publishCourtListType;
        this.lastUpdated = lastUpdated;
        this.publishStatus = publishStatus;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public PublishCourtListType getPublishCourtListType() {
        return publishCourtListType;
    }

    public void setCourtListType(PublishCourtListType publishCourtListType) {
        this.publishCourtListType = publishCourtListType;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }


}