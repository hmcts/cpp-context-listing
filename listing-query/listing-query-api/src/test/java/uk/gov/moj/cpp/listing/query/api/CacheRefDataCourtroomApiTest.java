package uk.gov.moj.cpp.listing.query.api;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.converter.JsonObjectToObjectConverterFactory;
import uk.gov.moj.cpp.listing.persistence.entity.CacheRefDataCourtroom;
import uk.gov.moj.cpp.listing.persistence.repository.CacheRefDataCourtroomRepository;
import uk.gov.moj.cpp.listing.query.view.CacheRefDataCourtroomView;
import uk.gov.moj.cpp.listing.query.view.service.CacheRefDataCourtroomLoader;

import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CacheRefDataCourtroomApiTest {

    private static final String ID = "id";
    private static final String COURTROOM_NAME = "courtroomName";
    private static final String COURTROOM_NAME_VALUE = "Court Room 1";

    @InjectMocks
    private CacheRefDataCourtroomApi cacheRefdataCourtroomApi;

    @Spy
    private CacheRefDataCourtroomView cacheRefDataCourtroomView;

    @Spy
    private CacheRefDataCourtroomLoader cacheRefDataCourtroomLoader;

    @Mock
    private CacheRefDataCourtroomRepository cacheRefdataCourtroomRepository;

    @BeforeEach
    public void setup() {
        setField(this.cacheRefdataCourtroomApi, "cacheRefDataCourtroomView", cacheRefDataCourtroomView);
        setField(this.cacheRefDataCourtroomView, "cacheRefdataCourtroomLoader", cacheRefDataCourtroomLoader);
        setField(this.cacheRefDataCourtroomLoader, "jsonObjectConverter", new JsonObjectToObjectConverterFactory().createJsonObjectToObjectConverter());
        setField(this.cacheRefDataCourtroomLoader, "cacheRefdataCourtroomRepository", cacheRefdataCourtroomRepository);
    }

    @Test
    public void shouldAddRefDataCourtroom() {
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .add(COURTROOM_NAME, COURTROOM_NAME_VALUE)
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), payload);

        cacheRefdataCourtroomApi.addRefDataCourtroom(envelope);

        ArgumentCaptor<CacheRefDataCourtroom> resultCaptor = ArgumentCaptor.forClass(CacheRefDataCourtroom.class);
        verify(cacheRefdataCourtroomRepository).save(resultCaptor.capture());
        verify(cacheRefdataCourtroomRepository, times(1)).save(resultCaptor.capture());
    }

    @Test
    public void shouldCloseRefDataCourtroom() {
        final JsonObject payload = JsonObjects.createObjectBuilder()
                .add(ID, UUID.randomUUID().toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), payload);

        cacheRefdataCourtroomApi.closeRefDataCourtroom(envelope);

        ArgumentCaptor<CacheRefDataCourtroom> resultCaptor = ArgumentCaptor.forClass(CacheRefDataCourtroom.class);
        verify(cacheRefdataCourtroomRepository).remove(resultCaptor.capture());
        verify(cacheRefdataCourtroomRepository, times(1)).remove(resultCaptor.capture());
    }
} 