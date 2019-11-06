package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.transform.FirmListTransformer;
import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.GenerationFailedException;

import java.io.ByteArrayInputStream;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

public class CourtListXmlGenerator {

    @Inject
    private FirmListTransformer firmListTransformer;

    @Inject
    private XmlConverter xmlConverter;

    @SuppressWarnings({"squid:S1172"})
    public ByteArrayInputStream generateCourtListInputStream(final JsonEnvelope envelope,
                                                    final PublishCourtListRequestParameters requestParameters,
                                                    final CourtListMetadata courtListMetadata) throws GenerationFailedException {

        final CourtListGenerationContext courtListGenerationContext = new CourtListGenerationContext(requestParameters, courtListMetadata);

        final String courtListXml = getXmlString(courtListGenerationContext);

        return new ByteArrayInputStream(courtListXml.getBytes());
    }

    @SuppressWarnings({"squid:S1166", "squid:S1162"})
    // Allow the usage of the checked exception 'GenerationFailedException'.
    public String getXmlString(final CourtListGenerationContext context) throws GenerationFailedException {

        final FirmListStructure firmListStructure = firmListTransformer.transform(context);

        try {
            return xmlConverter.convertToXml(firmListStructure);
        } catch (final JAXBException e) {
            throw new GenerationFailedException("Court list XML generation for XHIBIT failed", e);
        }
    }
}
