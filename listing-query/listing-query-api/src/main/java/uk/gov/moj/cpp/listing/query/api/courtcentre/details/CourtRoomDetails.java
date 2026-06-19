package uk.gov.moj.cpp.listing.query.api.courtcentre.details;

import java.util.Objects;
import java.util.UUID;

@SuppressWarnings({"squid:S00107", "squid:S00121", "squid:S1067"})
public class CourtRoomDetails {

    private final UUID id;
    private final String courtRoomName;
    private final String welshCourtRoomName;

    public CourtRoomDetails(UUID id, String courtroomName, String welshCourtRoomName) {
        this.id = id;
        this.courtRoomName = courtroomName;
        this.welshCourtRoomName = welshCourtRoomName;
    }

    public UUID getId() {
        return id;
    }

    public String getCourtRoomName() {
        return courtRoomName;
    }

    public String getWelshCourtRoomName() {
        return welshCourtRoomName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CourtRoomDetails)) return false;
        CourtRoomDetails that = (CourtRoomDetails) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(courtRoomName, that.courtRoomName) &&
                Objects.equals(welshCourtRoomName, that.welshCourtRoomName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courtRoomName, welshCourtRoomName);
    }
    public static Builder courtRoomDetails() {
        return new CourtRoomDetails.Builder();
    }

    public static class Builder {
        private UUID id;
        private String courtRoomName;
        private String welshCourtRoomName;




        public CourtRoomDetails.Builder withCourtRoomName(final String courtRoomName) {
            this.courtRoomName = courtRoomName;
            return this;
        }


        public CourtRoomDetails.Builder withWelshCourtRoomName(final String welshCourtRoomName) {
            this.welshCourtRoomName = welshCourtRoomName;
            return this;
        }


        public CourtRoomDetails.Builder withId(final UUID id) {
            this.id = id;
            return this;
        }

        public CourtRoomDetails build() {
            return new CourtRoomDetails(id, courtRoomName, welshCourtRoomName);
        }

    }
}
