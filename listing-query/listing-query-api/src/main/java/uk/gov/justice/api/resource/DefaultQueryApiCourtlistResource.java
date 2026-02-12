package uk.gov.justice.api.resource;

import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;
import static uk.gov.justice.services.core.interceptor.InterceptorContext.interceptorContextWithInput;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ALPHABETICAL;
import static uk.gov.moj.cpp.listing.domain.CourtListType.JUDGE;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ONLINE_PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.interceptor.InterceptorChainProcessor;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.utils.NullAwareJsonObjectBuilder;
import uk.gov.moj.cpp.listing.query.api.service.AlphabeticalCourtListService;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.document.generator.DocumentGeneratorClient;
import uk.gov.moj.cpp.listing.query.document.generator.JudgeListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.document.generator.StandardPublicCourtListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;

import java.io.ByteArrayInputStream;
import java.util.Optional;
import java.util.UUID;

import javax.ejb.Stateless;
import javax.inject.Inject;
import uk.gov.justice.services.messaging.JsonObjects;
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
    private InterceptorChainProcessor interceptorChainProcessor;

    @Inject
    private DocumentGeneratorClient documentGeneratorClient;

    @Inject
    private AlphabeticalCourtListService alpbhabeticalCourtListService;

    @Inject
    private StandardPublicCourtListTemplateAssembler standardPublicCourtListAssembler;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private HearingQueryView hearingQueryView;

    @Inject
    private JudgeListTemplateAssembler judgeListTemplateAssembler;


    @Override
    public Response getCourtList(final String courtCentreId, final String courtRoomId, final String listId,
                                 final String startDate, final String endDate, final boolean restricted, final UUID userId) {
        final Optional<CourtListType> courtListType = CourtListType.valueFor(listId);
        if (courtListType.isPresent()) {
            final JsonObjectBuilder builder =
                    NullAwareJsonObjectBuilder.wrap(JsonObjects.createObjectBuilder());
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



             interceptorChainProcessor.process(interceptorContextWithInput(documentQuery));
            final CourtListType listType = courtListType.get();
            return getDocumentContent(listType == JUDGE ? hearingQueryView.rangeSearchHearingsForJudge(documentQuery) : hearingQueryView.getCourtListContent(documentQuery), courtCentreId, courtRoomId, listType, restricted, startDate);
        }
        return Response.status(BAD_REQUEST).entity(String.format("Bad request - No matching list type found for %s", listId)).build();
    }

    private Response getDocumentContent(final JsonEnvelope queryResponse, final String courtCentreId, final String courtRoomId, final CourtListType courtListType, final boolean restricted, final String startDate) {
        final String pdfMimeType = "application/pdf";
        if (!JsonValue.NULL.equals(queryResponse.payload())) {
            final Optional<JsonObject> courtListData = buildCourtListData(queryResponse, courtCentreId, courtRoomId , courtListType, restricted, startDate);
            if (courtListData.isPresent()) {
                final JsonObject courtListPayload = courtListData.get();
                final boolean isWelsh = referenceDataService.isHearingLanguageWelsh(queryResponse, courtCentreId).orElse(false);
                final String templateName = getTemplateName(courtListType, isWelsh);
                LOGGER.info("getDocumentContent() :: templateName {} :: isWelsh :: {} courtListPayload {} ", templateName, isWelsh, courtListPayload);
                final byte[] binaryDocument = documentGeneratorClient.generateDocument(
                        courtListPayload, templateName);
                final Response.ResponseBuilder responseBuilder = status(OK).entity(new ByteArrayInputStream(binaryDocument));
                return responseBuilder.header(CONTENT_TYPE, pdfMimeType)
                        .header(CONTENT_DISPOSITION,
                                DISPOSITION).
                                build();
            }
        }
        return Response.status(BAD_REQUEST).entity("Bad request - No data found for the supplied parameters").build();
    }

    private Optional<JsonObject> buildCourtListData(JsonEnvelope queryResponse, final String courtCentreId, final String courtRoomId, final CourtListType courtListType, final boolean restricted, final String startDate) {
        LOGGER.info("Received request for listType {}", courtListType);
        if (ALPHABETICAL.equals(courtListType)) {
            return alpbhabeticalCourtListService.buildAlphabeticalCourtListData(queryResponse, courtCentreId);
        }

        if (JUDGE.equals(courtListType)) {
            return judgeListTemplateAssembler.assemble(queryResponse, courtCentreId, courtRoomId, courtListType, startDate);
        }

        return standardPublicCourtListAssembler.assemble(queryResponse, courtCentreId, courtRoomId, courtListType, restricted);
    }

    private String getTemplateName(final CourtListType courtListType, boolean welsh) {
        if ((ALPHABETICAL.equals(courtListType) || PUBLIC.equals(courtListType) || ONLINE_PUBLIC.equals(courtListType)) && welsh) {
            return courtListType.getWelshTemplateName();
        }
        return courtListType.getTemplateName();
    }

}

