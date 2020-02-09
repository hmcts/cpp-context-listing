package uk.gov.moj.cpp.listing.steps.data.factory;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.BOOLEAN;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtApplicationPartyListingNeeds;
import uk.gov.justice.core.courts.CustodyTimeLimit;
import uk.gov.justice.listing.courts.HearingLanguageNeeds;
import uk.gov.moj.cpp.listing.steps.data.ApplicantRespondentData;
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

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

public class HearingsDataFactory {

    public static final String JURISDICTION_TYPE = "CROWN";
    private static final int HEARING_ESTIMATE_MINUTES = 15;
    private static final HearingTypeData PTP_HEARING_TYPE = new HearingTypeData(UUID.fromString("52edf232-3c09-4c74-a6ad-737985c2e662"), "PTP");
    private static final BailStatus BAIL_CONDITIONAL = new BailStatus.Builder().withId(fromString("34443c87-fa6f-34c0-897f-0cce45773df5")).withCode("P").withDescription("Custody or remanded into custody").build();
    public static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");

    public static List<HearingData> hearingsData() {
        return manyRandomHearings(2);
    }

    public static List<HearingData> hearingsDataWithLegalEntity() {
        return manyRandomHearingsWithLegalEntity(1);
    }

    public static List<HearingData> singleHearingData() {
        return manyRandomHearings(1);
    }

    public static List<HearingData> hearingsDataWithAllocationDataAndJudiciary() {
        return manyRandomHearingsWithAllocationData(2);
    }

    public static List<HearingData> hearingsDataStandaloneApplication() {
        return manyRandomHearingsStandaloneApplication(2);
    }


