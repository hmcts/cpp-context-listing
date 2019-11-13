package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate.CourtServicesGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate.FirmListGenerator;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;

public class CourtListFileGenerator {

    private static final String XHIBIT_XSD_PATH = "xhibit/xsd/";

    @Inject
    private XmlUtils xmlUtils;

    @Inject
    private ListingService listingService;

    @Inject
    private XhibitReferenceDataService xhibitReferenceDataService;

    public String generateXml(final JsonEnvelope envelope,
                              final PublishCourtListRequestParameters requestParameters,
                              final CourtListMetadata courtListMetadata) {

        final CourtListGenerationContext context =
                new CourtListGenerationContext(envelope, requestParameters, courtListMetadata);

        final CourtServicesGenerator courtServicesGenerator = new CourtServicesGenerator(context, xhibitReferenceDataService);

        final FirmListGenerator generator = new FirmListGenerator(context, listingService, courtServicesGenerator);

        final JAXBElement<?> documentRoot = generator.generate();

        return xmlUtils.convertToXml(documentRoot);
    }

    public void validateXml(final PublishCourtListRequestParameters requestParameters, final String courtListXml) {

        final String schemaFile = XHIBIT_XSD_PATH + requestParameters.getPublishCourtListType().getSchemaName();

        xmlUtils.validate(courtListXml, schemaFile);
    }
}
