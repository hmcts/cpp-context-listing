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

    public static HearingsData hearingsData(final String jurisdictionType) {
        return new HearingsData(HearingsDataFactory.hearingsData(jurisdictionType));
    }

    public static HearingsData hearingsData(final UUID hearingId) {
        return new HearingsData(HearingsDataFactory.hearingsData(hearingId));
    }

    public static HearingsData hearingsDataForWeekCommencing(final LocalDate startDate, final Integer duration) {
        return new HearingsData(HearingsDataFactory.hearingsDataForWeekCommencing(startDate, duration));
    }

    public static HearingsData hearingsDataForBookedSlot() {
        return new HearingsData(HearingsDataFactory.hearingsDataForBookedSlot());
    }

    public static HearingsData hearingsDataWithLegalEntity() {
        return new HearingsData(HearingsDataFactory.hearingsDataWithLegalEntity());
    }

    public static HearingsData singleHearingData() {
        return new HearingsData(HearingsDataFactory.singleHearingData());
    }

    public static HearingsData singleHearingDataMultipleCasesWithSingleOffence() {
        return new HearingsData(HearingsDataFactory.singleHearingDataSingleOffence());
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

    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(final Integer numberOfHearings) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(numberOfHearings));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final UUID courtCentreId) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary(courtCentreId));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary(courtCentreId, courtRoomId, judiciaryType));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(final UUID courtCentreId, final String judiciaryType) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCentreId, judiciaryType));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final CaseAndDefendantData caseAndDefendantData) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData));
    }

    public static HearingsData hearingsDataStandaloneApplication() {
        return new HearingsData(HearingsDataFactory.hearingsDataStandaloneApplication());
    }

    public static HearingsData hearingsDataWithShadowListedOffences() {
        return new HearingsData(HearingsDataFactory.hearingsDataWithShadowListedOffences());
    }


    public HearingsData combine(final HearingsData moreHearingsData) {
        final List<HearingData> combinedHearingsData = new ArrayList<>();
        combinedHearingsData.addAll(this.getHearingData());
        combinedHearingsData.addAll(moreHearingsData.getHearingData());
        return new HearingsData(combinedHearingsData);
    }

    private HearingsData(final List<HearingData> hearingData) {
        this.hearingData = hearingData;
    }

    private final List<HearingData> hearingData;

    public List<HearingData> getHearingData() {
        return hearingData;
    }

}
