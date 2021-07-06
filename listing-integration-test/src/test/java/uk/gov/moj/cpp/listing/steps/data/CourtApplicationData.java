package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class CourtApplicationData {
    private final UUID id;
    private final UUID linkedCaseId;
    private final UUID parentApplicationId;
    private final ApplicantRespondentData applicant;
    private final ApplicantRespondentData respondent;
    private final String type;
    private final Boolean requiresResponse;
    private final Boolean restrictCourtApplicationType;
    private final Boolean restrictFromCourtList;
    private final Boolean isEjected;
    private final String applicationParticulars;
    private final UUID offenceId;

    public CourtApplicationData(UUID id, UUID linkedCaseId, UUID parentApplicationId, ApplicantRespondentData applicant, ApplicantRespondentData respondent, String type, Boolean requiresResponse,
                                final Boolean restrictCourtApplicationType, final Boolean restrictFromCourtList, final Boolean isEjected, final String applicationParticulars, final UUID offenceId) {
        this.id = id;
        this.linkedCaseId = linkedCaseId;
        this.parentApplicationId = parentApplicationId;
        this.applicant = applicant;
        this.respondent = respondent;
        this.type = type;
        this.requiresResponse = requiresResponse;
        this.restrictCourtApplicationType = restrictCourtApplicationType;
        this.restrictFromCourtList = restrictFromCourtList;
        this.isEjected = isEjected;
        this.applicationParticulars = applicationParticulars;
        this.offenceId = offenceId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLinkedCaseId() {
        return linkedCaseId;
    }

    public UUID getParentApplicationId() {
        return parentApplicationId;
    }

    public ApplicantRespondentData getApplicant() {
        return applicant;
    }

    public Boolean getEjected() {
        return isEjected;
    }

    public ApplicantRespondentData getRespondent() {
        return respondent;
    }

    public String getType() {
        return type;
    }

    public Boolean getRestrictCourtApplicationType() {
        return restrictCourtApplicationType;
    }

    public Boolean getRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public Boolean getRequiresResponse() {
        return requiresResponse;
    }

    public String getApplicationParticulars() {
        return applicationParticulars;
    }

    public UUID getOffenceId() {
        return offenceId;
    }
}
