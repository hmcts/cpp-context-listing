package uk.gov.moj.cpp.listing.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;

@Event("listing.events.court-centre-added")
@JsonInclude(value = Include.NON_NULL)
public class CourtCentreAdded {

    private final String id;
    private final String courtCentreName;

    public CourtCentreAdded(@JsonProperty(value = "id") final String id,
                            @JsonProperty(value = "courtCentreName") final String courtCentreName) {
        this.id = id;
        this.courtCentreName = courtCentreName;
    }

    public String getId() {
        return id;
    }

    public String getCourtCentreName() {
        return courtCentreName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourtCentreAdded that = (CourtCentreAdded) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(courtCentreName, that.courtCentreName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courtCentreName);
    }
}
