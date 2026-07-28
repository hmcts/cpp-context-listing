package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;

import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.util.UUID;

import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;

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
    private static final String PUBLIC_EVENT_HEARING_UPDATED = "public.listing.hearing-updated";
    private static final String PUBLIC_EVENT_VACATED_TRIAL_UPDATED = "public.listing.vacated-trial-updated";

    private final String hearingId;
    private final UUID courtCentreId;
    private final UUID courtRoomId;

    private JmsMessageConsumerClient publicConsumerHearingUpdated;
    private JmsMessageConsumerClient publicConsumerVacatedTrialUpdated;

    public MoveHearingToPastDateSteps(final HearingsData hearingsData) {
        final HearingData hearingData = hearingsData.getHearingData().get(0);
        this.hearingId = hearingData.getId().toString();
        this.courtCentreId = hearingData.getCourtCentreId();
        this.courtRoomId = hearingData.getCourtRoomId();
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public String getHearingId() {
        return hearingId;
    }

    public Response whenHearingIsMovedToPastDate(final String jurisdictionDir, final LocalDate date) {
        final String payload = getPayload("test-data/" + jurisdictionDir + "/move-to-past-date/move-hearing-to-past-date.json")
                .replace("%%COURT_CENTRE_ID%%", courtCentreId.toString())
                .replace("%%COURT_ROOM_ID%%", courtRoomId.toString())
                .replace("%%START_TIME%%", utc(date))
                .replace("%%END_TIME%%", utcEnd(date));

        return postMove(payload);
    }

    public Response whenHearingIsMovedWithMissingCourtCentre(final LocalDate date) {
        // courtCentreId omitted (schema-mandatory); every other mandatory field present so the 400 is
        // unambiguously the missing centre.
        final String payload = "{\"courtRoomId\":\"" + courtRoomId + "\",\"startDateTime\":\"" + utc(date)
                + "\",\"endDateTime\":\"" + utcEnd(date) + "\"}";
        return postMove(hearingId, payload);
    }

    public Response whenHearingIsMovedWithMissingCourtRoom(final LocalDate date) {
        // courtRoomId omitted (schema-mandatory); every other mandatory field present so the 400 is
        // unambiguously the missing courtRoomId.
        final String payload = "{\"courtCentreId\":\"" + courtCentreId + "\",\"startDateTime\":\"" + utc(date)
                + "\",\"endDateTime\":\"" + utcEnd(date) + "\"}";
        return postMove(hearingId, payload);
    }

    public Response whenHearingIsMovedWithMissingStartTime() {
        // startDateTime omitted (schema-mandatory); the other mandatory fields present so the 400 is
        // unambiguously the missing startDateTime. A fixed literal date avoids a wall-clock read.
        final String payload = "{\"courtCentreId\":\"" + courtCentreId + "\",\"courtRoomId\":\"" + courtRoomId
                + "\",\"endDateTime\":\"" + utcEnd(LocalDate.of(2026, 1, 1)) + "\"}";
        return postMove(hearingId, payload);
    }

    /** Submits the move with the raw startDateTime/endDateTime strings passed through verbatim -
     * for exercising instant-level validation (impossible calendar dates, end before start). */
    public Response whenHearingIsMovedWithRawTimes(final String startDateTime, final String endDateTime) {
        final String payload = "{\"courtCentreId\":\"" + courtCentreId + "\",\"courtRoomId\":\"" + courtRoomId
                + "\",\"startDateTime\":\"" + startDateTime + "\",\"endDateTime\":\"" + endDateTime + "\"}";
        return postMove(payload);
    }

    /** A multi-day move over [startDateTime, endDateTime], scoped to a specific room. */
    public Response whenHearingIsMovedToPastDateRange(final LocalDate startDate, final LocalDate endDate, final String courtRoomId) {
        final String payload = "{\"courtCentreId\":\"" + courtCentreId
                + "\",\"courtRoomId\":\"" + courtRoomId
                + "\",\"startDateTime\":\"" + utc(startDate)
                + "\",\"endDateTime\":\"" + utcEnd(endDate) + "\"}";
        return postMove(payload);
    }

    /** Submits the move against an arbitrary hearingId (e.g. one that was never listed), reusing this
     * steps' own courtCentreId/courtRoomId so only the hearingId lookup is exercised. The target hearing
     * is identified purely by the URL path - hearingId is not part of the body. */
    public Response whenHearingIsMovedToPastDateForHearing(final UUID otherHearingId, final LocalDate date) {
        final String payload = "{\"courtCentreId\":\"" + courtCentreId + "\",\"courtRoomId\":\"" + courtRoomId
                + "\",\"startDateTime\":\"" + utc(date) + "\",\"endDateTime\":\"" + utcEnd(date) + "\"}";
        return postMove(otherHearingId.toString(), payload);
    }

    /** Fixed 10:00 UTC instant for the given day, matching the move contract's absolute-UTC startDateTime. */
    private static String utc(final LocalDate date) {
        return date + "T10:00:00.000Z";
    }

    /** Fixed 10:30 UTC instant for the given day - a 30-minute single-day endDateTime window. */
    private static String utcEnd(final LocalDate date) {
        return date + "T10:30:00.000Z";
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

    public void verifyHearingDayCount(final int expectedCount) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(org.hamcrest.CoreMatchers.allOf(
                                withJsonPath("$.id", is(hearingId)),
                                withJsonPath("$.hearingDays.length()", is(expectedCount))
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

    /**
     * Subscribes to the public topic BEFORE the move is posted — topics do not retain messages, so a
     * consumer created after the command misses its events. Only the happy-path tests arm these;
     * rejection tests produce no events and skip the consumers (IT guide rule 10).
     */
    public void armPublicEventConsumers() {
        publicConsumerHearingUpdated = publicEvents.createPublicConsumer(PUBLIC_EVENT_HEARING_UPDATED);
        publicConsumerVacatedTrialUpdated = publicEvents.createPublicConsumer(PUBLIC_EVENT_VACATED_TRIAL_UPDATED);
    }

    /**
     * The cross-context contract of a move: public.listing.hearing-updated carries the moved days
     * (progression merges it, then relays public.hearing-detail-changed to the hearing context) and
     * must ALWAYS suppress party notifications — a past-date move never notifies. Matched by
     * hearingId so stale events from other tests on the shared public topic are skipped.
     */
    public void verifyPublicHearingUpdatedWithNotificationsSuppressed(final LocalDate expectedDate) {
        final JsonPath event = retrieveMessage(publicConsumerHearingUpdated, containsString(hearingId));
        assertThat(event.getString("updatedHearing.id"), is(hearingId));
        assertThat(event.getBoolean("sendNotificationToParties"), is(false));
        assertThat(event.getString("updatedHearing.hearingDays[0].sittingDay"), startsWith(expectedDate.toString()));
    }

    /** A date move is a reschedule: vacated-trial-updated with isVacated=false clears any
     * vacated-trial marker in progression and (for an allocated hearing) the hearing context. */
    public void verifyPublicVacatedTrialUpdatedAsRescheduled() {
        final JsonPath event = retrieveMessage(publicConsumerVacatedTrialUpdated, containsString(hearingId));
        assertThat(event.getString("hearingId"), is(hearingId));
        assertThat(event.getBoolean("isVacated"), is(false));
        assertThat(event.getBoolean("allocated"), is(true));
    }

    /** Main-level startDate/endDate must track the hearing days: earliest and latest hearing-day date. */
    public void verifyStartAndEndDatesUpdated(final LocalDate expectedStartDate, final LocalDate expectedEndDate) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_HEARING), hearingId));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARING).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(org.hamcrest.CoreMatchers.allOf(
                                withJsonPath("$.id", is(hearingId)),
                                withJsonPath("$.startDate", is(expectedStartDate.toString())),
                                withJsonPath("$.endDate", is(expectedEndDate.toString()))
                        )));
    }
}
