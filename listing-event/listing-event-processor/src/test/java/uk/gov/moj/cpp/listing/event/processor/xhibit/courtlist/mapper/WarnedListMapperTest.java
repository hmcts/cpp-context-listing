package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.time.ZonedDateTime.parse;
import static java.util.Collections.singletonMap;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlTestUtils.assertXmlEquals;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.domain.referencedata.HearingType;
import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.ListingService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.xml.bind.JAXBElement;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class WarnedListMapperTest {

    private static final Logger LOGGER = getLogger(WarnedListMapperTest.class);

    private static final String HEARING_TYPE_CODE = "ABC";

    private UUID courtCentreId1 = fromString("f34a5dba-8c4b-4ec8-8b9a-6af405c00ebf");

    private LocalDate startDate = LocalDate.parse("2019-11-04");

    private UUID courtCentreId2 = fromString("f46ddec0-928e-4236-9d1b-142715e8b570");

    private static final String COURT_LIST_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE = "/xhibit/mock-data/listing.query.courtlist-with-fixed-date-corresponding-hearing-types-list.json";

    public PublishCourtListType publishCourtListType = PublishCourtListType.WARN;

    @Mock
    private CourtServicesMapper courtServicesMapper;

    @Mock
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private Logger logger;

    @Mock
    private ListingService listingService;

    @Spy
    private XmlUtils xmlUtils;

    @InjectMocks
    private WarnedListMapper warnedListMapper;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        xmlUtils.setLogger(logger);
        xmlUtils.postConstruct();
        when(courtServicesMapper.getHearingTypeForHearing(fromString("5ae4c090-0f70-4694-b4fc-707633d2b430"))).thenReturn(HEARING_TYPE_CODE);

        final String crestCourtId = "000";
        final CourtLocation courtLocation1 = createCourtLocation(crestCourtId, "1");
        final CourtLocation courtLocation2 = createCourtLocation(crestCourtId, "2");

        final List<UUID> courtCentreIds = Arrays.asList(courtCentreId1, courtCentreId2);

        when(commonXhibitReferenceDataService.getCrownCourtDetails(courtCentreId1)).thenReturn(courtLocation1);
        when(commonXhibitReferenceDataService.getCrownCourtDetails(courtCentreId2)).thenReturn(courtLocation2);
        when(commonXhibitReferenceDataService.getMagsCourtDetails(courtCentreId1)).thenReturn(courtLocation1);
        when(commonXhibitReferenceDataService.getMagsCourtDetails(courtCentreId2)).thenReturn(courtLocation2);
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
        return Json.createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtLists", Json.createArrayBuilder()
                        .add(
                                Json.createObjectBuilder()
                                        .add("crestCourtSite", crestCourtSite())
                                        .add("sittings", Json.createArrayBuilder().build())
                                        .build()
                        ).build()
                ).build();
    }

    private JsonObjectBuilder crestCourtSite() {
        return Json.createObjectBuilder()
                .add("crestCourtSiteId", "002")
                .add("crestCourtSiteName", "MOCKCOURTNAME2")
                .add("courtType", "CROWN_COURT");
    }


    @Test
    public void shouldGetXhibitHearingType() {
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/hearingTypesData.json");
        final String actual = warnedListMapper.getXhibitHearingType(courtListJson, fromString("5ae4c090-0f70-4694-b4fc-707633d2b430"));
        assertThat(actual, is(HEARING_TYPE_CODE));
    }

    @Test
    public void shouldGetXhibitHearingTypeEmpty() {
        final JsonObject courtListJson = givenPayload("/xhibit/mock-data/hearingTypesEmptyData.json");
        final String actual = warnedListMapper.getXhibitHearingType(courtListJson, fromString("5ae4c090-0f70-4694-b4fc-707633d2b430"));
        assertThat(actual, is(nullValue()));
    }

    @Test
    public void shouldGenerateCourtList() throws IOException {

        ZonedDateTime timeStamp = ZonedDateTime.now();
        final PublishCourtListRequestParameters requestParameters = withDefaults()
                .withCourtCentreId(courtCentreId1)
                .publishCourtListType(publishCourtListType)
                .withStartDate(startDate)
                .withRequestedTime(timeStamp)
                .build();
        final CourtListMetadata metadata = new CourtListMetadata(publishCourtListType.name() + "-FILENAME",
                "UNIQUEID", parse("2018-01-02T13:04:05+00:00[Europe/London]"));

        final CourtListGenerationContext context =
                new CourtListGenerationContext(envelope, requestParameters, metadata);

        final JsonObject courtListJson = givenPayload(COURT_LIST_WITH_CASE_WITH_DIFFERENT_HEARING_TYPE_INPUT_LIST_JSON_FILE);
        final List<JsonObject> courtCentreCourtLists = courtListJson.getJsonArray("courtLists").getValuesAs(JsonObject.class);
        warnedListMapper = new WarnedListMapper(context, courtCentreCourtLists, new CourtServicesMapper(context, commonXhibitReferenceDataService));
        final JAXBElement actual = warnedListMapper.generate();
        LOGGER.info("generatedXml:\n{}", xmlUtils.convertToXml(actual));
        assertXmlEquals(xmlUtils.convertToXml(actual), "xhibit/expectedSingleWarnedListMapper.xml", singletonMap("#TIME_STAMP#", requestParameters.getRequestedTime().toLocalDateTime().toString()));
    }
}
