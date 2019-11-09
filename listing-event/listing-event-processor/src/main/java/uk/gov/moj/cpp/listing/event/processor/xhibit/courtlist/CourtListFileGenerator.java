package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate.FirmListGenerator;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;

import com.google.common.annotations.VisibleForTesting;

public class CourtListFileGenerator {

    public static final String XHIBIT_XSD_PATH = "xhibit/xsd/";
    @Inject
    private FirmListGenerator firmListGenerator;

    @Inject
    private XmlUtils xmlUtils;

    @SuppressWarnings({"squid:S1172"})
    public ByteArrayInputStream generateCourtListInputStream(final JsonEnvelope envelope,
                                                             final PublishCourtListRequestParameters requestParameters,
                                                             final CourtListMetadata courtListMetadata) {

        final CourtListGenerationContext courtListGenerationContext =
                new CourtListGenerationContext(envelope, requestParameters, courtListMetadata);

        final String courtListXml = getXmlString(courtListGenerationContext);

        return new ByteArrayInputStream(courtListXml.getBytes());
    }

    @VisibleForTesting
    public String getXmlString(final CourtListGenerationContext context) {

        final JAXBElement<?> documentRoot = firmListGenerator.generate(context);

        final String schemaFile = XHIBIT_XSD_PATH + context.getParameters().getPublishCourtListType().getSchemaName();

        xmlUtils.validate(documentRoot, schemaFile);

        return xmlUtils.convertToXml(documentRoot);
    }
}
