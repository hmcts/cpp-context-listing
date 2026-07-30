package uk.gov.moj.cpp.listing.event.processor.xhibit;

import static java.util.stream.Collectors.joining;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.XhibitService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListFileGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.apache.commons.lang3.exception.ExceptionUtils;
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
    private XhibitService xhibitService;

    @SuppressWarnings("squid:S2221")
    // Allow any exception to be handled by recording it as a failed export
    public void exportCourtList(final JsonEnvelope envelope, final PublishCourtListRequestParameters parameters, final JsonObject courtListJson) {
        try {

            final CourtListMetadata courtListMetadata = courtListMetadataGenerator.generate(parameters);

            final String courtListXml = courtListFileGenerator.generateXml(envelope, parameters, courtListMetadata, courtListJson);

            courtListFileGenerator.validateXml(parameters, courtListXml);

            publishCourtListCommandSender.publishPublicMessageWithDailyList(envelope, parameters, courtListXml);

            try (final InputStream courtListXmlInputStream = new ByteArrayInputStream(courtListXml.getBytes())) {
                xhibitService.sendToXhibit(courtListXmlInputStream, courtListMetadata.getFilename());
            }
            publishCourtListCommandSender.recordCourtListExportSuccessful(parameters, courtListMetadata.getFilename());

        } catch (final Exception e) {
            logger.error("Court List export failed", e);
            publishCourtListCommandSender.recordCourtListExportFailed(parameters,
                    exportFailureDetail(e), "NONE");
        }
    }

    /**
     * The recorded errorMessage is the only diagnostic that escapes this service on CI, where the
     * Wildfly server.log is not reachable from the build agent. {@code getMessage(e)} alone reports
     * only the outermost wrapper - typically an EJBException, which hides the real fault - so record
     * the whole causal chain plus the frame the root cause was thrown from.
     */
    private String exportFailureDetail(final Exception e) {
        final List<Throwable> causalChain = ExceptionUtils.getThrowableList(e);

        final String messages = causalChain.stream()
                .map(ExceptionUtils::getMessage)
                .collect(joining(" | caused by: "));

        final StackTraceElement[] rootCauseTrace = causalChain.get(causalChain.size() - 1).getStackTrace();

        return rootCauseTrace.length == 0 ? messages : messages + " | at " + rootCauseTrace[0];
    }
}
