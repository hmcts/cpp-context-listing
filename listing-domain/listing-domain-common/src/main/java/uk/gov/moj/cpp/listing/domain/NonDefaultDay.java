package uk.gov.moj.cpp.listing.domain;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1948", "squid:S1067", "pmd:BeanMembersShouldSerialize"})
public class NonDefaultDay implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String courtScheduleId;

    private final Integer courtRoomId;

    private final Integer duration;

    private final String oucode;

    private final String session;

    private final ZonedDateTime startTime;

    public NonDefaultDay(final Optional<String> courtScheduleId, final Optional<Integer> courtRoomId, final Optional<Integer> duration, final Optional<String> oucode, final Optional<String> session, final ZonedDateTime startTime) {
        this.courtScheduleId = courtScheduleId.orElse(null);
        this.courtRoomId = courtRoomId.orElse(null);
        this.duration = duration.orElse(null);
        this.oucode = oucode.orElse(null);
        this.session = session.orElse(null);
        this.startTime = startTime;
    }

    public Optional<String> getCourtScheduleId() {
        return courtScheduleId != null ? of(courtScheduleId) : empty();
    }

    public Optional<Integer> getCourtRoomId() {
        return courtRoomId != null ? of(courtRoomId) : empty();
    }

    public Optional<Integer> getDuration() {
        return duration != null ? of(duration) : empty();
    }

    public Optional<String> getOucode() {
        return oucode != null ? of(oucode) : empty();
    }

    public Optional<String> getSession() {
        return session != null ? of(session) : empty();
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public static Builder nonDefaultDay() {
        return new NonDefaultDay.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final NonDefaultDay that = (NonDefaultDay) obj;

        return Objects.equals(this.courtScheduleId, that.courtScheduleId) &&
                Objects.equals(this.courtRoomId, that.courtRoomId) &&
                Objects.equals(this.duration, that.duration) &&
                Objects.equals(this.oucode, that.oucode) &&
                Objects.equals(this.session, that.session) &&
                Objects.equals(this.startTime, that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courtScheduleId, courtRoomId, duration, oucode, session, startTime);
    }

    @Override
    public String toString() {
        return "NonDefaultDay{" +
                "courtScheduleId='" + courtScheduleId + "'," +
                "courtRoomId='" + courtRoomId + "'," +
                "duration='" + duration + "'," +
                "oucode='" + oucode + "'," +
                "session='" + session + "'," +
                "startTime='" + startTime + "'" +
                "}";
    }

    public static class Builder {
        private Optional<String> courtScheduleId = empty();

        private Optional<Integer> courtRoomId = empty();

        private Optional<Integer> duration;

        private Optional<String> oucode = empty();

        private Optional<String> session = empty();

        private ZonedDateTime startTime;

        public Builder withCourtScheduleId(final Optional<String> courtScheduleId) {
            this.courtScheduleId = courtScheduleId;
            return this;
        }

        public Builder withCourtRoomId(final Optional<Integer> courtroomId) {
            this.courtRoomId = courtroomId;
            return this;
        }

        public Builder withDuration(final Optional<Integer> duration) {
            this.duration = duration;
            return this;
        }

        public Builder withOucode(final Optional<String> oucode) {
            this.oucode = oucode;
            return this;
        }

        public Builder withSession(final Optional<String> session) {
            this.session = session;
            return this;
        }

        public Builder withStartTime(final ZonedDateTime startTime) {
            this.startTime = startTime;
            return this;
        }

        public NonDefaultDay build() {
            return new NonDefaultDay(courtScheduleId, courtRoomId, duration, oucode, session, startTime);
        }
    }
}
