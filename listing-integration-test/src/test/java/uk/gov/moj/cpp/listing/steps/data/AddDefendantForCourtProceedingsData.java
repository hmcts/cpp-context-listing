package uk.gov.moj.cpp.listing.steps.data;


import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ListHearingRequest;

import java.util.List;

public class AddDefendantForCourtProceedingsData {
    private final List<Defendant> defendants;
    private final List<ListHearingRequest> listHearingRequests ;

    public AddDefendantForCourtProceedingsData(final List<Defendant> defendants, final List<ListHearingRequest> listHearingRequests) {
        this.defendants = defendants;
        this.listHearingRequests = listHearingRequests;
    }

    public List<Defendant> getDefendants() {
        return defendants;
    }

    public List<ListHearingRequest> getListHearingRequests() {
        return listHearingRequests;
    }

    public static Builder addDefendantForCourtProceedingsData() {
        return new AddDefendantForCourtProceedingsData.Builder();
    }


    public static class Builder {
        private List<Defendant> defendants;
        private List<ListHearingRequest> listHearingRequests;

        public Builder withDefendant(final List<Defendant> defendant) {
            this.defendants =  defendant;
            return this;
        }

        public Builder withListHearingRequest(final List<ListHearingRequest> listHearingRequest){
            this.listHearingRequests = listHearingRequest ;
            return this ;
        }

        public AddDefendantForCourtProceedingsData build() {
            return new AddDefendantForCourtProceedingsData(defendants, listHearingRequests);
        }
    }
}
