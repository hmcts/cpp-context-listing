package uk.gov.moj.cpp.listing.steps.data;

import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.MAGISTRATES_JURISDICTION;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.singleHearingsData;
import static uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory.singleNotHmiEnabledHearingsData;

import uk.gov.moj.cpp.listing.steps.data.factory.HearingsDataFactory;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HearingsData {

    public static HearingsData hearingsData() {
        return new HearingsData(HearingsDataFactory.hearingsData());
    }

    public static HearingsData hearingsDataWithExParteOffence() {
        return new HearingsData(HearingsDataFactory.hearingsDataForCasesWithExParte());
    }

    public static HearingsData trialHearingsData() {
        return new HearingsData(HearingsDataFactory.trialHearingsData());
    }

    public static HearingsData notHmiEnabledHearingsData() {
        return new HearingsData(HearingsDataFactory.notHmiEnabledHearingsData());
    }

    public static HearingsData mixtureHmiEnabledAndNotHmiEnabledHearingsData() {
        List<HearingData> hmiEnabledHearingData = singleHearingsData();
        List<HearingData> notHmiEnabledHearingData = singleNotHmiEnabledHearingsData();
        notHmiEnabledHearingData.addAll(hmiEnabledHearingData);
        return new HearingsData(notHmiEnabledHearingData);
    }

    public static HearingsData nextHearingsData(final List<HearingData> hearings) {
        return new HearingsData(HearingsDataFactory.hearingsData(hearings));
    }

    public static HearingsData nextAllocatedHearingsData(final List<HearingData> hearings) {
        return new HearingsData(List.of(HearingsDataFactory.allocatedHearingsData(hearings)));
    }

    public static HearingsData hearingsData(final String jurisdictionType) {
        return new HearingsData(HearingsDataFactory.hearingsData(jurisdictionType));
    }

    public static HearingsData notHmiEnabledHearingsData(final String jurisdictionType) {
        return new HearingsData(HearingsDataFactory.notHmiEnabledHearingsData(jurisdictionType));
    }

    public static HearingsData hearingsData(final UUID hearingId) {
        return new HearingsData(HearingsDataFactory.hearingsData(hearingId));
    }

    public static HearingsData hearingsDataForWeekCommencing(final LocalDate startDate, final Integer duration) {
        return new HearingsData(HearingsDataFactory.hearingsDataForWeekCommencing(startDate, duration));
    }

    public static HearingsData hearingsDataForWeekCommencing(final LocalDate startDate, final Integer duration, UUID courtCenterId, UUID courtRoomId, String roles) {
        return new HearingsData(HearingsDataFactory.hearingsDataForWeekCommencing(startDate, duration, courtCenterId, courtRoomId, roles));
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

    public static HearingsData singleHearingDataForHMI() {
        return new HearingsData(HearingsDataFactory.singleHearingDataForHMI());
    }

    public static HearingsData singleHearingDataMultipleCasesWithSingleOffence() {
        return new HearingsData(HearingsDataFactory.singleHearingDataSingleOffence());
    }

    public static HearingsData singleHearingDataSingleCaseWithTwoDefendantAndTwoOffence(final UUID courtCenterId, final UUID courtRoomId, final String judiciaryType, final String court, final Integer numberOfCases) {
        return new HearingsData(HearingsDataFactory.singleHearingDataTwoDefendantWithCourtRoomCourtCenterAndJudiciaryType(courtCenterId, courtRoomId, judiciaryType, court, numberOfCases));
    }

    public static HearingsData singleHearingDataSingleCaseWithSingleOffence(final UUID courtCenterId, final UUID courtRoomId, final String judiciaryType, final String court, final Integer numberOfCases) {
        return new HearingsData(HearingsDataFactory.singleHearingDataSingleOffenceWithCourtRoomCourtCenterAndJudiciaryType(courtCenterId, courtRoomId, judiciaryType, court, numberOfCases));
    }

    public static HearingsData singleHearingDataSingleCaseMultipleOffences() {
        return new HearingsData(HearingsDataFactory.singleHearingDataMultipleOffences());
    }

    public static HearingsData singleHearingDataSingleCaseMultipleDefendents() {
        return new HearingsData(HearingsDataFactory.singleHearingDataMultipleDefendants());
    }

    public static HearingsData hearingsDataForWeekCommencing(final UUID hearingId, final LocalDate hearingEndDate,
                                                             final UUID courtRoomId, final LocalDate weekCommencingStartDate,
                                                             final LocalDate weekCommencingEndDate, final LocalDate startDate) {
        return new HearingsData(HearingsDataFactory.hearingsDataForWeekCommencing(hearingId, hearingEndDate, courtRoomId, weekCommencingStartDate, weekCommencingEndDate, startDate));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary() {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary());
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate(final UUID courtCenter) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCenter, MAGISTRATES_JURISDICTION));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryWithCourtCenterForMagistrate(final UUID courtCenter,
                                                                                                      final UUID courtRoomId,
                                                                                                      final LocalDate hearingEndDate,
                                                                                                      final ZonedDateTime hearingStartTime) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(courtCenter, MAGISTRATES_JURISDICTION, courtRoomId, hearingEndDate, hearingStartTime));
    }

    public static HearingsData singleHearingsDataWithAllocationDataAndJudiciary() {
        return new HearingsData(HearingsDataFactory.singleHearingsDataWithAllocationDataAndJudiciary());
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final String jurisdictionType) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary(jurisdictionType));
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate() {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate());
    }

    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDateWithParameters(Integer numberOfHearing, UUID courtCenterId, UUID courtRoomId, String judiciaryTpe) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDateWithParameters(numberOfHearing, courtCenterId, courtRoomId, judiciaryTpe));
    }


    public static HearingsData hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(final Integer numberOfHearings) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(numberOfHearings));
    }

    public static HearingsData hearingsDataWithPossibleDisqualification() {
        return new HearingsData(HearingsDataFactory.singleHearingsDataWithPossibleDisqualification());
    }

    public static HearingsData hearingsDataWithAllocationDataAndAdjournmentFromDateWithoutJudiciary(final Integer numberOfHearings) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndAdjournmentFromDateWithoutJudiciary(numberOfHearings));
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

    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final CaseAndDefendantData caseAndDefendantData,
                                                                          final UUID courtCentreId,
                                                                          final UUID courtRoomId) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciary(caseAndDefendantData, courtCentreId, courtRoomId));
    }


    public static HearingsData hearingsDataWithAllocationDataAndJudiciary(final CaseAndDefendantData caseAndDefendantData,
                                                                          final UUID courtCentreId,
                                                                          final UUID courtRoomId,
                                                                          final LocalDate hearingEndDate,
                                                                          final ZonedDateTime hearingStartTime) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryWithDate(caseAndDefendantData, courtCentreId, courtRoomId, hearingEndDate, hearingStartTime));
    }


    public static HearingsData hearingsDataWithUnAllocationDataAndJudiciary(final CaseAndDefendantData caseAndDefendantData) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithUnAllocationDataAndJudiciary(caseAndDefendantData));
    }

    public static HearingsData hearingsDataStandaloneApplication() {
        return new HearingsData(HearingsDataFactory.hearingsDataStandaloneApplication());
    }

    public static HearingsData hearingsDataStandaloneApplicationWithSubject() {
        return new HearingsData(HearingsDataFactory.hearingsDataStandaloneApplicationWithSubject());
    }


    public static HearingsData hearingsDataWithShadowListedOffences() {
        return new HearingsData(HearingsDataFactory.hearingsDataWithShadowListedOffences());
    }

    public static HearingsData hearingsDataWithForPublishingCourtListsWithoutReportingRestriction(final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithAllocationDataAndJudiciaryWithNoReportingRestriction(courtCentreId, courtRoomId, judiciaryType));
    }

    public static HearingsData hearingsDataWithRestriction(final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType) {
        return new HearingsData(HearingsDataFactory.hearingsDataWithRestriction(courtCentreId, courtRoomId, judiciaryType));
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