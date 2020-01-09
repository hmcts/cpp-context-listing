package uk.gov.moj.cpp.listing.event.processor.xhibit;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.XhibitService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListFileGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;

import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

public class CourtListExportService {

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private PublishCourtListCommandSender publishCourtListCommandSender;

    @Inject
    private CourtListMetadataGenerator courtListMetadataGenerator;

    @Inject
    private CourtListFileGenerator courtListFileGenerator;

    @Inject
    private FileServiceClient fileServiceClient;

    @Inject
    private XhibitService xhibitService;

    @SuppressWarnings("squid:S2221")
    // Allow any exception to be handled by recording it as a failed export
    public void exportCourtList(final JsonEnvelope envelope, final PublishCourtListRequestParameters parameters) {
        try {

            final CourtListMetadata courtListMetadata = courtListMetadataGenerator.generate(envelope, parameters);

            final String courtListXml = courtListFileGenerator.generateXml(envelope, parameters, courtListMetadata);

            courtListFileGenerator.validateXml(parameters, courtListXml);

            final UUID fileId = fileServiceClient.store(courtListMetadata, courtListXml);

            xhibitService.sendToXhibit(fileId, courtListMetadata.getFilename());
            publishCourtListCommandSender.recordCourtListExportSuccessful(parameters);

        } catch (final Exception e) {
            logger.error("Court List export failed", e);
            publishCourtListCommandSender.recordCourtListExportFailed(parameters,
                    e.getMessage());
        }
    }
}
