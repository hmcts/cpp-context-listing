package uk.gov.moj.cpp.listing.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseRemovedFromHearingEventProcessorTest {

    private static final UUID HEARING_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @InjectMocks
    private CaseRemovedFromHearingEventProcessor processor;

    @Test
    public void processPublicEventCaseRemovedFromHearing() {
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("public.progression.case-removed-from-hearing"),
                JsonObjects.createObjectBuilder()
                        .add("hearingId", HEARING_ID.toString())
                        .add("caseId", CASE_ID.toString())
                        .build());

        processor.processPublicProgressionCaseRemovedFromHearing(event);

        verify(this.sender).send(this.envelopeArgumentCaptor.capture());

        assertThat(envelopeArgumentCaptor.getValue(),
                jsonEnvelope(metadata().withName("listing.command.remove-case-from-hearing"), payloadIsJson(allOf(
                        withJsonPath("$.hearingId", is(HEARING_ID.toString())),
                        withJsonPath("$.caseId", is(CASE_ID.toString()))
                ))));
    }
}