    private static List<HearingData> manyRandomHearingsWithAllocationData(final Integer numberOfHearings) {
        LocalDate hearingEndDate = LocalDate.now().plusDays(1);
        UUID courtRoomId = randomUUID();
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing(hearingEndDate, courtRoomId, Arrays.asList(randomJudicalRole())))
                .collect(toList());
    }


    private static List<HearingData> manyRandomHearings(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearing())
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsWithLegalEntity(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingWithLegalEntity())
                .collect(toList());
    }

    private static List<HearingData> manyRandomHearingsStandaloneApplication(final Integer numberOfHearings) {
        return IntStream.range(0, numberOfHearings)
                .mapToObj((int i) -> randomHearingStandaloneApplication())
                .collect(toList());
    }

    private static List<ListedCaseData> manyRandomListingCases(final Integer numberOfListingCases) {
        return IntStream.range(0, numberOfListingCases)
                .mapToObj((int i) -> randomListedCase())
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

    private static List<LaaReferenceData> manyRandomLaaReferencesData(final Integer numberOfLaaReferenceData) {
        return IntStream.range(0, numberOfLaaReferenceData)
                .mapToObj((int i) -> randomLaaReferenceData())
                .collect(toList());
    }

    private static List<DefendantData> manyRandomDefendants(final Integer numberOfDefendants) {
        return IntStream.range(0, numberOfDefendants)
                .mapToObj((int i) -> randomDefendant())
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

    private static ListedCaseData randomListedCase() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), STRING.next(), manyRandomDefendants(2), false, false, manyRandomCaseMarkers(1), STRING.next());
    }

    private static ListedCaseData randomListedCaseWithLegalEntity() {
        return new ListedCaseData(randomUUID(), randomUUID(), STRING.next(), STRING.next(), manyRandomDefendantsWithLegalEntity(1), false, false, manyRandomCaseMarkers(1), STRING.next());
    }

    private static LaaReferenceData randomLaaReferenceData() {
        return new LaaReferenceData(STRING.next(), of(LocalDate.now()), Optional.of(LocalDate.now()), STRING.next(), LocalDate.now(), STRING.next(), randomUUID());
    }

    private static OffenceData randomOffence() {
        return new OffenceData(randomUUID(), STRING.next(), LocalDate.now(),
                LocalDate.now(), STRING.next(), STRING.next(), STRING.next(),
                1, randomUUID(), Optional.of(randomCustodyTimeLimit()), Optional.of(randomLaaReferenceData()), LocalDate.now());
    }

    private static CustodyTimeLimit randomCustodyTimeLimit() {
        return new CustodyTimeLimit(Optional.of(1), "2020-01-06");
    }

    private static DefendantData randomDefendant() {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(3), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()), Boolean.FALSE, Boolean.TRUE, Boolean.FALSE);
    }

    private static DefendantData randomDefendantWithLegalEntityDefendant() {
        return new DefendantData(randomUUID(), STRING.next(), STRING.next(),
                LocalDate.now(), LocalDate.now(), BAIL_CONDITIONAL, STRING.next(),
                manyRandomOffences(1), new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData()), false, Boolean.FALSE, Boolean.FALSE);
    }

    private static CaseMarkerData randomCaseMarker() {
        return new CaseMarkerData(randomUUID(), randomUUID(), STRING.next(), STRING.next());
    }

    private static HearingData randomHearing() {
        return randomHearing(null, null, null);
    }

    private static HearingData randomHearingWithLegalEntity() {
        return randomHearingWithLegalEntity(null, null, null);
    }

    private static HearingData randomHearing(LocalDate hearingEndDate, UUID courtRoomId, List<JudicialRoleData> judicialRoles) {
        List<ListedCaseData> listedCaseData = manyRandomListingCases(2);
        return new HearingData(randomUUID(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now(), listedCaseData,
                judicialRoles, JURISDICTION_TYPE, STRING.next(),
                singletonList(randomCourtApplicationData(listedCaseData.get(0).getCaseId())),
                singletonList(randomCourtApplicationPartyNeed()));
    }

    private static HearingData randomHearingWithLegalEntity(LocalDate hearingEndDate, UUID courtRoomId, List<JudicialRoleData> judicialRoles) {
        return new HearingData(randomUUID(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                hearingEndDate, HEARING_ESTIMATE_MINUTES,
                courtRoomId, ZonedDateTime.now(), manyRandomListingCasesWithLegalEntity(1),
                judicialRoles, JURISDICTION_TYPE, STRING.next(),
                singletonList(randomCourtApplicationDataWithLegalEntity(randomUUID())),
                singletonList(randomCourtApplicationPartyNeed()));
    }

    private static HearingData randomHearingStandaloneApplication() {
        return new HearingData(randomUUID(), randomUUID(), PTP_HEARING_TYPE, LocalDate.now(),
                null, HEARING_ESTIMATE_MINUTES,
                null, ZonedDateTime.now(), null,
                null, JURISDICTION_TYPE, STRING.next(),
                singletonList(randomCourtApplicationData(null)),
                singletonList(randomCourtApplicationPartyNeed()));
    }


    private static CourtApplicationData randomCourtApplicationData(UUID linkedCaseId) {
        return new CourtApplicationData(randomUUID(), linkedCaseId, randomUUID(),
                new ApplicantRespondentData(randomUUID(), STRING.next(), Boolean.FALSE, STRING.next(), CourtApplicationPartyType.PERSON, null),
                new ApplicantRespondentData(randomUUID(), STRING.next(), Boolean.TRUE, STRING.next(), CourtApplicationPartyType.PERSON, null),
                STRING.next(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
    }

    private static CourtApplicationData randomCourtApplicationDataWithLegalEntity(UUID linkedCaseId) {
        return new CourtApplicationData(randomUUID(), linkedCaseId, randomUUID(),
                new ApplicantRespondentData(randomUUID(), STRING.next(), Boolean.FALSE, STRING.next(), CourtApplicationPartyType.PERSON_DEFENDANT, new LegalEntityDefendantData(UUID.randomUUID(), getOrganisationData())),
                new ApplicantRespondentData(randomUUID(), STRING.next(), Boolean.TRUE, STRING.next(), null, null),
                STRING.next(), Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
    }

    private static JudicialRoleData randomJudicalRole() {
        return new JudicialRoleData(Optional.ofNullable(null), Optional.ofNullable(BOOLEAN.next()), randomUUID(),
                new JudicialRoleTypeData(Optional.empty(), "MAGISTRATE"));

    }

    private static CourtApplicationPartyListingNeeds randomCourtApplicationPartyNeed() {
        return new CourtApplicationPartyListingNeeds(randomUUID(), randomUUID(), of(HearingLanguageNeeds.ENGLISH));
    }

    private static OrganisationData getOrganisationData() {
        return new OrganisationData(UUID.randomUUID(), "ABC LTD");
    }
}

