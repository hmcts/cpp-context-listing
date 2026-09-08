package uk.gov.moj.cpp.listing.query.view.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;
import uk.gov.moj.cpp.listing.persistence.repository.CacheRefDataCourtroomRepository;

import java.util.List;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CacheRefDataCourtroomLoaderTest {

    private static final String ID = "id";
    private static final String COURTROOM_NAME = "courtroomName";
    private static final String COURTROOM_NAME_VALUE = "Court Room 1";

    @Mock
    private CacheRefDataCourtroomRepository cacheRefDataCourtroomRepository;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Requester requester;
    @InjectMocks
    private CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;

    @Test
    void shouldNotRefreshWhenListingCacheMatchesRefData() {
        final UUID roomId = UUID.randomUUID();
        final JsonObject refDataPayload = refDataPayloadWithCourtrooms(roomId);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class)).payload()).thenReturn(refDataPayload);
        when(cacheRefDataCourtroomRepository.findAll()).thenReturn(
                List.of(new CacheRefDataCourtroom(roomId, COURTROOM_NAME_VALUE)));

        assertThat(cacheRefdataCourtroomLoader.loadCourtRooms(), is(1));

        verify(cacheRefDataCourtroomRepository).findAll();
        verify(cacheRefDataCourtroomRepository, never()).deleteAll();
        verify(cacheRefDataCourtroomRepository, never()).save(any(CacheRefDataCourtroom.class));
    }

    @Test
    void shouldRefreshWhenListingCacheDiffersFromRefData() {
        final UUID roomId = UUID.randomUUID();
        final JsonObject refDataPayload = refDataPayloadWithCourtrooms(roomId);
        when(requester.requestAsAdmin(any(), eq(JsonObject.class)).payload()).thenReturn(refDataPayload);
        when(cacheRefDataCourtroomRepository.findAll()).thenReturn(List.of());

        assertThat(cacheRefdataCourtroomLoader.loadCourtRooms(), is(1));

        verify(cacheRefDataCourtroomRepository).findAll();
        verify(cacheRefDataCourtroomRepository, times(1)).deleteAll();
        verify(cacheRefDataCourtroomRepository, times(1)).flush();
        verify(cacheRefDataCourtroomRepository, times(1)).clear();
        verify(cacheRefDataCourtroomRepository, times(1)).save(any(CacheRefDataCourtroom.class));
    }

    @Test
    void shouldDoNothingWhenRefDataEmptyWithoutQueryingListingCache() {
        when(requester.requestAsAdmin(any(), eq(JsonObject.class)).payload())
                .thenReturn(JsonObjects.createObjectBuilder().build());

        assertThat(cacheRefdataCourtroomLoader.loadCourtRooms(), is(0));

        verify(cacheRefDataCourtroomRepository, never()).findAll();
        verify(cacheRefDataCourtroomRepository, never()).deleteAll();
    }

    private static JsonObject refDataPayloadWithCourtrooms(final UUID roomId) {
        return JsonObjects.createObjectBuilder()
                .add(CacheRefDataCourtroomLoader.ORGANISATION_UNITS,
                        JsonObjects.createArrayBuilder()
                                .add(JsonObjects.createObjectBuilder()
                                        .add(CacheRefDataCourtroomLoader.COURTROOMS,
                                                JsonObjects.createArrayBuilder()
                                                        .add(JsonObjects.createObjectBuilder()
                                                                .add(ID, roomId.toString())
                                                                .add(COURTROOM_NAME, CacheRefDataCourtroomLoaderTest.COURTROOM_NAME_VALUE)
                                                                .build())
                                                        .build())
                                        .build())
                                .build())
                .build();
    }
} 