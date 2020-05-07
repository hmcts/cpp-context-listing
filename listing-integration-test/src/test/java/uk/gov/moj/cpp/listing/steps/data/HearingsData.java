package uk.gov.moj.cpp.listing.steps.data;

import uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HearingsData {

    public static HearingsData hearingsData() {
        return new HearingsData(HearingsDataFactory.hearingsData());
    }

    public static HearingsData hearingsDataForWeekCommencing() {
        return new HearingsData(HearingsDataFactory.hearingsDataForWeekCommencing());
    }

    public static HearingsData hearingsDataWithLegalEntity() {
        return new HearingsData(HearingsDataFactory.hearingsDataWithLegalEntity());
    }

    public static HearingsData singleHearingData() {
        return new HearingsData(HearingsDataFactory.singleHearingData());
    }

    public static HearingsData hearingsDataForWeekCommencing(final UUID hearingId, final LocalDate hearingEndDate,
                                                             final UUID courtRoomId, final LocalDate weekCommencingStartDate,
                                                             final LocalDate weekCommencingEndDate, final LocalDate startDate) {
        return new HearingsData(HearingsDataFactory.hearingsDataForWeekCommencing(hearingId, hearingEndDate, courtRoomId, weekCommencingStartDate, weekCommencingEndDate, startDate));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary() {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary());
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final String jurisdictionType) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary(jurisdictionType));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate() {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate());
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final UUID courtCentreId) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary(courtCentreId));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final UUID courtCentreId, final String judiciaryType) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary(courtCentreId, judiciaryType));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(final UUID courtCentreId, final String judiciaryType) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, judiciaryType));
    }

    public static HearingsData hearingsDataStandaloneApplication() {
        return new HearingsData(HearingsDataFactory.hearingsDataStandaloneApplication());
    }


    public HearingsData combine(HearingsData moreHearingsData) {
        final List<HearingData> combinedHearingsData = new ArrayList<>();
        combinedHearingsData.addAll(this.getHearingData());
        combinedHearingsData.addAll(moreHearingsData.getHearingData());
        return new HearingsData(combinedHearingsData);
    }

    private HearingsData(List<HearingData> hearingData) {
        this.hearingData = hearingData;
    }

    private final List<HearingData> hearingData;

    public List<HearingData> getHearingData() {
        return hearingData;
    }

}
