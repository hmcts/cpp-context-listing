package uk.gov.moj.cpp.listing.steps;

import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.utils.DocumentGeneratorStub.stubDocumentCreate;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;

import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefDataCourtRoomCacheStep extends AbstractIT {
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final String COURT_LIST_DATA = "test";
    private  UpdatedHearingData updatedHearingData;
    private static final String MEDIA_TYPE_SEARCH_COURT_LIST = "application/vnd.listing.search.court.list+json";
    private static final String MEDIA_TYPE_SEARCH_COURT_LIST_PAYLOAD = "application/vnd.listing.search.court.list.payload+json";
    final static String LISTING_CACHE_REFRESH_URL = "listing.query.cache-refdata-courtroom.refresh";
    final static String LISTING_CACHE_REFRESH_MEDIA_TYPE = "application/vnd.listing.get.cache-refdata-courtrooms-refresh+json";
    private static final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();
    private static final Logger LOGGER = LoggerFactory.getLogger(RefDataCourtRoomCacheStep.class.getCanonicalName());
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RefDataCourtRoomCacheStep() { }

    public RefDataCourtRoomCacheStep(final UpdatedHearingData updatedHearingData) {
        this.updatedHearingData = updatedHearingData;
        stubDocumentCreate(COURT_LIST_DATA);
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), "Carmarthen Magistrates Court"));
        stubGetReferenceDataJudiciaries(updatedHearingData.getJudiciary().get(0).getJudicialId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
    }



    public void assertRefreshCache() throws JsonProcessingException {
        stubGetReferenceDataCpCourtRooms();

        assertThat(countCacheItemsInDb(), is(0));

        final CacheRefDataCourtroomRefreshStatus res = refresh();

        assertThat(res.count(), is(4));
        assertThat(res.timestamp(), is(notNullValue()));
        assertThat(countCacheItemsInDb(), is(4));
    }

    private CacheRefDataCourtroomRefreshStatus refresh() throws JsonProcessingException {
        final String listCaseForHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_CACHE_REFRESH_URL)));

        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue());

        final Response resp = restClient.query(listCaseForHearingUrl, LISTING_CACHE_REFRESH_MEDIA_TYPE, map);
        assertThat(resp.getStatus(), is(200));

        final String stringResp = resp.readEntity(String.class);
        final CacheRefDataCourtroomRefreshStatus respRecord = objectMapper.readValue(stringResp, CacheRefDataCourtroomRefreshStatus.class);
        return respRecord;

    }


    private int countCacheItemsInDb() {
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

    public record CacheRefDataCourtroomRefreshStatus(String timestamp, int count){

    }
}
