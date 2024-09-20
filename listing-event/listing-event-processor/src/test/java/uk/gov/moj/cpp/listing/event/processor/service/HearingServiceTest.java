package uk.gov.moj.cpp.listing.event.processor.service;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.listing.events.Hearing;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingServiceTest {

    private Enveloper enveloper = createEnveloper();

    @Mock
    private HearingQueryView hearingQueryView;

    @InjectMocks
    private HearingService hearingService;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @BeforeEach
    public void setup() {
        hearingService.setEnveloper(enveloper);
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
    }

    private static final String HEARING_QUERY_BY_HEARING_ID = "listing.search.hearing";


    @Test
    public void getHearingById() {
        final UUID HEARING_ID = randomUUID();
        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final JsonEnvelope returnedResponseEnvelope = generateEmptyEnvelope();
        when(hearingQueryView.getHearingById(any(JsonEnvelope.class))).thenReturn(returnedResponseEnvelope);
        final ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        hearingService.getHearingById(HEARING_ID, eventEnvelope);

        verify(hearingQueryView).getHearingById(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is(HEARING_QUERY_BY_HEARING_ID));
    }

    @Test
    public void getHearing() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();

        when(hearingQueryView.getHearingById(any())).thenReturn(generateHearingEnvelope(hearingId));
        final ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        final Hearing hearing = hearingService.getHearing(hearingId, eventEnvelope);

        assertThat(hearing.getId(), is(hearingId));
    }


    private JsonEnvelope generateHearingEnvelope(final UUID hearingId) {
        final JsonObjectBuilder hearingAllocated = createObjectBuilder()
                .add("id", hearingId.toString());

        return envelopeFrom(metadataWithDefaults().withName("."), hearingAllocated);
    }

    private JsonEnvelope generateEmptyEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }

}