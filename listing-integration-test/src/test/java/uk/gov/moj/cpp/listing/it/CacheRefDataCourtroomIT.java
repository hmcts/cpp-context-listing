package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRoomsThreeCourtroomsOnly;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.listing.steps.RefDataCourtRoomCacheStep;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1607")

public class CacheRefDataCourtroomIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheRefDataCourtroomIT.class.getCanonicalName());

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private static final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();
    final static String LISTING_CACHE_REFRESH_URL = "listing.query.cache-refdata-courtroom.refresh";
    final static String LISTING_CACHE_REFRESH_MEDIA_TYPE = "application/vnd.listing.get.cache-refdata-courtrooms-refresh+json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "cache_refdata_courtroom");
    }

    @Test
    public void shouldRefreshCache() throws JsonProcessingException {
        new RefDataCourtRoomCacheStep().assertRefreshCache();
    }

    @Test
    void shouldReturnOkAndRefreshCountWhenCallingCacheRefDataCourtroomsRefreshQueryEndpoint() throws JsonProcessingException {
        stubGetReferenceDataCpCourtRooms();

        assertThat(countCacheItemsInDb(), is(0));

        final Response response = callCacheRefDataCourtroomsRefreshQuery();

        assertThat(response.getStatus(), is(200));

        final JsonNode body = objectMapper.readTree(response.readEntity(String.class));
        assertThat(body.get("count").intValue(), is(4));
        assertThat(body.hasNonNull("timestamp"), is(true));
        assertThat(body.get("timestamp").asText().isEmpty(), is(false));

        assertThat(countCacheItemsInDb(), is(4));
    }

    @Test
    void shouldNotRewriteCachedRowsWhenSecondRefreshFindsNoDifferenceFromRefData()
            throws JsonProcessingException, SQLException {
        stubGetReferenceDataCpCourtRooms();
        assertThat(countCacheItemsInDb(), is(0));

        assertThat(callCacheRefDataCourtroomsRefreshQuery().getStatus(), is(200));
        assertThat(countCacheItemsInDb(), is(4));

        final List<String> rowFingerprintsAfterFirstLoad = cacheRefDataCourtroomRowFingerprints();

        final Response second = callCacheRefDataCourtroomsRefreshQuery();
        assertThat(second.getStatus(), is(200));
        final JsonNode secondBody = objectMapper.readTree(second.readEntity(String.class));
        assertThat(secondBody.get("count").intValue(), is(4));

        final List<String> rowFingerprintsAfterSecondLoad = cacheRefDataCourtroomRowFingerprints();
        assertThat(rowFingerprintsAfterSecondLoad, is(rowFingerprintsAfterFirstLoad));
        assertThat(countCacheItemsInDb(), is(4));
    }


    @Test
    void shouldRunFullRefreshWhenReferenceDataCourtroomsDifferFromListingCache()
            throws JsonProcessingException, SQLException {
        stubGetReferenceDataCpCourtRooms();
        assertThat(countCacheItemsInDb(), is(0));

        assertThat(callCacheRefDataCourtroomsRefreshQuery().getStatus(), is(200));
        assertThat(countCacheItemsInDb(), is(4));
        final List<String> fingerprintsWithFourRows = cacheRefDataCourtroomRowFingerprints();

        stubGetReferenceDataCpCourtRoomsThreeCourtroomsOnly();

        final Response second = callCacheRefDataCourtroomsRefreshQuery();
        assertThat(second.getStatus(), is(200));
        final JsonNode secondBody = objectMapper.readTree(second.readEntity(String.class));
        assertThat(secondBody.get("count").intValue(), is(3));

        assertThat(countCacheItemsInDb(), is(3));
        assertThat(cacheRefDataCourtroomRowFingerprints(), not(is(fingerprintsWithFourRows)));
    }

    private Response callCacheRefDataCourtroomsRefreshQuery() {
        final String refreshUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty(LISTING_CACHE_REFRESH_URL)));
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue());
        return restClient.query(refreshUrl, LISTING_CACHE_REFRESH_MEDIA_TYPE, headers);
    }

    /**
     * Stable ordered fingerprints of physical rows (PostgreSQL). Unchanged across refresh only if rows were not deleted and re-inserted.
     */
    private static List<String> cacheRefDataCourtroomRowFingerprints() throws SQLException {
        final String sql = "select id::text, ctid::text from cache_refdata_courtroom order by id::text";
        try (final Connection viewStoreConnection =
                     testJdbcConnectionProvider.getViewStoreConnection("listing");
             final Statement statement = viewStoreConnection.createStatement();
             final ResultSet resultSet = statement.executeQuery(sql)) {
            final List<String> rows = new ArrayList<>();
            while (resultSet.next()) {
                rows.add(resultSet.getString(1) + "@" + resultSet.getString(2));
            }
            return rows;
        }
    }

    private static int countCacheItemsInDb() {
        try (final Connection viewStoreConnection =
                     testJdbcConnectionProvider.getViewStoreConnection("listing");
             final Statement statement =
                     viewStoreConnection.createStatement()
        ) {
            ResultSet resultSet ;
            resultSet = statement.executeQuery("select count(*) from cache_refdata_courtroom");
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            String msg = "fail to count database items in cache";
            LOGGER.error(msg, e.getMessage());
        }
        return 0;
    }

}
