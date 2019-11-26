package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;

import java.time.LocalDate;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@Stateless
public class PublishCourtListCommandSender {
    private static final String ERROR_MESSAGE = "errorMessage";

    private static final String RECORD_COURT_LIST_PRODUCED = "listing.command.record-court-list-produced";
    private static final String RECORD_COURT_LIST_EXPORT_SUCCESSFUL = "listing.command.record-court-list-export-successful";
    private static final String RECORD_COURT_LIST_EXPORT_FAILED = "listing.command.record-court-list-export-failed";
    private static final String COURT_LIST_FILE_NAME = "courtListFileName";
    private static final String COURT_LIST_FILE_ID = "courtListFileId";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String PUBLISH_COURT_LIST_TYPE = "publishCourtListType";
    public static final String PUBLISH_COURT_LIST_REQUEST_ID = "publishCourtListRequestId";

    @Inject
    @FrameworkComponent("EVENT_PROCESSOR")
    private Sender sender;

    @Inject
    private UtcClock utcClock;

    public void recordCourtListProduced(final PublishCourtListRequestParameters requestParameters, final UUID courtListFileId,
                                        final String courtListFileName) {

        final JsonObject payload = createObjectBuilder()
                .add(PUBLISH_COURT_LIST_REQUEST_ID, requestParameters.getPublishCourtListRequestId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add(COURT_LIST_FILE_ID, courtListFileId.toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add("publishStatus", "COURT_LIST_PRODUCED")
                .add("producedTime", ZonedDateTimes.toString(utcClock.now()))
                .add("publishDate", LocalDate.now().toString())
                .build();

        sendCommandWith(RECORD_COURT_LIST_PRODUCED, requestParameters.getPublishCourtListRequestId(), payload);
    }

    public void recordCourtListExportSuccessful(final UUID publishCourtListRequestId) {

        final JsonObject payload = createObjectBuilder()
                .add(PUBLISH_COURT_LIST_REQUEST_ID, publishCourtListRequestId.toString())
                .add("publishedTime", ZonedDateTimes.toString(utcClock.now()))
                .build();

        sendCommandWith(RECORD_COURT_LIST_EXPORT_SUCCESSFUL, publishCourtListRequestId, payload);
    }

    public void recordCourtListExportFailed(final UUID publishCourtListRequestId,
                                            final String errorMessage) {

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add(PUBLISH_COURT_LIST_REQUEST_ID, publishCourtListRequestId.toString())
                .add("failedTime", ZonedDateTimes.toString(utcClock.now()))
                .add(ERROR_MESSAGE, errorMessage);

        sendCommandWith(RECORD_COURT_LIST_EXPORT_FAILED, publishCourtListRequestId, objectBuilder.build());
    }

    private void sendCommandWith(final String commandName, final UUID streamId, final JsonObject payload) {

        sender.send(envelopeFrom(
                metadataBuilder()
                        .withStreamId(streamId)
                        .createdAt(utcClock.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                payload));
    }
}
