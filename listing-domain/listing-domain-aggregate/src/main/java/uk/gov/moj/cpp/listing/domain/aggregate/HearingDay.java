package uk.gov.moj.cpp.listing.domain.aggregate;

import static java.lang.Boolean.FALSE;
import static java.util.Objects.isNull;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067", "squid:S1948", "PMD.BeanMembersShouldSerialize"})
public class HearingDay implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID courtScheduleId;

    private final Integer durationMinutes;

    private final ZonedDateTime endTime;

    private final LocalDate hearingDate;

    private final Integer sequence;

    private final ZonedDateTime startTime;

    private final Boolean isCancelled;

    public HearingDay(final Integer durationMinutes, final ZonedDateTime endTime, final LocalDate hearingDate, final Integer sequence, final ZonedDateTime startTime, final UUID courtScheduleId, final Boolean isCancelled) {
        this.durationMinutes = durationMinutes;
        this.endTime = endTime;
        this.hearingDate = hearingDate;
        this.sequence = sequence;
        this.startTime = startTime;
        this.courtScheduleId = courtScheduleId;
        this.isCancelled = isCancelled;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public Integer getSequence() {
        return sequence;
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public UUID getCourtScheduleId() {
        return courtScheduleId;
    }

    public Boolean isCancelled() {
        return isNull(isCancelled) ? FALSE : isCancelled;
    }

    public static Builder hearingDay() {
        return new Builder();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final HearingDay that = (HearingDay) o;
        return Objects.equals(getCourtScheduleId(), that.getCourtScheduleId()) &&
                Objects.equals(getDurationMinutes(), that.getDurationMinutes()) &&
                Objects.equals(getEndTime(), that.getEndTime()) &&
                Objects.equals(getHearingDate(), that.getHearingDate()) &&
                Objects.equals(getSequence(), that.getSequence()) &&
                Objects.equals(getStartTime(), that.getStartTime()) &&
                Objects.equals(isCancelled(), that.isCancelled());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCourtScheduleId(), getDurationMinutes(), getEndTime(), getHearingDate(), getSequence(), getStartTime(), isCancelled());
    }

    @Override
    public String toString() {
        return "HearingDay{" +
                "courtScheduleId=" + courtScheduleId +
                ", durationMinutes=" + durationMinutes +
                ", endTime=" + endTime +
                ", hearingDate=" + hearingDate +
                ", sequence=" + sequence +
                ", startTime=" + startTime +
                ", isCancelled=" + isCancelled +
                '}';
    }


    @SuppressWarnings({"PMD.BeanMembersShouldSerialize"})
    public static class Builder {
        private Integer durationMinutes;

        private ZonedDateTime endTime;

        private LocalDate hearingDate;

        private Integer sequence;

        private ZonedDateTime startTime;

        private UUID courtScheduleId;

        private Boolean isCancelled;

        public Builder withDurationMinutes(final Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public Builder withEndTime(final ZonedDateTime endTime) {
            this.endTime = endTime;
            return this;
        }

        public Builder withHearingDate(final LocalDate hearingDate) {
            this.hearingDate = hearingDate;
            return this;
        }

        public Builder withSequence(final Integer sequence) {
            this.sequence = sequence;
            return this;
        }

        public Builder withStartTime(final ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withCourtScheduleId(final UUID courtScheduleId) {
            this.courtScheduleId = courtScheduleId;
            return this;
        }

        public Builder withIsCancelled(final Boolean isCancelled) {
            this.isCancelled = isCancelled;
            return this;
        }

        public HearingDay build() {
            return new HearingDay(durationMinutes, endTime, hearingDate, sequence, startTime, courtScheduleId, isCancelled);
        }
    }
}
