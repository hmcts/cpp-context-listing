package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory;

import java.util.List;

public class HearingsData {

    public static HearingsData hearingsData(){
        return new HearingsData(HearingsDataFactory.hearingsData());
    }

    public static HearingsData singleHearingData(){
        return new HearingsData(HearingsDataFactory.singleHearingData());
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(){
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary());
    }

    public static HearingsData hearingsDataStandaloneApplication(){
        return new HearingsData(HearingsDataFactory.hearingsDataStandaloneApplication());
    }

    private HearingsData(List<HearingData> hearingData) {
        this.hearingData = hearingData;
    }

    private final List<HearingData> hearingData;

    public List<HearingData> getHearingData() {
        return hearingData;
    }
}
