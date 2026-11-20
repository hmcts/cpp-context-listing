package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubSearchCourtSchedulesByIdServerError;

import uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import java.io.StringReader;

import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

/**
 * Black-box IT for {@code POST /courtScheduleDraftStatus} (action
 * {@code listing.query.court.schedule.draft.status}).
 *
 * <p>Exercises the full chain: HTTP request → listing-query-api Resource →
 * CourtSchedulerServiceAdapter.getCourtScheduleDraftStatus → HearingSlotsService → WireMock-stubbed
 * courtscheduler. The wire format from courtscheduler is FLAT - each courtSchedules[] element is a
 * single CourtSchedule with isDraft at the top level. These ITs lock that contract in: if anyone
 * mistakenly re-introduces nested "sessions" parsing or stubs that shape, the tests fail.
 *
 * <p>The endpoint is consumed by cpp-context-progression to decide whether to strip
 * courtCentre.roomId from unallocated CROWN hearings before they reach hearing.payload or the
 * new-hearing notification email.
 */
class CourtScheduleDraftStatusIT extends AbstractIT {

    private static final String COURT_SCHEDULE_DRAFT_STATUS_URL = "listing.query.court-schedule-draft-status";
    private static final String CONTENT_TYPE = "application/vnd.listing.query.court.schedule.draft.status+json";

    private static final String DRAFT_SCHEDULE_ID = "ea73df0c-2cbf-4f27-80ce-8b88ac1df702";
    private static final String NON_DRAFT_SCHEDULE_ID = "d474db0a-8c3e-4e0c-8f98-66ed7eda57f0";

    @Test
    void shouldReturnAnyDraftTrueWhenCourtschedulerReportsDraftSessionUnderIsDraftKey() {
        // Stubs the FLAT wire shape with the "isDraft" field name. Some Jackson configurations
        // serialise the CourtSchedule.isDraft() getter as JSON property "isDraft"; the parser
        // must handle this name.
        CourtSchedulerServiceStub.stubSearchCourtSchedulesByIdWithKey(DRAFT_SCHEDULE_ID, "isDraft", true);

        final Response response = postDraftStatusCheck(DRAFT_SCHEDULE_ID);

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        final JsonObject body = readBody(response);
        assertThat(body.getBoolean("anyDraft"), is(true));
    }

    @Test
    void shouldReturnAnyDraftTrueWhenCourtschedulerReportsDraftSessionUnderDraftKey() {
        // Stubs the FLAT wire shape with the "draft" field name. Jackson's default JavaBean
        // convention for a boolean getter `isDraft()` strips the "is" prefix and serialises
        // as JSON property "draft" - this was the field name the parser missed in the bug
        // where curl returned anyDraft=false for a known-draft courtScheduleId.
        CourtSchedulerServiceStub.stubSearchCourtSchedulesByIdWithKey(DRAFT_SCHEDULE_ID, "draft", true);

        final Response response = postDraftStatusCheck(DRAFT_SCHEDULE_ID);

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        final JsonObject body = readBody(response);
        assertThat(body.getBoolean("anyDraft"), is(true));
    }

    @Test
    void shouldReturnAnyDraftFalseWhenCourtschedulerReportsNonDraftSession() {
        CourtSchedulerServiceStub.stubSearchCourtSchedulesByIdWithKey(NON_DRAFT_SCHEDULE_ID, "draft", false);

        final Response response = postDraftStatusCheck(NON_DRAFT_SCHEDULE_ID);

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        final JsonObject body = readBody(response);
        assertThat(body.getBoolean("anyDraft"), is(false));
    }

    @Test
    @ExpectedServerErrors("courtscheduler stub returns 500 -> ERROR 'Retrieve ...court-schedules-by-id+json failed with status code:500' + WARN 'failing-safe by returning anyDraft=true'")
    void shouldFailClosedToAnyDraftTrueWhenCourtschedulerReturnsServerError() {
        // Listing-side fail-safe direction: if courtscheduler is unreachable we cannot prove
        // the session is non-draft, so we report anyDraft=true. Progression then strips
        // conservatively rather than leaking a phantom courtroom. (Progression's own fail-safe
        // is the opposite direction - it fails-open when its call to listing fails. The two
        // layers' defaults are intentionally asymmetric.)
        stubSearchCourtSchedulesByIdServerError();

        final Response response = postDraftStatusCheck(DRAFT_SCHEDULE_ID);

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        final JsonObject body = readBody(response);
        assertThat(body.getBoolean("anyDraft"), is(true));
    }

    private Response postDraftStatusCheck(final String courtScheduleId) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(COURT_SCHEDULE_DRAFT_STATUS_URL)));
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue());
        return restClient.postCommand(url, CONTENT_TYPE, requestPayload(courtScheduleId), headers);
    }

    private static String requestPayload(final String courtScheduleId) {
        // Request shape is a flat array of UUID strings - no per-entry object wrapping.
        return createObjectBuilder()
                .add("courtScheduleIdList", createArrayBuilder().add(courtScheduleId))
                .build()
                .toString();
    }

    private static JsonObject readBody(final Response response) {
        return createReader(new StringReader(response.readEntity(String.class))).readObject();
    }
}
