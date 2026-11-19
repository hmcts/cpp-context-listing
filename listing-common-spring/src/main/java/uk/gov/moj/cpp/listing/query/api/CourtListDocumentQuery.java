package uk.gov.moj.cpp.listing.query.api;

import org.springframework.transaction.annotation.Transactional;

import static uk.gov.moj.cpp.listing.domain.CourtListType.ALPHABETICAL;
import static uk.gov.moj.cpp.listing.domain.CourtListType.JUDGE;
import static uk.gov.moj.cpp.listing.domain.CourtListType.ONLINE_PUBLIC;
import static uk.gov.moj.cpp.listing.domain.CourtListType.PUBLIC;

import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.json.JsonObjects;
import uk.gov.moj.cpp.listing.domain.utils.NullAwareJsonObjectBuilder;
import uk.gov.moj.cpp.listing.query.api.service.AlphabeticalCourtListService;
import uk.gov.moj.cpp.listing.query.api.service.ReferenceDataService;
import uk.gov.moj.cpp.listing.query.document.generator.DocumentGeneratorClient;
import uk.gov.moj.cpp.listing.query.document.generator.JudgeListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.document.generator.StandardPublicCourtListTemplateAssembler;
import uk.gov.moj.cpp.listing.query.view.HearingQueryView;
import uk.gov.moj.cpp.listing.query.view.exception.QueryBadRequestException;

import java.util.Optional;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The court list PDF ({@code GET /courtlist}, query name {@code listing.search.court.list}), ported
 * from the hand-written JAX-RS adapter {@code DefaultQueryApiCourtlistResource}: read the court list
 * content (judge list: range search by judge), assemble the template payload for the list type and
 * render it with systemdocgenerator. Access control and audit, which the adapter ran through the
 * interceptor chain, are the filters' job.
 */
/*
 * P2.1: read-only transaction around each query, as the JEE query ran inside the container's
 * transaction: the ported code reads lazy entity collections (listedCases, courtApplications)
 * after the repository call.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourtListDocumentQuery {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtListDocumentQuery.class);

    private final DocumentGeneratorClient documentGeneratorClient;
    private final AlphabeticalCourtListService alphabeticalCourtListService;
    private final StandardPublicCourtListTemplateAssembler standardPublicCourtListAssembler;
    private final ReferenceDataService referenceDataService;
    private final HearingQueryView hearingQueryView;
    private final JudgeListTemplateAssembler judgeListTemplateAssembler;

    /**
     * @return the PDF
     * @throws QueryBadRequestException (400) for an unknown list type or when there is no data
     */
    public byte[] getCourtList(final String courtCentreId, final String courtRoomId, final String listId,
                               final String startDate, final String endDate, final boolean restricted) {
        final Optional<CourtListType> courtListType = CourtListType.valueFor(listId);
        if (courtListType.isEmpty()) {
            throw new QueryBadRequestException(String.format("Bad request - No matching list type found for %s", listId));
        }
        final JsonObject documentQuery = NullAwareJsonObjectBuilder.wrap(JsonObjects.createObjectBuilder())
                .add("courtCentreId", courtCentreId)
                .add("listId", listId)
                .add("courtRoomId", courtRoomId)
                .add("startDate", startDate)
                .add("endDate", endDate)
                .build();
        final CourtListType listType = courtListType.get();
        final JsonObject queryResponse = listType == JUDGE
                ? hearingQueryView.rangeSearchHearingsForJudge(documentQuery)
                : hearingQueryView.getCourtListContent(documentQuery);
        return getDocumentContent(queryResponse, courtCentreId, courtRoomId, listType, restricted, startDate);
    }

    private byte[] getDocumentContent(final JsonObject queryResponse, final String courtCentreId, final String courtRoomId,
                                      final CourtListType courtListType, final boolean restricted, final String startDate) {
        if (queryResponse != null && !JsonValue.NULL.equals(queryResponse)) {
            final Optional<JsonObject> courtListData = buildCourtListData(queryResponse, courtCentreId, courtRoomId, courtListType, restricted, startDate);
            if (courtListData.isPresent()) {
                final JsonObject courtListPayload = courtListData.get();
                final boolean isWelsh = referenceDataService.isHearingLanguageWelsh(courtCentreId).orElse(false);
                final String templateName = getTemplateName(courtListType, isWelsh);
                LOGGER.info("getDocumentContent() :: templateName {} :: isWelsh :: {} courtListPayload {} ", templateName, isWelsh, courtListPayload);
                return documentGeneratorClient.generateDocument(courtListPayload, templateName);
            }
        }
        throw new QueryBadRequestException("Bad request - No data found for the supplied parameters");
    }

    private Optional<JsonObject> buildCourtListData(final JsonObject queryResponse, final String courtCentreId, final String courtRoomId,
                                                    final CourtListType courtListType, final boolean restricted, final String startDate) {
        LOGGER.info("Received request for listType {}", courtListType);
        if (ALPHABETICAL.equals(courtListType)) {
            return alphabeticalCourtListService.buildAlphabeticalCourtListData(queryResponse, courtCentreId);
        }
        if (JUDGE.equals(courtListType)) {
            return judgeListTemplateAssembler.assemble(queryResponse, courtCentreId, courtRoomId, courtListType, startDate);
        }
        return standardPublicCourtListAssembler.assemble(queryResponse, courtCentreId, courtRoomId, courtListType, restricted, false);
    }

    private String getTemplateName(final CourtListType courtListType, final boolean welsh) {
        if ((ALPHABETICAL.equals(courtListType) || PUBLIC.equals(courtListType) || ONLINE_PUBLIC.equals(courtListType)) && welsh) {
            return courtListType.getWelshTemplateName();
        }
        return courtListType.getTemplateName();
    }
}
