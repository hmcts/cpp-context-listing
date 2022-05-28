package uk.gov.moj.cpp.listing.event.processor;

import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CasesAddedToHearing;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.UpdateExistingHearingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
public class UpdateExistingHearingEventProcessorTest {

    @Mock
    private Sender sender;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private UpdateExistingHearingEventProcessor processor;


    @Test
    public void shouldEmitAddCasesForHearingCommand() {

        final UUID hearingId = randomUUID();
        final UUID prosecutionCaseId = randomUUID();
        final UUID shadowOffenceId = randomUUID();

        final UpdateExistingHearingRequested updateExistingHearingRequested = UpdateExistingHearingRequested.updateExistingHearingRequested()
                .withHearingId(hearingId)
                .withProsecutionCases(Arrays.asList(ProsecutionCase.prosecutionCase()
                        .withId(prosecutionCaseId)
                        .build()))
                .withShadowListedOffences(Arrays.asList(shadowOffenceId))
                .build();


        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.update-existing-hearing-requested"),
                objectToJsonObjectConverter.convert(updateExistingHearingRequested));

        processor.handleUpdateExistingHearingRequestedEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        final JsonObject jsonObject = commandEvent.payloadAsJsonObject();

        assertThat(commandEvent.metadata().name(), is("listing.command.add-cases-to-hearing"));

        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonObject.getJsonArray("prosecutionCases").getJsonObject(0).getString("id"), is(prosecutionCaseId.toString()));
        assertThat(jsonObject.getJsonArray("shadowListedOffences").getJsonString(0).getString(), is(shadowOffenceId.toString()));

    }

    @Test
    public void shouldRaisePublicEventWhenCasesAddedWithSeedingHearingId() {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.case-added-to-hearing"),
                objectToJsonObjectConverter.convert(CasesAddedToHearing.casesAddedToHearing()
                        .withHearingId(hearingId)
                        .withSeedingHearingId(seedingHearingId)
                        .build()));
        processor.handleCasesAddedToHearingEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope publicEvent = this.senderJsonEnvelopeCaptor.getValue();

        final JsonObject jsonObject = publicEvent.payloadAsJsonObject();

        assertThat(publicEvent.metadata().name(), is("public.events.listing.cases-added-for-updated-related-hearing"));

        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonObject.getString("seedingHearingId"), is(seedingHearingId.toString()));
    }

    @Test
    public void shouldNotRaisePublicEventWhenCasesAddedWithoutSeedingHearingId() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.case-added-to-hearing"),
                objectToJsonObjectConverter.convert(CasesAddedToHearing.casesAddedToHearing()
                        .withHearingId(hearingId)
                        .build()));
        processor.handleCasesAddedToHearingEvent(event);

        verify(this.sender, never()).send(this.senderJsonEnvelopeCaptor.capture());

    }

    @Test
    public void shouldRaisePublicEventWhenSendPublicEventExists() {
        final UUID hearingId = randomUUID();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.case-added-to-hearing"),
                objectToJsonObjectConverter.convert(CasesAddedToHearing.casesAddedToHearing()
                        .withHearingId(hearingId)
                        .withUnAllocatedListedCases(Collections.singletonList(ListedCase.listedCase()
                                .withId(randomUUID())
                                .withIsEjected(true)
                                .withDefendants(Collections.singletonList(Defendant.defendant()
                                        .withId(randomUUID())
                                        .withLastName("Summer")
                                        .withOffences(Collections.singletonList(Offence.offence()
                                                .withId(randomUUID())
                                                .withOffenceCode("offenceCode")
                                                .build()))
                                        .build()))
                                .build()))
                        .withAddCasesToUnAllocatedHearing(true)
                        .build()));
        processor.handleCasesAddedToHearingEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());
        final JsonEnvelope publicEvent = this.senderJsonEnvelopeCaptor.getValue();
        assertThat(publicEvent.metadata().name(), is ("public.listing.cases-added-to-hearing"));
        assertThat(publicEvent.payloadAsJsonObject().getString("hearingId"), is(hearingId.toString()));
    }
}
