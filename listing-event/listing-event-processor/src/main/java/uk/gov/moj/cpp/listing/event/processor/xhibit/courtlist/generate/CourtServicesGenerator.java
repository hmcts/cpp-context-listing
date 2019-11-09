package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate;

import uk.gov.moj.cpp.listing.domain.xhibit.CourtLocation;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtHouseStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.CourtType;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.DocumentIDstructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ListHeaderStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ObjectFactory;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;
import uk.gov.moj.cpp.listing.event.processor.xhibit.exception.GenerationFailedException;

import java.util.UUID;

import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.google.common.annotations.VisibleForTesting;

public class CourtServicesGenerator {

    private static final ObjectFactory objectFactory = new ObjectFactory();
    @Inject
    private XhibitReferenceDataService xhibitReferenceDataService;

    public DocumentIDstructure generateDocumentID(final CourtListGenerationContext context) {

        final DocumentIDstructure documentIDstructure = objectFactory.createDocumentIDstructure();

        documentIDstructure.setDocumentName(context.getMetadata().getFilename());
        documentIDstructure.setDocumentType(context.getParameters().getPublishCourtListType().getDocumentType());
        documentIDstructure.setUniqueID(context.getMetadata().getDocumentUniqueId());

        return documentIDstructure;
    }

    public ListHeaderStructure generateListHeader(final CourtListGenerationContext context) {

        final ListHeaderStructure listHeaderStructure = objectFactory.createListHeaderStructure();

        listHeaderStructure.setListCategory("Criminal");
        listHeaderStructure.setStartDate(context.getParameters().getStartDate());
        listHeaderStructure.setEndDate(context.getParameters().getEndDate());
        listHeaderStructure.setVersion("NOT VERSIONED");
        final String publishedDate = context.getMetadata().getCreatedDate().toInstant().toString();
        try {
            listHeaderStructure.setPublishedTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(publishedDate));
        } catch (final DatatypeConfigurationException e) {
            throw new GenerationFailedException("Cannot convert date: " + publishedDate, e);
        }

        return listHeaderStructure;
    }

    public CourtHouseStructure generateCourtHouseStructure(final CourtListGenerationContext context, final UUID courtCentreId) {

        final CourtLocation courtLocation = xhibitReferenceDataService.getCourtDetails(context.getEnvelope(), courtCentreId);

        final CourtHouseStructure courtHouseStructure = objectFactory.createCourtHouseStructure();

        courtHouseStructure.setCourtHouseType(CourtType.CROWN_COURT);
        courtHouseStructure.setCourtHouseCode(generateCourtHouseCode(courtLocation));
        courtHouseStructure.setCourtHouseName(courtLocation.getCourtFullName());

        return courtHouseStructure;
    }

    private CourtHouseStructure.CourtHouseCode generateCourtHouseCode(final CourtLocation courtLocation) {

        final CourtHouseStructure.CourtHouseCode courtHouseCode = objectFactory.createCourtHouseStructureCourtHouseCode();
        courtHouseCode.setValue(courtLocation.getCrestCourtSiteId());
        courtHouseCode.setCourtHouseShortName(courtLocation.getCourtShortName());

        return courtHouseCode;
    }

    @VisibleForTesting
    public void setXhibitReferenceDataService(final XhibitReferenceDataService xhibitReferenceDataService) {
        this.xhibitReferenceDataService = xhibitReferenceDataService;
    }

}
