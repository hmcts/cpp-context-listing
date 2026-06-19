package uk.gov.moj.cpp.listing.steps.data;

import java.util.UUID;

public class CourtApplicationData {
    private final UUID id;
    private final UUID linkedCaseId;
    private final UUID parentApplicationId;
    private final CourtApplicationPartyData applicant;
    private final CourtApplicationPartyData respondent;
    private final CourtApplicationPartyData subject;
    private final String type;
    private final Boolean requiresResponse;
    private final Boolean restrictCourtApplicationType;
    private final Boolean restrictFromCourtList;
    private final Boolean isEjected;
    private final String applicationParticulars;
    private final UUID offenceId;

    public CourtApplicationData(UUID id, UUID linkedCaseId, UUID parentApplicationId, CourtApplicationPartyData applicant, CourtApplicationPartyData respondent, String type, Boolean requiresResponse,
                                final Boolean restrictCourtApplicationType, final Boolean restrictFromCourtList, final Boolean isEjected, final String applicationParticulars, final UUID offenceId) {
        this(id, linkedCaseId, parentApplicationId, applicant, respondent, null, type, requiresResponse, restrictCourtApplicationType, restrictFromCourtList, isEjected, applicationParticulars, offenceId);
    }

    public CourtApplicationData(UUID id, UUID linkedCaseId, UUID parentApplicationId, CourtApplicationPartyData applicant, CourtApplicationPartyData respondent, CourtApplicationPartyData subject, String type, Boolean requiresResponse,
                                final Boolean restrictCourtApplicationType, final Boolean restrictFromCourtList, final Boolean isEjected, final String applicationParticulars, final UUID offenceId) {
        this.id = id;
        this.linkedCaseId = linkedCaseId;
        this.parentApplicationId = parentApplicationId;
        this.applicant = applicant;
        this.respondent = respondent;
        this.subject = subject;
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

    public CourtApplicationPartyData getApplicant() {
        return applicant;
    }

    public Boolean getEjected() {
        return isEjected;
    }

    public CourtApplicationPartyData getRespondent() {
        return respondent;
    }

    public CourtApplicationPartyData getSubject() {
        return subject;
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
