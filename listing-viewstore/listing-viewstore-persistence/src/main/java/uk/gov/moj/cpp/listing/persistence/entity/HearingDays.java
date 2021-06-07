package uk.gov.moj.cpp.listing.persistence.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@SuppressWarnings({"squid:S1948","pmd:BeanMembersShouldSerialize","squid:S1067"})
@Entity
@Table(name = "hearing_days")
public class HearingDays implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID id;

    @Column(name = "sequence")
    private Integer sequence;

    @Column(name = "end_time")
    private ZonedDateTime endTime;

    @Column(name = "start_time")
    private ZonedDateTime startTime;

    @Column(name = "court_room_id")
    private UUID courtRoomId;

    @Column(name = "hearing_date")
    private LocalDate hearingDate;

    @Column(name = "court_centre_id")
    private UUID courtCentreId;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @ManyToOne
    @JoinColumn(name = "hearing_id", nullable = false)
    private Hearing hearing;


    public HearingDays() {
        // for JPA
    }

    public HearingDays(final UUID id,
                       final Integer sequence,
                       final ZonedDateTime endTime,
                       final ZonedDateTime startTime,
                       final UUID courtRoomId,
                       final LocalDate hearingDate,
                       final UUID courtCentreId,
                       final Integer durationMinutes,
                       final Hearing hearing) {
        this.id = id;
        this.sequence = sequence;
        this.endTime = endTime;
        this.startTime = startTime;
        this.courtRoomId = courtRoomId;
        this.hearingDate = hearingDate;
        this.courtCentreId = courtCentreId;
        this.durationMinutes = durationMinutes;
        this.hearing = hearing;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(final Integer sequence) {
        this.sequence = sequence;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final ZonedDateTime endTime) {
        this.endTime = endTime;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final ZonedDateTime startTime) {
        this.startTime = startTime;
    }

    public UUID getCourtRoomId() {
        return courtRoomId;
    }

    public void setCourtRoomId(final UUID courtRoomId) {
        this.courtRoomId = courtRoomId;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public void setHearingDate(final LocalDate hearingDate) {
        this.hearingDate = hearingDate;
    }

    public UUID getCourtCentreId() {
        return courtCentreId;
    }

    public void setCourtCentreId(final UUID courtCentreId) {
        this.courtCentreId = courtCentreId;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(final Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Hearing getHearing() {
        return hearing;
    }

    public void setHearing(final Hearing hearing) {
        this.hearing = hearing;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HearingDays that = (HearingDays) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(sequence, that.sequence) &&
                Objects.equals(endTime, that.endTime) &&
                Objects.equals(startTime, that.startTime) &&
                Objects.equals(courtRoomId, that.courtRoomId) &&
                Objects.equals(hearingDate, that.hearingDate) &&
                Objects.equals(courtCentreId, that.courtCentreId) &&
                Objects.equals(durationMinutes, that.durationMinutes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sequence, endTime, startTime, courtRoomId, hearingDate, courtCentreId, durationMinutes);
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings({"pmd:BeanMembersShouldSerialize"})
    public static class Builder {
        private UUID id;
        private Integer sequence;
        private ZonedDateTime endTime;
        private ZonedDateTime startTime;
        private UUID courtRoomId;
        private LocalDate hearingDate;
        private UUID courtCentreId;
        private Integer durationMinutes;
        private Hearing hearing;

        public static Builder aHearingDays() {
            return new Builder();
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withSequence(Integer sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder withEndTime(ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withStartTime(ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withCourtRoomId(UUID courtRoomId) {
            this.courtRoomId = courtRoomId;
            return this;
        }

        public Builder withHearingDate(LocalDate hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withCourtCentreId(UUID courtCentreId) {
            this.courtCentreId = courtCentreId;
            return this;
        }

        public Builder withDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public Builder withHearing(Hearing hearing) {
            this.hearing = hearing;
            return this;
        }

        public HearingDays build() {
            return new HearingDays(id, sequence, endTime, startTime, courtRoomId, hearingDate, courtCentreId, durationMinutes, hearing);
        }
    }
}
