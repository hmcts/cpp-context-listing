package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static java.time.DayOfWeek.MONDAY;
import static java.time.temporal.TemporalAdjusters.previousOrSame;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.POLL_INTERVAL;
import static uk.gov.moj.cpp.listing.steps.PublishCourtListSteps.loadHearingDataWithJudiciary;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.it.util.ViewStoreCleaner;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the listing dashboard query (listing.query.listing-dashboard).
 */
public class ListingDashboardIT extends AbstractIT {

    private static final String LISTING_QUERY_LISTING_DASHBOARD = "listing.query.listing-dashboard";
    private static final String LISTING_DASHBOARD_MEDIA_TYPE = "application/vnd.listing.query.listing-dashboard+json";

    private HearingData hearing;

    @BeforeEach
    public void initialize() {
        new ViewStoreCleaner().cleanViewStoreTables();
        final UUID courtCentreId = getRandomCourtCenterId();
        final UUID courtRoomUUID = getRandomCourtRoomId();

        stubGetReferenceDataCourtCentreById(courtCentreId);
        final HearingsData data = loadHearingDataWithJudiciary(courtCentreId, courtRoomUUID);
        hearing = data.getHearingData().get(0);

        stubOrganisationUnit(courtCentreId);
        stubGetReferenceDataCourtMappings(new CourtCentreData(courtCentreId, LocalTime.of(10, 30), "6:30", null, STRING.next()));
        stubGetReferenceDataCpCourtRooms(hearing.getCourtRoomId(), 231);
        stubGetReferenceDataXhibitCourtRoomMappings(hearing.getCourtRoomId());

        new ListCourtHearingSteps(data).verifyHearingIsCreated(hearing.getId(), hearing.getListedCases().size());
    }

    @Test
    void shouldReturnHearingForTheWeekOfItsStartDate() {
        final LocalDate hearingStartDate = hearing.getHearingStartDate();
        final LocalDate monday = hearingStartDate.with(previousOrSame(MONDAY));
        final MultivaluedMap<String, Object> loggedInHeader = getLoggedInHeader();
        final String url = listingDashboardUrl(hearing.getCourtCentreId(), hearingStartDate);

        final AtomicReference<JsonObject> responseRef = new AtomicReference<>();
        await().atMost(15, SECONDS).pollInterval(POLL_INTERVAL).until(() -> {
            final Response response = restClient.query(url, LISTING_DASHBOARD_MEDIA_TYPE, loggedInHeader);
            if (response.getStatus() != OK.getStatusCode()) {
                return false;
            }
            final JsonObject payload = readJson(response.readEntity(String.class));
            responseRef.set(payload);
            return rowsFor(payload, hearing.getId()).stream()
                    .anyMatch(row -> row.getBoolean("allocated", false) && row.containsKey("courtRoomId"));
        });

        final JsonObject payload = responseRef.get();
        assertThat(payload.getString("courtCentreId"), is(hearing.getCourtCentreId().toString()));
        assertThat(payload.getString("startDate"), is(monday.toString()));
        assertThat(payload.getString("endDate"), is(monday.plusDays(6).toString()));

        final JsonObject row = rowsFor(payload, hearing.getId()).get(0);
        final LocalDate hearingDate = LocalDate.parse(row.getString("hearingDate"));
        assertThat(!hearingDate.isBefore(monday) && !hearingDate.isAfter(monday.plusDays(6)), is(true));
        assertThat(row.getString("courtRoomId"), is(hearing.getCourtRoomId().toString()));
        assertThat(row.getString("hearingTypeId"), is(hearing.getHearingTypeData().getTypeId().toString()));
        assertThat(row.getString("hearingType"), is(hearing.getHearingTypeData().getTypeDescription()));
        assertThat(row.getBoolean("allocated"), is(true));
        assertThat(row.getJsonArray("caseUrns").getValuesAs(JsonString.class).stream().map(JsonString::getString).toList(),
                hasItem(hearing.getListedCases().get(0).getCaseReference()));
    }

    @Test
    void shouldNotReturnHearingForAnotherWeek() {
        final LocalDate monthBefore = hearing.getHearingStartDate().minusWeeks(4);

        final Response response = restClient.query(listingDashboardUrl(hearing.getCourtCentreId(), monthBefore),
                LISTING_DASHBOARD_MEDIA_TYPE, getLoggedInHeader());

        assertThat(response.getStatus(), is(OK.getStatusCode()));
        assertThat(rowsFor(readJson(response.readEntity(String.class)), hearing.getId()).isEmpty(), is(true));
    }

    @Test
    void shouldRejectInvalidStartDate() {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_LISTING_DASHBOARD), hearing.getCourtCentreId(), "26-11-2026"));

        final Response response = restClient.query(url, LISTING_DASHBOARD_MEDIA_TYPE, getLoggedInHeader());

        assertThat(response.getStatus(), is(BAD_REQUEST.getStatusCode()));
    }

    private static List<JsonObject> rowsFor(final JsonObject payload, final UUID hearingId) {
        return payload.getJsonArray("hearings").getValuesAs(JsonObject.class).stream()
                .filter(row -> hearingId.toString().equals(row.getString("hearingId")))
                .toList();
    }

    private static JsonObject readJson(final String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readObject();
        }
    }

    private static String listingDashboardUrl(final UUID courtCentreId, final LocalDate startDate) {
        return String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_QUERY_LISTING_DASHBOARD), courtCentreId, startDate.toString()));
    }
}
