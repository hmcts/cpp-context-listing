package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.generate;

import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmCourtListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.FirmListStructure;
import uk.gov.moj.cpp.listing.domain.xhibit.generated.ObjectFactory;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListGenerationContext;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.ListingService;

import java.util.List;

import javax.json.JsonObject;
import javax.xml.bind.JAXBElement;

import com.google.common.annotations.VisibleForTesting;

public class FirmListGenerator {

    private static final ObjectFactory objectFactory = new ObjectFactory();

    private ListingService listingService;

    private CourtServicesGenerator courtServicesGenerator;

    private CourtListGenerationContext context;

    public FirmListGenerator(final CourtListGenerationContext context, final ListingService listingService,
                             final CourtServicesGenerator courtServicesGenerator) {
        this.context = context;
        this.listingService = listingService;
        this.courtServicesGenerator = courtServicesGenerator;
    }

    public JAXBElement<FirmListStructure> generate() {

        return objectFactory.createFirmList(transform());
    }

    public FirmListStructure transform() {

        final FirmListStructure firmListStructure = objectFactory.createFirmListStructure();

        firmListStructure.setDocumentID(courtServicesGenerator.generateDocumentID());
        firmListStructure.setListHeader(courtServicesGenerator.generateListHeader());
        firmListStructure.setCrownCourt(courtServicesGenerator.generateCourtHouseStructure(context.getParameters().getCourtCentreId()));
        firmListStructure.setCourtLists(generateCourtLists());

        objectFactory.createFirmList(firmListStructure);

        return firmListStructure;
    }

    @VisibleForTesting
    public void setCourtServicesGenerator(final CourtServicesGenerator courtServicesGenerator) {
        this.courtServicesGenerator = courtServicesGenerator;
    }

    @VisibleForTesting
    public void setListingService(final ListingService listingService) {
        this.listingService = listingService;
    }

    private FirmListStructure.CourtLists generateCourtLists() {

        final FirmListStructure.CourtLists courtLists = objectFactory.createFirmListStructureCourtLists();

        courtLists.getCourtList().add(generateFirmCourtListStructure());

        return courtLists;
    }

    private FirmCourtListStructure generateFirmCourtListStructure() {

        final FirmCourtListStructure firmCourtListStructure = objectFactory.createFirmCourtListStructure();

        firmCourtListStructure.setCourtHouse(courtServicesGenerator.generateCourtHouseStructure(
                context.getParameters().getCourtCentreId()));

        firmCourtListStructure.setSittings(generateSittings());
        firmCourtListStructure.setSittingDate(context.getParameters().getStartDate());

        return firmCourtListStructure;

    }

    private FirmCourtListStructure.Sittings generateSittings() {

        final FirmCourtListStructure.Sittings sittings = objectFactory.createFirmCourtListStructureSittings();

        final List<JsonObject> hearings = listingService.getHearingsForPublishing(context.getEnvelope(), context.getParameters());

        int sittingSequenceNumber = 1;
        for (final JsonObject hearing : hearings) {
            sittings.getSitting().add(courtServicesGenerator.generateSittingStructure(hearing, sittingSequenceNumber++));
        }

        return sittings;
    }

}
