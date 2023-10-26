package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.time.ZonedDateTime.parse;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.listing.event.utils.FileUtil.givenPayload;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.framework.api.JsonObjectConvertersFactory;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.util.HearingObjectsListingToCoreConverter;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PublishCourtListCommandSenderTest {

    @Mock
    Sender sender;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter = new JsonObjectConvertersFactory().objectToJsonObjectConverter();

    @Spy
    private HearingObjectsListingToCoreConverter hearingObjectsListingToCoreConverter = new HearingObjectsListingToCoreConverter();

    @Spy
    private JsonObjectToObjectConverter jsonObjectConverter = new JsonObjectConvertersFactory().jsonObjectToObjectConverter();

    @InjectMocks
    PublishCourtListCommandSender publishCourtListCommandSender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

    @Mock
    ProgressionService progressionService;

    @Test
    public void shouldSendPublicMessageWithTheDailyListXML_WhenRequestIsForDRAFT() {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.now();
        final String mockFileContent = "FILE_CONTENT";
        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");
        final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                courtListId,
                courtCentreId,
                startDate,
                startDate.plusDays(5),
                PublishCourtListType.DRAFT,
                requestedTime,
                true
        );
        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.DRAFT.name())
                .add("startDate", startDate.toString())
                .add("requestedTime", requestedTime.toString())
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("DUMMY")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        publishCourtListCommandSender.publishPublicMessageWithDailyList(tEnvelope, parameters, mockFileContent);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope value = envelopeArgumentCaptor.getValue();

        assertThat(value.metadata().name(), is("public.listing.court-daily-list"));

        final JsonObject jsonObject = value.payloadAsJsonObject();

        assertThat(jsonObject.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(jsonObject.getString("dailyListDocument"), is(mockFileContent));
    }


    @Test
    public void shouldSendPublicMessageWithTheDailyListXML_WhenRequestIsForFINAL() {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.now();
        final String mockFileContent = "FILE_CONTENT";
        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");
        final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                courtListId,
                courtCentreId,
                startDate,
                startDate.plusDays(5),
                PublishCourtListType.FINAL,
                requestedTime,
                true
        );
        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.FINAL.name())
                .add("startDate", startDate.toString())
                .add("requestedTime", requestedTime.toString())
                .add("sendNotificationToParties", true)
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("DUMMY")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        publishCourtListCommandSender.publishPublicMessageWithDailyList(tEnvelope, parameters, mockFileContent);

        verify(sender).send(envelopeArgumentCaptor.capture());

        final JsonEnvelope value = this.envelopeArgumentCaptor.getValue();

        assertThat(value.metadata().name(), is("public.listing.court-daily-list"));

        final JsonObject jsonObject = value.payloadAsJsonObject();

        assertThat(jsonObject.getString("courtCentreId"), is(courtCentreId.toString()));
        assertThat(jsonObject.getString("dailyListDocument"), is(mockFileContent));
    }


    @Test
    public void shouldNotSendPublicMessageXML_WhenRequestIsForOthers() {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.now();
        final String mockFileContent = "FILE_CONTENT";
        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");
        final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                courtListId,
                courtCentreId,
                startDate,
                startDate.plusDays(5),
                PublishCourtListType.WARN,
                requestedTime,
                true
        );
        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.WARN.name())
                .add("startDate", startDate.toString())
                .add("requestedTime", requestedTime.toString())
                .add("sendNotificationToParties", true)
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("DUMMY")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        publishCourtListCommandSender.publishPublicMessageWithDailyList(tEnvelope, parameters, mockFileContent);

        verify(sender, never()).send(envelopeArgumentCaptor.capture());
    }

    @Test
    public void shouldMapDefendantsWithinCourtListJsonToCoreDefendants() {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.now();
        final JsonObject courtListJson = givenPayload("/test-data/listing.event.court-list-export-requested-warn-courtListJson.json");
        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");
        final Boolean sendNotificationToParties = true;
        final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                courtListId,
                courtCentreId,
                startDate,
                startDate.plusDays(5),
                PublishCourtListType.FIRM,
                requestedTime,
                sendNotificationToParties
        );
        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.FIRM.name())
                .add("startDate", startDate.toString())
                .add("requestedTime", requestedTime.toString())
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("DUMMY")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        final Optional<JsonObject> jsonObject = Optional.of(createObjectBuilder().add("caseId", randomUUID().toString()).build());

        when(progressionService.caseExistsByCaseUrn(any(JsonEnvelope.class), any(String.class))).thenReturn(jsonObject);

        publishCourtListCommandSender.publishPublicMessageForCourtList(tEnvelope, parameters, courtListJson);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        JsonEnvelope jsonEnvelope = envelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("public.listing.court-list-published"));
        JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        assertThat(payload, notNullValue());
        verifyCourtListPublicEventDetails(payload, courtCentreId.toString(), startDate.toString(), PublishCourtListType.FIRM.toString(), 4);


    }

    @Test
    public void shouldMapDefendantsWithinCourtListJson_MultipleDefendants() {

        final UUID courtCentreId = randomUUID();
        final UUID courtListId = randomUUID();
        final LocalDate startDate = LocalDate.now();
        final JsonObject courtListJson = givenPayload("/test-data/listing.event.court-list-export-requested-warn-courtListJson-multiple.json");
        final ZonedDateTime requestedTime = parse("2018-01-02T13:04:05+00:00[UTC]");
        final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                courtListId,
                courtCentreId,
                startDate,
                startDate.plusDays(5),
                PublishCourtListType.FIRM,
                requestedTime,
                Boolean.TRUE
        );
        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.FIRM.name())
                .add("startDate", startDate.toString())
                .add("requestedTime", requestedTime.toString())
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("DUMMY")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        final Optional<JsonObject> jsonObject = Optional.of(createObjectBuilder().add("caseId", randomUUID().toString()).build());

        when(progressionService.caseExistsByCaseUrn(any(JsonEnvelope.class), any(String.class))).thenReturn(jsonObject);

        publishCourtListCommandSender.publishPublicMessageForCourtList(tEnvelope, parameters, courtListJson);

        verify(sender, times(1)).send(envelopeArgumentCaptor.capture());
        JsonEnvelope jsonEnvelope = envelopeArgumentCaptor.getValue();
        assertThat(jsonEnvelope.metadata().name(), is("public.listing.court-list-published"));
        JsonObject payload = jsonEnvelope.payloadAsJsonObject();
        assertThat(payload, notNullValue());
        verifyCourtListPublicEventDetails(payload, courtCentreId.toString(), startDate.toString(), PublishCourtListType.FIRM.toString(), 6);


    }

    private void verifyCourtListPublicEventDetails(final JsonObject payload, final String courtCentreId, final String startDate, final String publishCourtListType, final int courtListSize){
            assertThat(payload.getString("courtCentreId",null), notNullValue());
            assertThat(payload.getString("courtCentreId",null), is(courtCentreId));
            assertThat(payload.getString("startDate",null), notNullValue());
            assertThat(payload.getString("startDate",null), is(startDate));
            assertThat(payload.getString("publishCourtListType",null), notNullValue());
            assertThat(payload.getString("publishCourtListType",null), is(publishCourtListType));
            assertThat(payload.getJsonArray("courtLists"), Matchers.hasSize(courtListSize));
            payload.getJsonArray("courtLists").getValuesAs(JsonObject.class).stream()
                .forEach(listItem -> assertThat(listItem.getString("sittingDate"), Matchers.anyOf(is("2023-02-20"), is("2023-02-21"))));
    }
}