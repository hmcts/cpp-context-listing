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

    public CourtApplicationData(UUID id, UUID linkedCaseId, UUID parentApplicationId, ApplicantRespondentData applicant, ApplicantRespondentData respondent, String type, Boolean requiresResponse) {
        this.id = id;
        this.linkedCaseId = linkedCaseId;
        this.parentApplicationId = parentApplicationId;
        this.applicant = applicant;
        this.respondent = respondent;
        this.type = type;
        this.requiresResponse = requiresResponse;
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

    public ApplicantRespondentData getRespondent() {
        return respondent;
    }

    public String getType() {
        return type;
    }

    public Boolean getRequiresResponse() {
        return requiresResponse;
    }
}
