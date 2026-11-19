package uk.gov.moj.cpp.listing.query.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.testutil.Envelopes.envelopeFrom;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.listing.testutil.Envelopes.metadataBuilder;
import static uk.gov.moj.cpp.listing.query.api.CacheRefDataCourtroomApi.LISTING_GET_CACHE_REF_DATA_COURTROOMS_REFRESH;


import uk.gov.moj.cpp.listing.query.view.service.CacheRefDataCourtroomLoader;

import java.util.UUID;
import jakarta.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CacheRefDataCourtroomApiTest {

    @Mock
    private CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;

    @InjectMocks
    private CacheRefDataCourtroomApi cacheRefDataCourtroomApi;

    @Test
    public void shouldReturnCountFromLoaderInResponse() {
        final int expectedCount = 5;
        when(cacheRefdataCourtroomLoader.loadCourtRooms()).thenReturn(expectedCount);

        final JsonObject response = cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms();

        assertThat(response.getInt("count"), is(expectedCount));
    }

    @Test
    public void shouldReturnTimestampInResponse() {
        when(cacheRefdataCourtroomLoader.loadCourtRooms()).thenReturn(1);

        final JsonObject response = cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms();

        assertThat(response.getString("timestamp"), notNullValue());
    }

    @Test
    public void shouldReturnZeroCountWhenNoCourtroomsLoaded() {
        when(cacheRefdataCourtroomLoader.loadCourtRooms()).thenReturn(0);

        final JsonObject response = cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms();

        assertThat(response.getInt("count"), is(0));
    }

    @Test
    public void shouldDelegateToCourtroomLoader() {
        when(cacheRefdataCourtroomLoader.loadCourtRooms()).thenReturn(0);

        cacheRefDataCourtroomApi.refreshCacheRefDataCourtrooms();

        verify(cacheRefdataCourtroomLoader).loadCourtRooms();
    }

}
