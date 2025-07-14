package uk.gov.moj.cpp.listing.query.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.converter.JsonObjectToObjectConverterFactory;
import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;
import uk.gov.moj.cpp.listing.persistence.repository.CacheRefDataCourtroomRepository;
import uk.gov.moj.cpp.listing.query.view.service.CacheRefDataCourtroomLoader;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

@ExtendWith(MockitoExtension.class)
class CacheRefDataCourtroomViewTest {

    @Mock
    private CacheRefDataCourtroomRepository cacheRefdataCourtroomRepository;

    @InjectMocks
    private CacheRefDataCourtroomLoader cacheRefdataCourtroomLoader;

    private static final String ID = "id";
    private static final String COURTROOM_NAME = "courtroomName";
    private static final String COURTROOM_NAME_VALUE = "Court Room 1";

    @BeforeEach
    public void setup(){
        setField(this.cacheRefdataCourtroomLoader,"cacheRefdataCourtroomRepository", cacheRefdataCourtroomRepository);
        setField(this.cacheRefdataCourtroomLoader, "jsonObjectConverter",  new JsonObjectToObjectConverterFactory().createJsonObjectToObjectConverter());
    }

    @Test
    void addRefDataCourtroom() {
        final JsonObject payload = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(COURTROOM_NAME, COURTROOM_NAME_VALUE)
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), payload);
        cacheRefdataCourtroomLoader.addCourtRoom(envelope);

        ArgumentCaptor<CacheRefDataCourtroom> resultCaptor = ArgumentCaptor.forClass(CacheRefDataCourtroom.class);
        verify(cacheRefdataCourtroomRepository).save(resultCaptor.capture());
    }

    @Test
    void closeRefDataCourtroom() {
        final JsonObject payload = Json.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), payload);
        cacheRefdataCourtroomLoader.closeCourtRoom(envelope);

        ArgumentCaptor<CacheRefDataCourtroom> resultCaptor = ArgumentCaptor.forClass(CacheRefDataCourtroom.class);
        verify(cacheRefdataCourtroomRepository).remove(resultCaptor.capture());
    }
}