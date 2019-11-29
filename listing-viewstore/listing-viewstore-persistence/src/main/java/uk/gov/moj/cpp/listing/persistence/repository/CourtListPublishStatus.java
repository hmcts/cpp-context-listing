package uk.gov.moj.cpp.listing.persistence.repository;


import static javax.persistence.EnumType.STRING;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.listing.event.PublishStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "court_list_publish_status")
public class CourtListPublishStatus implements Serializable {

    private static final long serialVersionUID = 8137443412665L;

    @Id
    @Column(name = "court_list_publish_status_id", nullable = false)
    private UUID publishCourtListStatusId;

    @Column(name = "court_centre_id", nullable = false)
    private UUID courtCentreId;

    @Enumerated(STRING)
    @Column(name = "publish_court_list_type", nullable = false)
    private PublishCourtListType publishCourtListType;

    @Enumerated(STRING)
    @Column(name = "publish_status", nullable = false)
    private PublishStatus publishStatus;

    @Column(name = "last_updated", nullable = false)
    private ZonedDateTime lastUpdated;

    @Column(name = "court_list_file_id")
    private UUID courtListFileId;

    @Column(name = "court_list_file_name")
    private String courtListFileName;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Column(name = "week_commencing")
    private boolean weekCommencing;

    public CourtListPublishStatus() {

    }

    public CourtListPublishStatus(final UUID publishCourtListStatusId,
                                  final UUID courtCentreId,
                                  final PublishCourtListType publishCourtListType,
                                  final PublishStatus publishStatus,
                                  final ZonedDateTime lastUpdated,
                                  final LocalDate publishDate,
                                  final boolean weekCommencing
    ) {
        this.publishCourtListStatusId = publishCourtListStatusId;
        this.courtCentreId = courtCentreId;
        this.publishCourtListType = publishCourtListType;
        this.publishStatus = publishStatus;
        this.lastUpdated = lastUpdated;
        this.publishDate = publishDate;
        this.weekCommencing = weekCommencing;
    }

    public CourtListPublishStatus(final UUID publishCourtListStatusId,
                                  final UUID courtCentreId,
                                  final PublishCourtListType publishCourtListType,
                                  final PublishStatus publishStatus,
                                  final ZonedDateTime lastUpdated,
                                  final UUID courtListFileId,
                                  final String courtListFileName,
                                  final String errorMessage,
                                  final LocalDate publishDate,
                                  final boolean weekCommencing
    ) {
        this.publishCourtListStatusId = publishCourtListStatusId;
        this.courtCentreId = courtCentreId;
        this.courtListFileId = courtListFileId;
        this.courtListFileName = courtListFileName;
        this.publishCourtListType = publishCourtListType;
        this.publishStatus = publishStatus;
        this.lastUpdated = lastUpdated;
        this.errorMessage = errorMessage;
        this.publishDate = publishDate;
        this.weekCommencing = weekCommencing;

    }

    public UUID getPublishCourtListStatusId() {
        return publishCourtListStatusId;
    }

    public void setPublishCourtListStatusId(UUID publishCourtListStatusId) {
        this.publishCourtListStatusId = publishCourtListStatusId;
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

    public void setPublishCourtListType(PublishCourtListType publishCourtListType) {
        this.publishCourtListType = publishCourtListType;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
    }

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public UUID getCourtListFileId() {
        return courtListFileId;
    }

    public void setCourtListFileId(UUID courtListFileId) {
        this.courtListFileId = courtListFileId;
    }

    public String getCourtListFileName() {
        return courtListFileName;
    }

    public void setCourtListFileName(String courtListFileName) {
        this.courtListFileName = courtListFileName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("CourtListPublishStatus{")
                .append(" publishCourtListStatusId=").append(publishCourtListStatusId)
                .append(", courtCentreId=").append(courtCentreId)
                .append(", publishCourtListType=").append(publishCourtListType)
                .append(", publishStatus=").append(publishStatus)
                .append(", lastUpdated=").append(lastUpdated)
                .append(", courtListFileId=").append(courtListFileId)
                .append(", courtListFileName=").append(courtListFileName)
                .append(", courtListFileId=").append(courtListFileId)
                .append(", errorMessage=").append(errorMessage)
                .append(", publishDate=").append(publishDate)
                .append(", weekCommencing=").append(weekCommencing).append("}").toString();
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(LocalDate publishDate) {
        this.publishDate = publishDate;
    }

    public boolean isWeekCommencing() {
        return weekCommencing;
    }

    public void setWeekCommencing(boolean weekCommencing) {
        this.weekCommencing = weekCommencing;
    }

}
