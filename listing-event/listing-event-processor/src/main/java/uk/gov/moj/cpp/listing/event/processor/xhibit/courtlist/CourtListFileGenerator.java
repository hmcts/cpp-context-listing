package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist;

import static org.apache.commons.collections.ListUtils.union;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.mapper.AbstractCourtListMapper;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
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
    private CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    public String generateXml(final JsonEnvelope envelope,
                              final PublishCourtListRequestParameters requestParameters,
                              final CourtListMetadata courtListMetadata,
                              final JsonObject courtListJson) {

        final CourtListGenerationContext context =
                new CourtListGenerationContext(envelope, requestParameters, courtListMetadata);

        final String crownCourtCrestId = commonXhibitReferenceDataService.getCrownCourtDetails(requestParameters.getCourtCentreId()).getCrestCourtId();

        final List<UUID> courtCentreIds = commonXhibitReferenceDataService.getCrownCourtCentreIdsForCrestId(crownCourtCrestId);

        // Use thread-safe local variables
        final List<JsonObject> publishedCourtListsJson = courtCentreIds.stream()
                .filter(courtCentreId -> !courtCentreId.equals(requestParameters.getCourtCentreId()))
                .map(courtCentreId ->
                        getCourtLists(envelope, requestParameters, courtCentreId)
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList()); // Immutable list

        final List<JsonObject> courtCentreCourtLists = courtListJson.getJsonArray("courtLists").getValuesAs(JsonObject.class);

        // Ensure mapperFactory and xmlUtils are thread-safe
        final AbstractCourtListMapper mapper = mapperFactory.createCourtListMapper(context, union(courtCentreCourtLists, publishedCourtListsJson));

        return xmlUtils.convertToXml(mapper.generate());
    }

    private List<JsonObject> getCourtLists(final JsonEnvelope envelope, final PublishCourtListRequestParameters requestParameters, final UUID courtCentreId) {

        final JsonObject publishedCourtListForCourtCentre = listingService.getPublishedCourtListForCourtCentre(
                envelope,
                courtCentreId,
                requestParameters.getPublishCourtListType(),
                requestParameters.getStartDate());

        final JsonArray courtLists = publishedCourtListForCourtCentre.getJsonArray("courtLists");

        return courtLists.getValuesAs(JsonObject.class);
    }

    public void validateXml(final PublishCourtListRequestParameters requestParameters, final String courtListXml) {

        final String schemaFile = XHIBIT_XSD_PATH + requestParameters.getPublishCourtListType().getSchemaName();

        xmlUtils.validate(courtListXml, schemaFile);
    }
}
