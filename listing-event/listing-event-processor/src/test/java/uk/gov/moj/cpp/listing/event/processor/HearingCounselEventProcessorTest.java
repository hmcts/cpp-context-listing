package uk.gov.moj.cpp.listing.event.processor;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.StringReader;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class HearingCounselEventProcessorTest {


    public static final String LISTING_COMMAND_HANDLER_MODIFY_HEARING_COUNSEL = "listing.command.handler.modify-hearing-counsel";
    @Mock
    private Sender sender;

    @InjectMocks
    private HearingCounselEventProcessor hearingCounselEventProcessor;

    private final ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor =
            ArgumentCaptor.forClass(JsonEnvelope.class);

    @Test
    public void hearingDefenceCounselAdded() {
        verifyEventsHandled("/test-data/public.hearing.defence-counsel-added-or-updated.json",
                getPayload("test-data/listing.command.handler.modify-hearing-defence-counsel.json")
                        .replace("ACTION", "ADD"),
                envelope -> hearingCounselEventProcessor.hearingDefenceCounselAdded(envelope));
    }

    @Test
    public void hearingDefenceCounselUpdated() {
        verifyEventsHandled("/test-data/public.hearing.defence-counsel-added-or-updated.json",
                getPayload("test-data/listing.command.handler.modify-hearing-defence-counsel.json")
                        .replace("ACTION", "UPDATE"),
                envelope -> hearingCounselEventProcessor.hearingDefenceCounselUpdated(envelope));
    }

    @Test
    public void hearingDefenceCounselRemoved() {
        verifyEventsHandled("/test-data/public.hearing.counsel-removed.json",
                getPayload("test-data/listing.command.handler.modify-hearing-counsel-remove.json")
                        .replace("COUNSEL_TYPE", "DEFENCE"),
                envelope -> hearingCounselEventProcessor.hearingDefenceCounselRemoved(envelope));
    }


    @Test
    public void hearingProsecutionCounselAdded() {
        verifyEventsHandled("/test-data/public.hearing.prosecution-counsel-added-or-updated.json",
                getPayload("test-data/listing.command.handler.modify-hearing-prosecution-counsel.json")
                        .replace("ACTION", "ADD"),
                envelope -> hearingCounselEventProcessor.hearingProsecutionCounselAdded(envelope));
    }

    @Test
    public void hearingProsecutionCounselUpdated() {
        verifyEventsHandled("/test-data/public.hearing.prosecution-counsel-added-or-updated.json",
                getPayload("test-data/listing.command.handler.modify-hearing-prosecution-counsel.json")
                        .replace("ACTION", "UPDATE"),
                envelope -> hearingCounselEventProcessor.hearingProsecutionCounselUpdated(envelope));
    }

    @Test
    public void hearingProsecutionCounselRemoved() {
        verifyEventsHandled("/test-data/public.hearing.counsel-removed.json",
                getPayload("test-data/listing.command.handler.modify-hearing-counsel-remove.json")
                        .replace("COUNSEL_TYPE", "PROSECUTION"),
                envelope -> hearingCounselEventProcessor.hearingProsecutionCounselRemoved(envelope));
    }


    private void verifyEventsHandled(String inputPayloadFile, String expectedPayload,
                                     Consumer<JsonEnvelope> handler) {
        //when

        handler.accept(envelopeFrom(metadataWithRandomUUIDAndName(),
                givenPayload(inputPayloadFile)));

        //then
        verifyCommandHandlerCalled(expectedPayload);
    }

    private void verifyCommandHandlerCalled(final String expectedPayload) {
        verify(sender).send(senderJsonEnvelopeCaptor.capture());
        assertThat(senderJsonEnvelopeCaptor.getValue().metadata().name(),
                is(LISTING_COMMAND_HANDLER_MODIFY_HEARING_COUNSEL));
        assertThat(senderJsonEnvelopeCaptor.getValue().payloadAsJsonObject(),
                is(toJsonObject(expectedPayload)));
    }

    private JsonObject toJsonObject(final String value) {
        return Json.createReader(new StringReader(value)).readObject();
    }
}
