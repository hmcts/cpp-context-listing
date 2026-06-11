package uk.gov.moj.cpp.listing.scenario;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;

import org.hamcrest.Matcher;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getJsonObject;
import static uk.gov.justice.services.test.utils.core.messaging.JsonObjects.getUUID;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;

public class ListCourtHearingStepsSpi extends AbstractIT {
    private static final String LISTING_COMMAND_LIST_COURT_HEARING = "listing.command.list-court-hearing";
    private static final String MEDIA_TYPE_LIST_COURT_HEARING = "application/vnd.listing.command.list-court-hearing+json";
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);

    protected final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    protected String request;

    private final ListingEventStoreUtility listingEventStoreUtility = new ListingEventStoreUtility();

    public ListCourtHearingStepsSpi() {

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public JsonObject preparePayloadToListCourtHearing(final String fileName, final Map<String, String> values) throws IOException {

        final String eventPayloadString = getStringFromResource(fileName)
                .replaceAll("%%HEARING_ID%%", values.get("hearingId"))
                .replaceAll("%%CASE_ID%%", values.get("caseId"))
                .replaceAll("%%COURT_CENTRE_ID%%", values.get("courtCentreId"))
                .replaceAll("%%COURT_ROOM_ID%%", values.get("courtRoomId"))
                .replaceAll("%%JURISDICTION_TYPE%%", values.get("jurisdictionType"))
                .replaceAll("%%CASE_URN%%", values.get("caseUrn"))
                .replaceAll("%%EARLIEST_START_TIME%%", values.get("hearingStartTime"))
                .replaceAll("ESTIMATED_MINUTES", values.get("estimatedMinutes"))
                .replaceAll("%%PROSECUTION_CASE_ID%%", values.get("prosecutionCaseId"))
                .replaceAll("%%HEARING_TYPE_ID%%", values.get("hearingTypeId"))
                .replaceAll("%%LISTED_START_DATE_TIME%%", values.get("listedStartDateTime"));

        return new StringToJsonObjectConverter().convert(eventPayloadString);
    }

    public void listCourtHearing(JsonObject listCourtHearingJsonObject) {
        final UUID courtCentreId = getUUID(getJsonObject(listCourtHearingJsonObject, "courtCentre").get(), "id").orElse(null);
        final UUID hearingTypeId = fromString(listCourtHearingJsonObject.getJsonObject("type").getString("id"));
        final JsonArray judiciary = listCourtHearingJsonObject.getJsonArray("judiciary");
        if (judiciary != null && !judiciary.isEmpty()) {
            final UUID judicialId = fromString(judiciary.getValuesAs(JsonObject.class).stream().findFirst().get().getString("id"));
            stubGetReferenceDataJudiciaries(judicialId);
        }

        final CourtCentreData courtCentreData = new CourtCentreData(courtCentreId, DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, null, "City of London Magistrates' Court");
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataHearingTypes(hearingTypeId);

        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_LIST_COURT_HEARING)));

        final JsonObjectBuilder listCourtHearingPayload = createObjectBuilder().add("hearings", createArrayBuilder().add(listCourtHearingJsonObject));

        request = listCourtHearingPayload.build().toString();

        final Response response = restClient.postCommand(listCaseForHearingUrl, MEDIA_TYPE_LIST_COURT_HEARING,
                request, getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }


    public void verifyAllocatedHearingFound(final Map<String,String> payload) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated"), true));

        // Poll on content (not just HTTP 200): the by.allocated read model lags the async
        // list-court-hearing command, so wait until the hearing actually appears.
        final String response = pollForHearing(searchHearingUrl, getLoggedInUser().toString(),
                new Matcher[]{withJsonPath("$.hearings[0].id", equalTo(payload.get("hearingId")))});
        final JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
        final JsonObject hearingJsonObject = jsonObject.getJsonArray("hearings").getJsonObject(0);

        assertThat(hearingJsonObject.getString("id"), is(payload.get("hearingId")));
        assertThat(hearingJsonObject.getString("courtCentreId"), is(payload.get("courtCentreId")));
        assertThat(hearingJsonObject.getString("courtRoomId"), is( payload.get("courtRoomId")));
        assertThat(hearingJsonObject.getString("jurisdictionType"), is( payload.get("jurisdictionType")));
        assertThat(hearingJsonObject.getJsonObject("type").getString("id"), is(payload.get("hearingTypeId")));
        assertThat(hearingJsonObject.getInt("estimatedMinutes"), is(Integer.parseInt(payload.get("estimatedMinutes"))));

        assertTrue(hearingJsonObject.getBoolean("allocated"));

        assertTrue(listingEventStoreUtility.checkEventExists(UUID.fromString(payload.get("hearingId"))));
    }

    public void verifyUnallocatedHearingFound(final Map<String,String> payload) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated"), false));

        final String response = pollForHearing(searchHearingUrl, getLoggedInUser().toString(),
                new Matcher[]{withJsonPath("$.hearings[0].id", equalTo(payload.get("hearingId")))});
        final JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
        final JsonObject hearingJsonObject = jsonObject.getJsonArray("hearings").getJsonObject(0);

        assertThat(hearingJsonObject.getString("id"), is(payload.get("hearingId")));
        assertThat(hearingJsonObject.getString("courtCentreId"), is(payload.get("courtCentreId")));
        assertThat(hearingJsonObject.getString("jurisdictionType"), is( payload.get("jurisdictionType")));
        assertThat(hearingJsonObject.getJsonObject("type").getString("id"), is(payload.get("hearingTypeId")));
        assertThat(hearingJsonObject.getInt("estimatedMinutes"), is(Integer.parseInt(payload.get("estimatedMinutes"))));

        assertFalse(hearingJsonObject.getBoolean("allocated"));

        assertTrue(listingEventStoreUtility.checkEventExists(UUID.fromString(payload.get("hearingId"))));
    }

    public void verifyUnallocatedTwoDefendantsHearingFound(final Map<String,String> payload) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated"), false));

        final String response = pollForHearing(searchHearingUrl, getLoggedInUser().toString(),
                new Matcher[]{withJsonPath("$.hearings[0].id", equalTo(payload.get("hearingId")))});
        final JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
        final JsonObject hearingJsonObject = jsonObject.getJsonArray("hearings").getJsonObject(0);
        final JsonArray listedCases = hearingJsonObject.getJsonArray("listedCases");
        assertEquals(1, listedCases.size());

        final JsonObject firstCase = listedCases.getJsonObject(0);
        final JsonArray defendants = firstCase.getJsonArray("defendants");
        assertEquals(2, defendants.size());
        assertThat(hearingJsonObject.getString("id"), is(payload.get("hearingId")));
        assertThat(hearingJsonObject.getString("courtCentreId"), is(payload.get("courtCentreId")));
        assertThat(hearingJsonObject.getString("jurisdictionType"), is( payload.get("jurisdictionType")));
        assertThat(hearingJsonObject.getJsonObject("type").getString("id"), is(payload.get("hearingTypeId")));
        assertThat(hearingJsonObject.getInt("estimatedMinutes"), is(Integer.parseInt(payload.get("estimatedMinutes"))));
        assertFalse(hearingJsonObject.getBoolean("allocated"));

        assertTrue(listingEventStoreUtility.checkEventExists(UUID.fromString(payload.get("hearingId"))));
    }
}