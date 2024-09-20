package uk.gov.moj.cpp.listing.command.service;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingServiceTest {

    private Enveloper enveloper = createEnveloper();

    @Mock
    private HearingQueryView hearingQueryView;

    @InjectMocks
    private HearingService hearingService;

    @BeforeEach
    public void setup() {
        hearingService.setEnveloper(enveloper);
    }

    private static final String HEARING_QUERY_BY_HEARING_ID = "listing.search.hearing";

    @Test
    public void getHearingById() {
        final UUID HEARING_ID = UUID.randomUUID();
        final JsonEnvelope eventEnvelope = generateEmptyEnvelope();
        final ArgumentCaptor<JsonEnvelope> argumentCaptorForRequestEnvelope = ArgumentCaptor.forClass(JsonEnvelope.class);

        hearingService.getHearingById(HEARING_ID, eventEnvelope);

        verify(hearingQueryView).getHearingById(argumentCaptorForRequestEnvelope.capture());
        final JsonEnvelope requestEnvelope = argumentCaptorForRequestEnvelope.getValue();
        assertThat(requestEnvelope.metadata().name(), is(HEARING_QUERY_BY_HEARING_ID));
    }
    private JsonEnvelope generateEmptyEnvelope() {
        return createEnvelope(".", createObjectBuilder().build());
    }
}