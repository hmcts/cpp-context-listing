package uk.gov.moj.cpp.listing.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.listing.domain.Hearing;

import java.time.LocalDate;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("listing.events.case-sent-for-listing")
@JsonInclude(value = Include.NON_NULL)
public class CaseSentForListing {

    private final String caseId;
    private final String urn;
    private final LocalDate sendingCommittalDate;
    private final Hearing hearing;

    public CaseSentForListing(@JsonProperty(value = "caseId") final String caseId,
                              @JsonProperty(value = "urn") final String urn,
                              @JsonProperty(value = "sendingCommittalDate") final LocalDate
                                      sendingCommittalDate,
                              @JsonProperty(value = "hearing") final Hearing  hearing) {
        this.caseId = caseId;
        this.urn = urn;
        this.sendingCommittalDate = sendingCommittalDate;
        this.hearing = hearing;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }

    public LocalDate getSendingCommittalDate() {
        return sendingCommittalDate;
    }

    public Hearing getHearing() {
        return hearing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseSentForListing that = (CaseSentForListing) o;
        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(urn, that.urn) &&
                Objects.equals(sendingCommittalDate, that.sendingCommittalDate) &&
                Objects.equals(hearing, that.hearing);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, urn, sendingCommittalDate, hearing);
    }
}
