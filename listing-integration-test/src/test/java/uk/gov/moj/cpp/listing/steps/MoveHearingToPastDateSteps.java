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
    private final UUID courtRoomId;

    public MoveHearingToPastDateSteps(final HearingsData hearingsData) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        this.hearingId = hearingData.getId().toString();
        this.courtCentreId = hearingData.getCourtCentreId();
        // Some seed data (e.g. a freshly-listed hearing whose room is assigned later via a separate
        // update-hearing-for-listing call, not reflected back into this in-memory HearingData) has no
        // courtRoomId yet. courtscheduler is WireMock-stubbed in these tests regardless of which room
        // is requested, so any UUID is a valid stand-in - only its presence on the wire matters.
        this.courtRoomId = hearingData.getCourtRoomId() != null ? hearingData.getCourtRoomId() : UUID.randomUUID();
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public String getHearingId() {
        return hearingId;
    }

    /** Single-day move: startDateTime/endDateTime share the date, same 10:00-17:00 UTC window every stubbed session uses. */
    public Response whenHearingIsMovedToPastDate(final String jurisdictionDir, final LocalDate date) {
        return whenHearingIsMovedToPastDateRange(jurisdictionDir, date, date);
    }

    /** Explicit date-RANGE move: startDateTime/endDateTime carry different dates - the caller now
     * drives multi-day CROWN payback directly (rather than only via the hearing's duration estimate). */
    public Response whenHearingIsMovedToPastDateRange(final String jurisdictionDir, final LocalDate startDate, final LocalDate endDate) {
        final String payload = getPayload("test-data/" + jurisdictionDir + "/move-to-past-date/move-hearing-to-past-date.json")
                .replace("%%COURT_CENTRE_ID%%", courtCentreId.toString())
                .replace("%%COURT_ROOM_ID%%", courtRoomId.toString())
                .replace("%%START_DATE_TIME%%", startDate + "T10:00:00Z")
                .replace("%%END_DATE_TIME%%", endDate + "T17:00:00Z");

        return postMove(payload);
    }

    public Response whenHearingIsMovedWithMissingCourtCentre(final LocalDate date) {
        final String payload = "{\"courtRoomId\":\"" + courtRoomId + "\",\"startDateTime\":\"" + date + "T10:00:00Z\","
                + "\"endDateTime\":\"" + date + "T17:00:00Z\"}";
        return postMove(hearingId, payload);
    }

    public Response whenHearingIsMovedWithMissingCourtRoom(final LocalDate date) {
        final String payload = "{\"courtCentreId\":\"" + courtCentreId + "\",\"startDateTime\":\"" + date + "T10:00:00Z\","
                + "\"endDateTime\":\"" + date + "T17:00:00Z\"}";
        return postMove(hearingId, payload);
    }

    public Response whenHearingIsMovedWithMissingEndDateTime(final LocalDate date) {
        final String payload = "{\"courtCentreId\":\"" + courtCentreId + "\",\"courtRoomId\":\"" + courtRoomId + "\","
                + "\"startDateTime\":\"" + date + "T10:00:00Z\"}";
        return postMove(hearingId, payload);
    }

    /**
     * An IMPOSSIBLE calendar date (June has only 30 days), not garbage text: the request schema's
     * {@code pattern} constraint only checks digit SHAPE (YYYY-MM-DDTHH:MM:SSZ), so this passes the
     * framework's 400 schema gate and reaches {@code ListingCommandApi}'s own {@code parseInstant},
     * which is where the 422 INVALID_DATE this test exercises actually gets raised. A literal
     * non-date string like "not-a-date" never gets that far - it fails the pattern and is rejected
     * as a 400 instead, so it cannot exercise this application-level validation.
     */
    public Response whenHearingIsMovedWithImpossibleCalendarStartDateTime(final LocalDate referenceDate) {
        final String payload = "{\"courtCentreId\":\"" + courtCentreId + "\",\"courtRoomId\":\"" + courtRoomId + "\","
                + "\"startDateTime\":\"" + referenceDate.getYear() + "-06-31T10:00:00Z\","
                + "\"endDateTime\":\"" + referenceDate + "T17:00:00Z\"}";
        return postMove(hearingId, payload);
    }

    /** Submits the move against an arbitrary hearingId (e.g. one that was never listed), reusing this
     * steps' own courtCentreId/courtRoomId so only the hearingId lookup is exercised. The target hearing
     * is identified purely by the URL path - hearingId is not part of the body. */
    public Response whenHearingIsMovedToPastDateForHearing(final UUID otherHearingId, final LocalDate date) {
        final String payload = "{\"courtCentreId\":\"" + courtCentreId + "\",\"courtRoomId\":\"" + courtRoomId + "\","
                + "\"startDateTime\":\"" + date + "T10:00:00Z\",\"endDateTime\":\"" + date + "T17:00:00Z\"}";
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

    /** Start AND end date must both follow the move: end date = last booked past session. */
    public void verifyStartAndEndDateUpdated(final LocalDate expectedStartDate, final LocalDate expectedEndDate) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(org.hamcrest.CoreMatchers.allOf(
                                withJsonPath("$.id", is(hearingId)),
                                withJsonPath("$.startDate", is(expectedStartDate.toString())),
                                withJsonPath("$.endDate", is(expectedEndDate.toString())),
                                withJsonPath("$.hearingDays[0].hearingDate", is(expectedStartDate.toString()))
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
