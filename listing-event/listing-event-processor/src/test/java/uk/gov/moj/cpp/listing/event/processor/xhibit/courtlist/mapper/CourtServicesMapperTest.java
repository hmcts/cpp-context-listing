package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersBuilder.withDefaults;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtHouseStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DocumentIDstructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ListHeaderStructure;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.ProsecutingAuthorityType;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.SittingStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlTestUtils;
import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.InvalidDataException;

import javax.json.JsonObject;

public class CourtServicesMapperTest extends BaseMapperTest {

    private CourtServicesMapper courtServicesMapper;

    @Before
    public void before() {
        requestParameters = withDefaults()
                .withRequestedTime(ZonedDateTime.now())
                .build();
        context = new CourtListGenerationContext(envelope, requestParameters, metadata);
        courtServicesMapper = new CourtServicesMapper(context, commonXhibitReferenceDataService);
    }

    @Test
    public void generateDocumentID() {

        DocumentIDstructure documentIDstructure = courtServicesMapper.generateDocumentID();

        assertThat(documentIDstructure.getUniqueID(), is("UNIQUEID"));
        assertThat(documentIDstructure.getDocumentType(), is("FL"));
        assertThat(documentIDstructure.getDocumentName(), is("FIRM-FILENAME"));
        assertThat(LocalDateTime.parse(documentIDstructure.getTimeStamp().toString()), is(requestParameters.getRequestedTime().toLocalDateTime()));

    }

    @Test
    public void generateListHeader() {

        ListHeaderStructure listHeaderStructure = courtServicesMapper.generateListHeader();

        assertThat(listHeaderStructure.getVersion(), is("NOT VERSIONED"));
        assertThat(listHeaderStructure.getListCategory(), is("Criminal"));
        assertThat(listHeaderStructure.getStartDate(), is(LocalDate.parse("2019-11-04")));
        assertThat(listHeaderStructure.getEndDate(), is(LocalDate.parse("2019-11-05")));
        assertThat(listHeaderStructure.getPublishedTime().toString(), is("2018-01-02T13:04:05Z"));
    }

    @Test
    public void generateSittingStructure() {

        final JsonObject sitting = givenPayload("/xhibit/mock-data/listing.query.sittings.json")
                .getJsonArray("sittings").getValuesAs(JsonObject.class).get(0);


        SittingStructure listHeaderStructure = courtServicesMapper.generateSittingStructure(sitting, 1);

        assertThat(listHeaderStructure.getHearings(), is(notNullValue()));
        assertThat(listHeaderStructure.getHearings().getHearing().get(0).getProsecution(), is(notNullValue()));
        assertThat(listHeaderStructure.getHearings().getHearing().get(0).getProsecution().getProsecutingAuthority(), is(ProsecutingAuthorityType.OTHER_PROSECUTOR));
    }

    @Test
    public void generateSittingStructureForProsecutor() {

        final JsonObject sitting = givenPayload("/xhibit/mock-data/listing.query.sittings.json")
                .getJsonArray("sittings").getValuesAs(JsonObject.class).get(1);


        SittingStructure listHeaderStructure = courtServicesMapper.generateSittingStructure(sitting, 1);

        assertThat(listHeaderStructure.getHearings(), is(notNullValue()));
        assertThat(listHeaderStructure.getHearings().getHearing().get(0).getProsecution(), is(notNullValue()));
        assertThat(listHeaderStructure.getHearings().getHearing().get(0).getProsecution().getProsecutingAuthority(), is(ProsecutingAuthorityType.CROWN_PROSECUTION_SERVICE));
    }

    @Test
    public void generateCourtHouseStructure() {

        UUID courtCentreId = UUID.fromString("973961d2-ae3f-44d1-8926-c9b66edc2df2");

        CourtHouseStructure courtHouseStructure = courtServicesMapper.generateCourtHouseStructure(courtCentreId);

        assertThat(courtHouseStructure.getCourtHouseType().value(), is("Crown Court"));
        assertThat(courtHouseStructure.getCourtHouseName(), is("MOCKCOURTNAME"));
        assertThat(courtHouseStructure.getCourtHouseCode().getValue(), is("001"));
    }

