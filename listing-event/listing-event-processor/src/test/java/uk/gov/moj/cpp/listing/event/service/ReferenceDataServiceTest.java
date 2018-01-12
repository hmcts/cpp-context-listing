package uk.gov.moj.cpp.listing.event.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    private static final UUID COURT_CENTRE_ID = UUID.randomUUID();
    private static final UUID JUDGE_ID = UUID.randomUUID();
    private static final String REFERENCEDATA_GET_JUDGE = "referencedata.get.judge";
    private static final String REFERENCEDATA_GET_COURT_CENTRE = "referencedata.get.court-centre";


    @Mock
    private Requester requester;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;


    @InjectMocks
    private ReferenceDataService referenceDataService;


 

    @Test
    public void getJudgeById() throws Exception {

        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(JUDGE_ID.toString(), REFERENCEDATA_GET_JUDGE)
                .build();
        referenceDataService.getJudgeById(JUDGE_ID, event);

        verify(requester).request(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName(REFERENCEDATA_GET_JUDGE),
                payloadIsJson(
                        withJsonPath("$.id", equalTo(JUDGE_ID.toString()))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }

    @Test
    public void getCourtCentreById() throws Exception {

        final JsonEnvelope event = envelope()
                .with(metadataWithRandomUUIDAndName())
                .withPayloadOf(COURT_CENTRE_ID.toString(), REFERENCEDATA_GET_COURT_CENTRE)
                .build();
        referenceDataService.getCourtCentreById(COURT_CENTRE_ID, event);

        verify(requester).request(envelopeArgumentCaptor.capture());
        assertThat(envelopeArgumentCaptor.getValue(), is(jsonEnvelope(
                withMetadataEnvelopedFrom(event)
                        .withName(REFERENCEDATA_GET_COURT_CENTRE),
                payloadIsJson(
                        withJsonPath("$.id", equalTo(COURT_CENTRE_ID.toString()))
                ))
        ));
        verifyNoMoreInteractions(requester);
    }
}