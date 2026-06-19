package uk.gov.moj.cpp.listing.domain.referencedata;

import java.util.Objects;
import java.util.UUID;

public class Courtroom {
    private final UUID id;
    private final Integer courtroomId;
    private final String venueName;
    private final String courtroomName;
    private final String welshVenueName;
    private final String welshCourtroomName;
    private final Integer venueId;

    public UUID getId() {
        return id;
    }

    public Integer getCourtroomId() {
        return courtroomId;
    }

    public String getVenueName() {
        return venueName;
    }

    public String getCourtroomName() {
        return courtroomName;
    }

    public String getWelshVenueName() {
        return welshVenueName;
    }

    public String getWelshCourtroomName() {
        return welshCourtroomName;
    }

    public Integer getVenueId() {
        return venueId;
    }

    public Courtroom(final UUID id, final Integer courtroomId, final String venueName, final String courtroomName, final String welshVenueName, final String welshCourtroomName, final Integer venueId) {
        this.id = id;
        this.courtroomId = courtroomId;
        this.venueName = venueName;
        this.courtroomName = courtroomName;
        this.welshVenueName = welshVenueName;
        this.welshCourtroomName = welshCourtroomName;
        this.venueId = venueId;
    }

    public static Builder courtroom() {
        return new Builder();
    }

    public static final class Builder {
        private UUID id;
        private Integer courtroomId;
        private String venueName;
        private String courtroomName;
        private String welshVenueName;
        private String welshCourtroomName;
        private Integer venueId;

        private Builder() {
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withCourtroomId(Integer courtroomId) {
            this.courtroomId = courtroomId;
            return this;
        }

        public Builder withVenueName(String venueName) {
            this.venueName = venueName;
            return this;
        }

        public Builder withCourtroomName(String courtroomName) {
            this.courtroomName = courtroomName;
            return this;
        }

        public Builder withWelshVenueName(String welshVenueName) {
            this.welshVenueName = welshVenueName;
            return this;
        }

        public Builder withWelshCourtroomName(String welshCourtroomName) {
            this.welshCourtroomName = welshCourtroomName;
            return this;
        }

        public Builder withVenueId(Integer venueId) {
            this.venueId = venueId;
            return this;
        }

        public Courtroom build() {
            return new Courtroom(id, courtroomId, venueName, courtroomName, welshVenueName, welshCourtroomName, venueId);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof final Courtroom courtroom)) return false;
        return Objects.equals(getId(), courtroom.getId()) && Objects.equals(getCourtroomId(), courtroom.getCourtroomId()) && Objects.equals(getVenueName(), courtroom.getVenueName()) && Objects.equals(getCourtroomName(), courtroom.getCourtroomName()) && Objects.equals(getWelshVenueName(), courtroom.getWelshVenueName()) && Objects.equals(getWelshCourtroomName(), courtroom.getWelshCourtroomName()) && Objects.equals(getVenueId(), courtroom.getVenueId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getCourtroomId(), getVenueName(), getCourtroomName(), getWelshVenueName(), getWelshCourtroomName(), getVenueId());
    }

    @Override
    public String toString() {
        return "Courtroom{" +
                "id=" + id +
                ", courtroomId=" + courtroomId +
                ", venueName='" + venueName + '\'' +
                ", courtroomName='" + courtroomName + '\'' +
                ", welshVenueName='" + welshVenueName + '\'' +
                ", welshCourtroomName='" + welshCourtroomName + '\'' +
                ", venueId=" + venueId +
                '}';
    }
}
