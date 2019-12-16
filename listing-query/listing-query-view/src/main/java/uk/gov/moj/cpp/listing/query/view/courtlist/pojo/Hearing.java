package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class Hearing {

    private LocalDate startTime;

    private LocalDate endTime;

    private JsonObject hearingType;

    private Optional<UUID> committingCourtCentreId;

    private boolean restrictFromCourtList;

    private Optional<CaseDetails> caseDetails;

    private Optional<CourtApplicationDetails> courtApplicationDetails;

    public LocalDate getStartTime() {
        return startTime;
    }

    public void setStartTime(final LocalDate startTime) {
        this.startTime = startTime;
    }

    public LocalDate getEndTime() {
        return endTime;
    }

    public void setEndTime(final LocalDate endTime) {
        this.endTime = endTime;
    }

    public JsonObject getHearingType() {
        return hearingType;
    }

    public void setHearingType(final JsonObject hearingType) {
        this.hearingType = hearingType;
    }

    public Optional<UUID> getCommittingCourtCentreId() {
        return committingCourtCentreId;
    }

    public void setCommittingCourtCentreId(final Optional<UUID> committingCourtCentreId) {
        this.committingCourtCentreId = committingCourtCentreId;
    }

    public boolean isRestrictFromCourtList() {
        return restrictFromCourtList;
    }

    public void setRestrictFromCourtList(final boolean restrictFromCourtList) {
        this.restrictFromCourtList = restrictFromCourtList;
    }

    public Optional<CaseDetails> getCaseDetails() {
        return caseDetails;
    }

    public void setCaseDetails(final Optional<CaseDetails> caseDetails) {
        this.caseDetails = caseDetails;
    }

    public Optional<CourtApplicationDetails> getCourtApplicationDetails() {
        return courtApplicationDetails;
    }

    public void setCourtApplicationDetails(final Optional<CourtApplicationDetails> courtApplicationDetails) {
        this.courtApplicationDetails = courtApplicationDetails;
    }
}
