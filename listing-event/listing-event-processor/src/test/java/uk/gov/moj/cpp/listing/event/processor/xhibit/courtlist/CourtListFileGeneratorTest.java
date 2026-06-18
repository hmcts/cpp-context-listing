package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.time.ZonedDateTime.parse;
import static java.util.Collections.singletonMap;
import static java.util.UUID.fromString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.DRAFT;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FINAL;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.FIRM;
import static uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.WARN;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlTestUtils.assertXmlEquals;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingType;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.log4j.BasicConfigurator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.slf4j.Logger;

// FIXME!!! Temporarily using lenient strictness to get this
// context running with junit 5.
@MockitoSettings(strictness = LENIENT)
@ExtendWith(MockitoExtension.class)
public class CourtListFileGeneratorTest {

    private static final String DAILY_COURT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-daily-list.json";
    private static final String DAILY_COURT_LIST_SUMMER_TIME_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-daily-list-summer-time.json";
    private static final String WEEK_COMMENCING_COURT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-week-commencing-list.json";
    private static final String RESTRICTED_DAILY_COURT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-restricted-daily-list.json";
    private static final String RESTRICTED_DAILY_COURT_LIST_WithMultipleCaseConvertedIntoMultipleHearing_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-restricted-daily-list-with-multiple-case-coverted-into-multiple-hearing.json";
    private static final String COURT_LIST_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-with-fixed-date-corresponding-hearing-types-list.json";
    private static final String COURT_LIST_2_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist2-with-fixed-date-corresponding-hearing-types-list.json";
    private static final String COURT_LIST_WITH_CASE_HIDDEN_INPUT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-case-hidden.json";
    private static final String COURT_LIST_FOR_COURT_APPLICATION_WHEN_SUBJECT_DETAIL_PRESENT_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-for-court-application-with-subject-detail-present.json";
    private static final String COURT_LIST_FOR_COURT_APPLICATION_WHEN_SUBJECT_DETAIL_ABSENT_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-for-court-application-with-subject-detail-absent.json";
    private static final String COURT_LIST_FOR_COURT_APPLICATION_INACTIVE_WARN_LIST_JSON_FILE = "/xhibit/mock-data/InactiveWarnList.json";

    private UUID courtCentreId1 = fromString("f34a5dba-8c4b-4ec8-8b9a-6af405c00ebf");
    private UUID courtCentreId2 = fromString("f46ddec0-928e-4236-9d1b-142715e8b570");
    private LocalDate startDate = LocalDate.parse("2019-11-04");

    @Spy
    private XmlUtils xmlUtils;

    @Mock
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    @Mock
    private ListingService listingService;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Logger logger;

    @Spy
    private MapperFactory mapperFactory;

    @InjectMocks
    private CourtListFileGenerator courtListFileGenerator;

    private JsonObject courtListJson;

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(WARN, WEEK_COMMENCING_COURT_LIST_JSON_FILE, "xhibit/expectedWarnedList.xml"),
                Arguments.of(DRAFT, DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedDraftList.xml"),
                Arguments.of(DRAFT, DAILY_COURT_LIST_SUMMER_TIME_JSON_FILE, "xhibit/expectedDraftListSummerTime.xml"),
                Arguments.of(FINAL, DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedFinalList.xml"),
                Arguments.of(FIRM, WEEK_COMMENCING_COURT_LIST_JSON_FILE, "xhibit/expectedFirmList.xml"),
                Arguments.of(FINAL, RESTRICTED_DAILY_COURT_LIST_JSON_FILE, "xhibit/expectedRestrictedFinalList.xml"),
                Arguments.of(WARN, COURT_LIST_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE, "xhibit/expectedWarnedListWithDifferentHearingTypesInCase.xml"),
                Arguments.of(WARN, COURT_LIST_2_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE, "xhibit/expectedWarnedListWithMultipleHearingTypesInCase.xml"),
                Arguments.of(WARN, COURT_LIST_WITH_CASE_HIDDEN_INPUT_LIST_JSON_FILE, "xhibit/expectedWarnedListWithCaseHidden.xml"),
                Arguments.of(FINAL, RESTRICTED_DAILY_COURT_LIST_WithMultipleCaseConvertedIntoMultipleHearing_JSON_FILE, "xhibit/expectedRestrictedFinalListIeWithMultipleCaseConvertedIntoMultipleHearing.xml"),
                Arguments.of(FINAL, COURT_LIST_FOR_COURT_APPLICATION_WHEN_SUBJECT_DETAIL_PRESENT_JSON_FILE, "xhibit/expectedCourtApplicationWithSubjectDetailPresentFinalList.xml"),
                Arguments.of(FIRM, COURT_LIST_FOR_COURT_APPLICATION_WHEN_SUBJECT_DETAIL_ABSENT_JSON_FILE, "xhibit/expectedCourtApplicationWithSubjectDetailAbsentFirmList.xml"),
                Arguments.of(WARN, COURT_LIST_FOR_COURT_APPLICATION_INACTIVE_WARN_LIST_JSON_FILE, "xhibit/expectedInactiveCaseWarnList.xml")
        );
    }

    @BeforeEach
    public void initialiseLogger() {
        BasicConfigurator.configure();
    }

