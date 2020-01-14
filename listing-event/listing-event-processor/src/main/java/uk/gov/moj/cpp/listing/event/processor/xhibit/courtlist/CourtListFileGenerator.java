package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.xhibit.XhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.AbstractCourtListMapper;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CourtListFileGenerator {

    private static final String XHIBIT_XSD_PATH = "xhibit/xsd/";

    @Inject
    private XmlUtils xmlUtils;

    @Inject
    private ListingService listingService;

    @Inject
    private MapperFactory mapperFactory;

    @Inject
    private XhibitReferenceDataService xhibitReferenceDataService;

    public String generateXml(final JsonEnvelope envelope,
                              final PublishCourtListRequestParameters requestParameters,
                              final CourtListMetadata courtListMetadata) {

        final CourtListGenerationContext context =
                new CourtListGenerationContext(envelope, requestParameters, courtListMetadata);

        final String crownCourtCrestId = xhibitReferenceDataService.getCourtDetails(envelope, requestParameters.getCourtCentreId()).getCrestCourtId();

        final List<UUID> courtCentreIds = xhibitReferenceDataService.getCourtCentreIdsForCrestId(envelope, crownCourtCrestId);

        final List<JsonObject> courtListsJson = courtCentreIds.stream()
                .map(courtCentreId -> listingService.getPublishedCourtListForCourtCentre(
                        envelope,
                        courtCentreId,
                        requestParameters.getPublishCourtListType(),
                        requestParameters.getStartDate()).getJsonObject("courtList"))
                .collect(toList());

        final AbstractCourtListMapper mapper = mapperFactory.createCourtListMapper(context, courtListsJson);

        return xmlUtils.convertToXml(mapper.generate());
    }

    public void validateXml(final PublishCourtListRequestParameters requestParameters, final String courtListXml) {

        final String schemaFile = XHIBIT_XSD_PATH + requestParameters.getPublishCourtListType().getSchemaName();

        xmlUtils.validate(courtListXml, schemaFile);
    }
}
