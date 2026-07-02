package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.setupLoggedInUserPermissionsWithChangeHearingToPastDate;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.util.UUID;

import javax.ws.rs.core.Response;

/**
 * Steps for the listing.command.move-hearing-to-past-date wrapper endpoint. Same
 * {@code POST /hearings/{hearingId}} resource as vacate-trial/extend-hearing, distinguished by
 * media type {@code application/vnd.listing.command.move-hearing-to-past-date+json}.
 */
public class MoveHearingToPastDateSteps extends AbstractIT {

    private static final String LISTING_QUERY_HEARING = "listing.search.hearing";
    private static final String MEDIA_TYPE_SEARCH_HEARING = "application/vnd.listing.search.hearing+json";
    private static final String LISTING_COMMAND_MOVE = "listing.command.move-hearing-to-past-date";
    private static final String MEDIA_TYPE_MOVE = "application/vnd.listing.command.move-hearing-to-past-date+json";

    private final String hearingId;
    private final UUID courtCentreId;

    public MoveHearingToPastDateSteps(final HearingsData hearingsData) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        this.hearingId = hearingData.getId().toString();
        this.courtCentreId = hearingData.getCourtCentreId();
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        setupLoggedInUserPermissionsWithChangeHearingToPastDate();
    }

    public String getHearingId() {
        return hearingId;
    }

    public Response whenHearingIsMovedToPastDate(final String jurisdictionDir, final LocalDate date) {
        final String payload = getPayload("test-data/" + jurisdictionDir + "/move-to-past-date/move-hearing-to-past-date.json")
                .replace("%%HEARING_ID%%", hearingId)
                .replace("%%COURT_CENTRE_ID%%", courtCentreId.toString())
                .replace("%%START_DATE%%", date.toString());

        return postMove(payload);
    }

    public Response whenHearingIsMovedWithMissingCourtCentre(final LocalDate date) {
        final String payload = "{\"hearingId\":\"" + hearingId + "\",\"startDate\":\"" + date + "\"}";
        return postMove(hearingId, payload);
    }

    /** Submits the move against an arbitrary hearingId (e.g. one that was never listed), reusing this
     * steps' own courtCentreId so only the hearingId lookup is exercised. */
    public Response whenHearingIsMovedToPastDateForHearing(final UUID otherHearingId, final LocalDate date) {
        final String payload = "{\"hearingId\":\"" + otherHearingId + "\",\"courtCentreId\":\"" + courtCentreId
                + "\",\"startDate\":\"" + date + "\"}";
        return postMove(otherHearingId.toString(), payload);
    }

    private Response postMove(final String payload) {
        return postMove(hearingId, payload);
    }

    private Response postMove(final String targetHearingId, final String payload) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_COMMAND_MOVE), targetHearingId));
        return restClient.postCommand(url, MEDIA_TYPE_MOVE, payload, getLoggedInHeader());
    }

    public void verifyCourtScheduleStored(final String expectedCourtScheduleId) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(org.hamcrest.CoreMatchers.allOf(
                                withJsonPath("$.id", is(hearingId)),
                                withJsonPath("$.hearingDays[*].courtScheduleId", hasItem(expectedCourtScheduleId))
                        )));
    }

    public void verifyStartDateUpdated(final LocalDate expectedStartDate) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(org.hamcrest.CoreMatchers.allOf(
                                withJsonPath("$.id", is(hearingId)),
                                withJsonPath("$.startDate", is(expectedStartDate.toString())),
                                withJsonPath("$.hearingDays[0].hearingDate", is(expectedStartDate.toString()))
                        )));
    }
}
