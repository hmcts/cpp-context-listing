package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.lang.String.format;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.listing.event.processor.ListingEventProcessor.EVENT_PAYLOAD_DEBUG_STRING;

import uk.gov.justice.listing.event.PublishCourtListProduced;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.XhibitService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListFileGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParametersParser;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@ServiceComponent(EVENT_PROCESSOR)
@SuppressWarnings("squid:S1192")
public class CourtListEventProcessor {

    private static final String PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED = "listing.event.publish-court-list-produced";

    @Inject
    private XhibitService xhibitService;

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
    private CourtListMetadataGenerator courtListMetadataGenerator;

    @Inject
    private CourtListFileGenerator courtListFileGenerator;

    @Inject
    private FileServiceClient fileServiceClient;

    @Handles("listing.event.publish-court-list-requested")
    @SuppressWarnings("squid:S2221")
    // Allow any exception to be handled by recording it as a failed export
    public void handlePublishCourtListRequested(final JsonEnvelope envelope) {

        final PublishCourtListRequestParameters parameters = publishCourtListRequestParametersParser.parse(envelope);

        try {

            logger.info("handlePublishCourtListRequested: parameters={}", parameters);

            final CourtListMetadata courtListMetadata = courtListMetadataGenerator.generate(envelope, parameters);

            final String courtListXml = courtListFileGenerator.generateXml(envelope, parameters, courtListMetadata);

            courtListFileGenerator.validateXml(parameters, courtListXml);

            final UUID fileId = fileServiceClient.store(courtListMetadata, courtListXml);

            publishCourtListCommandSender.recordCourtListProduced(parameters, fileId, courtListMetadata.getFilename());
        } catch (final Exception e) {
            logger.error("Court List generation failed", e);
            publishCourtListCommandSender.recordCourtListExportFailed(parameters.getPublishCourtListRequestId(),
                    e.getMessage());
        }
    }

    @SuppressWarnings({"squid:S1166","squid:S2221"})
    // Allow any exception to be handled by recording it as a failed export
    @Handles(PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED)
    public void handleProducedCourtList(final JsonEnvelope envelope) {
        if (logger.isInfoEnabled()) {
            logger.info(EVENT_PAYLOAD_DEBUG_STRING, PRIVATE_EVENT_PUBLISH_COURT_LIST_PRODUCED, envelope.toObfuscatedDebugString());
        }

        final PublishCourtListProduced event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), PublishCourtListProduced.class);

        final UUID fileId = event.getCourtListFileId();
        final String fileName = event.getCourtListFileName();
        try {
            xhibitService.sendToXhibit(event.getCourtListFileId(), event.getCourtListFileName());
            publishCourtListCommandSender.recordCourtListExportSuccessful(event.getPublishCourtListRequestId());
        } catch (final Exception e) {
            logger.error(format("Export failed for %s %s %s", fileId, fileName, e.getMessage()), e);
            publishCourtListCommandSender.recordCourtListExportFailed(event.getPublishCourtListRequestId(),
                    e.getMessage());
        }
    }
}
