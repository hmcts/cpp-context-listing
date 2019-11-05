package uk.gov.moj.cpp.listing.persistence.repository;


import static javax.persistence.EnumType.STRING;

import uk.gov.justice.listing.event.PublishStatus;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Enumerated;
import javax.persistence.Table;

@Entity
@Table(name = "courtlist")
public class CourtList implements Serializable {

    private static final long serialVersionUID = 8137443412665L;

    @EmbeddedId
    @Column(name = "shared_court_list_primary_key", unique = true, nullable = false)
    private CourtListPK courtListPK;

    @Enumerated(STRING)
    @Column(name = "publish_status", nullable = false)
    private PublishStatus publishStatus;

    @Column(name = "court_list_file_id")
    private UUID courtListFileId;

    @Column(name = "court_list_file_name")
    private String courtListFileName;

    @Column(name = "last_updated", nullable = false)
    private ZonedDateTime lastUpdated;

    @Column(name = "error_message")
    private String errorMessage;

    public CourtList() {

    }

    public CourtList(final CourtListPK courtListPK,
                     final PublishStatus publishStatus,
                     final UUID courtListFileId,
                     final String courtListFileName,
                     final ZonedDateTime lastUpdated
    ) {
        this.courtListPK = courtListPK;
        this.publishStatus = publishStatus;
        this.courtListFileId = courtListFileId;
        this.courtListFileName = courtListFileName;
        this.lastUpdated = lastUpdated;
    }

    public CourtList(CourtListPK courtListPK, PublishStatus courtListRequested, ZonedDateTime requestedTime) {
        this.courtListPK = courtListPK;
        this.publishStatus = courtListRequested;
        this.lastUpdated = requestedTime;
    }

    public CourtListPK getCourtListPK() {
        return courtListPK;
    }

    public void setCourtListPK(CourtListPK courtListPK) {
        this.courtListPK = courtListPK;
    }

    public PublishStatus getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(PublishStatus publishStatus) {
        this.publishStatus = publishStatus;
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
