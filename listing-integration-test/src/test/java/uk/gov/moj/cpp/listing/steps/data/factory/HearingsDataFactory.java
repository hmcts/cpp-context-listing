package uk.gov.moj.cpp.listing.steps.data.factory;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.core.courts.HearingLanguage;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.services.test.utils.core.random.BigDecimalGenerator;
import uk.gov.justice.services.test.utils.core.random.StringGenerator;
import uk.gov.moj.cpp.listing.domain.Address;
import uk.gov.moj.cpp.listing.steps.CivilOffenceData;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationPartyData;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.CaseMarkerData;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationPartyType;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingTypeData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleData;
import uk.gov.moj.cpp.listing.steps.data.JudicialRoleTypeData;
import uk.gov.moj.cpp.listing.steps.data.LaaReferenceData;
import uk.gov.moj.cpp.listing.steps.data.LegalEntityDefendantData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;
import uk.gov.moj.cpp.listing.steps.data.OrganisationData;
import uk.gov.moj.cpp.listing.steps.data.ReportingRestrictionData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

public class HearingsDataFactory {

    public static final String CROWN_JURISDICTION = "CROWN";
    public static final String MAGISTRATES_JURISDICTION = "MAGISTRATES";
    private static final int HEARING_ESTIMATE_MINUTES = 30;
    public static final String ESTIMATED_DURATION = "1 week";
    private static final HearingTypeData PTP_HEARING_TYPE = new HearingTypeData(UUID.fromString("52edf232-3c09-4c74-a6ad-737985c2e662"), "PTP", "welshPTP");
    private static final HearingTypeData TRIAL_HEARING_TYPE = new HearingTypeData(UUID.fromString("bf8155e1-90b9-4080-b133-bfbad895d6e4"), "Trial", "welsh trial");
    private static final BailStatus BAIL_CONDITIONAL = new BailStatus.Builder().withId(fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).withCode("P").withDescription("Custody or remanded into custody").build();

    public static final ZonedDateTime SLOT_START_TIME = ZonedDateTime.now();
    public static final Integer SLOT_DURATION = 25;
    public static final String SLOT_SCHEDULE_ID = randomUUID().toString();
    public static final String SLOT_SESSION = "AM";
    public static final String SLOT_OUCODE = "121RTY";
    public static final Integer SLOT_COURT_ROOM_ID = 123498;
    private static final UUID JUDICIAL_RESULT_ID = UUID.fromString("065b6fcb-0787-4f0d-a9cd-af4b5c36e047");
    public static final int OFFENCE_COUNT = 1;
    public static final int OFFENCE_ORDER_INDEX = 0;
    public static final String OFFENCE_LEGISLATION = "legislation";


    public static List<HearingData> hearingsData() {
        return manyRandomHearings(2);
    }

    public static List<HearingData> trialHearingsData() {
        return manyRandomHearings(2, TRIAL_HEARING_TYPE);
    }

    public static List<HearingData> singleHearingsData() {
        return manyRandomHearings(1);
    }

    public static List<HearingData> notHmiEnabledHearingsData(){
        return notHmiEnabledManyRandomHearings(2);
    }

    public static List<HearingData> singleNotHmiEnabledHearingsData(){
        return notHmiEnabledManyRandomHearings(1);
    }

    public static List<HearingData> hearingsData(final List<HearingData> hearings) {
        return manyRandomHearings(hearings);
    }

    public static HearingData allocatedHearingsData(final List<HearingData> hearings) {
        return singleRandomAllocatedHearings(hearings);
    }

    public static List<HearingData> hearingsData(final String jurisdictionType) {
        return manyRandomHearings(2, jurisdictionType);
    }

    public static List<HearingData> notHmiEnabledHearingsData(final String jurisdictionType) {
        return notHmiEnabledManyRandomHearings(2, jurisdictionType);
    }

    public static List<HearingData> hearingsData(final UUID hearingId) {
        return manyRandomHearings(2, hearingId);
    }

    public static List<HearingData> hearingsDataForWeekCommencing(final LocalDate weekCommencingStartDate, final Integer weekCommencingDuration) {
        return createRandomHearingWithWeekCommencing(1, weekCommencingStartDate, weekCommencingDuration);
    }

    public static List<HearingData> hearingsDataForWeekCommencing(final LocalDate weekCommencingStartDate, final Integer weekCommencingDuration, UUID courtCenterId, UUID courtRoomId, String role) {
        return createRandomHearingWithWeekCommencing(1, weekCommencingStartDate, weekCommencingDuration, courtCenterId, courtRoomId, role);
    }


    public static List<HearingData> hearingsDataForBookedSlot() {
        return createRandomHearingsWitBookedSlots(1);
    }

    public static List<HearingData> hearingsDataWithLegalEntity() {
        return manyRandomHearingsWithLegalEntity(1);
    }

    public static List<HearingData> singleHearingData() {
        return manyRandomHearings(1);
    }

    public static List<HearingData> singleHearingDataForHMI() {
        return manyRandomHearingsForHMI(1);
    }

    public static List<HearingData> singleHearingDataSingleOffence() {
        return singleRandomHearing();
    }

    public static List<HearingData> singleHearingDataTwoDefendantWithCourtRoomCourtCenterAndJudiciaryType(final UUID courtCenterId, final UUID courtRoomId, final String judiciaryType, final String court, final Integer numberOfCases) {
        return singleRandomHearingWithTwoDefendantAndTwoOffence(courtCenterId, courtRoomId, judiciaryType, court, numberOfCases);
    }

    public static List<HearingData> singleHearingDataSingleOffenceWithCourtRoomCourtCenterAndJudiciaryType(final UUID courtCenterId, final UUID courtRoomId, final String judiciaryType, final String court, final Integer numberOfCases) {
        return singleRandomHearingWithCourtRoomCourtCenterAndJudiciaryType(courtCenterId, courtRoomId, judiciaryType, court, numberOfCases);
    }


    public static List<HearingData> singleHearingDataMultipleOffences() {
        return singleRandomHearingWithMultipleOffences();
    }

