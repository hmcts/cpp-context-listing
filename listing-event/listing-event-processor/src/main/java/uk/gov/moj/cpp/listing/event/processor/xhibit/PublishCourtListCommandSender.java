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

import java.time.ZonedDateTime;
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

    private static final String PUBLISH_STATUS = "publishStatus";
    private static final String PRODUCED_TIME = "producedTime";
    private static final String PUBLISHED_TIME = "publishedTime";
    private static final String PUBLISH_DATE = "publishDate";


    private static final String WEEK_COMMENCING = "weekCommencing";

    @Inject
    @FrameworkComponent("EVENT_PROCESSOR")
    private Sender sender;

    @Inject
    private UtcClock utcClock;

    public void recordCourtListProduced(final PublishCourtListRequestParameters requestParameters, final UUID courtListFileId,
                                        final String courtListFileName) {
        final ZonedDateTime now = utcClock.now();

        final JsonObject payload = createObjectBuilder()
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add(COURT_LIST_FILE_ID, courtListFileId.toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add(PUBLISH_STATUS, "COURT_LIST_PRODUCED")
                .add(PRODUCED_TIME, ZonedDateTimes.toString(now))
                .add(WEEK_COMMENCING, requestParameters.isWeekCommencing())
                .add(PUBLISH_DATE, now.toLocalDate().toString())
                .build();

        sendCommandWith(RECORD_COURT_LIST_PRODUCED, courtListFileId, payload);
    }

    @SuppressWarnings("squid:S1192")
    public void recordCourtListExportSuccessful(final UUID courtListFileId,
                                                final String courtListFileName,
                                                final boolean weekCommencing) {

        final JsonObject payload = createObjectBuilder()
                .add(COURT_LIST_FILE_ID, courtListFileId.toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add(PUBLISHED_TIME, ZonedDateTimes.toString(utcClock.now()))
                .add(WEEK_COMMENCING, weekCommencing)
                .build();

        sendCommandWith(RECORD_COURT_LIST_EXPORT_SUCCESSFUL, courtListFileId, payload);
    }

    public void recordCourtListExportFailed(final UUID courtListFileId,
                                            final String courtListFileName,
                                            final String errorMessage,
                                            final boolean weekCommencing) {

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add(COURT_LIST_FILE_ID, courtListFileName)
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add("failedTime", ZonedDateTimes.toString(utcClock.now()))
                .add(ERROR_MESSAGE, errorMessage)
                .add(WEEK_COMMENCING, weekCommencing)
                ;

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
