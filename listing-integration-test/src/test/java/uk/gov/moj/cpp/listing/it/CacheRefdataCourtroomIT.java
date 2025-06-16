package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;

import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("squid:S1607")

public class CacheRefdataCourtroomIT extends AbstractIT {
    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private static final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();
    final static String LISTING_CACHE_REFRESH_URL = "listing.query.cache-refdata-courtroom.refresh";
    final static String LISTING_CACHE_REFRESH_MEDIA_TYPE = "application/vnd.listing.get.cache-refdata-courtrooms-refresh+json";
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void cleanPublishedEventTable() {
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "cache_refdata_courtroom");
    }

    @Test
    public void shouldRefreshCache() throws JsonProcessingException {
        stubGetReferenceDataCpCourtRooms();

        assertThat(countCacheItemsInDb(), is(0));

        final CacheRefdataCourtroomRefreshStatus res = refresh();
        assertThat(res.count(), is(4));
        assertThat(res.timestamp(), is(notNullValue()));

        assertThat(countCacheItemsInDb(), is(4));

    }


    private static int countCacheItemsInDb() {
        try (final Connection viewstoreConnection =
                     testJdbcConnectionProvider.getViewStoreConnection("listing");
             final Statement statement =
                     viewstoreConnection.createStatement()
        ) {
            ResultSet resultSet ;
            resultSet = statement.executeQuery("select count(*) from cache_refdata_courtroom");
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private CacheRefdataCourtroomRefreshStatus refresh() throws JsonProcessingException {
        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_CACHE_REFRESH_URL)));

        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue());

        final Response resp = restClient.query(listCaseForHearingUrl, LISTING_CACHE_REFRESH_MEDIA_TYPE, map);
        assertThat(resp.getStatus(), is(200));

        final String stringResp = resp.readEntity(String.class);
        final CacheRefdataCourtroomRefreshStatus respRecord = objectMapper.readValue(stringResp, CacheRefdataCourtroomRefreshStatus.class);
       return respRecord;

    }

    public record CacheRefdataCourtroomRefreshStatus(String timestamp, int count){

    }
}