    public static List<HearingData> singleHearingDataMultipleDefendants() {
        return singleRandomHearingWithMultipleDefendants();
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciary() {
        return manyRandomHearingsWithAllocationData(2);
    }

    public static List<HearingData> singleHearingsDataWithAllocationDataAndJudiciary() {
        return manyRandomHearingsWithAllocationDataSingleCase(1);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciary(final String jurisdictionType) {
        return manyRandomHearingsWithAllocationDataWithJudiciaryType(2, randomUUID(), jurisdictionType);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(final UUID courtCentreId, final String jurisdictionType) {
        return manyRandomHearingsWithAllocationDataWithJudiciaryType(2, courtCentreId, jurisdictionType);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciaryAndJudiciaryType(final UUID courtCentreId,
                                                                                               final String jurisdictionType,
                                                                                             final UUID courtRoomId,
                                                                                             final LocalDate hearingEndDate,
                                                                                             final ZonedDateTime hearingStartTime) {
        return manyRandomHearingsWithAllocationDataWithJudiciaryType(2, courtCentreId, jurisdictionType, courtRoomId, hearingEndDate, hearingStartTime);
    }



    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate() {
        return manyRandomHearingsWithAllocationDataAndIsAdjournment(2);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDateWithParameters(Integer numberOfHearing,UUID courtCenterId, UUID courtRoomId, String judiciaryType) {
        return manyRandomHearingsWithAllocationDataAndIsAdjournmentWithParameters(numberOfHearing, courtCenterId, courtRoomId, judiciaryType);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciaryWithAdjournmentFromDate(final Integer numberOfHearings) {
        return manyRandomHearingsWithAllocationDataAndIsAdjournment(numberOfHearings);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndAdjournmentFromDateWithoutJudiciary(final Integer numberOfHearings) {
        return manyRandomHearingsWithAllocationDataAndIsAdjournmentWithoutJudiciary(numberOfHearings);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciary(final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType) {
        return manyRandomHearingsWithAllocationDataAndJurisdictionType(1, courtCentreId, courtRoomId, judiciaryType, CROWN_JURISDICTION);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciaryWithNoReportingRestriction(final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType) {
        return manyRandomHearingWithoutReportingRestriction(1, courtCentreId, courtRoomId, judiciaryType, CROWN_JURISDICTION);
    }

    public static List<HearingData> manyRandomHearingWithoutReportingRestriction(final Integer numberOfHearings, final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType, final String jurisdictionType) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithoutReportingRestriction(courtCentreId, courtRoomId, singletonList(randomJudicialRole(judiciaryType)), jurisdictionType))
                .collect(toList());
    }

    public static List<HearingData> hearingsDataWithRestriction(final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType) {
        return manyRandomHearingWithRestriction(1, courtCentreId, courtRoomId, judiciaryType, CROWN_JURISDICTION);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciary(final CaseAndDefendantData caseAndDefendantData) {
        return manyRandomHearingsWithAllocationData(2, caseAndDefendantData, randomUUID(), randomUUID());
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciary(final CaseAndDefendantData caseAndDefendantData,
                                                                               final UUID courtCentreId,
                                                                               final UUID courtRoomId) {
        return manyRandomHearingsWithAllocationData(2, caseAndDefendantData, courtCentreId, courtRoomId);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciaryWithDate(final CaseAndDefendantData caseAndDefendantData,
                                                                                       final UUID courtCentreId,
                                                                                       final UUID courtRoomId,
                                                                                       final LocalDate hearingEndDate,
                                                                                       final ZonedDateTime hearingStartTime) {
        return manyRandomHearingsWithAllocationDataWithDate(1, caseAndDefendantData, courtCentreId, courtRoomId,  hearingEndDate, hearingStartTime);
    }

    public static List<HearingData> hearingsDataWithUnAllocationDataAndJudiciary(final CaseAndDefendantData caseAndDefendantData) {
        return manyRandomHearingsWithUnAllocationData(2, caseAndDefendantData);
    }

    public static List<HearingData> hearingsDataStandaloneApplication() {
        return manyRandomHearingsStandaloneApplication(2, false);
    }

    public static List<HearingData> hearingsDataStandaloneApplicationWithSubject() {
        return manyRandomHearingsStandaloneApplication(2, true);
    }


    public static List<HearingData> hearingsDataForCasesWithExParte() {
        return manyRandomHearingsWithExParte(2);
    }

    public static List<HearingData> hearingsDataForWeekCommencing(final UUID hearingId, final LocalDate hearingEndDate,
                                                                  final UUID courtRoomId, final LocalDate weekCommencingStartDate,
                                                                  final LocalDate weekCommencingEndDate, final LocalDate startDate) {
        return singletonList(randomHearingForWeekCommencingDate(hearingId, hearingEndDate, courtRoomId, null, weekCommencingStartDate, weekCommencingEndDate, startDate));
    }

    public static List<HearingData> singleHearingsDataWithPossibleDisqualification() {
        return singletonList(randomHearingWithPossibleDisqualification());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationData(final Integer numberOfHearings) {
        return manyRandomHearingsWithAllocationData(numberOfHearings, UUID.randomUUID());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataSingleCase(final Integer numberOfHearings) {
        return manyRandomHearingsWithAllocationDataSingleCase(numberOfHearings, UUID.randomUUID());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournment(final Integer numberOfHearings) {
        return manyRandomHearingsWithAllocationDataAndIsAdjournment(numberOfHearings, UUID.randomUUID());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournmentWithParameters(final Integer numberOfHearings, UUID courtCenterId, UUID courtRoomid, String judiciaryType) {
         return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithAdjournmentFromDateWithParameters(courtCenterId, courtRoomid, singletonList(randomJudicialRole(judiciaryType))))
                .collect(toList());
    }


    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournmentWithoutJudiciary(final Integer numberOfHearings) {
        return manyRandomHearingsWithAllocationDataAndIsAdjournmentWithoutJudiciary(numberOfHearings, UUID.randomUUID());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationData(final Integer numberOfHearings, final UUID courtCentreId) {
        return manyRandomHearingsWithAllocationData(numberOfHearings, courtCentreId, MAGISTRATES_JURISDICTION);
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataSingleCase(final Integer numberOfHearings, final UUID courtCentreId) {
        return manyRandomHearingsWithAllocationDataSingleCase(numberOfHearings, courtCentreId, MAGISTRATES_JURISDICTION);
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournment(final Integer numberOfHearings, final UUID courtCentreId) {
        return manyRandomHearingsWithAllocationDataAndIsAdjournment(numberOfHearings, courtCentreId, MAGISTRATES_JURISDICTION);
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournmentWithCourt(final Integer numberOfHearings, final UUID courtCentreId, final UUID courtRoomUUID,  final String court) {
        return manyRandomHearingsWithAllocationDataAndIsAdjournment(numberOfHearings, courtCentreId, courtRoomUUID, CROWN_JURISDICTION, court);
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournmentWithoutJudiciary(final Integer numberOfHearings, final UUID courtCentreId) {
        return manyRandomHearingsWithAllocationDataAndIsAdjournmentWithoutJudiciaries(numberOfHearings, courtCentreId);
    }

    private static List<HearingData> manyRandomHearingsWithAllocationData(final Integer numberOfHearings, final UUID courtCentreId, final String judiciaryType) {
        final UUID courtRoomId = randomUUID();
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithAdjournmentFromDate(courtCentreId, courtRoomId, singletonList(randomJudicialRole(judiciaryType))))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataSingleCase(final Integer numberOfHearings, final UUID courtCentreId, final String judiciaryType) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithAdjournmentFromDateSingleCase(courtCentreId, singletonList(randomJudicialRole(judiciaryType))))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataAndJurisdictionType(final Integer numberOfHearings, final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType, final String jurisdictionType) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithAdjournmentFromDate(courtCentreId, courtRoomId, singletonList(randomJudicialRole(judiciaryType)), jurisdictionType))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataWithJudiciaryType(final Integer numberOfHearings, final UUID courtCentreId, final String judiciaryType) {
        final UUID courtRoomId = randomUUID();
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing(courtCentreId, courtRoomId, singletonList(randomJudicialRole(judiciaryType)), judiciaryType))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataWithJudiciaryType(final Integer numberOfHearings, final UUID courtCentreId, final String judiciaryType,
                                                                                           final UUID courtRoomId,
                                                                                           final LocalDate hearingEndDate,
                                                                                           final ZonedDateTime hearingStartTime ) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithHearingDate(courtCentreId, courtRoomId, singletonList(randomJudicialRole(judiciaryType)), judiciaryType,  hearingStartTime, hearingEndDate))
                .collect(toList());
    }



    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournment(final Integer numberOfHearings, final UUID courtCentreId, final String judiciaryType) {
        final UUID courtRoomId = randomUUID();
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithAdjournmentFromDate(courtCentreId, courtRoomId, singletonList(randomJudicialRole(judiciaryType))))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournment(final Integer numberOfHearings, final UUID courtCentreId, UUID courtRoomUUID, final String judiciaryType, String court) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithAdjournmentFromDateWithCourt(courtCentreId, courtRoomUUID, singletonList(randomJudicialRole(judiciaryType)), court))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataAndIsAdjournmentWithoutJudiciaries(final Integer numberOfHearings, final UUID courtCentreId) {
        final UUID courtRoomId = randomUUID();
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithAdjournmentFromDate(courtCentreId, courtRoomId, Collections.emptyList()))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationData(final Integer numberOfHearings,
                                                                          final CaseAndDefendantData caseAndDefendantData,
                                                                          final UUID courtCentreId,
                                                                          final UUID courtRoomId) {
        final LocalDate hearingEndDate = LocalDate.now().plusDays(1);
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing(hearingEndDate, courtCentreId, courtRoomId, singletonList(randomJudicialRole()), caseAndDefendantData))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithAllocationDataWithDate(final Integer numberOfHearings,
                                                                                  final CaseAndDefendantData caseAndDefendantData,
                                                                                  final UUID courtCentreId,
                                                                                  final UUID courtRoomId,
                                                                                  final LocalDate hearingEndDate,
                                                                                  final ZonedDateTime hearingStartTime) {

        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing(hearingStartTime, hearingEndDate, courtCentreId, courtRoomId, singletonList(randomJudicialRole()), caseAndDefendantData))
                .collect(toList());
    }


    private static List<HearingData> manyRandomHearingsWithUnAllocationData(final Integer numberOfHearings, final CaseAndDefendantData caseAndDefendantData) {
        final LocalDate hearingEndDate = LocalDate.now().plusDays(1);
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomUnAllocatedHearing(hearingEndDate, null, singletonList(randomJudicialRole()), caseAndDefendantData))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearings(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing())
                .collect(toList());
    }


    private static List<HearingData> manyRandomHearingsWithExParte(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithExParte())
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearings(final Integer numberOfHearings, UUID courtCenterId, UUID courtRoomId) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing(courtCenterId,null, courtRoomId, null))
                .collect(toList());
    }

    private static List<HearingData> notHmiEnabledManyRandomHearings(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> notHmiEnabledRandomHearing())
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsForHMI(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingForHMI())
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearings(final List<HearingData> hearings) {
        return hearings.stream()
                .map(hearingData -> randomHearing(hearingData))
                .collect(toList());
    }

    private static HearingData singleRandomAllocatedHearings(final List<HearingData> hearings) {
        return randomAllocatedHearing(hearings.get(0));

    }

    private static List<HearingData> manyRandomHearingWithRestriction(final Integer numberOfHearings, final UUID courtCentreId, final UUID courtRoomId, final String judiciaryType, final String jurisdictionType) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithoutReportingRestriction(courtCentreId, courtRoomId, singletonList(randomJudicialRole(judiciaryType)), jurisdictionType))
                .collect(toList());
    }

    private static List<HearingData> singleRandomHearing() {
        return singletonList(randomHearingWithSingleDefendantSingleOffence());
    }

    private static List<HearingData> singleRandomHearingWithTwoDefendantAndTwoOffence(final UUID courtCenterId, final UUID courtRoomId, final String judiciaryType, final String court, final Integer numberOfCases) {
        return singletonList(randomHearingWithTwoDefendantTwoOffenceWithCourtCenterIdCourtRoomIdJudiciaryType(courtCenterId, courtRoomId,judiciaryType, court, numberOfCases));
    }
    private static List<HearingData> singleRandomHearingWithCourtRoomCourtCenterAndJudiciaryType(final UUID courtCenterId, final UUID courtRoomId, final String judiciaryType, final String court, final Integer numberOfCases) {
        return singletonList(randomHearingWithSingleDefendantSingleOffenceWithCourtCenterIdCourtRoomIdJudiciaryType(courtCenterId, courtRoomId,judiciaryType, court, numberOfCases));
    }

    private static List<HearingData> singleRandomHearingWithMultipleOffences(){
        return singletonList(randomHearingMultipleOffences());
    }

    private static List<HearingData> singleRandomHearingWithMultipleDefendants(){
        return singletonList(randomHearingMultipleDefendants());
    }

    private static List<HearingData> createRandomHearingWithWeekCommencing(final Integer numberOfHearings, final LocalDate weekCommencingStartDate, final Integer weekCommencingDuration) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithWeekCommencingDates(null, null, null, weekCommencingStartDate, weekCommencingDuration))
                .collect(toList());
    }

    private static List<HearingData> createRandomHearingWithWeekCommencing(final Integer numberOfHearings, final LocalDate weekCommencingStartDate, final Integer weekCommencingDuration, UUID courtCenterId, UUID courtRoomId, String judicialRoles) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithWeekCommencingDates(null, courtCenterId, courtRoomId, singletonList(randomJudicialRole(judicialRoles)), weekCommencingStartDate, weekCommencingDuration))
                .collect(toList());
    }

    private static List<HearingData> createRandomHearingsWitBookedSlots(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> createRandomHearingWithBookedSlot(null, randomUUID(), null))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearings(final Integer numberOfHearings, final UUID hearingId) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing(hearingId))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearings(final Integer numberOfHearings, final String jurisdictionType) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing(jurisdictionType))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearings(final Integer numberOfHearings, final HearingTypeData trialHearingType) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing(trialHearingType))
                .collect(toList());
    }

    private static List<HearingData> notHmiEnabledManyRandomHearings(final Integer numberOfHearings, final String jurisdictionType) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> notHmiEnabledRandomHearing(jurisdictionType))
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithLegalEntity(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithLegalEntity())
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsStandaloneApplication(final Integer numberOfHearings, final boolean withSubject) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingStandaloneApplication(withSubject))
                .collect(toList());
    }


    public static List<HearingData> hearingsDataWithShadowListedOffences() {
        final UUID courtCentreId = UUID.randomUUID();
        final String judiciaryType = MAGISTRATES_JURISDICTION;
        final LocalDate hearingEndDate = LocalDate.now().plusDays(1);
        final UUID courtRoomId = randomUUID();

        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        final List<HearingData> listHearingData = new ArrayList<>();
        listHearingData.add(new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                Arrays.asList(randomJudicialRole(judiciaryType)), judiciaryType, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court", LocalDate.now().toString()));

        listHearingData.get(0)
                .getListedCases()
                .stream()
                .flatMap(listedCase -> listedCase.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffences().stream())
                .forEach(offence -> offence.setShadowListed(of(Boolean.TRUE)));

        return listHearingData;
    }

    private static List<ListedCaseData> manyRandomListingCases(final Integer numberOfListingCases) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCase())
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCasesSingleDefendant(final Integer numberOfListingCases) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCaseSingleDefendant())
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCases(final Integer numberOfListingCases, final String listingReason) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCaseWithDefendantHavingListingReason(listingReason))
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCases(final List<ListedCaseData> listedCaseData) {
        return listedCaseData.stream()
                .map(lcd -> randomListedCase(lcd))
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCasesWithoutReportingRestriction(final Integer numberOfListingCases) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCaseWithoutReportingRestriction())
                .collect(toList());
    }
    private static List<ListedCaseData> manyRandomListingCasesWithExParteOffenceListedCase() {
        List<ListedCaseData> listingCases = new ArrayList<>();

        listingCases.add(randomListedCaseWithExParteOffenceListedCase());
        listingCases.add(randomListedCaseWithSingleOffence());

        return listingCases;
    }

    private static List<ListedCaseData> manyRandomListingCasesSingleOffence(final Integer numberOfListingCases) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCaseWithSingleOffence())
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCasesWithGivenDefendantAndOffences(final Integer numberOfListingCases, final Integer defendants, final Integer offences) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCaseWithGivenDefendantAndOffences(defendants, offences))
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCasesMultipleDefendantsOffences(final Integer numberOfListingCases){
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCaseWithMultipleDefendants())
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCasesMultipleOffences(final Integer numberOfListingCases){
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCaseWithMultipleOffences())
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCases(final Integer numberOfListingCases, final CaseAndDefendantData caseAndDefendantData) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCase(caseAndDefendantData))
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCasesWithLegalEntity(final Integer numberOfListingCases) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCaseWithLegalEntity())
                .collect(toList());
    }

    private static List<OffenceData> manyRandomOffences(final Integer numberOfOffences) {
        return IntStream.range(0, numberOfOffences)
                .mapToObj((int i) -> randomOffence())
                .collect(toList());
    }

    private static List<OffenceData> manyRandomOffences(final List<OffenceData> offences) {
        return offences.stream()
                .map(offenceData -> randomOffence(offenceData))
                .collect(toList());
    }

    private static List<OffenceData> manyRandomOffencesWithoutReportingRestriction(final Integer numberOfOffences) {
        return IntStream.range(0, numberOfOffences)
                .mapToObj((int i) -> randomOffenceWithoutReportingRestriction())
                .collect(toList());
    }

    private static List<OffenceData> manyRandomOffencesWithExParteOffenceListedCase(final Integer numberOfOffences) {
        return IntStream.range(0, numberOfOffences)
                .mapToObj((int i) -> randomOffenceWithExParteOffenceListedCase())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendants(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendant())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendantsTwoOffences(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendant(2))
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendants(final List<DefendantData> defendants) {
        return defendants.stream()
                .map(defendantData -> randomDefendant(defendantData.getOffences()))
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendantsWithoutReportingRestriction(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendantWithoutReportingRestriction())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendantWithExParteOffenceListedCase(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendantWithExParteOffenceListedCase())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendantSingleOffence(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendantSingleOffence())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendantWithGivenOffence(final Integer numberOfDefendants, final Integer numberOfOffences) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendantWithGivenNumberOfOffences(numberOfOffences))
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendants(final Integer numberOfDefendants, final CaseAndDefendantData caseAndDefendantData) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendant(caseAndDefendantData))
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendantsWithMultipleOffences(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendantMultipleOffences())
                .collect(toList());
    }

    private static List<CaseMarkerData> manyRandomCaseMarkers(final Integer numberOfCaseMarkers) {
        return IntStream.range(0, numberOfCaseMarkers)
                .mapToObj((int i) -> randomCaseMarker())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendantsWithLegalEntity(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendantWithLegalEntityDefendant())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendantsWithListingReason(final String listingReason) {
        return IntStream.range(0, 2)
                .mapToObj((int i) -> randomDefendantWithListingReason(listingReason))
                .collect(toList());
    }

    private static ListedCaseData randomListedCase() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendants(2), false, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static ListedCaseData randomListedCaseSingleDefendant() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantsTwoOffences(1), false, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static ListedCaseData randomListedCase(final ListedCaseData listedCaseData) {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendants(listedCaseData.getDefendants()), false, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static ListedCaseData randomListedCaseWithoutReportingRestriction() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantsWithoutReportingRestriction(2), true, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static ListedCaseData randomListedCaseWithExParteOffenceListedCase() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantWithExParteOffenceListedCase(2), true, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static ListedCaseData randomListedCaseWithGivenDefendantAndOffences(Integer numberOfDefendants, Integer numberOfOffences) {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantWithGivenOffence(numberOfDefendants, numberOfOffences), false, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }
    private static ListedCaseData randomListedCaseWithSingleOffence() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantSingleOffence(1), false, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static ListedCaseData randomListedCaseWithMultipleOffences() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantsWithMultipleOffences(1), false, false, manyRandomCaseMarkers(1), STRING.next(), randomUUID(), false, false, false);
    }

    private static ListedCaseData randomListedCaseWithMultipleDefendants() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantsWithMultipleOffences(2), false, false, manyRandomCaseMarkers(1), STRING.next(), randomUUID(), false, false, false);
    }

    private static ListedCaseData randomListedCase(final CaseAndDefendantData caseAndDefendantData) {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), caseAndDefendantData.getCaseUrn(), manyRandomDefendants(1, caseAndDefendantData), false, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static ListedCaseData randomListedCaseWithLegalEntity() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantsWithLegalEntity(1), false, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static ListedCaseData randomListedCaseWithDefendantHavingListingReason(final String listingReason) {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), randomCaseReference(), manyRandomDefendantsWithListingReason(listingReason), false, false, manyRandomCaseMarkers(1), STRING.next(), null, null, null, null);
    }

    private static LaaReferenceData randomLaaReferenceData() {
        return new LaaReferenceData(STRING.next(), of(LocalDate.now()), Optional.of(LocalDate.now()), STRING.next(), LocalDate.now(), STRING.next(), randomUUID());
    }

    private static String randomCaseReference() {
        return String.format("%s%s",
                new StringGenerator(4).next(),
                new BigDecimalGenerator(1000000, 9999999, 0).next().toString());
    }

    private static OffenceData randomOffence() {
        final CivilOffenceData civilOffenceData = new CivilOffenceData(false);
        return new OffenceData(randomUUID(), STRING.next(), LocalDate.now(),
                LocalDate.now(), STRING.next(), STRING.next(), STRING.next(),
                OFFENCE_COUNT, OFFENCE_ORDER_INDEX, OFFENCE_LEGISLATION, randomUUID(), Optional.of(randomCustodyTimeLimit()), Optional.of(randomLaaReferenceData()), LocalDate.now(), of(Boolean.FALSE), manyRandomReportingRestriction(2), STRING.next(), civilOffenceData);
    }

    private static OffenceData randomOffence(OffenceData offence) {

        final CivilOffenceData civilOffenceData = new CivilOffenceData(BOOLEAN.next());
        return new OffenceData(offence.getOffenceId(), STRING.next(), LocalDate.now(),
                LocalDate.now(), STRING.next(), STRING.next(), STRING.next(),
                OFFENCE_COUNT, OFFENCE_ORDER_INDEX, OFFENCE_LEGISLATION, randomUUID(), Optional.of(randomCustodyTimeLimit()), Optional.of(randomLaaReferenceData()), LocalDate.now(), of(Boolean.FALSE), manyRandomReportingRestriction(2), STRING.next(), civilOffenceData);
    }

    private static OffenceData randomOffenceWithoutReportingRestriction() {
        final CivilOffenceData civilOffenceData = new CivilOffenceData(BOOLEAN.next());
        return new OffenceData(randomUUID(), STRING.next(), LocalDate.now(),
                LocalDate.now(), STRING.next(), STRING.next(), STRING.next(),
                OFFENCE_COUNT, OFFENCE_ORDER_INDEX, OFFENCE_LEGISLATION, randomUUID(), Optional.of(randomCustodyTimeLimit()), Optional.of(randomLaaReferenceData()), LocalDate.now(), of(Boolean.FALSE), null, STRING.next(), civilOffenceData);
    }

    private static OffenceData randomOffenceWithExParteOffenceListedCase() {
        final CivilOffenceData civilOffenceData = new CivilOffenceData(true);
        return new OffenceData(randomUUID(), STRING.next(), LocalDate.now(),
                LocalDate.now(), STRING.next(), STRING.next(), STRING.next(),
                OFFENCE_COUNT, OFFENCE_ORDER_INDEX, OFFENCE_LEGISLATION, randomUUID(), Optional.of(randomCustodyTimeLimit()), Optional.of(randomLaaReferenceData()), LocalDate.now(), of(Boolean.FALSE), null, STRING.next(), civilOffenceData);
    }

    private static List<ReportingRestrictionData> manyRandomReportingRestriction(final Integer numberOfReportingRestrictions) {
        return IntStream.range(0, numberOfReportingRestrictions)
                .mapToObj((int i) -> randomReportingRestriction())
                .collect(toList());
    }

    private static ReportingRestrictionData randomReportingRestriction() {
        return new ReportingRestrictionData(randomUUID(), Optional.of(JUDICIAL_RESULT_ID), "RestrictionApplied", Optional.of(LocalDate.now()));
    }

    private static CustodyTimeLimit randomCustodyTimeLimit() {
        return new CustodyTimeLimit(1, false, "2020-01-06");
    }

    private static DefendantData randomDefendant() {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(3), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendant(final int numberOfOffences) {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(numberOfOffences), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendant(final List<OffenceData> offences) {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(offences), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendantWithoutReportingRestriction() {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffencesWithoutReportingRestriction(3), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendantWithExParteOffenceListedCase() {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffencesWithExParteOffenceListedCase(3), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendantSingleOffence() {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(1), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendantWithGivenNumberOfOffences(final Integer offence) {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(offence), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendantMultipleOffences() {
        return new DefendantData(randomUUID(),STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(2), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendant(final CaseAndDefendantData caseAndDefendantData) {
        final UUID masterDefendantId = caseAndDefendantData.getMasterDefendantId();
        return new DefendantData(masterDefendantId, STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(1), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, masterDefendantId, ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendantWithLegalEntityDefendant() {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(1), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                false, Boolean.FALSE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), STRING.next());
    }

    private static DefendantData randomDefendantWithListingReason(final String listingReason) {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(3), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()),
                Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, randomUUID(), ZonedDateTime.now(), listingReason);
    }

    private static CaseMarkerData randomCaseMarker() {
        return new CaseMarkerData(randomUUID(), randomUUID(), STRING.next(), STRING.next());
    }

    private static HearingData randomHearing() {
        return randomHearing(null, null, null);
    }
    private static HearingData randomHearingWithExParte() {
        return randomHearingWithExParte(LocalDate.now(), UUID.randomUUID(), null);
    }

    private static HearingData notHmiEnabledRandomHearing(){
        return randomHearingNotHmiEnabled(null, null, null);
    }

    private static HearingData randomHearingForHMI() {
        return randomHearingForHMI(null, null, null);
    }

    private static HearingData randomHearing(final HearingData hearingData) {
        return randomHearing(hearingData, null, null, null);
    }

    private static HearingData randomAllocatedHearing(final HearingData hearingData) {
        return randomHearing(hearingData, null,hearingData.getCourtCentreId(), hearingData.getCourtRoomId(), null);
    }

    private static HearingData randomHearingWithTwoDefendantTwoOffenceWithCourtCenterIdCourtRoomIdJudiciaryType(final UUID courtCenterId, final UUID courtRoomId, final String judiciaryType, final String court, final Integer numberOfCases) {
        return randomHearingWithGivenNumberDefendantAndOffencesForFixedDateHearing(courtCenterId, courtRoomId, null, singletonList(randomJudicialRole((judiciaryType))),court,numberOfCases, 2, 2);
    }
    private static HearingData randomHearingWithSingleDefendantSingleOffenceWithCourtCenterIdCourtRoomIdJudiciaryType(final UUID courtCenterId, final UUID courtRoomId, final String judiciaryType, final String court, final Integer numberOfCases) {
        return randomHearingSingleOffenceWithAdditionalParameters(null,courtCenterId, courtRoomId, singletonList(randomJudicialRole((judiciaryType))),court,numberOfCases);
    }

    private static HearingData randomHearingWithSingleDefendantSingleOffence() {
        return randomHearingSingleOffence(null, null, null);
    }

    private static HearingData randomHearing(final UUID hearingId) {
        return randomHearing(null, null, null, hearingId);
    }

    private static HearingData randomHearing(final String jurisdictionType) {
        return randomHearing(randomUUID(), null, null, jurisdictionType);
    }

    private static HearingData randomHearing(final HearingTypeData trialHearingType) {
        return randomHearing(randomUUID(), null, null, trialHearingType);
    }

    private static HearingData notHmiEnabledRandomHearing(final String jurisdictionType) {
        return randomHearing(UUID.fromString("16ed5e59-40bb-3e05-b525-4ddfbb8fca13"), null, null, jurisdictionType);
    }

    private static HearingData randomHearingWithLegalEntity() {
        return randomHearingWithLegalEntity(null, null, null);
    }

    private static HearingData randomHearing(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        return randomHearing(randomUUID(), hearingEndDate, courtRoomId, judicialRoles);
    }

    private static HearingData randomHearingWithExParte(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        return randomHearingWithExParte(randomUUID(), hearingEndDate, courtRoomId, judicialRoles);
    }

    private static HearingData randomHearingNotHmiEnabled(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        return randomHearing(UUID.fromString("16ed5e59-40bb-3e05-b525-4ddfbb8fca13"), hearingEndDate, courtRoomId, judicialRoles);
    }

    private static HearingData randomHearingForHMI(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        return randomHearingForHMI(randomUUID(), hearingEndDate, courtRoomId, judicialRoles);
    }

    private static HearingData randomHearing(final HearingData hearingData, final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        return randomHearing(hearingData, randomUUID(), hearingEndDate, courtRoomId, judicialRoles);
    }
    private static HearingData randomHearing(final HearingData hearingData, final LocalDate hearingEndDate, final UUID courtCenterId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        return randomHearing(hearingData,courtCenterId , hearingEndDate, courtRoomId, judicialRoles);
    }

    private static HearingData randomHearingSingleOffence(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        return randomHearingSingleOffence(randomUUID(), hearingEndDate, courtRoomId, judicialRoles);
    }

    private static HearingData randomHearingSingleOffenceWithAdditionalParameters(final LocalDate hearingEndDate, final UUID courtCenterId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final String court, final Integer numberOfCases) {
        return randomHearingSingleOffenceForFixedDateHearing(courtCenterId,courtRoomId, hearingEndDate, judicialRoles,court, numberOfCases);
    }

    private static HearingData randomHearing(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final UUID hearingId) {
        return randomHearing(randomUUID(), hearingEndDate, courtRoomId, judicialRoles, hearingId);
    }

    private static HearingData randomHearing(final UUID courtCentreId, final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }
    private static HearingData randomHearingWithExParte(final UUID courtCentreId, final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCasesWithExParteOffenceListedCase();
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearing(final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final String jurisdictionType) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now().plusDays(3), HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, isBlank(jurisdictionType) ? CROWN_JURISDICTION : jurisdictionType,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearingForHMI(final UUID courtCentreId, final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                null, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Worcester Crown Court");
    }

    private static HearingData randomHearing(final HearingData hearingData, final UUID courtCentreId, final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(hearingData.getListedCases());
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION,
                STRING.next(),
                singletonList(randomCourtApplicationData(hearingData.getCourtApplications().get(0), listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearingWithGivenNumberDefendantAndOffencesForFixedDateHearing(final UUID courtCentreId, final UUID courtRoomUUID, LocalDate hearingEndDate, final List<JudicialRoleData> judicialRoles, String court, final Integer numberOfCases, final Integer numberOfDefendants, final Integer numberOfOffences) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCasesWithGivenDefendantAndOffences(numberOfCases, numberOfDefendants, numberOfOffences);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now(), HEARING_ESTIMATE_MINUTES,
                courtRoomUUID, ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS), listedCaseData,
                judicialRoles, MAGISTRATES_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), court, LocalDate.now().toString());
    }

    private static HearingData randomHearingSingleOffenceForFixedDateHearing(final UUID courtCentreId, final UUID courtRoomUUID, LocalDate hearingEndDate, final List<JudicialRoleData> judicialRoles, String court, final Integer numberOfCases) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCasesSingleOffence(numberOfCases);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now(), HEARING_ESTIMATE_MINUTES,
                courtRoomUUID, ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS), listedCaseData,
                judicialRoles, MAGISTRATES_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), court, LocalDate.now().toString());
    }

    private static HearingData randomHearingSingleOffence(final UUID courtCentreId, final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCasesSingleOffence(2);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearingMultipleOffences() {
        final List<ListedCaseData> listedCaseData = manyRandomListingCasesMultipleOffences(1);
        return new HearingData(randomUUID(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now().plusDays(1), HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                null, ZonedDateTime.now(), listedCaseData,
                null, MAGISTRATES_JURISDICTION,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Wycombe Magistrates Court");
    }

    private static HearingData randomHearingMultipleDefendants() {
        final List<ListedCaseData> listedCaseData = manyRandomListingCasesMultipleDefendantsOffences(1);
        return new HearingData(randomUUID(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now().plusDays(1), HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                null, ZonedDateTime.now(), listedCaseData,
                null, MAGISTRATES_JURISDICTION,
                STRING.next(),
                null,
               null, "Wycombe Magistrates Court");
    }

    private static HearingData randomHearing(final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final HearingTypeData trialHearingType) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCentreId, trialHearingType, LocalDate.now(),
                LocalDate.now().plusDays(3), HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearingWithPossibleDisqualification() {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2, "For disqualification");
        return new HearingData(randomUUID(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now().plusDays(3), HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                null, ZonedDateTime.now(), listedCaseData,
                null, CROWN_JURISDICTION,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearingWithWeekCommencingDates(final LocalDate hearingEndDate, final UUID courtCenterId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final LocalDate weekCommencingStartDate, final Integer weekCommencingDuration) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCenterId, STRING.next(), PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()),
                weekCommencingStartDate, null, weekCommencingDuration);
    }

    private static HearingData randomHearingWithWeekCommencingDates(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final LocalDate weekCommencingStartDate, final Integer weekCommencingDuration) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), randomUUID(), STRING.next(), PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()),
                weekCommencingStartDate, null, weekCommencingDuration);
    }

    private static HearingData createRandomHearingWithBookedSlot(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        final List<RotaSlot> bookedSlots = Arrays.asList(RotaSlot.rotaSlot()
                .withCourtRoomId(SLOT_COURT_ROOM_ID)
                .withDuration(SLOT_DURATION)
                .withCourtScheduleId(SLOT_SCHEDULE_ID)
                .withOucode(SLOT_OUCODE)
                .withSession(SLOT_SESSION)
                .withStartTime(SLOT_START_TIME)
                .build());

        return new HearingData(randomUUID(), randomUUID(), STRING.next(), PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()),
                bookedSlots);
    }

    private static HearingData randomHearingWithAdjournmentFromDate(final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now(), HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS), listedCaseData,
                judicialRoles, MAGISTRATES_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court", LocalDate.now().toString());
    }

    private static HearingData randomHearingWithAdjournmentFromDateSingleCase(final UUID courtCentreId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCasesSingleDefendant(1);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now(), HEARING_ESTIMATE_MINUTES,
                randomUUID(), ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS), listedCaseData,
                judicialRoles, MAGISTRATES_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court", LocalDate.now().toString());
    }

    private static HearingData randomHearingWithAdjournmentFromDateWithParameters(final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now(), HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS), listedCaseData,
                judicialRoles, MAGISTRATES_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court", LocalDate.now().toString());
    }

    private static HearingData randomHearingWithAdjournmentFromDateWithCourt(final UUID courtCentreId, final UUID courtRoomUUID, final List<JudicialRoleData> judicialRoles, String court) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCentreId, court, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now().plusDays(7), HEARING_ESTIMATE_MINUTES,
                randomUUID(), ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()),
                LocalDate.now(), null, 7);
    }

    private static HearingData randomHearingWithAdjournmentFromDate(final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final String jurisdictionType) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now(), HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, jurisdictionType, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court", LocalDate.now().toString());
    }

    private static HearingData randomHearingWithoutReportingRestriction(final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final String jurisdictionType) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCasesWithoutReportingRestriction(2);
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                LocalDate.now(), HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, jurisdictionType, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court", LocalDate.now().toString());
    }

    private static HearingData randomHearing(final UUID courtCentreId, final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final UUID hearingId) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(hearingId, courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, CROWN_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearing(final LocalDate hearingEndDate, final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final CaseAndDefendantData caseAndDefendantData) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(1, caseAndDefendantData);
        return new HearingData(caseAndDefendantData.getHearingId(), courtCentreId, PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, caseAndDefendantData.getJurisdictionType(), STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearing(final ZonedDateTime hearingStartTime,
                                 final LocalDate hearingEndDate,
                                 final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final CaseAndDefendantData caseAndDefendantData) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(1, caseAndDefendantData);
        final LocalDate hearingStartDate = hearingStartTime.toLocalDate();
        return new HearingData(caseAndDefendantData.getHearingId(), courtCentreId, PTP_HEARING_TYPE, hearingStartDate,
                hearingEndDate, HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                courtRoomId, hearingStartTime, listedCaseData,
                judicialRoles, caseAndDefendantData.getJurisdictionType(), STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearingWithHearingDate(final UUID courtCentreId, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles,
                                             final String jurisdictionType,
                                             final ZonedDateTime hearingStartTime,
                                             final LocalDate hearingEndDate) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        final LocalDate hearingStartDate = hearingStartTime.toLocalDate();
        return new HearingData(randomUUID(), courtCentreId, PTP_HEARING_TYPE, hearingStartDate,
                hearingEndDate, HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                courtRoomId, hearingStartTime, listedCaseData,
                judicialRoles, isBlank(jurisdictionType) ? CROWN_JURISDICTION : jurisdictionType,
                STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomUnAllocatedHearing(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final CaseAndDefendantData caseAndDefendantData) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(1, caseAndDefendantData);
        return new HearingData(caseAndDefendantData.getHearingId(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                null, ZonedDateTime.now(), listedCaseData,
                judicialRoles, caseAndDefendantData.getJurisdictionType(), STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearingForWeekCommencingDate(final UUID hearingId, final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles, final LocalDate weekCommencingStartDate, final LocalDate weekCommencingEndDate, final LocalDate startDate) {
        final List<ListedCaseData> listedCaseData = manyRandomListingCases(2);

        return weekCommencingStartDate == null ?
                new HearingData(hearingId, randomUUID(), PTP_HEARING_TYPE, now(),
                        hearingEndDate, HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                        courtRoomId, ZonedDateTime.now(), listedCaseData,
                        judicialRoles, CROWN_JURISDICTION, STRING.next(),
                        singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                        singletonList(randomCourtApplicationPartyNeed()), STRING.next()) :
                new HearingData(hearingId, randomUUID(), STRING.next(), PTP_HEARING_TYPE, startDate,
                        hearingEndDate, HEARING_ESTIMATE_MINUTES,
                        courtRoomId, ZonedDateTime.now(), listedCaseData,
                        judicialRoles, CROWN_JURISDICTION, STRING.next(),
                        singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                        singletonList(randomCourtApplicationPartyNeed()),
                        weekCommencingStartDate, weekCommencingEndDate, 30);
    }

    private static HearingData randomHearingWithLegalEntity(final LocalDate hearingEndDate, final UUID courtRoomId, final List<JudicialRoleData> judicialRoles) {
        return new HearingData(randomUUID(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,ESTIMATED_DURATION,
                courtRoomId, ZonedDateTime.now(), manyRandomListingCasesWithLegalEntity(1),
                judicialRoles, CROWN_JURISDICTION, STRING.next(),
                singletonList(randomCourtApplicationDataWithLegalEntity(randomUUID())),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static HearingData randomHearingStandaloneApplication(final boolean withSubject) {
        final CourtApplicationData courtApplicationData = withSubject
                ? randomCourtApplicationDataWithSubject(null)
                : randomCourtApplicationData(null);
        return new HearingData(randomUUID(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                null, HEARING_ESTIMATE_MINUTES, ESTIMATED_DURATION,
                null, ZonedDateTime.now(), null,
                null, CROWN_JURISDICTION, STRING.next(),
                singletonList(courtApplicationData),
                singletonList(randomCourtApplicationPartyNeed()), "Carmarthen Magistrates Court");
    }

    private static CourtApplicationData randomCourtApplicationData(final UUID linkedCaseId) {
        return new CourtApplicationData(randomUUID(), linkedCaseId, randomUUID(),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.FALSE, STRING.next(), CourtApplicationPartyType.PERSON, null, randomAddress()),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.TRUE, STRING.next(), CourtApplicationPartyType.PERSON, null, randomAddress()),
                STRING.next(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, STRING.next(), randomUUID());
    }

    private static CourtApplicationData randomCourtApplicationDataWithSubject(final UUID linkedCaseId) {
        return new CourtApplicationData(randomUUID(), linkedCaseId, randomUUID(),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.FALSE, STRING.next(), CourtApplicationPartyType.PERSON, null, randomAddress()),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.TRUE, STRING.next(), CourtApplicationPartyType.PERSON, null, randomAddress()),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.TRUE, STRING.next(), CourtApplicationPartyType.PERSON, null, randomAddress()),
                STRING.next(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, STRING.next(), randomUUID());
    }

    private static CourtApplicationData randomCourtApplicationData(final CourtApplicationData courtApplicationData, final UUID linkedCaseId) {
        return new CourtApplicationData(randomUUID(), linkedCaseId, randomUUID(),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.FALSE, STRING.next(), CourtApplicationPartyType.PERSON, null, randomAddress()),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.TRUE, STRING.next(), CourtApplicationPartyType.PERSON, null, randomAddress()),
                STRING.next(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, STRING.next(), courtApplicationData.getOffenceId());
    }

    private static CourtApplicationData randomCourtApplicationDataWithLegalEntity(final UUID linkedCaseId) {
        return new CourtApplicationData(randomUUID(), linkedCaseId, randomUUID(),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.FALSE, STRING.next(), CourtApplicationPartyType.PERSON_DEFENDANT, new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()), randomAddress()),
                new CourtApplicationPartyData(randomUUID(), STRING.next(), Boolean.TRUE, STRING.next(), null, null, randomAddress()),
                STRING.next(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, STRING.next(), randomUUID());
    }

    private static JudicialRoleData randomJudicialRole() {
        return randomJudicialRole("MAGISTRATE");
    }

    private static Address randomAddress() {
        return Address
                .address()
                .withAddress1(STRING.next())
                .withAddress2(of(STRING.next()))
                .withAddress3(of(STRING.next()))
                .withAddress4(of(STRING.next()))
                .withAddress5(of(STRING.next()))
                .withPostcode(of("SW13 0AA"))
                .build();
    }

    public static JudicialRoleData randomJudicialRole(final String judiciaryType) {
        return new JudicialRoleData(Optional.empty(), Optional.ofNullable(BOOLEAN.next()), randomUUID(), randomUUID(),
                new JudicialRoleTypeData(Optional.empty(), judiciaryType));
    }

    private static CourtApplicationPartyListingNeeds randomCourtApplicationPartyNeed() {
        return new CourtApplicationPartyListingNeeds(randomUUID(), randomUUID(), null, HearingLanguage.ENGLISH, null, null);
    }

    private static OrganisationData getOrganisationData() {
        return new OrganisationData(UUID.randomUUID(), "ABC LTD");
    }
}
