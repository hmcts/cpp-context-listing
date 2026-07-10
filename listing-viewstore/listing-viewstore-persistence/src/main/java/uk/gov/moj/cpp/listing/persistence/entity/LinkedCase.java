package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "linked_case")
public class LinkedCase implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "case_urn")
    private String caseUrn;

    @ManyToOne
    @JoinColumn(name = "listed_case_id", nullable = false)
    private ListedCases listedCase;

    public LinkedCase() {
    }

    public LinkedCase(final UUID id, final UUID caseId, final String caseUrn, final ListedCases listedCase) {
        this.id = id;
        this.caseId = caseId;
        this.caseUrn = caseUrn;
        this.listedCase = listedCase;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public ListedCases getListedCase() {
        return listedCase;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public void setCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public void setListedCase(final ListedCases listedCase) {
        this.listedCase = listedCase;
    }
}
