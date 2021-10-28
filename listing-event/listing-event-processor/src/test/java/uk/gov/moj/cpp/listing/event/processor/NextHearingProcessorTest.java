
package uk.gov.moj.cpp.listing.event.processor;


import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.core.courts.HearingListingNeeds;
import uk.gov.justice.core.courts.HearingUnscheduledListingNeeds;
import uk.gov.justice.listing.events.CourtCentreDetails;
import uk.gov.justice.listing.events.DeleteNextHearingRequested;
import uk.gov.justice.listing.events.AllocatedHearingDeleted;

import uk.gov.justice.listing.events.HearingDeleted;
import uk.gov.justice.listing.events.NextHearingDayChanged;
import uk.gov.justice.listing.events.NextHearingRequested;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingAllocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromExistingUnallocatedHearing;
import uk.gov.justice.listing.events.OffencesRemovedFromHearing;
import uk.gov.justice.listing.events.RemoveOffencesFromExistingHearingRequested;
import uk.gov.justice.listing.events.UnscheduledNextHearingRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NextHearingProcessorTest {

    @Mock
    private Sender sender;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Spy
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Spy
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Captor
    private ArgumentCaptor<JsonEnvelope> senderJsonEnvelopeCaptor;

    @InjectMocks
    private NextHearingProcessor nextHearingProcessor;

    @Before
    public void setup() {
        final ObjectToJsonValueConverter objectToJsonValueConverter = new JsonObjectConvertersFactory().objectToJsonValueConverter();

        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.jsonObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.listToJsonArrayConverter, "mapper", new ObjectMapperProducer().objectMapper());
        setField(this.listToJsonArrayConverter, "stringToJsonObjectConverter", stringToJsonObjectConverter);
        setField(this.nextHearingProcessor, "jsonObjectConverter", jsonObjectConverter);
        setField(this.nextHearingProcessor, "objectToJsonValueConverter", objectToJsonValueConverter);
        setField(this.nextHearingProcessor, "listToJsonArrayConverter", listToJsonArrayConverter);
    }

    @Test
    public void shouldHandleNextHearingRequested() {

        final String adjournedFromDate = "2020-01-25";
        final UUID courtCentreId = randomUUID();
        final UUID hearingId = randomUUID();
        final UUID shadowOffenceId = randomUUID();

        final NextHearingRequested nextHearingRequested = NextHearingRequested.nextHearingRequested()
                .withAdjournedFromDate(adjournedFromDate)
                .withHearing(HearingListingNeeds.hearingListingNeeds()
                        .withId(hearingId)
                        .build())
                .withCourtCentreDetails(Arrays.asList(CourtCentreDetails.courtCentreDetails()
                        .withDefaultDuration(30)
                        .withDefaultStartTime(LocalTime.of(10, 0))
                        .withId(courtCentreId)
                        .build()))
                .withShadowListedOffences(Arrays.asList(shadowOffenceId))
                .build();


        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.next-hearing-requested"),
                objectToJsonObjectConverter.convert(nextHearingRequested));

        nextHearingProcessor.handleNextHearingRequested(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        final JsonObject jsonObject = commandEvent.payloadAsJsonObject();
        final JsonObject hearing = jsonObject.getJsonObject("hearing");
        assertThat(hearing.getString("id"), is(hearingId.toString()));
        assertThat(jsonObject.getString("adjournedFromDate"), is(adjournedFromDate));
        final JsonObject jsonCourtCentreDetailsObject = (JsonObject) jsonObject.getJsonArray("courtCentresDetails").get(0);
        assertThat(jsonCourtCentreDetailsObject.getString("id"), is(courtCentreId.toString()));
        assertThat(jsonObject.getJsonArray("shadowListedOffences").getJsonString(0).getString(), is(shadowOffenceId.toString()));

        assertThat(commandEvent.metadata().name(), is("listing.command.list-next-hearing"));
    }

    @Test
    public void shouldHandleDeleteNextHearingRequested() {

        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final DeleteNextHearingRequested deleteNextHearingRequested = DeleteNextHearingRequested.deleteNextHearingRequested()
                .withHearingId(hearingId)
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.delete-next-hearing-requested"),
                objectToJsonObjectConverter.convert(deleteNextHearingRequested));
        nextHearingProcessor.handleDeleteNextHearingRequested(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();
        final JsonObject jsonObject = commandEvent.payloadAsJsonObject();
        assertThat(commandEvent.metadata().name(), is("listing.command.delete-seeded-hearing"));
        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldHandleAllocatedHearingDeleted() {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();

        final AllocatedHearingDeleted hearingDeletedV2 = AllocatedHearingDeleted.allocatedHearingDeleted()
                .withHearingId(hearingId)
                .withCaseIds(Arrays.asList(caseId))
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.allocated-hearing-deleted"),
                objectToJsonObjectConverter.convert(hearingDeletedV2));

        nextHearingProcessor.handleAllocatedHearingDeleted(event);

        verify(this.sender, times(2)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> events = this.senderJsonEnvelopeCaptor.getAllValues();

        final JsonObject publicEventObject = events.get(0).payloadAsJsonObject();
        assertThat(events.get(0).metadata().name(), is("public.events.listing.allocated-hearing-deleted"));
        assertThat(publicEventObject.getString("hearingId"), is(hearingId.toString()));

        final JsonObject commandObject = events.get(1).payloadAsJsonObject();
        assertThat(events.get(1).metadata().name(), is("listing.command.mark-hearing-as-duplicate-for-case"));
        assertThat(commandObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(commandObject.getString("caseId"), is(caseId.toString()));
    }

    @Test
    public void shouldHandleUnallocatedHearingDeleted() {
        final UUID hearingId = randomUUID();
        final UUID caseId = randomUUID();

        final AllocatedHearingDeleted hearingDeletedV2 = AllocatedHearingDeleted.allocatedHearingDeleted()
                .withHearingId(hearingId)
                .withCaseIds(Arrays.asList(caseId))
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.unallocated-hearing-deleted"),
                objectToJsonObjectConverter.convert(hearingDeletedV2));

        nextHearingProcessor.handleUnallocatedHearingDeleted(event);

        verify(this.sender, times(2)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> events = this.senderJsonEnvelopeCaptor.getAllValues();

        final JsonObject publicEventObject = events.get(0).payloadAsJsonObject();
        assertThat(events.get(0).metadata().name(), is("public.events.listing.unallocated-hearing-deleted"));
        assertThat(publicEventObject.getString("hearingId"), is(hearingId.toString()));

        final JsonObject commandObject = events.get(1).payloadAsJsonObject();
        assertThat(events.get(1).metadata().name(), is("listing.command.mark-hearing-as-duplicate-for-case"));
        assertThat(commandObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(commandObject.getString("caseId"), is(caseId.toString()));
    }

    @Test
    public void shouldHandleHearingDeleted() {
        final UUID hearingId = randomUUID();

        final HearingDeleted hearingDeleted = HearingDeleted.hearingDeleted()
                .withHearingIdToBeDeleted(hearingId)
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.hearing-deleted"),
                objectToJsonObjectConverter.convert(hearingDeleted));

        nextHearingProcessor.handleHearingDeleted(event);

        verify(this.sender, times(1)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> events = this.senderJsonEnvelopeCaptor.getAllValues();

        final JsonObject publicEventObject = events.get(0).payloadAsJsonObject();
        assertThat(events.get(0).metadata().name(), is("public.events.listing.hearing-deleted"));
        assertThat(publicEventObject.getString("hearingId"), is(hearingId.toString()));
    }

    @Test
    public void shouldHandleHearingUnallocatedForListing() {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final OffencesRemovedFromHearing offencesRemovedFromHearing = OffencesRemovedFromHearing.offencesRemovedFromHearing()
                .withHearingId(hearingId)
                .withCaseIdsSeededByOnlySeedingHearingId(Arrays.asList(caseId))
                .withSeedingHearingId(seedingHearingId)
                .withSeededOffences(Arrays.asList(offenceId))
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.offences-removed-from-hearing"),
                objectToJsonObjectConverter.convert(offencesRemovedFromHearing));

        nextHearingProcessor.handleOffencesRemovedFromHearing(event);

        verify(this.sender, times(2)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> publishedStreams = this.senderJsonEnvelopeCaptor.getAllValues();


        final JsonEnvelope publishedPublicEvent = publishedStreams.get(0);
        final JsonObject eventObject = publishedPublicEvent.payloadAsJsonObject();
        assertThat(publishedPublicEvent.metadata().name(), is("public.events.listing.offences-removed-from-unallocated-hearing"));
        assertThat(eventObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(eventObject.getJsonArray("offenceIds").size(), is(1));

        final JsonEnvelope publishedCommand = publishedStreams.get(1);
        final JsonObject commandObject = publishedCommand.payloadAsJsonObject();
        assertThat(publishedCommand.metadata().name(), is("listing.command.mark-hearing-as-duplicate-for-case"));
        assertThat(commandObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(commandObject.getString("caseId"), is(caseId.toString()));
    }

    @Test
    public void shouldHandleHearingUnallocatedForListingWhenUnallocatedHearing() {
        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();
        final UUID caseId = randomUUID();
        final UUID offenceId = randomUUID();

        final OffencesRemovedFromHearing offencesRemovedFromHearing = OffencesRemovedFromHearing.offencesRemovedFromHearing()
                .withHearingId(hearingId)
                .withCaseIdsSeededByOnlySeedingHearingId(Arrays.asList(caseId))
                .withSeedingHearingId(seedingHearingId)
                .withSeededOffences(Arrays.asList(offenceId))
                .withUnallocated(true)
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.offences-removed-from-hearing"),
                objectToJsonObjectConverter.convert(offencesRemovedFromHearing));

        nextHearingProcessor.handleOffencesRemovedFromHearing(event);

        verify(this.sender, times(2)).send(this.senderJsonEnvelopeCaptor.capture());

        final List<JsonEnvelope> publishedStreams = this.senderJsonEnvelopeCaptor.getAllValues();

        final JsonEnvelope publishedPublicEvent = publishedStreams.get(0);
        final JsonObject eventObject = publishedPublicEvent.payloadAsJsonObject();
        assertThat(publishedPublicEvent.metadata().name(), is("public.events.listing.hearing-unallocated"));
        assertThat(eventObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(eventObject.getJsonArray("offenceIds").size(), is(1));

        final JsonEnvelope publishedCommand = publishedStreams.get(1);

        final JsonObject commandObject = publishedCommand.payloadAsJsonObject();
        assertThat(publishedCommand.metadata().name(), is("listing.command.mark-hearing-as-duplicate-for-case"));
        assertThat(commandObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(commandObject.getString("caseId"), is(caseId.toString()));
    }

    @Test
    public void shouldHandleUnscheduledNextHearingRequested() {
        final UUID courtCentreId = randomUUID();
        final UUID hearingId = randomUUID();

        final UnscheduledNextHearingRequested nextHearingRequested = UnscheduledNextHearingRequested.unscheduledNextHearingRequested()
                .withHearing(HearingUnscheduledListingNeeds.hearingUnscheduledListingNeeds()
                        .withId(hearingId)
                        .build())
                .withCourtCentreDetails(Arrays.asList(CourtCentreDetails.courtCentreDetails()
                        .withDefaultDuration(30)
                        .withDefaultStartTime(LocalTime.of(10, 0))
                        .withId(courtCentreId)
                        .build()))
                .build();


        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.unscheduled-next-hearing-requested"),
                objectToJsonObjectConverter.convert(nextHearingRequested));

        nextHearingProcessor.handleUnscheduledNextHearingRequested(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope commandEvent = this.senderJsonEnvelopeCaptor.getValue();

        final JsonObject jsonObject = commandEvent.payloadAsJsonObject();
        final JsonObject hearing = jsonObject.getJsonObject("hearing");
        assertThat(hearing.getString("id"), is(hearingId.toString()));
        final JsonObject jsonCourtCentreDetailsObject = (JsonObject) jsonObject.getJsonArray("courtCentresDetails").get(0);
        assertThat(jsonCourtCentreDetailsObject.getString("id"), is(courtCentreId.toString()));

        assertThat(commandEvent.metadata().name(), is("listing.command.list-unscheduled-next-hearing"));
    }

    public void shouldHandleSeedHearingEarliestNextHearingDateUpdated() {

        final UUID seedingHearingId = randomUUID();
        final UUID hearingId = randomUUID();
        final ZonedDateTime startTime = ZonedDateTime.now();
        final NextHearingDayChanged nextHearingDayChanged = NextHearingDayChanged.nextHearingDayChanged()
                .withHearingId(hearingId)
                .withHearingStartDate(startTime)
                .withSeedingHearingId(seedingHearingId)
                .build();
        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.seed-hearing-earliest-next-hearing-date-updated"),
                objectToJsonObjectConverter.convert(nextHearingDayChanged));

        nextHearingProcessor.handleSeedHearingEarliestNextHearingDayUpdated(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope publishedEvent = this.senderJsonEnvelopeCaptor.getValue();
        final JsonObject jsonObject = publishedEvent.payloadAsJsonObject();

        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonObject.getString("seedingHearingId"), is(seedingHearingId.toString()));
        assertThat(jsonObject.getString("earliestHearingStartDate"), is(startTime.withFixedOffsetZone().toString()));
    }

    @Test
    public void shouldHandleOffencesRemovedFromExistingAllocatedHearingEvent() {

        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final OffencesRemovedFromExistingAllocatedHearing offencesToBeDeleted = OffencesRemovedFromExistingAllocatedHearing.offencesRemovedFromExistingAllocatedHearing()
                .withHearingId(hearingId)
                .withOffenceIds(Arrays.asList(offenceId1, offenceId2))
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.offence-from-existing-allocated-hearing-deleted"),
                objectToJsonObjectConverter.convert(offencesToBeDeleted));

        nextHearingProcessor.handleOffencesRemovedFromExistingAllocatedHearingEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope publishedEvent = this.senderJsonEnvelopeCaptor.getValue();
        final JsonObject jsonObject = publishedEvent.payloadAsJsonObject();

        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonObject.getJsonArray("offenceIds").getString(0), is(offenceId1.toString()));
        assertThat(jsonObject.getJsonArray("offenceIds").getString(1), is(offenceId2.toString()));

    }

    @Test
    public void shouldHandleOffencesRemovedFromExistingUnallocatedHearingEvent() {

        final UUID hearingId = randomUUID();
        final UUID offenceId1 = randomUUID();
        final UUID offenceId2 = randomUUID();

        final OffencesRemovedFromExistingUnallocatedHearing offencesToBeDeleted = OffencesRemovedFromExistingUnallocatedHearing.offencesRemovedFromExistingUnallocatedHearing()
                .withHearingId(hearingId)
                .withOffenceIds(Arrays.asList(offenceId1, offenceId2))
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.offence-from-existing-unallocated-hearing-deleted"),
                objectToJsonObjectConverter.convert(offencesToBeDeleted));

        nextHearingProcessor.handleOffencesRemovedFromExistingUnallocatedHearingEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope publishedEvent = this.senderJsonEnvelopeCaptor.getValue();
        final JsonObject jsonObject = publishedEvent.payloadAsJsonObject();

        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonObject.getJsonArray("offenceIds").getString(0), is(offenceId1.toString()));
        assertThat(jsonObject.getJsonArray("offenceIds").getString(1), is(offenceId2.toString()));

    }

    @Test
    public void shouldHandleRemoveOffencesFromExistingHearingRequestedEvent() {

        final UUID hearingId = randomUUID();
        final UUID seedingHearingId = randomUUID();

        final RemoveOffencesFromExistingHearingRequested removeOffencesFromExistingHearingRequested = RemoveOffencesFromExistingHearingRequested.removeOffencesFromExistingHearingRequested()
                .withHearingId(hearingId)
                .withSeedingHearingId(seedingHearingId)
                .build();

        final JsonEnvelope event = envelopeFrom(metadataWithRandomUUID("listing.events.delete-offence-from-existing-hearing-requested"),
                objectToJsonObjectConverter.convert(removeOffencesFromExistingHearingRequested));

        nextHearingProcessor.handleRemoveOffencesFromExistingHearingRequestedEvent(event);

        verify(this.sender).send(this.senderJsonEnvelopeCaptor.capture());

        final JsonEnvelope publishedEvent = this.senderJsonEnvelopeCaptor.getValue();
        final JsonObject jsonObject = publishedEvent.payloadAsJsonObject();

        assertThat(jsonObject.getString("hearingId"), is(hearingId.toString()));
        assertThat(jsonObject.getString("seedingHearingId"), is(seedingHearingId.toString()));

    }

}
