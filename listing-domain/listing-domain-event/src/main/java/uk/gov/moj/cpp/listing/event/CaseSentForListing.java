package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.listing.domain.Hearing;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.case-sent-for-listing")
@JsonInclude(value = Include.NON_NULL)
public class CaseSentForListing {

    private final String caseProgressionId;
    private final String urn;
    private final List<Hearing> hearings;

    public CaseSentForListing(@JsonProperty(value = "caseProgressionId") final String caseProgressionId,
                              @JsonProperty(value = "urn") final String urn,
                              @JsonProperty(value = "hearings") final List<Hearing> hearings) {
        this.caseProgressionId = caseProgressionId;
        this.urn = urn;
        this.hearings = hearings;
    }

    public String getCaseProgressionId() {
        return caseProgressionId;
    }

    public String getUrn() {
        return urn;
    }

    public List<Hearing> getHearings() {
        return hearings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseSentForListing that = (CaseSentForListing) o;
        return Objects.equals(caseProgressionId, that.caseProgressionId) &&
                Objects.equals(urn, that.urn) &&
                Objects.equals(hearings, that.hearings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseProgressionId, urn, hearings);
    }
}
