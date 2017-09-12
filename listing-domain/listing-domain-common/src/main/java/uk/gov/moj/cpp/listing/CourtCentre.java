package uk.gov.moj.cpp.listing;

import java.io.Serializable;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value=Include.NON_NULL)
public class CourtCentre implements Serializable {

    private final String id;
    private final String courtCentreName;

    @JsonCreator
    public CourtCentre(@JsonProperty(value = "id") final String id,
                     @JsonProperty(value = "courtCentreName") final String courtCentreName) {
        this.id = id;
        this.courtCentreName = courtCentreName;
    }

    public String getId() { return id; }

    public String getCourtCentreName() { return courtCentreName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourtCentre courtCentre = (CourtCentre) o;
        return Objects.equals(id, courtCentre.id) &&
                Objects.equals(courtCentreName, courtCentre.courtCentreName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courtCentreName);
    }
}
