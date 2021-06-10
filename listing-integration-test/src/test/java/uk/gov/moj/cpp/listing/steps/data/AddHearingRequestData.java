package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;

import java.util.List;


public class AddHearingRequestData {

    private CourtCentre courtCentre;

    private HearingType hearingType;

    private JurisdictionType jurisdictionType;

    private List<ListDefendantRequestData> listDefendantRequests;


    public AddHearingRequestData(final CourtCentre courtCentre, final HearingType hearingType, final JurisdictionType jurisdictionType,
                                 final List<ListDefendantRequestData> listDefendantRequests) {
        this.courtCentre = courtCentre;
        this.hearingType = hearingType;
        this.jurisdictionType = jurisdictionType;
        this.listDefendantRequests = listDefendantRequests;
    }

    public CourtCentre getCourtCentre() {
        return courtCentre;
    }

    public HearingType getHearingType() {
        return hearingType;
    }

    public JurisdictionType getJurisdictionType() {
        return jurisdictionType;
    }

    public List<ListDefendantRequestData> getListDefendantRequests() {
        return listDefendantRequests;
    }

    public static class Builder {

        private CourtCentre courtCentre;

        private HearingType hearingType;

        private JurisdictionType jurisdictionType;

        private List<ListDefendantRequestData> listDefendantRequests;

        public static Builder AddHearingRequestData() {
            return new Builder();
        }

        public Builder withCourtCentre(final CourtCentre courtCentre) {
            this.courtCentre = courtCentre;
            return this;
        }

        public Builder withHearingType(final HearingType hearingType){
            this.hearingType = hearingType ;
            return this ;
        }

        public Builder withJurisdictionType(final JurisdictionType jurisdictionType) {
            this.jurisdictionType = jurisdictionType;
            return this;
        }


        public AddHearingRequestData build() {
            return new AddHearingRequestData(courtCentre,hearingType,jurisdictionType,listDefendantRequests);
        }
    }
}

