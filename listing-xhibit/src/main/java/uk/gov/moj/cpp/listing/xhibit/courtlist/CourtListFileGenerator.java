package uk.gov.moj.cpp.listing.xhibit.courtlist;

import static org.apache.commons.collections.ListUtils.union;

import uk.gov.moj.cpp.listing.common.xhibit.CommonXhibitReferenceDataService;
import uk.gov.moj.cpp.listing.xhibit.courtlist.mapper.AbstractCourtListMapper;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.springframework.stereotype.Component;

/**
 * Renders a court list as XHIBIT XML (port of the JEE processor's {@code CourtListFileGenerator},
 * unchanged logic): the requesting court centre's court lists plus the <em>published</em> court
 * lists of every other CPP court centre that shares its CREST court id, through the mapper for the
 * list type; then validation against the list type's XSD.
 */
@Component
public class CourtListFileGenerator {

    private static final String XHIBIT_XSD_PATH = "xhibit/xsd/";

    private final XmlUtils xmlUtils;
    private final PublishedCourtListReader publishedCourtListReader;
    private final MapperFactory mapperFactory;
    private final CommonXhibitReferenceDataService commonXhibitReferenceDataService;

    public CourtListFileGenerator(final XmlUtils xmlUtils, final PublishedCourtListReader publishedCourtListReader,
                                  final MapperFactory mapperFactory, final CommonXhibitReferenceDataService commonXhibitReferenceDataService) {
        this.xmlUtils = xmlUtils;
        this.publishedCourtListReader = publishedCourtListReader;
        this.mapperFactory = mapperFactory;
        this.commonXhibitReferenceDataService = commonXhibitReferenceDataService;
    }

    @SuppressWarnings("unchecked")
    public String generateXml(final PublishCourtListRequestParameters requestParameters,
                              final CourtListMetadata courtListMetadata,
                              final JsonObject courtListJson) {

        final CourtListGenerationContext context = new CourtListGenerationContext(requestParameters, courtListMetadata);

        final String crownCourtCrestId = commonXhibitReferenceDataService.getCrownCourtDetails(requestParameters.getCourtCentreId()).getCrestCourtId();

        final List<UUID> courtCentreIds = commonXhibitReferenceDataService.getCrownCourtCentreIdsForCrestId(crownCourtCrestId);

        final List<JsonObject> publishedCourtListsJson = courtCentreIds.stream()
                .filter(courtCentreId -> !courtCentreId.equals(requestParameters.getCourtCentreId()))
                .map(courtCentreId -> getCourtLists(requestParameters, courtCentreId))
                .flatMap(Collection::stream)
                .collect(Collectors.toUnmodifiableList());

        final List<JsonObject> courtCentreCourtLists = courtListJson.getJsonArray("courtLists").getValuesAs(JsonObject.class);

        final AbstractCourtListMapper mapper = mapperFactory.createCourtListMapper(context, union(courtCentreCourtLists, publishedCourtListsJson));

        return xmlUtils.convertToXml(mapper.generate());
    }

    private List<JsonObject> getCourtLists(final PublishCourtListRequestParameters requestParameters, final UUID courtCentreId) {

        final JsonObject publishedCourtListForCourtCentre = publishedCourtListReader.getPublishedCourtListForCourtCentre(
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
