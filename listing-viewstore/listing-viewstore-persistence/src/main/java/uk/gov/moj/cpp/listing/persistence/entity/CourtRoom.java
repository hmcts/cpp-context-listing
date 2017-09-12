package uk.gov.moj.cpp.listing.persistence.entity;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "court_room")
public class CourtRoom {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "court_centre")
    private String courtCentre;

    @Column(name = "court_room_name")
    private String courtRoomName;


    public CourtRoom() {
        // for JPA
    }

    public CourtRoom(final UUID id, final String courtCentre, final String courtRoomName) {
        this.id = id;
        this.courtRoomName = courtRoomName;
        this.courtCentre = courtCentre;
    }

    public UUID getId() {
        return id;
    }

    public String getCourtCentre() {
        return courtCentre;
    }

    public String getCourtRoomName() { return courtRoomName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CourtRoom courtRoom = (CourtRoom) o;

        return id.equals(courtRoom.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
