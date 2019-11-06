package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.transform;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.DocumentIDstructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ObjectFactory;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

public class CourtServicesTransformer {

    private static final ObjectFactory objectFactory = new ObjectFactory();

    public DocumentIDstructure transformDocumentID(final CourtListGenerationContext context) {

        final DocumentIDstructure documentIDstructure = objectFactory.createDocumentIDstructure();

        documentIDstructure.setDocumentName(context.getMetadata().getFilename());
        documentIDstructure.setDocumentType("FL");  // TODO vary according to XhibitCourtListType
        documentIDstructure.setUniqueID(context.getMetadata().getDocumentUniqueId());

        return documentIDstructure;
    }
}
