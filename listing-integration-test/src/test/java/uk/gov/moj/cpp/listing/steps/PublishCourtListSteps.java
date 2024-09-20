package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithForPublishingCourtListsWithoutReportingRestriction;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.it.util.XmlEditor;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;
import uk.gov.moj.cpp.listing.utils.WebDavStub;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PublishCourtListSteps extends CommonHearingSteps {

    private static final Logger LOGGER = LoggerFactory.getLogger(PublishCourtListSteps.class);

    private static final String MEDIA_TYPE_QUERY_COURT_LIST_STATUS = "application/vnd.listing.court.list.publish.status+json";
    private static final String LISTING_COMMAND_PUBLISH_COURT_LIST = "listing.command.publish-court-list";
    private static final String LISTING_COMMAND_EXPORT_COURT_LIST = "listing.command.export-court-list";
    private static final String MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST = "application/vnd.listing.command.publish-court-list+json";
    private static final String MEDIA_TYPE_QUERY_PUBLISHEDCOURTLIST = "application/vnd.listing.publishedcourtlist+json";
    private static final String MEDIA_TYPE_LISTING_COMMAND_EXPORT_COURT_LIST = "application/vnd.listing.command.court-list-request-export+json";

    private static final String PRESTON_COURT_NAME = "PRESTON";
    private static final String PRESTON_COURT_SITE_NAME = "PRESTON";
    private static final String PRESTON_COURT_ID = "448";
    private static final String PRESTON_COURT_SITE_ID = "448";
    private static final String EVENT_SELECTED_PUBLIC_COURT_LIST_STAGING_DARTS = "public.listing.court-daily-list";
    private static final String EVENT_SELECTED_PUBLIC_COURT_LIST_PUBLISHED = "public.listing.court-list-published";


    private JsonObject commandJsonObject;
    private JmsMessageConsumerClient publicMessageConsumerStagingDartsUpdated;
    private JmsMessageConsumerClient publicMessageConsumerPublishCourtList;

    public PublishCourtListSteps(final HearingsData hearingsData, final JsonObject commandJsonObject) {
        super(hearingsData);
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        this.commandJsonObject = commandJsonObject;
    }

    private static void createHearingListed(final HearingsData hearingsData) {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
    }

    public void acceptCourtListXmlFiles() {
        WebDavStub.acceptCourtListXmlFile(Response.Status.OK);
    }

    public void sendPublishCourtListCommand() {

        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(LISTING_COMMAND_PUBLISH_COURT_LIST),
                commandJsonObject.getString("courtCentreId")));
        final String request = commandJsonObject.toString();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public JsonObject verifyCourtListPublishStatus(final String expectedPublishStatus, final String weekCommencing) {
        final String courtCentreId = commandJsonObject.getString("courtCentreId");
        final String courtListType = commandJsonObject.getString("publishCourtListType");
        final String publishDate = LocalDate.now().toString();
        final String queryPart = format(readConfig().getProperty("listing.court.list.publish.status"),
                courtCentreId,
                courtListType,
                publishDate,
                weekCommencing);
        final String searchCourtListUrl = String.format("%s/%s", getBaseUri(), queryPart);

        final ResponseData response = poll(requestParams(searchCourtListUrl, MEDIA_TYPE_QUERY_COURT_LIST_STATUS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.publishCourtListStatuses[0].courtCentreId",
                                        equalTo(courtCentreId)),
                                withJsonPath("$.publishCourtListStatuses[0].publishCourtListType",
                                        equalTo(courtListType)),
                                withJsonPath("$.publishCourtListStatuses[0].lastUpdated",
                                        is(notNullValue())),
                                withJsonPath("$.publishCourtListStatuses[0].publishStatus",
                                        equalTo(expectedPublishStatus))
                        )));

        return jsonFromString(response.getPayload());
    }

    public void createMessageConsumer() {
        publicMessageConsumerStagingDartsUpdated = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_COURT_LIST_STAGING_DARTS);
        publicMessageConsumerPublishCourtList = publicEvents.createPublicConsumer(EVENT_SELECTED_PUBLIC_COURT_LIST_PUBLISHED);
    }


    public static HearingsData loadHearingDataWithJudiciary(final UUID courtCentreId) {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(courtCentreId, UUID.randomUUID(), "DISTRICT_JUDGE");
        createHearingListed(hearingsData);
        return hearingsData;
    }

    public static HearingsData loadHearingDataWithJudiciary(final UUID courtCentreId, final UUID courtRoomId) {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(courtCentreId, courtRoomId, "DISTRICT_JUDGE");
        createHearingListed(hearingsData);
        return hearingsData;
    }

    public static HearingsData loadHearingData(final UUID courtCentreId, final UUID courtRoomId) {
        final HearingsData hearingsData = hearingsDataWithForPublishingCourtListsWithoutReportingRestriction(courtCentreId, courtRoomId, "DISTRICT_JUDGE");
        createHearingForListing(hearingsData);
        return hearingsData;
    }

    public void verifySentPublishedCourtListHearingData(final boolean checkVideoLink, final String videoLinkDetails) throws Exception {

        verifyCourtHeader();
        verifyCourtCourtList(checkVideoLink, videoLinkDetails);

    }

    public void verifySentRestrictedPublishedCourtListHearingData() throws Exception {

        verifyCourtHeader();
        verifyCourtCourtList();

    }

    public void verifySentPublishedCourtListHearingDataForDraft(final boolean checkVideoLink, final String videoLinkDetails) throws Exception {

        verifyCourtHeaderDailyList();
        verifyCourtCourtListDailyList(checkVideoLink, videoLinkDetails);

    }
    public void verifySentPublishedCourtListHearingDataForFirm() throws Exception {

        verifyCourtHeaderFirmList();
        final String sentXml = getSentXml();
        XpathEngine simpleXpathEngine = XMLUnit.newXpathEngine();
        assertEquals("1", simpleXpathEngine.evaluate("count(/*[local-name()='FirmList']/*[local-name()='ReserveList'])", XMLUnit.buildControlDocument(sentXml)));
        assertEquals("4", simpleXpathEngine.evaluate("count(/*[local-name()='FirmList']/*[local-name()='ReserveList']" +
                "/*[local-name()='Hearing'])", XMLUnit.buildControlDocument(sentXml)));
    }

    public void verifySentPublishedCourtListHearingDataFirmWithSittingTagPresent() throws Exception {

        verifyCourtHeaderFirmList();
        final String sentXml = getSentXml();
        XpathEngine simpleXpathEngine = XMLUnit.newXpathEngine();
        assertEquals("4", simpleXpathEngine.evaluate("count(/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']/*[local-name()='Sittings'])", XMLUnit.buildControlDocument(sentXml)));
    }

    public void verifySentPublishedCourtListHearingDataForWarn() throws Exception {

        verifyCourtHeaderWarnList();
        final String sentXml = getSentXml();
        XpathEngine simpleXpathEngine = XMLUnit.newXpathEngine();
        assertEquals("2", simpleXpathEngine.evaluate("count(/*[local-name()='WarnedList']/*[local-name()='CourtLists']/*[local-name()='CourtList']/*[local-name()='WithoutFixedDate'])", XMLUnit.buildControlDocument(sentXml)));
        assertEquals("4", simpleXpathEngine.evaluate("count(/*[local-name()='WarnedList']/*[local-name()='CourtLists']/*[local-name()='CourtList']/*[local-name()='WithoutFixedDate']" +
                "/*[local-name()='Fixture']/*[local-name()='Cases']/*[local-name()='Case'])", XMLUnit.buildControlDocument(sentXml)));
    }

    private void verifyCourtHeader() throws Exception {

        final String sentXml = getSentXml();

        assertXpathEvaluatesTo(PRESTON_COURT_NAME, "//*[local-name()='CourtHouseName']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_ID, "/*[local-name()='FirmList']/*[local-name()='CrownCourt']/*[local-name()='CourtHouseCode']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_NAME, "/*[local-name()='FirmList']/*[local-name()='CrownCourt']/*[local-name()='CourtHouseName']/text()", sentXml);

    }

    private void verifyCourtHeaderDailyList() throws Exception {

        final String sentXml = getSentXml();

        assertXpathEvaluatesTo(PRESTON_COURT_NAME, "//*[local-name()='CourtHouseName']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_ID, "/*[local-name()='DailyList']/*[local-name()='CrownCourt']/*[local-name()='CourtHouseCode']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_NAME, "/*[local-name()='DailyList']/*[local-name()='CrownCourt']/*[local-name()='CourtHouseName']/text()", sentXml);

    }

    private void verifyCourtHeaderWarnList() throws Exception {

        final String sentXml = getSentXml();

        assertXpathEvaluatesTo(PRESTON_COURT_NAME, "//*[local-name()='CourtHouseName']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_ID, "/*[local-name()='WarnedList']/*[local-name()='CrownCourt']/*[local-name()='CourtHouseCode']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_NAME, "/*[local-name()='WarnedList']/*[local-name()='CrownCourt']/*[local-name()='CourtHouseName']/text()", sentXml);
    }

    private void verifyCourtHeaderFirmList() throws Exception {

        final String sentXml = getSentXml();

        assertXpathEvaluatesTo(PRESTON_COURT_NAME, "//*[local-name()='CourtHouseName']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_ID, "/*[local-name()='FirmList']/*[local-name()='CrownCourt']/*[local-name()='CourtHouseCode']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_NAME, "/*[local-name()='FirmList']/*[local-name()='CrownCourt']/*[local-name()='CourtHouseName']/text()", sentXml);
    }

    private void verifyCourtCourtList() throws Exception {

        final String sentXml = getSentXml();

        assertXpathEvaluatesTo(PRESTON_COURT_SITE_NAME, "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='CourtHouse']/*[local-name()='CourtHouseName']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_SITE_ID, "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='CourtHouse']/*[local-name()='CourtHouseCode']/text()", sentXml);
        assertXpathEvaluatesTo("1", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='CourtRoomNumber']/text()", sentXml);
        assertXpathEvaluatesTo("Mark J", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameForename']/text()", sentXml);
        assertXpathEvaluatesTo("Ainsworth", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameSurname']/text()", sentXml);
        assertXpathEvaluatesTo("Recorder Ainsworth judge", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameRequestedName']/text()", sentXml);


    }

    private void verifyCourtCourtList(final boolean checkVideoLink, final String videoLinkDetails) throws Exception {

        final String sentXml = getSentXml();

        assertXpathEvaluatesTo(PRESTON_COURT_SITE_NAME, "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='CourtHouse']/*[local-name()='CourtHouseName']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_SITE_ID, "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='CourtHouse']/*[local-name()='CourtHouseCode']/text()", sentXml);
        assertXpathEvaluatesTo("1", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='CourtRoomNumber']/text()", sentXml);
        assertXpathEvaluatesTo("Mark J", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameForename']/text()", sentXml);
        assertXpathEvaluatesTo("Ainsworth", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameSurname']/text()", sentXml);
        assertXpathEvaluatesTo("Recorder Ainsworth judge", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameRequestedName']/text()", sentXml);
        assertXpathEvaluatesTo("PTP", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='HearingDetails']/@HearingType", sentXml);

        if(checkVideoLink) {
            assertXpathEvaluatesTo(videoLinkDetails, "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                    "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='ListNote']/text()", sentXml);
        }

    }

    private void verifyCourtCourtListDailyList(final boolean checkVideoLink, final String videoLinkDetails) throws Exception {

        final String sentXml = getSentXml();

        assertXpathEvaluatesTo(PRESTON_COURT_SITE_NAME, "/*[local-name()='DailyList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='CourtHouse']/*[local-name()='CourtHouseName']/text()", sentXml);
        assertXpathEvaluatesTo(PRESTON_COURT_SITE_ID, "/*[local-name()='DailyList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='CourtHouse']/*[local-name()='CourtHouseCode']/text()", sentXml);
        assertXpathEvaluatesTo("1", "/*[local-name()='DailyList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='CourtRoomNumber']/text()", sentXml);
        assertXpathEvaluatesTo("Mark J", "/*[local-name()='DailyList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameForename']/text()", sentXml);
        assertXpathEvaluatesTo("Ainsworth", "/*[local-name()='DailyList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameSurname']/text()", sentXml);
        assertXpathEvaluatesTo("Recorder Ainsworth judge", "/*[local-name()='DailyList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameRequestedName']/text()", sentXml);
        assertXpathEvaluatesTo("PTP", "/*[local-name()='DailyList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='HearingDetails']/@HearingType", sentXml);

        if(checkVideoLink) {
            assertXpathEvaluatesTo(videoLinkDetails, "/*[local-name()='DailyList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                    "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='ListNote']/text()", sentXml);
        }

    }

    public void verifySentPublishedCourtListHasNoHearings() throws Exception {
        verifySentPublishedCourtListFileIsExpected("expectations/FirmList-with-no-hearings.xml");
    }

    public void verifySentPublishedCourtListFileIsExpected(final String expectedXmlFile) throws Exception {

        String expectedXml = getPayload(expectedXmlFile);

        expectedXml = updateDynamicElements(expectedXml);

        final String sentXml = getSentXml();

        assertXMLEqual(expectedXml, sentXml);
    }

    private String getSentXml() {
        String sentXml = WebDavStub.getSentXml();

        sentXml = replaceIndeterminantElements(sentXml);

        LOGGER.info("sentXml=\n" + sentXml);
        return sentXml;
    }

    private String updateDynamicElements(final String expectedXml) {
        return XmlEditor.edit(expectedXml)
                .replaceElementValue("cs:StartDate", commandJsonObject.getString("startDate"))
                .replaceElementValue("cs:EndDate", commandJsonObject.getString("endDate"))
                .replaceAttributeValue("cs:CourtList", "SittingDate", commandJsonObject.getString("startDate"))
                .save();
    }

    private String replaceIndeterminantElements(final String sentXml) {
        return XmlEditor.edit(sentXml)
                .replaceElementValue("cs:DocumentName", "FILENAME")
                .replaceElementValue("cs:UniqueID", "UNIQUEID")
                .replaceElementValue("cs:PublishedTime", "PUBLISHEDTIME")
                .replaceElementValue("cs:TimeStamp", "TIMESTAMP")
                .save();
    }

    private JsonObject jsonFromString(final String jsonObjectStr) {

        JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        JsonObject object = jsonReader.readObject();
        jsonReader.close();

        return object;
    }


    /**
     * Note that we cannot be sure as to the exact status, but if there is a record for this Court
     * Centre (which is presumed to have a unique id), then it's enough to confirm that we've
     * requested the publication, at least.
     */
    public void verifyThatWeSuccessfullyRequestedAFinalListPublication(final UUID courtCentreId, final LocalDate expectedPublishDate) {
        final String expectedCourtListType = "FINAL";
        final String weekCommencing = "false";
        final String queryPart = format(readConfig().getProperty("listing.court.list.publish.status"),
                courtCentreId,
                expectedCourtListType,
                expectedPublishDate,
                weekCommencing);

        final String searchCourtListUrl = String.format("%s/%s", getBaseUri(), queryPart);

        poll(requestParams(searchCourtListUrl, MEDIA_TYPE_QUERY_COURT_LIST_STATUS)
                .withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.publishCourtListStatuses[0].courtCentreId",
                                        equalTo(courtCentreId.toString()))
                        )));
    }

    public static JsonObject buildPublishCourtListCommandPayload(final UUID courtCentreId,
                                                                 final PublishCourtListType publishCourtListType,
                                                                 final LocalDate startDate) {
        return createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("startDate", startDate.toString())
                .add("endDate", startDate.plusDays(5).toString())
                .add("sendNotificationToParties", true)
                .add("publishCourtListType", publishCourtListType.name())
                .build();
    }

    public static JsonObject buildCourtListCommandPayload(final UUID courtCentreId,
                                                                 final CourtListType courtListType,
                                                                 final LocalDate startDate) {
        return createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("startDate", startDate.toString())
                .add("endDate", startDate.plusDays(5).toString())
                .add("listId", courtListType.name())
                .build();
    }


    public void waitForCompletedExport(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {

        final String queryPart = format(readConfig().getProperty("listing.publishedcourtlist"),
                courtCentreId,
                publishCourtListType,
                startDate);
        final String url = String.format("%s/%s", getBaseUri(), queryPart);

        final ResponseData response = poll(requestParams(url, MEDIA_TYPE_QUERY_PUBLISHEDCOURTLIST).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.publishedCourtLists[0].lastExported",
                                        is(notNullValue()))
                        )));
    }

    public void waitForPublishedCourtListStored(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {

        final String queryPart = format(readConfig().getProperty("listing.publishedcourtlist"),
                courtCentreId,
                publishCourtListType,
                startDate);
        final String url = String.format("%s/%s", getBaseUri(), queryPart);

        final ResponseData response = poll(requestParams(url, MEDIA_TYPE_QUERY_PUBLISHEDCOURTLIST).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.publishedCourtLists[0].lastUpdated",
                                        is(notNullValue()))
                        )));
    }

    public void triggerExportTimer(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {

        final String commandUrl = String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(LISTING_COMMAND_EXPORT_COURT_LIST),
                courtCentreId.toString(), publishCourtListType.name(), startDate.toString()));
        final String request = commandJsonObject.toString();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", commandUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request);

        final Response response = restClient.postCommand(commandUrl, MEDIA_TYPE_LISTING_COMMAND_EXPORT_COURT_LIST, request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    private static void createHearingForListing(final HearingsData hearingsData) {
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedAndListed();
    }

    public void verifyPublicEventForCourtList(final UUID courtCentreId) {

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerStagingDartsUpdated);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingUpdated: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId.toString()));
        assertThat(jsonResponse.get("dailyListDocument"), containsString("DailyList"));

    }

    public void verifyPublicEventForCourtListPublished(final String courtCentreId, final String publishCourtListType, final Boolean weekCommencing, final Boolean sendNotificationToParties, final int courtListItems) {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicMessageConsumerPublishCourtList);
        LOGGER.info("jsonResponse from publicMessageConsumerHearingUpdated: {}", jsonResponse.prettify());
        LOGGER.info("jsonResponse from publicMessageConsumerHearingUpdated ");
        assertThat(jsonResponse.get("courtCentreId"), is(courtCentreId.toString()));
        assertThat(jsonResponse.get("publishCourtListType"), is(publishCourtListType));
        assertThat(jsonResponse.getBoolean("weekCommencing"), is(weekCommencing));
        assertThat(jsonResponse.getBoolean("sendNotificationToParties"), is(sendNotificationToParties));
        assertThat(jsonResponse.getList("courtLists"), Matchers.hasSize(courtListItems));
    }



    public void verifyDefendantNameIsMasked() throws Exception {

        final String sentXml = getSentXml();
        assertXpathEvaluatesTo("yes", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='Defendants']" +
                "/*[local-name()='Defendant']/*[local-name()='PersonalDetails']/*[local-name()='IsMasked']/text()", sentXml);

        assertXpathEvaluatesTo("******", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='Defendants']" +
                "/*[local-name()='Defendant']/*[local-name()='PersonalDetails']/*[local-name()='Name']/*[local-name()='CitizenNameForename']/text()", sentXml);

        assertXpathEvaluatesTo("******", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='Defendants']" +
                "/*[local-name()='Defendant']/*[local-name()='PersonalDetails']/*[local-name()='Name']/*[local-name()='CitizenNameSurname']/text()", sentXml);

        assertXpathEvaluatesTo("******", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='Defendants']" +
                "/*[local-name()='Defendant']/*[local-name()='PersonalDetails']/*[local-name()='Name']/*[local-name()='CitizenNameRequestedName']/text()", sentXml);
    }
}
