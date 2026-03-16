package uk.gov.moj.cpp.listing.it;

import static java.text.MessageFormat.format;
import static java.time.Duration.ofSeconds;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.POLL_INTERVAL;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.listing.steps.RefDataCourtRoomCacheStep;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("squid:S1607")

public class CacheRefDataCourtroomIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheRefDataCourtroomIT.class.getCanonicalName());
    public static final String LISTING_QUERY_ADD_COURTROOM_URL = "listing.query.add-courtroom";
    public static final String ADD_COURTROOM_CONTENT_TYPE = "application/vnd.listing.update.add-courtroom+json";
    public static final String LISTING_QUERY_CLOSE_COURTROOM_URL = "listing.query.close-courtroom";
    public static final String COURTROOM_JSON_CONTENT_TYPE = "application/vnd.listing.update.close-courtroom+json";

    private final DatabaseCleaner databaseCleaner = new DatabaseCleaner();
    private static final TestJdbcConnectionProvider testJdbcConnectionProvider = new TestJdbcConnectionProvider();
    final static String LISTING_CACHE_REFRESH_URL = "listing.query.cache-refdata-courtroom.refresh";
    final static String LISTING_CACHE_REFRESH_MEDIA_TYPE = "application/vnd.listing.get.cache-refdata-courtrooms-refresh+json";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String REFERENCE_DATA_EVENT_COURTROOM_ADDED_JSON = "public.referencedata.event.courtroom-added.json";
    private static final String REFERENCE_DATA_EVENT_COURTROOM_CLOSED_JSON = "public.referencedata.event.courtroom-closed.json";
    private static final String PUBLIC_REFERENCE_COURTROOM_ADDED = "public.referencedata.event.courtroom-added";
    private static final String PUBLIC_REFERENCE_COURTROOM_CLOSED = "public.referencedata.event.courtroom-closed";
    private final UUID roomId = getRandomCourtRoomId();
    private final String roomName = "Courtroom 06";
    private final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    private JmsMessageProducerClient publicMessageProducer;

    @BeforeEach
    public void cleanPublishedEventTable() {
        publicMessageProducer = QueueUtil.publicEvents.createPublicProducer();
        databaseCleaner.cleanViewStoreTables(CONTEXT_NAME, "cache_refdata_courtroom");
    }

    @Test
    public void shouldRefreshCache() throws JsonProcessingException {
        new RefDataCourtRoomCacheStep().assertRefreshCache();
    }

    @Test
    void shouldAddCourtRoom() throws JsonProcessingException {
        new RefDataCourtRoomCacheStep().assertRefreshCache();
        addNewCourtRoomDetails();
        assertThat(countCacheItemsInDb(), is(4));
    }


    @Test
    void shouldCloseCourtRoom() throws JsonProcessingException {

        new RefDataCourtRoomCacheStep().assertRefreshCache();
        final JsonObject courtRoomJsonObject =
                getCourtRoomJsonObject(REFERENCE_DATA_EVENT_COURTROOM_CLOSED_JSON, roomId.toString());
        addNewCourtRoomDetails();

        sendMessage(publicMessageProducer,
                PUBLIC_REFERENCE_COURTROOM_CLOSED,
                courtRoomJsonObject,
                metadataBuilder().withId(randomUUID())
                        .withName(PUBLIC_REFERENCE_COURTROOM_CLOSED)
                        .withUserId(randomUUID().toString())
                        .build());
        final boolean isRoomIdExists = checkCourtRoomIdExists(roomId);
        assertThat(isRoomIdExists, is(false));
        assertThat(countCacheItemsInDb(), is(3));
    }

    @Test
    void shouldAddCourtRoomAPICall() {
        String addCourtRoomPayload = "{\"id\":\"0baecac5-222b-402d-9047-84803679edaf\",\"courtroomName\":\"Court Room 1\"}";
        final String addCourtRoomUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_QUERY_ADD_COURTROOM_URL)));

        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue());

        final Response resp = restClient.postCommand(addCourtRoomUrl, ADD_COURTROOM_CONTENT_TYPE, addCourtRoomPayload, map);
        assertThat(resp.getStatus(), is(202));
    }

    @Test
    void shouldCloseCourtRoomAPICall() {

        MultivaluedMap<String, Object> map = new MultivaluedHashMap<>();
        map.add(CPP_UID_HEADER.getName(), CPP_UID_HEADER.getValue());

        String addCourtRoomPayload = "{\"id\":\"0baecac5-222b-402d-9047-84803679edaf\",\"courtroomName\":\"Court Room 1\"}";
        final String addCourtRoomUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_QUERY_ADD_COURTROOM_URL)));

        restClient.postCommand(addCourtRoomUrl, ADD_COURTROOM_CONTENT_TYPE, addCourtRoomPayload, map);

        String closeCourtRoomPayload = "{\"id\":\"0baecac5-222b-402d-9047-84803679edaf\"}";

        final String closeCourtRoomUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_QUERY_CLOSE_COURTROOM_URL)));

        final Response resp = restClient.postCommand(closeCourtRoomUrl, COURTROOM_JSON_CONTENT_TYPE, closeCourtRoomPayload, map);
        assertThat(resp.getStatus(), is(202));
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

    private static UUID findCourtRoomInDbById(UUID roomId) {
        final String queryTemplate = "select id from cache_refdata_courtroom where id = ?";
        final AtomicReference<UUID> courtRoomId = new AtomicReference<>();
        await().pollInterval(POLL_INTERVAL).atMost(15, SECONDS).until(() -> {
            try (final Connection sjpEventStoreConnection = 
                         testJdbcConnectionProvider.getViewStoreConnection("listing");
                 final PreparedStatement statement =
                         sjpEventStoreConnection.prepareStatement(queryTemplate)
            ) {
                ResultSet resultSet;
                statement.setObject(1, roomId);
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    courtRoomId.set(fromString(resultSet.getString(1)));
                    return true;
                }
            } catch (SQLException e) {
                String msg = String.format("Fail to find court room id =%s", roomId);
                LOGGER.error(msg, e.getMessage());
            }
            return false;
        });
        
        return courtRoomId.get();
    }

    private static boolean checkCourtRoomIdExists(UUID roomId) {
        final String queryTemplate = "select id from cache_refdata_courtroom where id = ?";
        await().pollInterval(POLL_INTERVAL).atMost(15, SECONDS).until(() -> {
            try (final Connection sjpEventStoreConnection =
                         testJdbcConnectionProvider.getViewStoreConnection("listing");
                 final PreparedStatement statement =
                         sjpEventStoreConnection.prepareStatement(queryTemplate)
            ) {
                ResultSet resultSet;
                statement.setObject(1, roomId);
                resultSet = statement.executeQuery();
                if (resultSet.getRow() == 0) {
                    return true;
                }
            } catch (SQLException e) {
                String msg = String.format("Fail to check if court room id =%s exists", roomId);
                LOGGER.error(msg, e.getMessage());
            }
            return false;
        });

        return false;
    }


    private void addNewCourtRoomDetails() {
        final JsonObject courtRoomJsonObject =
                getCourtRoomJsonObject(REFERENCE_DATA_EVENT_COURTROOM_ADDED_JSON, roomId.toString(), roomName);

        sendMessage(publicMessageProducer,
                PUBLIC_REFERENCE_COURTROOM_ADDED,
                courtRoomJsonObject,
                metadataBuilder().withId(randomUUID())
                        .withName(PUBLIC_REFERENCE_COURTROOM_ADDED)
                        .withUserId(randomUUID().toString())
                        .build());

        UUID actualRoomId = findCourtRoomInDbById(roomId);
        assertThat(actualRoomId, is(roomId));
    }

    private JsonObject getCourtRoomJsonObject(final String path, final String courtId, final String courtRoomName) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("ROOM_ID", courtId)
                .replaceAll("ROOM_NAME", courtRoomName);
        LOGGER.info("Payload: {}", strPayload);
        LOGGER.info("ROOM_ID = {}", roomId);
        LOGGER.info("ROOM_NAME = {}", this.roomName);
        return stringToJsonObjectConverter.convert(strPayload);
    }

    private JsonObject getCourtRoomJsonObject(final String path, final String courtId) {
        final String strPayload = getPayloadForCreatingRequest(path)
                .replaceAll("ROOM_ID", courtId);

        LOGGER.info("Payload: {}", strPayload);
        LOGGER.info("ROOM_ID = {}", roomId);

        return stringToJsonObjectConverter.convert(strPayload);
    }


    private static String getPayloadForCreatingRequest(final String ramlPath) {
        String request = null;
        try {
            request = Resources.toString(
                    Resources.getResource(ramlPath),
                    Charset.defaultCharset()
            );
        } catch (final Exception e) {
            fail("Error consuming file from location " + ramlPath);
        }
        return request;
    }
}
