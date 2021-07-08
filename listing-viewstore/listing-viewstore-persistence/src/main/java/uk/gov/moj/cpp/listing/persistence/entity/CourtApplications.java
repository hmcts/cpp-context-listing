package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@SuppressWarnings({"squid:S1948"})
@Entity
@Table(name = "court_applications")
    public class CourtApplications implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "application_id", unique = true, nullable = false)
    private UUID applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hearing_id", nullable = false)
    private Hearing hearing;

    @Column(name = "application_type")
    private String applicationType;

    @Column(name = "parent_application_id")
    private UUID parentApplicationId;

    @Column(name = "application_reference")
    private String applicationReference;

    @Column(name = "application_particulars")
    private String applicationParticulars;

    @Column(name="is_ejected")
    private Boolean isEjected;

    public CourtApplications(){

    }

    public CourtApplications(final UUID id, final UUID applicationId, final Hearing hearing, final String applicationType, final UUID parentApplicationId, final String applicationReference, final String applicationParticulars, final Boolean isEjected) {
        this.id = id;
        this.applicationId = applicationId;
        this.hearing = hearing;
        this.applicationType = applicationType;
        this.parentApplicationId = parentApplicationId;
        this.applicationReference = applicationReference;
        this.applicationParticulars = applicationParticulars;
        this.isEjected = isEjected;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(final UUID applicationId) {
        this.applicationId = applicationId;
    }

    public Hearing getHearing() {
        return hearing;
    }

    public void setHearing(final Hearing hearing) {
        this.hearing = hearing;
    }

    public String getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(final String applicationType) {
        this.applicationType = applicationType;
    }

    public UUID getParentApplicationId() {
        return parentApplicationId;
    }

    public void setParentApplicationId(final UUID parentApplicationId) {
        this.parentApplicationId = parentApplicationId;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(final String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public String getApplicationParticulars() {
        return applicationParticulars;
    }

    public void setApplicationParticulars(final String applicationParticulars) {
        this.applicationParticulars = applicationParticulars;
    }

    public Boolean getEjected() {
        return isEjected;
    }

    public void setEjected(final Boolean ejected) {
        isEjected = ejected;
    }
}
