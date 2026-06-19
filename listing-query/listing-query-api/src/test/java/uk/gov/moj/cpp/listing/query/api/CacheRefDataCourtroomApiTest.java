package uk.gov.moj.cpp.listing.query.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.messaging.spi.DefaultJsonMetadata.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.moj.cpp.listing.query.api.CacheRefDataCourtroomApi.LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.service.CacheRefDataCourtroomLoader;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CacheRefDataCourtroomApiTest {

    @Spy
    private final Enveloper enveloper = createEnveloper();

    @Mock
    private CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;

    @InjectMocks
    private CacheRefDataCourtroomApi cacheRefDataCourtroomApi;

    @Test
    public void shouldReturnCountFromLoaderInResponse() {
        final int expectedCount = 5;
        when(cacheRefdataCourtroomLoader.loadCourtRooms()).thenReturn(expectedCount);

        final JsonEnvelope response = cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms(requestEnvelope());

        assertThat(response.payloadAsJsonObject().getInt("count"), is(expectedCount));
    }

    @Test
    public void shouldReturnTimestampInResponse() {
        when(cacheRefdataCourtroomLoader.loadCourtRooms()).thenReturn(1);

        final JsonEnvelope response = cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms(requestEnvelope());

        assertThat(response.payloadAsJsonObject().getString("timestamp"), notNullValue());
    }

    @Test
    public void shouldReturnZeroCountWhenNoCourtroomsLoaded() {
        when(cacheRefdataCourtroomLoader.loadCourtRooms()).thenReturn(0);

        final JsonEnvelope response = cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms(requestEnvelope());

        assertThat(response.payloadAsJsonObject().getInt("count"), is(0));
    }

    @Test
    public void shouldDelegateToCourtroomLoader() {
        when(cacheRefdataCourtroomLoader.loadCourtRooms()).thenReturn(0);

        cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms(requestEnvelope());

        verify(cacheRefdataCourtroomLoader).loadCourtRooms();
    }

    private JsonEnvelope requestEnvelope() {
        return envelopeFrom(
                metadataBuilder()
                        .withName(LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH)
                        .withId(UUID.randomUUID())
                        .build(),
                createObjectBuilder().build());
    }
}