    @BeforeEach
    public void wireBeans() {
        xmlUtils.setLogger(logger);
        xmlUtils.postConstruct();
        mapperFactory.setCommonXhibitReferenceDataService(commonXhibitReferenceDataService);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void shouldGenerateXml(
            final PublishCourtListType publishCourtListType,
            final String courtListJsonFile,
            final String expectedXmlFile) throws Exception {

        mockDataSources(publishCourtListType, courtListJsonFile);

        ZonedDateTime timeStamp = ZonedDateTime.now();
        final PublishCourtListRequestParameters requestParameters = withDefaults()
                .withCourtCentreId(courtCentreId1)
                .publishCourtListType(publishCourtListType)
                .withStartDate(startDate)
                .withRequestedTime(timeStamp)
                .build();

        final CourtListMetadata metadata = new CourtListMetadata(publishCourtListType.name() + "-FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        final String generatedXml = courtListFileGenerator.generateXml(envelope, requestParameters, metadata, courtListJson);

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + publishCourtListType.getSchemaName());
        assertXmlEquals(generatedXml, expectedXmlFile, singletonMap("#TIME_STAMP#", requestParameters.getRequestedTime().toLocalDateTime().toString()));
    }

    private void mockDataSources(final PublishCourtListType publishCourtListType, final String courtListJsonFile) {
        final String crestCourtId = "000";
        final CourtLocation courtLocation1 = createCourtLocation(crestCourtId, "1");
        final CourtLocation courtLocation2 = createCourtLocation(crestCourtId, "2");

        final List<UUID> courtCentreIds = Arrays.asList(courtCentreId1, courtCentreId2);

        when(commonXhibitReferenceDataService.getCrownCourtDetails(courtCentreId1)).thenReturn(courtLocation1);
        when(commonXhibitReferenceDataService.getCrownCourtDetails(courtCentreId2)).thenReturn(courtLocation2);
        when(commonXhibitReferenceDataService.getCriminalCourtDetails(eq(courtCentreId1), nullable(String.class))).thenReturn(courtLocation1);
        when(commonXhibitReferenceDataService.getCriminalCourtDetails(eq(courtCentreId2), nullable(String.class))).thenReturn(courtLocation2);
        when(commonXhibitReferenceDataService.getCrownCourtCentreIdsForCrestId(crestCourtId)).thenReturn(courtCentreIds);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId1, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(10);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId1, UUID.fromString("6508af42-e4d4-396d-a752-d676ebd38f6d"))).thenReturn(20);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId1, UUID.fromString("64b0f4cf-2dde-310b-b7da-cab57b285b6f"))).thenReturn(4);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId1, UUID.fromString("28813316-35dc-30b7-a94f-07aeec664d9f"))).thenReturn(3);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId1, UUID.fromString("1f9630dc-e4ba-3378-8880-2369883394b2"))).thenReturn(1);

        final JsonObject judiciary = givenPayload("/xhibit/mock-data/referencedata.query.judiciaries.json");
        when(commonXhibitReferenceDataService.getJudiciary(any())).thenReturn(judiciary);

        final HearingType hearingType = new HearingType.Builder()
                .withExhibitHearingCode("TRL")
                .withExhibitHearingDescription("XHIBIT_HEARING_DESCRIPTION-TRL")
                .build();

        final HearingType hearingType1 = new HearingType.Builder()
                .withExhibitHearingCode("PTP")
                .withExhibitHearingDescription("XHIBIT_HEARING_DESCRIPTION-PTP")
                .build();

        final HearingType hearingType2 = new HearingType.Builder()
                .withExhibitHearingCode("SBT")
                .withExhibitHearingDescription("XHIBIT_HEARING_DESCRIPTION-PTP")
                .build();

        final UUID hearingTypeId = UUID.fromString("bf8155e1-90b9-4080-b133-bfbad895d6e4");
        when(commonXhibitReferenceDataService.getXhibitHearingType(any())).thenReturn(hearingType);

        when(commonXhibitReferenceDataService.getXhibitHearingType(eq(hearingTypeId))).thenReturn(hearingType);

        final UUID hearingTypeId1 = UUID.fromString("06b0c2bf-3f98-46ed-ab7e-56efaf9ecced");

        when(commonXhibitReferenceDataService.getXhibitHearingType(eq(hearingTypeId1))).thenReturn(hearingType1);

        final UUID hearingTypeId2 = UUID.fromString("c6b0c2bf-3f98-46ed-ab7e-56efaf9ecceb");

        when(commonXhibitReferenceDataService.getXhibitHearingType(eq(hearingTypeId2))).thenReturn(hearingType2);

        courtListJson = givenPayload(courtListJsonFile);

        when(listingService.getPublishedCourtListForCourtCentre(
                envelope,
                courtCentreId2,
                publishCourtListType,
                startDate)).thenReturn(emptyCourtList(courtCentreId2));
    }

    private CourtLocation createCourtLocation(final String crestCourtId, final String nameSuffix) {
        return new CourtLocation(
                "OUCODE",
                crestCourtId,
                "00" + nameSuffix,
                "MOCK_CROWN_COURTNAME",
                "MOCK",
                "MOCKCOURTNAME" + nameSuffix,
                "000",
                "CROWN_COURT");
    }

    private JsonObject emptyCourtList(final UUID courtCentreId) {
        return JsonObjects.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", JsonObjects.createArrayBuilder()
                        .add(
                                JsonObjects.createObjectBuilder()
                                        .add("crestCourtSite", crestCourtSite())
                                        .add("sittings", JsonObjects.createArrayBuilder().build())
                                        .build()
                        ).build()
                ).build();
    }

    private JsonObjectBuilder crestCourtSite() {
        return JsonObjects.createObjectBuilder()
                .add("crestCourtSiteId", "002")
                .add("crestCourtSiteName", "MOCKCOURTNAME2")
                .add("courtType", "CROWN_COURT");
    }
}
