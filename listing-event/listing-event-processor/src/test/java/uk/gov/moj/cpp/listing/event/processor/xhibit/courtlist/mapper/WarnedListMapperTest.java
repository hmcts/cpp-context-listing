package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.time.ZonedDateTime.parse;
import static java.util.Collections.singletonMap;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
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
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WarnedListMapperTest {

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

    @Spy
    private XmlUtils xmlUtils;

    @InjectMocks
    private WarnedListMapper warnedListMapper;

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

    @BeforeEach
    public void setupXmlUtils() {
        xmlUtils.postConstruct();
    }

    @Test
    public void shouldGetXhibitHearingType() {

        when(courtServicesMapper.getHearingTypeForHearing(fromString("5ae4c090-0f70-4694-b4fc-707633d2b430"))).thenReturn(HEARING_TYPE_CODE);

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

        final String crestCourtId = "000";
        final CourtLocation courtLocation1 = createCourtLocation(crestCourtId, "1");

        when(commonXhibitReferenceDataService.getCrownCourtDetails(courtCentreId1)).thenReturn(courtLocation1);

        final HearingType hearingType = new HearingType.Builder()
                .withExhibitHearingCode("TRL")
                .withExhibitHearingDescription("XHIBIT_HEARING_DESCRIPTION-TRL")
                .build();

        final HearingType hearingType1 = new HearingType.Builder()
                .withExhibitHearingCode("PTP")
                .withExhibitHearingDescription("XHIBIT_HEARING_DESCRIPTION-PTP")
                .build();


        final UUID hearingTypeId = UUID.fromString("bf8155e1-90b9-4080-b133-bfbad895d6e4");
        when(commonXhibitReferenceDataService.getXhibitHearingType(any())).thenReturn(hearingType);

        when(commonXhibitReferenceDataService.getXhibitHearingType(eq(hearingTypeId))).thenReturn(hearingType);

        final UUID hearingTypeId1 = UUID.fromString("06b0c2bf-3f98-46ed-ab7e-56efaf9ecced");

        when(commonXhibitReferenceDataService.getXhibitHearingType(eq(hearingTypeId1))).thenReturn(hearingType1);

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
        assertXmlEquals(xmlUtils.convertToXml(actual), "xhibit/expectedSingleWarnedListMapper.xml", singletonMap("#TIME_STAMP#", requestParameters.getRequestedTime().toLocalDateTime().toString()));
    }
}
