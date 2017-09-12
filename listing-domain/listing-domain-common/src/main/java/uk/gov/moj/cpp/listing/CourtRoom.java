package uk.gov.moj.cpp.listing;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value=Include.NON_NULL)
public class CourtRoom implements Serializable {

    private final String id;
    private final String courtCentre;
    private final String courtRoomName;

    @JsonCreator
    public CourtRoom(@JsonProperty(value = "id") final String id,
                     @JsonProperty(value = "courtCentre") final String courtCentre,
                     @JsonProperty(value = "courtRoomName") final String courtRoomName) {
        this.id = id;
        this.courtCentre = courtCentre;
        this.courtRoomName = courtRoomName;
    }

    public String getId() { return id; }

    public String getCourtCentre() { return courtCentre; }

    public String getRoomName() { return courtRoomName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourtRoom courtRoom = (CourtRoom) o;
        return Objects.equals(id, courtRoom.id) &&
                Objects.equals(courtCentre, courtRoom.courtCentre) &&
                Objects.equals(courtRoomName, courtRoom.courtRoomName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courtCentre, courtRoomName);
    }
}