    @Test
    public void generateSortedByCourtRoomID() throws Exception {

        final UUID courtCentreId = context.getParameters().getCourtCentreId();

        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(30);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c40"))).thenReturn(10);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c41"))).thenReturn(20);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        final String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + PublishCourtListType.FIRM.getSchemaName());

        assertXml(generatedXml, "xhibit/mapper/expectedFirmListSortedSittingMapperTest.xml");
    }

    @Test
    public void generateSortedByCourtRoomIDWithVideoLinkForApplicationAndCase() throws Exception {

        final UUID courtCentreId = context.getParameters().getCourtCentreId();

        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(30);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c40"))).thenReturn(10);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c41"))).thenReturn(20);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-with-videolink.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        final String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + PublishCourtListType.FIRM.getSchemaName());

        assertXml(generatedXml, "xhibit/mapper/expectedFirmListSortedSittingWithVideoLinkMapperTest.xml");
    }


    @Test
    public void generateSortedByCourtRoomIDWithVideoLinkForApplicationAndCaseForDraftPublishType() throws Exception {

        final UUID courtCentreId = context.getParameters().getCourtCentreId();

        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(30);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c40"))).thenReturn(10);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c41"))).thenReturn(20);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-with-videolink.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final DailyListMapper dailyListMapper = new DailyListMapper(context, courtListsForPublishing, courtServicesMapper);

        final String generatedXml = xmlUtils.convertToXml(dailyListMapper.generate());

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + PublishCourtListType.DRAFT.getSchemaName());

        assertXml(generatedXml, "xhibit/mapper/expectedDraftListSortedSittingWithVideoLinkMapperTest.xml");
    }


    @Test
    public void generateSortedByCourtRoomIDWithVideoLinkForApplicationAndCaseForFinalPublishType() throws Exception {

        final UUID courtCentreId = context.getParameters().getCourtCentreId();

        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(30);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c40"))).thenReturn(10);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c41"))).thenReturn(20);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-with-videolink.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final DailyListMapper dailyListMapper = new DailyListMapper(context, courtListsForPublishing, courtServicesMapper);

        final String generatedXml = xmlUtils.convertToXml(dailyListMapper.generate());

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + PublishCourtListType.FINAL.getSchemaName());

        assertXml(generatedXml, "xhibit/mapper/expectedDraftListSortedSittingWithVideoLinkMapperTest.xml");
    }

    @Test
    public void generateSortedByCourtRoomIDWhenJudgeTitleJudicialPrefixIsNotPresent() throws Exception {

        final UUID courtCentreId = context.getParameters().getCourtCentreId();

        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(30);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c40"))).thenReturn(10);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c41"))).thenReturn(20);
        final JsonObject judiciary = givenPayload("/xhibit/mock-data/referencedata.query.judiciaries.titleJudicialPrefixNotThere.json");
        when(commonXhibitReferenceDataService.getJudiciary(any())).thenReturn(judiciary);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final FirmListMapper firmListMapper = new FirmListMapper(context, courtListsForPublishing, courtServicesMapper);

        final String generatedXml = xmlUtils.convertToXml(firmListMapper.generate());

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + PublishCourtListType.FIRM.getSchemaName());

        assertXml(generatedXml, "xhibit/mapper/expectedFirmListSortedSittingJudgeTitleJudicialEmptyMapperTest.xml");
    }

    @Test
    public void shouldGenerateDailyListXmlWhenDefendantFirstNameIsNotProvided() throws Exception {

        final UUID courtCentreId = context.getParameters().getCourtCentreId();

        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(30);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c40"))).thenReturn(10);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c41"))).thenReturn(20);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-with-defendant-firstname-not-provided.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final DailyListMapper dailyListMapper = new DailyListMapper(context, courtListsForPublishing, courtServicesMapper);

        final String generatedXml = xmlUtils.convertToXml(dailyListMapper.generate());

        xmlUtils.validate(generatedXml, "xhibit/xsd/" + PublishCourtListType.FINAL.getSchemaName());

        assertXml(generatedXml, "xhibit/mapper/expectedFirmListWhenDefendantFirstNameNotProvided.xml");
    }

    @Test(expected = InvalidDataException.class)
    public void shouldThrowInvalidDataExceptionWhenGenerateDailyListXmlAndSurNameIsNotProvided() throws Exception {

        final UUID courtCentreId = context.getParameters().getCourtCentreId();

        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c39"))).thenReturn(30);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c40"))).thenReturn(10);
        when(commonXhibitReferenceDataService.getCourtRoomNumber(courtCentreId, UUID.fromString("7cb09222-49e1-3622-a5a6-ad253d2b3c41"))).thenReturn(20);

        final List<JsonObject> courtListsForPublishing = givenPayload("/xhibit/mock-data/listing.query.courtlist-daily-list-sittings-with-defendant-surname-not-provided.json")
                .getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final DailyListMapper dailyListMapper = new DailyListMapper(context, courtListsForPublishing, courtServicesMapper);

        final String generatedXml = xmlUtils.convertToXml(dailyListMapper.generate());

        assertXml(generatedXml, "xhibit/mapper/expectedFirmListWhenDefendantFirstNameNotProvided.xml");
    }

    private void assertXml(final String generatedXml, final String expectedXmlResourceName) throws IOException {
        XmlTestUtils.assertXmlEquals(generatedXml, expectedXmlResourceName, singletonMap("#TIME_STAMP#", requestParameters.getRequestedTime().toLocalDateTime().toString()));
    }
}
