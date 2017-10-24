package uk.gov.moj.cpp.listing.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class Hearing implements Serializable {

    private final String id;
    private final String courtCentreId;
    private final String type;
    private final LocalDate startDate;
    private final int estimateMinutes;
    private final boolean allocated;
    private final List<Defendant> defendants;

    @JsonCreator
    public Hearing(@JsonProperty(value = "id") final String id,
                   @JsonProperty(value = "courtCentreId") final String courtCentreId,
                   @JsonProperty(value = "type") final String type,
                   @JsonProperty(value = "startDate") final LocalDate startDate,
                   @JsonProperty(value = "estimateMinutes") final int estimateMinutes,
                   @JsonProperty(value = "allocated") final boolean allocated,
                   @JsonProperty(value = "defendants") final List<Defendant> defendants
                   ) {
        this.id = id;
        this.courtCentreId = courtCentreId;
        this.type = type;
        this.startDate = startDate;
        this.estimateMinutes = estimateMinutes;
        this.allocated = allocated;
        this.defendants = new ArrayList(defendants);
    }

    public boolean isAllocated() {
        return allocated;
    }

    public String getId() { return id; }

    public String getCourtCentreId() { return courtCentreId; }

    public String getType() { return type; }

    public LocalDate getStartDate() { return startDate; }

    public int getEstimateMinutes() { return estimateMinutes; }

    public List<Defendant> getDefendants() {
        return new ArrayList(defendants);
    }
}
