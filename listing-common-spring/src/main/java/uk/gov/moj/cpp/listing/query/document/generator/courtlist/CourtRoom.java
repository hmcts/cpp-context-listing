package uk.gov.moj.cpp.listing.query.document.generator.courtlist;

import java.util.List;
import java.util.Objects;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067"})
public class CourtRoom {

    private String courtRoomName;
    private String welshCourtRoomName;
    private String judiciaryNames;
    private String welshJudiciaryNames;
    private List<Timeslot> timeslots;

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public String getJudiciaryNames() {
        return judiciaryNames;
    }

    public List<Timeslot> getTimeslots() {
        return timeslots;
    }

    public String getWelshCourtRoomName() {
        return welshCourtRoomName;
    }

    public String getWelshJudiciaryNames() {
        return welshJudiciaryNames;
    }

    public static Builder courtRoom() {
        return new CourtRoom.Builder();
    }


    public static final class Builder {
        private String courtRoomName;
        private String welshCourtRoomName;
        private String judiciaryNames;
        private String welshJudiciaryNames;

        private List<Timeslot> timeslots;

        private Builder() {
        }

        public Builder withCourtRoomName(String courtRoomName) {
            this.courtRoomName = courtRoomName;
            return this;
        }

        public Builder withWelshCourtRoomName(String welshCourtRoomName) {
            this.welshCourtRoomName = welshCourtRoomName;
            return this;
        }

        public Builder withJudiciaryNames(String judiciaryNames) {
            this.judiciaryNames = judiciaryNames;
            return this;
        }


        public Builder withWelshJudiciaryNames(String welshJudiciaryNames) {
            this.welshJudiciaryNames = welshJudiciaryNames;
            return this;
        }

        public Builder withTimeslots(List<Timeslot> timeslots) {
            this.timeslots = timeslots;
            return this;
        }

        public CourtRoom build() {
            final CourtRoom courtRoom = new CourtRoom();
            courtRoom.judiciaryNames = this.judiciaryNames;
            courtRoom.welshJudiciaryNames = this.welshJudiciaryNames;
            courtRoom.courtRoomName = this.courtRoomName;
            courtRoom.welshCourtRoomName = this.welshCourtRoomName;
            courtRoom.timeslots = this.timeslots;
            return courtRoom;
        }
    }

    @Override
    public String toString() {
        return "CourtRoom{" +
                "courtRoomName='" + courtRoomName + '\'' +
                ", welshCourtRoomName='" + welshCourtRoomName + '\'' +
                ", judiciaryNames='" + judiciaryNames + '\'' +
                ", welshJudiciaryNames='" + welshJudiciaryNames + '\'' +
                ", timeslots=" + timeslots +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CourtRoom)){
            return false;
        }
        CourtRoom courtRoom = (CourtRoom) o;
        return Objects.equals(courtRoomName, courtRoom.courtRoomName) &&
                Objects.equals(welshCourtRoomName, courtRoom.welshCourtRoomName) &&
                Objects.equals(judiciaryNames, courtRoom.judiciaryNames) &&
                Objects.equals(welshJudiciaryNames, courtRoom.welshJudiciaryNames) &&
                Objects.equals(timeslots, courtRoom.timeslots);
    }

    @Override
    public int hashCode() {
        return Objects.hash(courtRoomName, welshCourtRoomName, judiciaryNames, welshJudiciaryNames, timeslots);
    }
}
