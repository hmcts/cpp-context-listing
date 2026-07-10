package uk.gov.moj.cpp.listing.persistence.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "defendant")
public class Defendant {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "defendant_id", unique = true, nullable = false)
    private UUID defendantId;

    @Column(name = "master_defendant_id")
    private UUID masterDefendantId;

    @ManyToOne
    @JoinColumn(name = "listed_case_id", nullable = false)
    private ListedCases listedCase;

    public Defendant() {
    }

    public Defendant(final UUID id, final UUID defendantId, final UUID masterDefendantId, final ListedCases listedCase) {
        this.id = id;
        this.defendantId = defendantId;
        this.masterDefendantId = masterDefendantId;
        this.listedCase = listedCase;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public void setDefendantId(final UUID defendantId) {
        this.defendantId = defendantId;
    }

    public UUID getMasterDefendantId() {
        return masterDefendantId;
    }

    public void setMasterDefendantId(final UUID masterDefendantId) {
        this.masterDefendantId = masterDefendantId;
    }

    public ListedCases getListedCase() {
        return listedCase;
    }

    public void setListedCase(final ListedCases listedCase) {
        this.listedCase = listedCase;
    }
}
