package uk.gov.moj.cpp.listing.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;

@Event("listing.events.court-room-added")
@JsonInclude(value = Include.NON_NULL)
public class CourtRoomAdded {

    private final String id;
    private final String courtCentre;
    private final String courtRoomName;

    public CourtRoomAdded(@JsonProperty(value = "id") final String id,
                          @JsonProperty(value = "courtCentre") final String courtCentre,
                          @JsonProperty(value = "courtRoomName") final String courtRoomName) {
        this.id = id;
        this.courtCentre = courtCentre;
        this.courtRoomName = courtRoomName;
    }

    public String getId() {
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
        CourtRoomAdded that = (CourtRoomAdded) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(courtCentre, that.courtCentre) &&
                Objects.equals(courtRoomName, that.courtRoomName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courtCentre, courtRoomName);
    }
}
