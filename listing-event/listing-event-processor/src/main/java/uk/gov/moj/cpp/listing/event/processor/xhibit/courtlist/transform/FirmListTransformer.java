package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.transform;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ObjectFactory;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

public class FirmListTransformer {

    private static final ObjectFactory objectFactory = new ObjectFactory();
    private CourtServicesTransformer courtServicesTransformer;

    public void setCourtServicesTransformer(final CourtServicesTransformer courtServicesTransformer) {
        this.courtServicesTransformer = courtServicesTransformer;
    }

    public FirmListStructure transform(final CourtListGenerationContext context) {

        final FirmListStructure firmListStructure = objectFactory.createFirmListStructure();

        firmListStructure.setDocumentID(courtServicesTransformer.transformDocumentID(context));

        objectFactory.createFirmList(firmListStructure);

        return firmListStructure;
    }
}
