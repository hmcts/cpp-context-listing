package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithDefaults;

import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

@RunWith(MockitoJUnitRunner.class)
public class XhibitReferenceDataServiceTest {

    private static final String PUBLISH_COURT_LIST_REQUESTED = "listing.event.publish-court-list-requested";
    private static final String REFERENCEDATA_QUERY_COURTROOM = "referencedata.query.courtroom";

    @Mock
    private Requester requester;

    @Mock
    private Logger logger;

    @Mock
    private JsonEnvelope response;

    @Captor
    private ArgumentCaptor<Envelope> requestCaptor;

    @InjectMocks
    private XhibitReferenceDataService xhibitReferenceDataService;

    @Test
    public void shouldGetJudiciary() throws Exception {

        final String oucode = "C55BN00";
        final JsonEnvelope inputEnvelope =
                envelopeFrom(
                        metadataWithDefaults()
                                .withName(PUBLISH_COURT_LIST_REQUESTED),
                        createObjectBuilder()
                                .add("courtCentreId", UUID.randomUUID().toString()).build());

        final JsonEnvelope responseEnvelope =
                envelopeFrom(
                        metadataWithDefaults()
                                .withName(REFERENCEDATA_QUERY_COURTROOM),
                        createObjectBuilder()
                                .add("oucode", oucode).build());

        when(requester.request(any(Envelope.class))).thenReturn(responseEnvelope);

        final UUID judiciaryId = UUID.randomUUID();
        JsonObject judiciary = xhibitReferenceDataService.getJudiciary(inputEnvelope, judiciaryId);

        verify(requester).request(requestCaptor.capture());
        assertThat(judiciary.getString("oucode"), equalTo(oucode));

    }

}