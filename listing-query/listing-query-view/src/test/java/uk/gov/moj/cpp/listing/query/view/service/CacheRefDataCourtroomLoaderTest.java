package uk.gov.moj.cpp.listing.query.view.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;
import uk.gov.moj.cpp.listing.persistence.repository.CacheRefDataCourtroomRepository;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CacheRefDataCourtroomLoaderTest {

    private static final String ID = "id";
    private static final String COURTROOM_NAME = "courtroomName";
    private static final String COURTROOM_NAME_VALUE = "Court Room 1";

    @Mock
    private CacheRefDataCourtroomRepository cacheRefDataCourtroomRepository;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    @InjectMocks
    private CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;

    @Test
    void shouldAddCourtroomWhenCourtroomAddedEventReceived() {
        UUID roomId = UUID.randomUUID();
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add(ID, roomId.toString())
                .add(COURTROOM_NAME, COURTROOM_NAME_VALUE)
                .build();

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), payload);

        cacheRefdataCourtroomLoader.addCourtRoom(envelope);

        verify(cacheRefDataCourtroomRepository, times(1)).save(any(CacheRefDataCourtroom.class));
    }

    @Test
    void shouldCloseCourtRoomWhenCourtroomClosedEventReceived() {

        UUID roomId = UUID.randomUUID();
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add(ID, roomId.toString())
                .add(COURTROOM_NAME, COURTROOM_NAME_VALUE)
                .build();

        CacheRefDataCourtroom cacheRefDataCourtroom1 = new CacheRefDataCourtroom(roomId, COURTROOM_NAME_VALUE);
        when(cacheRefDataCourtroomRepository.findBy(roomId)).thenReturn(cacheRefDataCourtroom1);

        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), payload);

        cacheRefdataCourtroomLoader.closeCourtRoom(envelope);
        verify(cacheRefDataCourtroomRepository, times(1)).findBy(roomId);
        verify(cacheRefDataCourtroomRepository, times(1)).remove(cacheRefDataCourtroom1);
    }
} 