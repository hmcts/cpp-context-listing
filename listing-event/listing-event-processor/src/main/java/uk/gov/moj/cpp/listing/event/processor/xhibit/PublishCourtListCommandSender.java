package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;

import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@Stateless
public class PublishCourtListCommandSender {
    private static final String ERROR_MESSAGE = "errorMessage";

    private static final String RECORD_COURT_LIST_PUBLISHED = "listing.command.record-court-list-published";
    private static final String RECORD_COURT_LIST_EXPORT_SUCCESSFUL = "listing.command.record-court-list-export-successful";
    private static final String RECORD_COURT_LIST_EXPORT_FAILED = "listing.command.record-court-list-export-failed";
    private static final String COURT_LIST_FILE_NAME = "courtListFileName";
    private static final String COURT_LIST_FILE_ID = "courtListFileId";

    @Inject
    @FrameworkComponent("EVENT_PROCESSOR")
    private Sender sender;

    @Inject
    private UtcClock utcClock;

    public void recordCourtListProduced(final UUID courtListFileId,
                                        final String courtListFileName) {

        final JsonObject payload = createObjectBuilder()
                .add(COURT_LIST_FILE_ID, courtListFileId.toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add("producedTime", ZonedDateTimes.toString(utcClock.now()))
                .build();

        sendCommandWith(RECORD_COURT_LIST_PUBLISHED, courtListFileId, payload);
    }

    @SuppressWarnings("squid:S1192")
    public void recordCourtListExportSuccessful(final UUID courtListFileId,
                                                final String courtListFileName) {

        final JsonObject payload = createObjectBuilder()
                .add(COURT_LIST_FILE_ID, courtListFileId.toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add("publishedTime", ZonedDateTimes.toString(utcClock.now()))
                .build();

        sendCommandWith(RECORD_COURT_LIST_EXPORT_SUCCESSFUL, courtListFileId, payload);
    }

    public void recordCourtListExportFailed(final UUID courtListFileId,
                                            final String courtListFileName,
                                            final String errorMessage) {

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add(COURT_LIST_FILE_ID, courtListFileName.toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add("failedTime", ZonedDateTimes.toString(utcClock.now()))
                .add(ERROR_MESSAGE, errorMessage);

        sendCommandWith(RECORD_COURT_LIST_EXPORT_FAILED, courtListFileId, objectBuilder.build());
    }

    private void sendCommandWith(final String commandName, final UUID notificationId, final JsonObject payload) {

        sender.send(envelopeFrom(
                metadataBuilder()
                        .withStreamId(notificationId)
                        .createdAt(utcClock.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                payload));
    }
}
