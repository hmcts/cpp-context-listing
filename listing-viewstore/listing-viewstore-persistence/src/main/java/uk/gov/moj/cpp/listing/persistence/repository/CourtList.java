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
    private UUID courthouse_id;

    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "document_name", nullable = false)
    private String documentName;

    @Column(name = "date_actioned", nullable = false)
    private ZonedDateTime dateActioned;

    @Column(name = "error_message")
    private String errorMessage;

    public CourtList() {
    }

    public CourtList(final UUID courthouseId,
                     final Status status,
                     final UUID documentId,
                     final DocumentType documentType,
                     final String documentName,
                     final ZonedDateTime dateActioned
    ) {
        this.courthouse_id = courthouseId;
        this.status = status;
        this.documentId = documentId;
        this.documentName = documentName;
        this.documentType = documentType;
        this.dateActioned = dateActioned;
    }

    public UUID getCourthouse_id() {
        return courthouse_id;
    }

    public void setCourthouse_id(UUID courthouse_id) {
        this.courthouse_id = courthouse_id;
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