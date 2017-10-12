package uk.gov.moj.cpp.listing.query.view.hearing;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@Event("listing.events.search.hearings")
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class HearingSummary implements Serializable {


    private final UUID id;

    private final LocalDate date;

    private final Integer estimate;

    private final String type;

    private final Set<DefendantSummary> defendants;

    public HearingSummary(final UUID id, final LocalDate date, final Integer estimate,
                          final String type, final  Set<DefendantSummary> defendants ){
        this.id = id;
        this.date = date;
        this.estimate = estimate;
        this.type = type;
        this.defendants = defendants;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public Integer getEstimate() {
        return estimate;
    }

    public String getType() {
        return type;
    }

    public Set<DefendantSummary> getDefendants() {
        return defendants;
    }
}
