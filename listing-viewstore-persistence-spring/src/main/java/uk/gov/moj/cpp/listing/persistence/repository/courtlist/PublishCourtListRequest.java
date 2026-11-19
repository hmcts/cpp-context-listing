package uk.gov.moj.cpp.listing.persistence.repository.courtlist;

import static jakarta.persistence.EnumType.STRING;

import uk.gov.justice.listing.event.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A {@code listing.command.publish-court-list} request (MBD refactor P2.3.1, ADR 015 §3, changeset
 * 032): the state the JEE {@code PublishCourtListRequestAggregate} kept and the viewstore did not.
 * The aggregate's only field was {@code weekCommencing}; {@code record-court-list-produced} reads it.
 */
@Entity
@Table(name = "publish_court_list_request")
public class PublishCourtListRequest {

    @Id
    @Column(name = "publish_court_list_request_id", nullable = false)
    private UUID publishCourtListRequestId;

    @Column(name = "court_centre_id", nullable = false)
    private UUID courtCentreId;

    @Enumerated(STRING)
    @Column(name = "publish_court_list_type", nullable = false)
    private PublishCourtListType publishCourtListType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "week_commencing", nullable = false)
    private boolean weekCommencing;

    @Column(name = "requested_time", nullable = false)
    private ZonedDateTime requestedTime;

    public PublishCourtListRequest() {
    }

    public PublishCourtListRequest(final UUID publishCourtListRequestId, final UUID courtCentreId, final PublishCourtListType publishCourtListType,
                                   final LocalDate startDate, final LocalDate endDate, final boolean weekCommencing,
                                   final ZonedDateTime requestedTime) {
        this.publishCourtListRequestId = publishCourtListRequestId;
        this.courtCentreId = courtCentreId;
        this.publishCourtListType = publishCourtListType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.weekCommencing = weekCommencing;
        this.requestedTime = requestedTime;
    }

    public UUID getPublishCourtListRequestId() {
        return publishCourtListRequestId;
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

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isWeekCommencing() {
        return weekCommencing;
    }

    public ZonedDateTime getRequestedTime() {
        return requestedTime;
    }
}
