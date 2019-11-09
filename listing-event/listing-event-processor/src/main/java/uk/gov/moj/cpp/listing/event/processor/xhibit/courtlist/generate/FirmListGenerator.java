package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ObjectFactory;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;

import javax.inject.Inject;
import javax.xml.bind.JAXBElement;

import com.google.common.annotations.VisibleForTesting;

public class FirmListGenerator {

    @Inject
    private CourtServicesGenerator courtServicesGenerator;

    private static final ObjectFactory objectFactory = new ObjectFactory();

    public JAXBElement<FirmListStructure> generate(final CourtListGenerationContext context) {

        return objectFactory.createFirmList(transform(context));
    }

    public FirmListStructure transform(final CourtListGenerationContext context) {

        final FirmListStructure firmListStructure = objectFactory.createFirmListStructure();

        firmListStructure.setDocumentID(courtServicesGenerator.generateDocumentID(context));
        firmListStructure.setListHeader(courtServicesGenerator.generateListHeader(context));
        firmListStructure.setCrownCourt(courtServicesGenerator.generateCourtHouseStructure(context, context.getParameters().getCourtCentreId()));
        firmListStructure.setCourtLists(generateCourtLists(context));

        objectFactory.createFirmList(firmListStructure);

        return firmListStructure;
    }

    @VisibleForTesting
    public void setCourtServicesGenerator(final CourtServicesGenerator courtServicesGenerator) {
        this.courtServicesGenerator = courtServicesGenerator;
    }

    private FirmListStructure.CourtLists generateCourtLists(final CourtListGenerationContext context) {

        final FirmListStructure.CourtLists courtLists = objectFactory.createFirmListStructureCourtLists();

        courtLists.getCourtList().add(generateFirmCourtListStructure(context));

        return courtLists;
    }

    private FirmCourtListStructure generateFirmCourtListStructure(final CourtListGenerationContext context) {

        final FirmCourtListStructure firmCourtListStructure = objectFactory.createFirmCourtListStructure();

        firmCourtListStructure.setCourtHouse(courtServicesGenerator.generateCourtHouseStructure(context,
                context.getParameters().getCourtCentreId()));

        firmCourtListStructure.setSittings(generateSittings(context));
        firmCourtListStructure.setSittingDate(context.getParameters().getStartDate());

        return firmCourtListStructure;

    }

    @SuppressWarnings({"squid:S1172"})  // TODO use context to generate sittings
    private FirmCourtListStructure.Sittings generateSittings(final CourtListGenerationContext context) {

        final FirmCourtListStructure.Sittings sittings = objectFactory.createFirmCourtListStructureSittings();

        // TODO Generate sittings


        return sittings;
    }
}
