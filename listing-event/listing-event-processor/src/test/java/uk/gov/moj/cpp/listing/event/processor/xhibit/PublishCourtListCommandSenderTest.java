package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.time.ZonedDateTime.parse;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.Envelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PublishCourtListCommandSenderTest {

    @Mock
    Sender sender;

    @InjectMocks
    PublishCourtListCommandSender publishCourtListCommandSender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeArgumentCaptor;

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
                requestedTime
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

        publishCourtListCommandSender.publishPublicMessageWithDailyList(tEnvelope, parameters,  mockFileContent);

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
                requestedTime
        );
        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.FINAL.name())
                .add("startDate", startDate.toString())
                .add("requestedTime", requestedTime.toString())
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("DUMMY")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        publishCourtListCommandSender.publishPublicMessageWithDailyList(tEnvelope, parameters,  mockFileContent);

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
                requestedTime
        );
        final JsonObject courtListExportRequested = createObjectBuilder()
                .add("courtCentreId", courtCentreId.toString())
                .add("courtListId", courtListId.toString())
                .add("publishCourtListType", PublishCourtListType.WARN.name())
                .add("startDate", startDate.toString())
                .add("requestedTime", requestedTime.toString())
                .build();
        final Metadata metadata = metadataBuilder()
                .withId(randomUUID())
                .withStreamId(courtListId)
                .withName("DUMMY")
                .withUserId(randomUUID().toString()).build();
        final JsonEnvelope tEnvelope = envelopeFrom(metadata, courtListExportRequested);

        publishCourtListCommandSender.publishPublicMessageWithDailyList(tEnvelope, parameters,  mockFileContent);

        verify(sender, never()).send(envelopeArgumentCaptor.capture());
    }
}