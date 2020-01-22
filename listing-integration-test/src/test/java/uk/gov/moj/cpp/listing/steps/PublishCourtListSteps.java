package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsDataWithAllocationDataAndJudiciary;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.it.util.XmlEditor;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.WebDavStub;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.Response;

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

    private JsonObject commandJsonObject;

    public PublishCourtListSteps(final HearingsData hearingsData, final JsonObject commandJsonObject) {
        super(hearingsData);
        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
        this.commandJsonObject = commandJsonObject;
    }

    private static void createHearingListed(final HearingsData hearingsData) {
        try (final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData)) {
            listCourtHearingSteps.whenCaseIsSubmittedForListing();
        }
    }

    public void acceptCourtListXmlFiles() {
        WebDavStub.acceptCourtListXmlFile(Response.Status.OK);
    }

    public void sendPublishCourtListCommand() {

        final String updateHearingUrl = String.format("%s/%s", baseUri, format(ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_PUBLISH_COURT_LIST),
                commandJsonObject.getString("courtCentreId")));
        final String request = commandJsonObject.toString();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", updateHearingUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request, getLoggedInHeader());

        final Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }

    public JsonObject verifyCourtListPublishStatus(final String expectedPublishStatus) {
        final String courtCentreId = commandJsonObject.getString("courtCentreId");
        final String courtListType = commandJsonObject.getString("publishCourtListType");
        final String publishDate = LocalDate.now().toString();
        final String weekCommencing = "true";
        final String queryPart = format(ENDPOINT_PROPERTIES.getProperty("listing.court.list.publish.status"),
                courtCentreId,
                courtListType,
                publishDate,
                weekCommencing);
        final String searchCourtListUrl = String.format("%s/%s", baseUri, queryPart);

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

    public static HearingsData loadHearingDataWithJudiciary(final UUID courtCentreId) {
        final HearingsData hearingsData = hearingsDataWithAllocationDataAndJudiciary(courtCentreId, "DISTRICT_JUDGE");
        createHearingListed(hearingsData);
        return hearingsData;
    }

    public void verifySentPublishedCourtListHearingData() throws Exception {

        verifyCourtHeader();
        verifyCourtCourtList();

    }

    private void verifyCourtHeader() throws Exception {

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
        assertXpathEvaluatesTo("231", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='CourtRoomNumber']/text()", sentXml);
        assertXpathEvaluatesTo("Ainsworth", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Judiciary']/*[local-name()='Judge']/*[local-name()='CitizenNameSurname']/text()", sentXml);
        assertXpathEvaluatesTo("PTP", "/*[local-name()='FirmList']/*[local-name()='CourtLists']/*[local-name()='CourtList']" +
                "/*[local-name()='Sittings']/*[local-name()='Sitting']/*[local-name()='Hearings']/*[local-name()='Hearing']/*[local-name()='HearingDetails']/@HearingType", sentXml);

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
        final String queryPart = format(ENDPOINT_PROPERTIES.getProperty("listing.court.list.publish.status"),
                courtCentreId,
                expectedCourtListType,
                expectedPublishDate,
                weekCommencing);

        final String searchCourtListUrl = String.format("%s/%s", baseUri, queryPart);

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
                .add("publishCourtListType", publishCourtListType.name())
                .build();
    }


    public void waitForCompletedExport(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {

        final String queryPart = format(ENDPOINT_PROPERTIES.getProperty("listing.publishedcourtlist"),
                courtCentreId,
                publishCourtListType,
                startDate);
        final String url = String.format("%s/%s", baseUri, queryPart);

        final ResponseData response = poll(requestParams(url, MEDIA_TYPE_QUERY_PUBLISHEDCOURTLIST).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.publishedCourtLists[0].lastExported",
                                        is(notNullValue()))
                        )));
    }

    public void waitForPublishedCourtListStored(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {

        final String queryPart = format(ENDPOINT_PROPERTIES.getProperty("listing.publishedcourtlist"),
                courtCentreId,
                publishCourtListType,
                startDate);
        final String url = String.format("%s/%s", baseUri, queryPart);

        final ResponseData response = poll(requestParams(url, MEDIA_TYPE_QUERY_PUBLISHEDCOURTLIST).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.publishedCourtLists[0].lastUpdated",
                                        is(notNullValue()))
                        )));
    }

    public void triggerExportTimer(final UUID courtCentreId, final PublishCourtListType publishCourtListType, final LocalDate startDate) {

        final String commandUrl = String.format("%s/%s", baseUri, format(ENDPOINT_PROPERTIES.getProperty(LISTING_COMMAND_EXPORT_COURT_LIST),
                courtCentreId.toString(), publishCourtListType.name(), startDate.toString()));
        final String request = commandJsonObject.toString();

        LOGGER.info("Post call made: \n\n\tURL = {} \n\tMedia type = {} \n\tPayload = {}\n\n", commandUrl, MEDIA_TYPE_LISTING_COMMAND_PUBLISH_COURT_LIST, request, getLoggedInHeader());

        final Response response = restClient.postCommand(commandUrl, MEDIA_TYPE_LISTING_COMMAND_EXPORT_COURT_LIST, request, getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }
}
