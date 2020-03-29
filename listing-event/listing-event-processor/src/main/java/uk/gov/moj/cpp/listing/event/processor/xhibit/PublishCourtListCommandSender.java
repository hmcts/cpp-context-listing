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

import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@Stateless
public class PublishCourtListCommandSender {

    public static final String PUBLISH_COURT_LIST_REQUEST_ID = "publishCourtListRequestId";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String RECORD_COURT_LIST_EXPORT_SUCCESSFUL = "listing.command.record-court-list-export-successful";
    private static final String RECORD_COURT_LIST_EXPORT_FAILED = "listing.command.record-court-list-export-failed";
    private static final String STORE_PUBLISHED_COURT_LIST = "listing.command.store-published-court-list";
    private static final String COURT_LIST_REQUEST_EXPORT = "listing.command.court-list-request-export";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String PUBLISH_COURT_LIST_TYPE = "publishCourtListType";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String COURT_LIST_FILE_NAME = "courtListFileName";
    private static final String COURT_LIST_JSON = "courtListJson";

    @Inject
    @FrameworkComponent("EVENT_PROCESSOR")
    private Sender sender;

    @Inject
    private UtcClock utcClock;

    public void recordCourtListExportSuccessful(final PublishCourtListRequestParameters requestParameters, final String courtListFileName) {

        final JsonObject payload = createObjectBuilder()
                .add("courtListId", requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(END_DATE, requestParameters.getEndDate().toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add("exportedTime", ZonedDateTimes.toString(utcClock.now()))
                .build();

        sendCommandWith(RECORD_COURT_LIST_EXPORT_SUCCESSFUL, requestParameters.getCourtListId(), payload);
    }

    public void recordCourtListExportFailed(final PublishCourtListRequestParameters requestParameters,
                                            final String errorMessage,
                                            final String courtListFileName) {

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("courtListId", requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(END_DATE, requestParameters.getEndDate().toString())
                .add(COURT_LIST_FILE_NAME, courtListFileName)
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add("failedTime", ZonedDateTimes.toString(utcClock.now()))
                .add(ERROR_MESSAGE, errorMessage);

        sendCommandWith(RECORD_COURT_LIST_EXPORT_FAILED, requestParameters.getCourtListId(), objectBuilder.build());
    }

    public void storePublishedCourtList(final PublishCourtListRequestParameters requestParameters, final JsonObject courtListJson) {

        final JsonObject payload = createObjectBuilder()
                .add(PUBLISH_COURT_LIST_REQUEST_ID, requestParameters.getCourtListId().toString())
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add(COURT_LIST_JSON, courtListJson.toString())
                .build();

        sendCommandWith(STORE_PUBLISHED_COURT_LIST, requestParameters.getCourtListId(), payload);
    }

    public void requestExportCourtList(final PublishCourtListRequestParameters requestParameters, final JsonObject courtListJson) {

        final JsonObject payload = createObjectBuilder()
                .add(COURT_CENTRE_ID, requestParameters.getCourtCentreId().toString())
                .add(START_DATE, requestParameters.getStartDate().toString())
                .add(PUBLISH_COURT_LIST_TYPE, requestParameters.getPublishCourtListType().name())
                .add(COURT_LIST_JSON, courtListJson.toString())
                .build();

        sendCommandWith(COURT_LIST_REQUEST_EXPORT, requestParameters.getCourtListId(), payload);
    }

    private void sendCommandWith(final String commandName, final UUID streamId, final JsonObject payload) {

        sender.sendAsAdmin(envelopeFrom(
                metadataBuilder()
                        .withStreamId(streamId)
                        .createdAt(utcClock.now())
                        .withName(commandName)
                        .withId(randomUUID())
                        .build(),
                payload));
    }

}
