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

    private final String caseId;
    private final String urn;
    private final List<Hearing> hearings;

    public CaseSentForListing(@JsonProperty(value = "caseId") final String caseId,
                              @JsonProperty(value = "urn") final String urn,
                              @JsonProperty(value = "hearings") final List<Hearing> hearings) {
        this.caseId = caseId;
        this.urn = urn;
        this.hearings = hearings;
    }

    public String getCaseId() {
        return caseId;
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
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(urn, that.urn) &&
                Objects.equals(hearings, that.hearings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, urn, hearings);
    }
}
