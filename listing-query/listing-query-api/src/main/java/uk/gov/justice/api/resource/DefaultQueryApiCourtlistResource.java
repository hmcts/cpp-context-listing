package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ALPHABETICAL;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.STANDARD;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.utils.NullAwareJsonObjectBuilder;
import uk.gov.moj.cpp.listing.query.api.service.AlphabeticalCourtListService;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.document.generator.DocumentGeneratorClient;
import uk.gov.moj.cpp.listing.query.document.generator.StandardPublicCourtListTemplateAssembler;
import uk.gov.moj.cpp.systemusers.ServiceContextSystemUserProvider;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * http endpoint adapter which overrides default framework adapter. It handles transfer of files
 * binaries between docmosis and listing context. Class invoke standard interceptor chain.
 * At the end of interceptor chain, regular query handler is invoked and returns documents details
 * 
 */

@Stateless
@Adapter(Component.QUERY_API)
public class DefaultQueryApiCourtlistResource implements QueryApiCourtList {

    protected static final String COURT_LIST_QUERY_NAME = "listing.search.court.list";
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultQueryApiCourtlistResource.class);
    private static final String EXTRACT_FILE_NAME = "CourtList.pdf";
    protected static final String DISPOSITION = "attachment; filename=\"" + EXTRACT_FILE_NAME + "\"";

    @Inject
    private ServiceContextSystemUserProvider serviceContextSystemUserProvider;

    @Inject
    private InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private DocumentGeneratorClient documentGeneratorClient;

    @Inject
    private AlphabeticalCourtListService alpbhabeticalCourtListService;

    @Inject
    private StandardPublicCourtListTemplateAssembler standardPublicCourtListAssembler;

    @Inject
    private ReferenceDataService referenceDataService;

    @Override
    public Response getCourtList(final String courtCentreId, final String courtRoomId, final String listId,
                                 final String startDate, final String endDate, final boolean restricted, UUID userId) {
        final Optional<CourtListType> courtListType = CourtListType.valueFor(listId);
        if (courtListType.isPresent()) {
            final JsonObjectBuilder builder =
                    NullAwareJsonObjectBuilder.wrap(Json.createObjectBuilder());
            final JsonEnvelope documentQuery = envelopeFrom(
                    metadataBuilder()
                            .withId(randomUUID())
                            .withName(COURT_LIST_QUERY_NAME)
                            .withUserId(userId.toString())
                            .build(),

                    builder
                            .add("courtCentreId", courtCentreId)
                            .add("listId", listId)
                            .add("courtRoomId", courtRoomId)
                            .add("startDate", startDate)
                            .add("endDate", endDate)
                            .build());


            return interceptorChainProcessor.process(interceptorContextWithInput(documentQuery))
                    .map(queryResponse -> getDocumentContent(queryResponse, courtCentreId, courtRoomId, courtListType.get(), restricted))
                    .orElse(status(NOT_FOUND).build());
        }
        return Response.status(BAD_REQUEST).entity(String.format("Bad request - No matching list type found for %s", listId)).build();
    }

    private Response getDocumentContent(final JsonEnvelope queryResponse, final String courtCentreId, final String courtRoomId, final CourtListType courtListType, final boolean restricted) {
        final String pdfMimeType = "application/pdf";
        if (!JsonValue.NULL.equals(queryResponse.payload())) {
            final Optional<JsonObject> courtListData = buildCourtListData(queryResponse, courtCentreId, courtRoomId , courtListType, restricted);
            if (courtListData.isPresent()) {
                final JsonObject courtListPayload = courtListData.get();
                LOGGER.info("getDocumentContent() :: courtListPayload {} ", courtListPayload);
                final boolean isWelsh = referenceDataService.isHearingLanguageWelsh(queryResponse, courtCentreId).orElse(false);
                final byte[] binaryDocument = documentGeneratorClient.generateDocument(
                        courtListPayload, getTemplateName(courtListType, isWelsh));
                final Response.ResponseBuilder responseBuilder = status(OK).entity(new ByteArrayInputStream(binaryDocument));
                return responseBuilder.header(CONTENT_TYPE, pdfMimeType)
                        .header(CONTENT_DISPOSITION,
                                DISPOSITION).
                                build();
            }
        }
        return Response.status(BAD_REQUEST).entity("Bad request - No data found for the supplied parameters").build();
    }

    private Optional<JsonObject> buildCourtListData(JsonEnvelope queryResponse, final String courtCentreId, final String courtRoomId, final CourtListType courtListType, final boolean restricted) {
        LOGGER.info("Received request for listType {}", courtListType);
        if (ALPHABETICAL.equals(courtListType)) {
            return alpbhabeticalCourtListService.buildAlphabeticalCourtListData(queryResponse, courtCentreId);
        }
        else if(STANDARD.equals(courtListType) || PUBLIC.equals(courtListType)) {
            return standardPublicCourtListAssembler.assemble(queryResponse, courtCentreId, courtRoomId, courtListType, restricted);
        }
        return Optional.empty();
    }

    private String getTemplateName(final CourtListType courtListType, boolean welsh){
        LOGGER.info("getTemplateName() :: isWelsh {}", welsh);
        if( ALPHABETICAL.equals(courtListType) && welsh){
            return courtListType.getWelshTemplateName();
        }
        return courtListType.getTemplateName();
    }

}
