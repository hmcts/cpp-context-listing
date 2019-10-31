package uk.gov.moj.cpp.listing.persistence.repository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "courtList")
public class CourtList {

    @Id
    @Column(name = "courthouse_id", nullable = false)
    private UUID courtHouseId;

    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "document_name")
    private String documentName;

    @Column(name = "date_actioned", nullable = false)
    private ZonedDateTime dateActioned;

    @Column(name = "error_message")
    private String errorMessage;

    public CourtList(final UUID courtHouseId,
                     final Status status,
                     final DocumentType documentType,
                     final ZonedDateTime dateActioned
    ) {
        this.courtHouseId = courtHouseId;
        this.status = status;
        this.documentType = documentType;
        this.dateActioned = dateActioned;
    }

    public CourtList(final UUID courtHouseId,
                     final Status status,
                     final UUID documentId,
                     final String documentName,
                     final DocumentType documentType,
                     final ZonedDateTime dateActioned
    ) {
        this.courtHouseId = courtHouseId;
        this.status = status;
        this.documentId = documentId;
        this.documentName = documentName;
        this.documentType = documentType;
        this.dateActioned = dateActioned;
    }

    public UUID getCourtHouseId() {
        return courtHouseId;
    }

    public void setCourtHouseId(UUID courtHouseId) {
        this.courtHouseId = courtHouseId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public ZonedDateTime getDateActioned() {
        return dateActioned;
    }

    public void setDateActioned(ZonedDateTime dateActioned) {
        this.dateActioned = dateActioned;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }


}