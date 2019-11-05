package uk.gov.moj.cpp.listing.persistence.repository;

import static javax.persistence.EnumType.STRING;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "courtList")
public class CourtList {


    @Id
    @Column(name = "court_centre_id", nullable = false)
    private UUID courtCentreId;

    @Enumerated(STRING)
    @Column(name = "publish_status", nullable = false)
    private PublishStatus publishStatus;

    @Enumerated(STRING)
    @Column(name = "court_list_type", nullable = false)
    private CourtListType courtListType;

    @Column(name = "court_list_file_id")
    private UUID courtListFileId;

    @Column(name = "court_list_file_name")
    private String courtListFileName;

    @Column(name = "last_updated", nullable = false)
    private ZonedDateTime lastUpdated;

    @Column(name = "error_message")
    private String errorMessage;

    public CourtList(final UUID courtCentreId,
                     final PublishStatus publishStatus,
                     final CourtListType courtListType,
                     final ZonedDateTime lastUpdated
    ) {
        this.courtCentreId = courtCentreId;
        this.publishStatus = publishStatus;
        this.courtListType = courtListType;
        this.lastUpdated = lastUpdated;
    }

    public CourtList(final UUID courtCentreId,
                     final PublishStatus publishStatus,
                     final UUID courtListFileId,
                     final String courtListFileName,
                     final CourtListType courtListType,
                     final ZonedDateTime lastUpdated
    ) {
        this.courtCentreId = courtCentreId;
        this.publishStatus = publishStatus;
        this.courtListFileId = courtListFileId;
        this.courtListFileName = courtListFileName;
        this.courtListType = courtListType;
        this.lastUpdated = lastUpdated;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
    }

    public CourtListType getCourtListType() {
        return courtListType;
    }

    public void setCourtListType(CourtListType courtListType) {
        this.courtListType = courtListType;
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

    public ZonedDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(ZonedDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}