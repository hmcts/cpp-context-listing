package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.EVENT_PAYLOAD_DEBUG_STRING;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.ExportFailedException;
import uk.gov.moj.cpp.listing.common.xhibit.XhibitService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListXmlGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersParser;

import java.io.ByteArrayInputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:S1192")
public class CourtListEventProcessor {

    private static final String PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED = "listing.event.publish-court-list-produced";

    @Inject
    private XhibitService xhibitService;

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private PublishCourtListCommandSender publishCourtListCommandSender;

    @Inject
    private PublishCourtListRequestParametersParser publishCourtListRequestParametersParser;

    @Inject
    private CourtListMetadataGenerator courtListMetadataGenerator;

    @Inject
    private CourtListXmlGenerator courtListXmlGenerator;

    @Inject
    private FileServiceClient fileServiceClient;

    @Handles("listing.event.publish-court-list-requested")
    @SuppressWarnings("squid:S2221")
    // Allow any exception to be handled by recording it as a failed export
    public void handlePublishCourtListRequested(final JsonEnvelope envelope) {

        try {
            final PublishCourtListRequestParameters parameters = publishCourtListRequestParametersParser.parse(envelope);

            final CourtListMetadata courtListMetadata = courtListMetadataGenerator.generate(parameters);

            final ByteArrayInputStream courtListXml = courtListXmlGenerator.generateCourtListInputStream(envelope, parameters, courtListMetadata);

            final UUID fileId = fileServiceClient.store(courtListMetadata, courtListXml);

            publishCourtListCommandSender.recordCourtListPublished(fileId, courtListMetadata.getFilename());
        } catch (final Exception e) {
            logger.error("Court List generation failed", e);
            publishCourtListCommandSender.recordCourtListExportFailed(randomUUID(), "NONE", e.getMessage());
        }
    }

    @SuppressWarnings("squid:S1166")
    @Handles(PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED)
    public void handleProducedCourtList(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED, envelope.toObfuscatedDebugString());
        }
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID documentId = UUID.fromString(payload.getString("documentId"));
        final String documentName = payload.getString("documentName");
        try {
            xhibitService.sendToXhibit(documentId, documentName);
            publishCourtListCommandSender.recordCourtListExportSuccessful(documentId, documentName);
        } catch (final ExportFailedException e) {
            logger.error(format("Export failed for %s %s %s", documentId, documentName, e.getMessage()));
            publishCourtListCommandSender.recordCourtListExportFailed(documentId, documentName, e.getMessage());
        }

    }
}
