package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.listing.event.CourtListExportRequested;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.ListingService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersParser;

import java.io.StringReader;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:S1192")
public class CourtListEventProcessor {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private PublishCourtListCommandSender publishCourtListCommandSender;

    @Inject
    private PublishCourtListRequestParametersParser publishCourtListRequestParametersParser;

    @Inject
    private ListingService listingService;

    @Inject
    private CourtListExportService courtListExportService;

    @Handles("listing.event.publish-court-list-requested")
    public void handlePublishCourtListRequested(final JsonEnvelope envelope) {

        final PublishCourtListRequestParameters parameters = publishCourtListRequestParametersParser.parse(envelope);

        logger.info("handlePublishCourtListRequested: parameters={}", parameters);

        final JsonObject courtListJson = listingService.getUnpublishedCourtListForCourtCentre(envelope, parameters);

        publishCourtListCommandSender.storePublishedCourtList(parameters, courtListJson);

        publishCourtListCommandSender.requestExportCourtList(parameters, courtListJson, envelope);
    }

    @Handles("listing.event.court-list-export-requested")
    public void handleCourtListExportRequested(final JsonEnvelope envelope) {

        final CourtListExportRequested courtListExportRequested =
                jsonObjectConverter.convert(envelope.payloadAsJsonObject(), CourtListExportRequested.class);

        logger.info("courtListExportRequested: {}", courtListExportRequested);

        final uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType publishCourtListType =
                uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.valueOf(courtListExportRequested.getPublishCourtListType().name());

        final UUID courtListId = envelope.metadata().streamId().orElseThrow(RuntimeException::new);

        final PublishCourtListRequestParameters parameters = new PublishCourtListRequestParameters(
                courtListId,
                courtListExportRequested.getCourtCentreId(),
                courtListExportRequested.getStartDate(),
                calculateEndDate(publishCourtListType, courtListExportRequested),
                publishCourtListType,
                courtListExportRequested.getRequestedTime(),
                courtListExportRequested.getSendNotificationToParties());

        final JsonObject courtListJson = courtCentreCourtListJson(envelope);
        courtListExportService.exportCourtList(envelope, parameters, courtListJson);
        publishCourtListCommandSender.publishPublicMessageForCourtList(envelope, parameters, courtListJson);
    }

    private LocalDate calculateEndDate(final uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType publishCourtListType, final CourtListExportRequested courtListExportRequested) {
        if (publishCourtListType.isWeekCommencing() && courtListExportRequested.getEndDate() != null) {
            return courtListExportRequested.getEndDate();
        }
        final LocalDate startDate = courtListExportRequested.getStartDate();
        if (!startDate.getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            return publishCourtListType.isWeekCommencing() ? startDate.plusDays(5) : startDate;
        } else {
            return publishCourtListType.isWeekCommencing() ? startDate.plusDays(6) : startDate;
        }
    }

    private JsonObject courtCentreCourtListJson(final JsonEnvelope envelope) {

        final String courtListJsonString = envelope.payloadAsJsonObject().getString("courtListJson");

        try (final JsonReader jsonReader = createReader(new StringReader(courtListJsonString))) {
            return jsonReader.readObject();
        }
    }
}
