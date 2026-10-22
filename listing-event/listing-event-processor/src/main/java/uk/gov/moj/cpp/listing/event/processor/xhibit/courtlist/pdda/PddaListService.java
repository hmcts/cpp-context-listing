package uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.pdda;

import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListFileGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadata;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.CourtListMetadataGenerator;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.ListingService;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.PublishCourtListRequestParameters;
import uk.gov.moj.cpp.listing.event.processor.xhibit.courtlist.XmlUtils;

import java.io.StringReader;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.bind.JAXBElement;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Builds a court list in the JSON shape of the HMCTS PDDA schemas. It runs the same generation as the
 * Xhibit publish flow (same query, same mappers) and converts the resulting document directly, without
 * writing it out as XML, so the two outputs cannot drift apart.
 */
public class PddaListService {

    @Inject
    private ListingService listingService;

    @Inject
    private CourtListMetadataGenerator courtListMetadataGenerator;

    @Inject
    private CourtListFileGenerator courtListFileGenerator;

    @Inject
    private XmlUtils xmlUtils;

    @Inject
    private PddaListJsonConverter pddaListJsonConverter;

    public JsonObject generate(final JsonEnvelope envelope, final PublishCourtListRequestParameters parameters) {

        final JsonObject courtListJson = listingService.getUnpublishedCourtListForCourtCentre(envelope, parameters);

        final CourtListMetadata metadata = courtListMetadataGenerator.generate(parameters);

        final JAXBElement<?> courtListDocument = courtListFileGenerator.generateDocument(envelope, parameters, metadata, courtListJson);

        final JsonNode pddaJson = pddaListJsonConverter.convert(xmlUtils.convertToDom(courtListDocument), parameters.getPublishCourtListType());

        try (final JsonReader reader = createReader(new StringReader(pddaJson.toString()))) {
            return reader.readObject();
        }
    }
}
