package uk.gov.moj.cpp.listing.query.view.courtlist.pojo;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

public class Hearing {

    private LocalDateTime startTime;

    private Optional<LocalDateTime> endTime;

    private JsonObject hearingType;

    private Optional<UUID> committingCourtCentreId;

    private boolean restrictFromCourtList;

    private Optional<CaseDetails> caseDetails;

    private Optional<CourtApplicationDetails> courtApplicationDetails;

    private boolean weekCommencing;

    private boolean hasVideoLink;

    private String videoLinkDetails;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public Optional<LocalDateTime> getEndTime() {
        return endTime;
    }

    public void setEndTime(final Optional<LocalDateTime> endTime) {
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

    public boolean isWeekCommencing() {
        return weekCommencing;
    }

    public void setWeekCommencing(final boolean weekCommencing) {
        this.weekCommencing = weekCommencing;
    }

    public boolean hasVideoLink() {
        return hasVideoLink;
    }

    public void setHasVideoLink(final boolean hasVideoLink) {
        this.hasVideoLink = hasVideoLink;
    }

    public String getVideoLinkDetails() {
        return videoLinkDetails;
    }

    public void setVideoLinkDetails(final String videoLinkDetails) {
        this.videoLinkDetails = videoLinkDetails;
    }
}
