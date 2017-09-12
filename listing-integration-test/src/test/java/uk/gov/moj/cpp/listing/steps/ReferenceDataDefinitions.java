package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.Response.Status.OK;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.CourtRoomData;
import uk.gov.moj.cpp.listing.steps.data.JudgeData;

import java.util.UUID;

import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.Filter;
import com.jayway.jsonpath.JsonPath;

@SuppressWarnings("unchecked")
public class ReferenceDataDefinitions extends AbstractIT {

    private static final String MEDIA_TYPE_ADD_JUDGE = "application/vnd.listing" +
            ".command.add-judge+json";

    private static final String MEDIA_TYPE_GET_JUDGES = "application/vnd.listing" +
            ".get.judges+json";

    private static final String MEDIA_TYPE_ADD_COURT_CENTRE = "application/vnd.listing" +
            ".command.add-court-centre+json";

    private static final String MEDIA_TYPE_GET_COURT_CENTRES = "application/vnd.listing" +
            ".get.court-centres+json";

    private static final String MEDIA_TYPE_ADD_COURT_ROOM = "application/vnd.listing" +
            ".command.add-court-room+json";

    private static final String MEDIA_TYPE_GET_COURT_ROOMS = "application/vnd.listing" +
            ".get.court-rooms+json";


    private static final String FIELD_GENERIC_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_FIRST_NAME = "firstName";
    private static final String FIELD_LAST_NAME = "lastName";
    private static final String FIELD_COURT_CENTRE_NAME = "courtCentreName";
    private static final String FIELD_COURT_CENTRE = "courtCentre";
    private static final String FIELD_COURT_ROOM_NAME = "courtRoomName";

    public static void givenAUserHasLoggedInAsACourtClerk(final UUID validUserId) {
        setLoggedInUser(validUserId);
    }

    public static void whenJudgeHasBeenAddedItCanRetrieved(final JudgeData judgeData) {

        final String referenceDataUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.command.reference-data")));

        final JsonObjectBuilder judgeDataJson = prepareJsonForJudge(judgeData);

        final Response response = restClient.postCommand(referenceDataUrl, MEDIA_TYPE_ADD_JUDGE,
                judgeDataJson.build().toString(), getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));

        final String getJudgesUrl = String.format("%s/%s", baseUri, ENDPOINT_PROPERTIES.getProperty("listing.get.reference-data"));

        final Filter myFilter = filter(
                where("id").is(judgeData.getId().toString())
                        .and("firstName").is(judgeData.getFirstName())
                        .and("lastName").is(judgeData.getLastName())
                        .and("title").is(judgeData.getTitle()));
        final JsonPath judgeFilter = JsonPath.compile("$.judges[?]", myFilter);

        poll(requestParams(getJudgesUrl, MEDIA_TYPE_GET_JUDGES).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath(judgeFilter)));
    }


    private static JsonObjectBuilder prepareJsonForJudge(final JudgeData judgeData) {
        final JsonObjectBuilder judgeDataJson = createObjectBuilder();

        return judgeDataJson
                .add(FIELD_GENERIC_ID, judgeData.getId().toString())
                .add(FIELD_TITLE, judgeData.getTitle())
                .add(FIELD_FIRST_NAME, judgeData.getFirstName())
                .add(FIELD_LAST_NAME, judgeData.getLastName());
    }

    public static void whenCourtCentreHasBeenAddedAndItCanBeRetrieved(final CourtCentreData courtCentreData) {

        final String referenceDataUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.command.reference-data")));

        final JsonObjectBuilder courtCentreDataJson = prepareJsonForCourtCentre(courtCentreData);

        final Response response = restClient.postCommand(referenceDataUrl, MEDIA_TYPE_ADD_COURT_CENTRE,
                courtCentreDataJson.build().toString(), getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));

        final String getCourtCentresUrl = String.format("%s/%s", baseUri, ENDPOINT_PROPERTIES.getProperty("listing.get.reference-data"));

        final Filter myFilter = filter(
                where("id").is(courtCentreData.getId().toString())
                        .and("courtCentreName").is(courtCentreData.getCourtCentreName()));
        final JsonPath courtCentreFilter = JsonPath.compile("$.courtCentres[?]", myFilter);

        poll(requestParams(getCourtCentresUrl, MEDIA_TYPE_GET_COURT_CENTRES).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath(courtCentreFilter)));

    }

    private static JsonObjectBuilder prepareJsonForCourtCentre(final CourtCentreData courtCentreData) {
        final JsonObjectBuilder courtCentreDataJson = createObjectBuilder();

        return courtCentreDataJson
                .add(FIELD_GENERIC_ID, courtCentreData.getId().toString())
                .add(FIELD_COURT_CENTRE_NAME, courtCentreData.getCourtCentreName());
    }

    public static void whenCourtRoomHasBeenAddedAndCanBeRetrieved(final CourtRoomData courtRoomData) {

        final String referenceDataUrl = String.format("%s/%s", baseUri,
                format(ENDPOINT_PROPERTIES.getProperty("listing.command.reference-data")));

        final JsonObjectBuilder judgeDataJson = prepareJsonForCourtRoom(courtRoomData);

        final Response response = restClient.postCommand(referenceDataUrl, MEDIA_TYPE_ADD_COURT_ROOM,
                judgeDataJson.build().toString(), getLoggedInHeader());

        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));

        final String getCourtRoomUrl = String.format("%s/%s", baseUri, ENDPOINT_PROPERTIES.getProperty("listing.get.reference-data"));

        final Filter myFilter = filter(
                where("id").is(courtRoomData.getId().toString())
                        .and("courtCentre").is(courtRoomData.getCourtCentre())
                        .and("courtRoomName").is(courtRoomData.getCourtRoomName()));
        final JsonPath courtRoomFilter = JsonPath.compile("$.courtRooms[?]", myFilter);

        poll(requestParams(getCourtRoomUrl, MEDIA_TYPE_GET_COURT_ROOMS).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath(courtRoomFilter)));
    }

    private static JsonObjectBuilder prepareJsonForCourtRoom(final CourtRoomData courtRoomData) {
        final JsonObjectBuilder courtRoomDataJson = createObjectBuilder();

        return courtRoomDataJson
                .add(FIELD_GENERIC_ID, courtRoomData.getId().toString())
                .add(FIELD_COURT_CENTRE, courtRoomData.getCourtCentre())
                .add(FIELD_COURT_ROOM_NAME, courtRoomData.getCourtRoomName());
    }

}
