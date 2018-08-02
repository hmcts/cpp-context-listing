package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@SuppressWarnings("squid:S00107")
@Entity
@Table(name = "hearing")
public class Hearing implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "non_sitting_days")
    private String nonSittingDays;

    @Column(name = "start_times")
    private String startTimes;

    @Column(name = "estimate_minutes")
    private Integer estimateMinutes;

    @Column(name = "type")
    private String type;

    @Column(name = "court_centre_id")
    private UUID courtCentreId;

    @Column(name = "court_room_id")
    private UUID courtRoomId;

    @Column(name = "judge_id")
    private UUID judgeId;

    @Column(name = "case_id")
    private UUID listingCaseId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "hearing")
    private Set<Defendant> defendants = new LinkedHashSet<>();

    @Column(name = "allocated")
    private Boolean allocated;



    public Hearing() {
        // for JPA
    }

    public Hearing(final UUID id, final UUID listingCaseId, final Boolean allocated,
                   final Set<Defendant> defendants,
                   final HearingDetails hearingDetails) {
        this.id = id;
        this.startDate = hearingDetails.getStartDate();
        this.endDate = hearingDetails.getEndDate();
        this.startTimes = hearingDetails.getStartTimes();
        this.nonSittingDays = hearingDetails.getNonSittingDays();
        this.estimateMinutes = hearingDetails.getEstimateMinutes();
        this.type = hearingDetails.getType();
        this.courtCentreId = hearingDetails.getCourtCentreId();
        this.courtRoomId = hearingDetails.getCourtRoomId();
        this.judgeId = hearingDetails.getJudgeId();
        this.listingCaseId = listingCaseId;
        this.allocated = allocated;
        this.defendants = defendants;
    }




    public UUID getId() {
        return id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public  String getStartTimes() {
        return startTimes;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getNonSittingDays() {
        return nonSittingDays;
    }

    public Integer getEstimateMinutes() {
        return estimateMinutes;
    }

    public String getType() {
        return type;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public UUID getListingCaseId() { return listingCaseId; }

    public Boolean getAllocated() {
        return allocated;
    }

    public Set<Defendant> getDefendants() {
        return defendants;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public UUID getJudgeId() {
        return judgeId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Hearing hearing = (Hearing) o;
        return id.equals(hearing.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class HearingDetails {
        private LocalDate startDate;
        private LocalDate endDate;
        private String startTimes;
        private String nonSittingDays;
        private Integer estimateMinutes;
        private String type;
        private UUID courtCentreId;
        private UUID courtRoomId;
        private UUID judgeId;

        public HearingDetails(LocalDate startDate, String startTimes, Integer estimateMinutes, String type,
                              UUID courtCentreId, UUID courtRoomId, UUID judgeId, String nonSittingDays, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.nonSittingDays = nonSittingDays;
            this.startTimes = startTimes;
            this.estimateMinutes = estimateMinutes;
            this.type = type;
            this.courtCentreId = courtCentreId;
            this.courtRoomId = courtRoomId;
            this.judgeId = judgeId;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public String getStartTimes() {
            return startTimes;
        }

        public Integer getEstimateMinutes() {
            return estimateMinutes;
        }

        public String getType() {
            return type;
        }

        public UUID getCourtCentreId() {
            return courtCentreId;
        }

        public UUID getCourtRoomId() {
            return courtRoomId;
        }

        public UUID getJudgeId() {
            return judgeId;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public String getNonSittingDays() {
            return nonSittingDays;
        }
    }

}
