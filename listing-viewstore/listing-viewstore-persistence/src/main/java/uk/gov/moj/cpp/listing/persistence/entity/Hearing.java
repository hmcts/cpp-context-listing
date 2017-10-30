package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "hearing")
public class Hearing implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "start_time")
    private LocalTime startTime;

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

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "case_id")
    private ListingCase listingCase;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "hearing")
    private Set<Defendant> defendants = new LinkedHashSet<>();

    @Column(name = "allocated")
    private Boolean allocated;

    @Column(name = "not_before")
    private boolean notBefore;

    public Hearing() {
        // for JPA
    }

    public Hearing(final UUID id, final ListingCase listingCase, final Boolean allocated,
                   final Set<Defendant> defendants, boolean notBefore,
                   final HearingDetails hearingDetails) {
        this.id = id;
        this.startDate = hearingDetails.getStartDate();
        this.startTime = hearingDetails.getStartTime();
        this.estimateMinutes = hearingDetails.getEstimateMinutes();
        this.type = hearingDetails.getType();
        this.courtCentreId = hearingDetails.getCourtCentreId();
        this.courtRoomId = hearingDetails.getCourtRoomId();
        this.judgeId = hearingDetails.getJudgeId();
        this.notBefore = notBefore;
        this.listingCase = listingCase;
        this.allocated = allocated;
        this.defendants = defendants;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
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

    public ListingCase getListingCase() { return listingCase; }

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

    public boolean getNotBefore() {
        return notBefore;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Hearing hearing = (Hearing) o;

        return id.equals(hearing.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    public static class HearingDetails {
        private LocalDate startDate;
        private LocalTime startTime;
        private Integer estimateMinutes;
        private String type;
        private UUID courtCentreId;
        private UUID courtRoomId;
        private UUID judgeId;

        public HearingDetails(LocalDate startDate, LocalTime startTime, Integer estimateMinutes, String type,
                              UUID courtCentreId, UUID courtRoomId, UUID judgeId) {
            this.startDate = startDate;
            this.startTime = startTime;
            this.estimateMinutes = estimateMinutes;
            this.type = type;
            this.courtCentreId = courtCentreId;
            this.courtRoomId = courtRoomId;
            this.judgeId = judgeId;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public LocalTime getStartTime() {
            return startTime;
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
    }

}
