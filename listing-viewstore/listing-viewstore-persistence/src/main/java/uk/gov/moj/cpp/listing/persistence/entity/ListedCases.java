package uk.gov.moj.cpp.listing.persistence.entity;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@SuppressWarnings({"squid:S1948"})
@Entity
@Table(name = "listed_cases")
public class ListedCases implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "case_id", unique = true, nullable = false)
    private UUID caseId;

    @Embedded
    private CaseIdentifier caseIdentifier;

    @Embedded
    private Prosecutor prosecutor;

    @ManyToOne
    @JoinColumn(name = "hearing_id", nullable = false)
    private Hearing hearing;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "listedCase", orphanRemoval = true)
    private Set<LinkedCase> linkedCases = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "listedCase", orphanRemoval = true)
    private Set<Defendant> defendants = new HashSet<>();

    public ListedCases() {
        // for JPA
    }

    public ListedCases(final UUID id,
                       final UUID caseId,
                       final CaseIdentifier caseIdentifier,
                       final Prosecutor prosecutor,
                       final Hearing hearing,
                       final Set<LinkedCase> linkedCases,
                       Set<Defendant> defendants) {
        this.id = id;
        this.caseId = caseId;
        this.caseIdentifier = caseIdentifier;
        this.prosecutor = prosecutor;
        this.hearing = hearing;
        this.setLinkedCases(linkedCases);
        this.setDefendants(defendants);
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public CaseIdentifier getCaseIdentifier() {
        return caseIdentifier;
    }

    public void setCaseIdentifier(final CaseIdentifier caseIdentifier) {
        this.caseIdentifier = caseIdentifier;
    }

    public Hearing getHearing() {
        return hearing;
    }

    public void setHearing(final Hearing hearing) {
        this.hearing = hearing;
    }

    public Set<LinkedCase> getLinkedCases() {
        return linkedCases;
    }

    public void setLinkedCases(final Set<LinkedCase> linkedCases) {
        if(isNull(linkedCases)){
            return;
        }

        this.linkedCases.clear();
        linkedCases.forEach(linkedCase -> {
            linkedCase.setListedCase(this);
            this.linkedCases.add(linkedCase);
        });
    }

    public Set<Defendant> getDefendants() {
        return defendants;
    }

    public void setDefendants(final Set<Defendant> defendants) {
        if(isNull(defendants)){
            return;
        }

        this.defendants.clear();
        defendants.forEach(defendant -> {
            defendant.setListedCase(this);
            this.defendants.add(defendant);
        });
    }

    public Prosecutor getProsecutor() {
        return prosecutor;
    }

    public void setProsecutor(final Prosecutor prosecutor) {
        this.prosecutor = prosecutor;
    }
}
