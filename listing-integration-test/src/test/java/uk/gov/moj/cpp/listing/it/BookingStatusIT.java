package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub;

import java.io.StringReader;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

/**
 * Black-box IT for {@code POST /bookingStatus} (action {@code listing.query.booking.status}).
 *
 * <p>Exercises the full chain: HTTP request -&gt; listing-query-api Resource -&gt;
 * CourtSchedulerServiceAdapter.getBookingStatus -&gt; HearingSlotsService -&gt; WireMock-stubbed
 * courtscheduler GET /provisionalBooking/status. The reserve-a-slot feature holds court-session
 * capacity from slot-pick until share; this endpoint is a thin pass-through so the results UI can
 * gate a share synchronously against courtscheduler's live booking status.
 *
 * <p>The third test below is the one with no coverage before this class existed: it pins listing's
 * deliberate fail-open behaviour when courtscheduler is unreachable - blocking every share in the
 * building during a courtscheduler blip would be worse than letting an advisory check pass, so the
 * endpoint answers 200 with {@code status=UNKNOWN, safeToShare=true} for every requested id rather
 * than propagating the downstream failure.
 */
class BookingStatusIT extends AbstractIT {

    private static final String BOOKING_STATUS_URL = "listing.query.booking-status";
    private static final String CONTENT_TYPE = "application/vnd.listing.query.booking.status+json";

    @Test
    void shouldReturnCourtschedulerStatusesVerbatim() {
        CourtSchedulerServiceStub.stubBookingStatus(
                "[{\"bookingId\":\"bk-1\",\"safeToShare\":true,\"status\":\"RESERVED\"},"
              + "{\"bookingId\":\"bk-2\",\"safeToShare\":false,\"status\":\"NONE\"}]");

        final Response response = postBookingStatusCheck("bk-1", "bk-2");

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        final JsonArray bookings = readBody(response).getJsonArray("bookings");
        assertThat(bookings.size(), is(2));
        assertThat(bookings.getJsonObject(0).getString("status"), is("RESERVED"));
        assertThat(bookings.getJsonObject(1).getString("status"), is("NONE"));
        assertThat(bookings.getJsonObject(1).getBoolean("safeToShare"), is(false));
    }

    @Test
    void shouldPassALegacyStatusThroughUntouched() {
        CourtSchedulerServiceStub.stubBookingStatus(
                "[{\"bookingId\":\"legacy-1\",\"safeToShare\":true,\"status\":\"LEGACY\"}]");

        final JsonArray bookings = readBody(postBookingStatusCheck("legacy-1")).getJsonArray("bookings");

        assertThat("a pre-reserve-a-slot magistrates draft must not be reported as expired",
                bookings.getJsonObject(0).getString("status"), is("LEGACY"));
    }

    @Test
    @ExpectedServerErrors("courtscheduler stub returns 500 -> ERROR 'Retrieve ...provisionalBooking/status+json failed with status code:500' + WARN 'failing-safe by returning status=UNKNOWN, safeToShare=true'")
    void shouldFailOpenWithUnknownWhenCourtschedulerErrors() {
        CourtSchedulerServiceStub.stubBookingStatusServerError();

        final Response response = postBookingStatusCheck("bk-1", "bk-2");

        assertThat("a courtscheduler outage must not fail the request", response.getStatus(), is(OK.getStatusCode()));
        final JsonArray bookings = readBody(response).getJsonArray("bookings");
        assertThat(bookings.size(), is(2));
        for (int i = 0; i < bookings.size(); i++) {
            assertThat(bookings.getJsonObject(i).getString("status"), is("UNKNOWN"));
            assertThat("blocking every share in the building during a blip is worse than letting "
                    + "an advisory check pass", bookings.getJsonObject(i).getBoolean("safeToShare"), is(true));
        }
    }

    private Response postBookingStatusCheck(final String... bookingIds) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(BOOKING_STATUS_URL)));
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue());
        return restClient.postCommand(url, CONTENT_TYPE, requestPayload(bookingIds), headers);
    }

    private static String requestPayload(final String... bookingIds) {
        final JsonArrayBuilder bookingIdList = createArrayBuilder();
        for (final String bookingId : bookingIds) {
            bookingIdList.add(bookingId);
        }
        return createObjectBuilder()
                .add("bookingIds", bookingIdList)
                .build()
                .toString();
    }

    private static JsonObject readBody(final Response response) {
        return createReader(new StringReader(response.readEntity(String.class))).readObject();
    }
}
