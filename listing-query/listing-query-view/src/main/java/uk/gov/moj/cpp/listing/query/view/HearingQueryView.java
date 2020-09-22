package uk.gov.moj.cpp.listing.query.view;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.justice.services.messaging.JsonObjects.toJsonArray;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.ALL_AUTHORITY_CODES_SEARCH;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.AUTHORITY_ID_SEARCH;
import static uk.gov.moj.cpp.listing.query.view.dto.SearchCriteria.MATCHED_DEFENDANTS;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;
import uk.gov.moj.cpp.listing.query.view.courtlist.CourtListService;
import uk.gov.moj.cpp.listing.query.view.dto.LinkedCase;
import uk.gov.moj.cpp.listing.query.view.dto.ListedCase;
import uk.gov.moj.cpp.listing.query.view.dto.SearchCriteria;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingToJsonConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.NotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings({"squid:S1192", "squid:S00107"})
public class HearingQueryView {
    private static final String PUBLISH_COURT_LIST_TYPES = "publishCourtListTypes";
    private static final String PUBLISH_COURT_LIST_TYPE = "publishCourtListType";
    private static final String PUBLISH_DATE = "publishDate";
    private static final String WEEK_COMMENCING = "weekCommencing";

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingQueryView.class);
    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String AUTHORITY_ID = "authorityId";
    private static final String HEARING_TYPE = "hearingTypeId";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String START_DATE = "startDate";
    private static final String SEARCH_DATE = "searchDate";
    private static final String END_DATE = "endDate";
    private static final String START_TIME = "startTime";
    private static final String END_TIME = "endTime";
    private static final String LIST_ID = "listId";
    private static final String HEARINGS = "hearings";
    private static final String ID = "id";
    private static final String CASE_URN = "caseUrn";
    private static final String HEARING_ID = "hearingId";
    private static final String TYPE_OF_LIST = "typeOfList";
    private static final String COURT_CENTRE_IDS = "courtCentreIds";
    private static final String SEARCH_CRITERIA = "searchCriteria";
    private static final String NAME_LISTING_SEARCH_HEARING = "listing.search.hearing";
    private static final String EMPTY_STRING = "";  // It is needed as jsonb query cannot handle null as per our query condition
    private static final String MATCHED_DEFENDANT_IDS = "matchedDefendantIds";
    private static final String CASE_URN_FOR_LINKED_CASES = "caseUrnForLinkedCases";

    @Inject
    private HearingRepository repository;

    @Inject
    private PublishedCourtListRepository publishedCourtListRepository;

    @Inject
    private PublishedCourtListToJsonConverter publishedCourtListToJsonConverter;

    @Inject
    private CourtListPublishStatusJdbcRepository courtListRepository;

    @Inject
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    @Inject
    private RangeSearchQuery rangeSearchQuery;

    @Inject
    private CourtListService courtListService;

    @Inject
    private Enveloper enveloper;

    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER);
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String authorityIdSearchString = getAuthorityIdSearchString(authorityId);
        final String hearingTypeId = query.payloadAsJsonObject().getString(HEARING_TYPE, null);
        final String jurisdictionType = query.payloadAsJsonObject().getString(JURISDICTION_TYPE, null);
        final String searchDate = query.payloadAsJsonObject().getString(SEARCH_DATE);
        final String startTime = getDateTimeAsString(searchDate, query.payloadAsJsonObject().getString(START_TIME, MIN.toString()), MIN.toString());
        final String endTime = getDateTimeAsString(searchDate, query.payloadAsJsonObject().getString(END_TIME, MAX.toString()), MAX.toString());

        LOGGER.info("Query params -  " +
                        "allocated: {}, " +
                        "courtCentreId: {}, " +
                        "courtRoomId: {}, " +
                        "authorityId: {}, " +
                        "hearingTypeId: {}, " +
                        "jurisdictionType: {}, " +
                        "startDate: {}, " +
                        "startTime: {}, " +
                        "endTime: {}",
                allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, searchDate, startTime, endTime);

        final List<Hearing> hearings = repository.findHearings(
                allocated,
                courtCentreId,
                courtRoomId,
                authorityIdSearchString,
                hearingTypeId,
                jurisdictionType,
                searchDate,
                startTime,
                endTime
        );


        return enveloper.withMetadataFrom(query, "listing.search.hearings").apply(
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                        .build()
        );
    }

    @Handles("listing.unscheduled.search.hearings")
    public Envelope<JsonObject> searchUnscheduledHearings(final JsonEnvelope query) {
        final String caseUrnQueryParam = query.payloadAsJsonObject().getString(CASE_URN, null);
        final String typeOfListQueryParam = query.payloadAsJsonObject().getString(TYPE_OF_LIST, null);
        final String courtCentreIdQueryParam = query.payloadAsJsonObject().getString(COURT_CENTRE_IDS, null);

        LOGGER.info("listing.unscheduled.search.hearings Query params -  " +
                        "caseUrn: {}, " +
                        "typeOfList: {}, " +
                        "courtCentreId: {}, ",
                caseUrnQueryParam, typeOfListQueryParam, courtCentreIdQueryParam);

        final List<Hearing> hearings;
        if (!isNullOrEmpty(courtCentreIdQueryParam)) {
            final Set<String> courtCentreIds = Arrays.stream(courtCentreIdQueryParam.split(",")).collect(toSet());
            hearings = repository.findHearings(caseUrnQueryParam, typeOfListQueryParam, courtCentreIds);
        } else {
            hearings = repository.findHearings(caseUrnQueryParam, typeOfListQueryParam);
        }

        return Enveloper.envelop(createObjectBuilder()
                .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                .build()).withName("listing.unscheduled.search.hearings").withMetadataFrom(query);

    }

    @Handles("listing.available.search.hearings")
    public JsonEnvelope searchAvailableHearings(final JsonEnvelope query) throws IOException {
        final String hearingId = query.payloadAsJsonObject().getString(HEARING_ID, null);
        final String caseUrnsQueryParam = query.payloadAsJsonObject().getString(CASE_URN, null);
        final String searchCriteriaQueryParam = query.payloadAsJsonObject().getString(SEARCH_CRITERIA, null);
        final String jurisdictionType = query.payloadAsJsonObject().getString(JURISDICTION_TYPE, null);
        final String matchingDefendantIdsParam = query.payloadAsJsonObject().getString(MATCHED_DEFENDANT_IDS, null);
        final String caseUrnForLinkedCases = query.payloadAsJsonObject().getString(CASE_URN_FOR_LINKED_CASES, null);
        final boolean allocated = true;

        final Set<String> masterDefendantSet = new HashSet<>();
        final Set<String> caseUrnSet = new HashSet<>();
        final Set<String> linkedCaseUrnSet = new HashSet<>();
        final Set<String> jurisdictionTypeSet = new HashSet<>();

        if (nonNull(caseUrnsQueryParam)) {
            caseUrnSet.addAll(Stream.of(caseUrnsQueryParam.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet()));
        }

        if (nonNull(matchingDefendantIdsParam)) {
            masterDefendantSet.addAll(Stream.of(matchingDefendantIdsParam.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet()));
        }

        final List<ListedCase> listedCases = new ArrayList<>();

        if (nonNull(hearingId)) {
            // search hearing by incoming hearing id
            final Hearing hearing = repository.findBy(UUID.fromString(hearingId));

            if (nonNull(hearing)) {
                final JsonNode listedCasesJsonNode = hearing.getProperties().findPath("listedCases");
                listedCases.addAll(extractListedCases(listedCasesJsonNode));
            }
        }

        extractCaseUrn(searchCriteriaQueryParam, listedCases, caseUrnSet);
        extractMasterDefendantId(searchCriteriaQueryParam, listedCases, masterDefendantSet);
        extractLinkedCaseUrn(searchCriteriaQueryParam, listedCases, linkedCaseUrnSet);
        extractJurisdictionType(jurisdictionType, jurisdictionTypeSet);

        LOGGER.info("\n Query params -  " +
                        "allocated: {}, " +
                        "jurisdictionTypeSet : {}, " +
                        "hearingId : {}, " +
                        "caseUrns : {}, " +
                        "masterDefendantIds : {}, " +
                        "linkedCaseUrn : {}, " +
                        "caseUrnForLinkedCases: {}",
                allocated, jurisdictionTypeSet, hearingId, caseUrnSet, masterDefendantSet, linkedCaseUrnSet, caseUrnForLinkedCases);

        final List<Hearing> hearings = repository.findHearings(
                allocated,
                jurisdictionTypeSet,
                hearingId,
                caseUrnSet,
                masterDefendantSet,
                linkedCaseUrnSet,
                caseUrnForLinkedCases
        );

        return enveloper.withMetadataFrom(query, "listing.search.hearings").apply(
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                        .build()
        );
    }

    @Handles("listing.any-allocation.search.hearings")
    public Envelope<JsonObject> searchHearingsWithAnyAllocationState(final JsonEnvelope query) throws IOException {
        final String caseUrnQueryParam = query.payloadAsJsonObject().getString(CASE_URN).toUpperCase();

        LOGGER.info("\n Query params - caseUrn : {} ", caseUrnQueryParam);

        final List<Hearing> hearings = repository.findHearingsByCaseUrnAndAnyAllocationState(caseUrnQueryParam);

        return Enveloper.envelop(createObjectBuilder()
                .add(HEARINGS,  hearingJsonListConverterFilterEjectCases.convert(hearings))
                .build()).withName("listing.any-allocation.search.hearings").withMetadataFrom(query);
    }


    @Handles("listing.range.search.hearings")
    public JsonEnvelope rangeSearchHearings(final JsonEnvelope query) {
        return rangeSearchQuery.rangeSearchHearings(query);
    }

    @Handles("listing.search.court.list")
    public JsonEnvelope getCourtListContent(final JsonEnvelope query) {

        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, null);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, null);
        final String listId = query.payloadAsJsonObject().getString(LIST_ID);
        LOGGER.info("Parameters -  " +
                        COURT_CENTRE_ID + " : {}, " +
                        COURT_ROOM_ID + " : {}, " +
                        START_DATE + " : {}, " +
                        END_DATE + " : {}, " +
                        LIST_ID + " : {}, ",
                courtCentreId, courtRoomId, startDate, endDate, listId);
        final Optional<CourtListType> listType = CourtListType.valueFor(listId);
        if (listType.isPresent()) {
            if (listType.get().equals(CourtListType.PUBLIC) || listType.get().equals(CourtListType.STANDARD)) {
                final Hearing matchedHearingsJsonObject = repository.findHearingsForPublicStandardList(Boolean.TRUE, courtCentreId, startDate, endDate);
                return createPublicStandardCourtListJsonEnvelope(query, matchedHearingsJsonObject);
            } else {
                final List<Hearing> matchedHearings = repository.findHearingsForAlphabeticalList(Boolean.TRUE, courtCentreId, startDate);
                return createAlphabeticalListJsonEnvelope(query, matchedHearings);
            }
            // Plug in queries for other list types
        } else {
            LOGGER.error("Supplied CourtList type is not valid {} ", listId);
            return createEmptyResponse(query);
        }
    }

    @Handles("listing.courtlist")
    public JsonEnvelope retrieveCourtList(final JsonEnvelope queryEnvelope) {

        final JsonObject queryPayload = queryEnvelope.payloadAsJsonObject();

        final JsonObject courtListResponsePayload = (queryPayload.containsKey("published") &&
                queryPayload.getBoolean("published"))
                ? getPublishedCourtListResponsePayload(queryPayload)
                : getUnpublishedCourtListResponsePayload(queryEnvelope, queryPayload);

        return enveloper.withMetadataFrom(queryEnvelope, "listing.courtlist").apply(courtListResponsePayload);
    }

    private JsonObject getPublishedCourtListResponsePayload(final JsonObject queryPayload) {

        final UUID courtCentreId = fromString(queryPayload.getString("courtCentreId"));

        final PublishedCourtListPrimaryKey primaryKey = new PublishedCourtListPrimaryKey(
                courtCentreId,
                PublishCourtListType.valueOf(queryPayload.getString("publishCourtListType")),
                parse(queryPayload.getString("startDate")));

        final PublishedCourtList publishedCourtList = publishedCourtListRepository.findBy(primaryKey);

        return publishedCourtList != null
                ? publishedCourtListToJsonConverter.convert(publishedCourtList).getJsonObject("courtListJson")
                : courtListService.emptyCourtList(courtCentreId);
    }

    private JsonObject getUnpublishedCourtListResponsePayload(final JsonEnvelope query, final JsonObject queryPayload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format("getUnpublishedCourtListResponsePayload payLoad = %s", queryPayload));
        }
        final String endDate = trimToEmpty(queryPayload.getString("endDate", ""));
        return courtListService.retrieveUnPublishedCourtList(
                fromString(queryPayload.getString("courtCentreId")),
                uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.valueOf(queryPayload.getString("publishCourtListType")),
                parse(queryPayload.getString("startDate")),
                StringUtils.isNotBlank(endDate) ? endDate : StringUtils.EMPTY,
                query
        );
    }

    @Handles("listing.publishedcourtlist")
    @SuppressWarnings("WeakerAccess")
    public JsonEnvelope getPublishedCourtLists(final JsonEnvelope query) {

        final JsonObject queryPayload = query.payloadAsJsonObject();

        final List<PublishedCourtList> publishedCourtLists = queryPayload.containsKey(COURT_CENTRE_ID)
                ? getPublishedCourtList(queryPayload)
                : publishedCourtListRepository.findAll();

        return enveloper.withMetadataFrom(query, "listing.publishedcourtlist")
                .apply(publishedCourtListToJsonConverter.convert(publishedCourtLists));
    }

    private List<PublishedCourtList> getPublishedCourtList(final JsonObject queryPayload) {

        final PublishedCourtListPrimaryKey pk = new PublishedCourtListPrimaryKey(
                UUID.fromString(queryPayload.getString(COURT_CENTRE_ID)),
                PublishCourtListType.valueOf(queryPayload.getString(PUBLISH_COURT_LIST_TYPE)),
                LocalDate.parse(queryPayload.getString(START_DATE))
        );

        final PublishedCourtList result = publishedCourtListRepository.findBy(pk);

        return result != null
                ? singletonList(publishedCourtListRepository.findBy(pk))
                : new ArrayList<>();
    }

    @Handles("listing.court.list.publish.status")
    public JsonEnvelope getCourtListPublishStatus(final JsonEnvelope query) {
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID);
        final String publishCourtListTypes = query.payloadAsJsonObject().getString(PUBLISH_COURT_LIST_TYPES);
        final LocalDate publishDate = LocalDates.from(query.payloadAsJsonObject().getString(PUBLISH_DATE));
        final boolean weekCommencing = query.payloadAsJsonObject().getBoolean(WEEK_COMMENCING, false);

        LOGGER.info("Parameters -  " + COURT_CENTRE_ID + " : {}, " + PUBLISH_COURT_LIST_TYPES + " : {}, ", courtCentreId, publishCourtListTypes);

        final Set<PublishCourtListType> publishCourtListTypes1 = Stream.of(publishCourtListTypes.split(","))
                .map(PublishCourtListType::valueOf)
                .collect(toSet());

        final JsonArray courtListPublishStatuses = toJsonArray(courtListRepository
                        .courtListPublishStatuses(fromString(courtCentreId), publishCourtListTypes1, publishDate, weekCommencing),
                publishCourtListStatus -> {
                    final JsonObjectBuilder builder = createObjectBuilder();
                    builder.add("courtCentreId", publishCourtListStatus.getCourtCentreId().toString())
                            .add("publishCourtListType", publishCourtListStatus.getPublishCourtListType().name())
                            .add("lastUpdated", publishCourtListStatus.getLastUpdated().toString())
                            .add("publishStatus", publishCourtListStatus.getPublishStatus().toString())
                            .add("failureMessage", defaultIfEmpty(publishCourtListStatus.getFailureMessage(), ""))
                    ;
                    return builder.build();
                });

        return enveloper.withMetadataFrom(query, "listing.court.list.publish.status").apply(createObjectBuilder().add("publishCourtListStatuses", courtListPublishStatuses).build());
    }

    @Handles(NAME_LISTING_SEARCH_HEARING)
    /**
     * Note that all validation is done in HearingQueryApi; while
     * we could do it here, as well, it would be ineffective: for
     * example, if we did throw a BadRequestException from, it wouldn't
     * result in a 404.
     *
     * Note, as well, that this method must be public; if not you'll get the error:
     * "No handler registered to handle action listing.search.hearing
     */
    @SuppressWarnings("WeakerAccess")
    public JsonEnvelope getHearingById(final JsonEnvelope query) {
        final Hearing hearing = repository.findBy(extractUUID(query));
        if (hearing == null) {
            throw new NotFoundException("There is no Hearing for that ID.");
        }
        return enveloper.withMetadataFrom(query, NAME_LISTING_SEARCH_HEARING)
                .apply(HearingToJsonConverter.convert(hearing));
    }

    private UUID extractUUID(final JsonEnvelope query) {
        return fromString(query.payloadAsJsonObject().getString(ID, null));
    }

    private JsonEnvelope createAlphabeticalListJsonEnvelope(final JsonEnvelope query, final List<Hearing> matchedHearings) {
        return enveloper.withMetadataFrom(query, "listing.search.court.list").apply(
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convertHearingResultForAlphabeticalList(matchedHearings))
                        .build()
        );
    }

    private JsonEnvelope createPublicStandardCourtListJsonEnvelope(final JsonEnvelope query, final Hearing matchedHearingsJsonObject) {
        return enveloper.withMetadataFrom(query, "listing.search.court.list").apply(
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convertHearingResultForPublicList(matchedHearingsJsonObject))
                        .build()
        );
    }

    private JsonEnvelope createEmptyResponse(JsonEnvelope query) {
        return enveloper.withMetadataFrom(query, "listing.search.court.list").apply(
                createObjectBuilder()
                        .add(HEARINGS, createArrayBuilder().build())
                        .build());
    }

    private String getDateTimeAsString(final String date, final String time, final String defaultTime) {
        final String copyTime = isNullOrEmpty(time) ? defaultTime : time;
        final LocalDate localDate = parse(date);
        final LocalTime localTime = LocalTime.parse(copyTime);
        return localDate.atTime(localTime).toString();
    }

    private String getAuthorityIdSearchString(String authorityId) {
        if (authorityId != null) {
            return format(AUTHORITY_ID_SEARCH, authorityId);
        } else {
            return ALL_AUTHORITY_CODES_SEARCH;
        }
    }

    private void extractMasterDefendantId(String searchCriteria, List<ListedCase> listedCases, Set<String> masterDefendants) {
        if (nonNull(searchCriteria) && searchCriteria.contains(MATCHED_DEFENDANTS.name())) {
            masterDefendants.addAll(listedCases.stream()
                    .flatMap(l -> l.getDefendants().stream())
                    .map(d -> d.getMasterDefendantId().get().toString())
                    .collect(Collectors.toSet()));
        }

        if (masterDefendants.isEmpty()) {
            masterDefendants.add(EMPTY_STRING);
        }
    }

    private void extractCaseUrn(String searchCriteria, List<ListedCase> listedCases, Set<String> caseUrns) {
        if (nonNull(searchCriteria) && searchCriteria.contains(SearchCriteria.CASE_IN_HEARING.name())) {
            caseUrns.addAll(listedCases.stream()
                    .map(l -> l.getCaseIdentifier().getCaseReference().toUpperCase())
                    .collect(Collectors.toSet()));
        }
        if (caseUrns.isEmpty()) {
            caseUrns.add(EMPTY_STRING);
        }
    }

    private void extractLinkedCaseUrn(final String searchCriteriaQueryParam, final List<ListedCase> listedCases, final Set<String> linkedCaseUrnSet) {
        if (contains(searchCriteriaQueryParam, MATCHED_DEFENDANTS.name())) {
            linkedCaseUrnSet.addAll(listedCases.stream()
                    .map(ListedCase::getLinkedCases)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(LinkedCase::getCaseUrn)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet()));
        }

        if (linkedCaseUrnSet.isEmpty()) {
            linkedCaseUrnSet.add(EMPTY_STRING);
        }
    }

    private void extractJurisdictionType(String jurisdictionType, Set<String> jurisdictionTypeSet) {
        if (nonNull(jurisdictionType)) {
            jurisdictionTypeSet.add(jurisdictionType);
        } else {
            jurisdictionTypeSet.add(JurisdictionType.CROWN.name());
            jurisdictionTypeSet.add(JurisdictionType.MAGISTRATES.name());
        }
    }

    private List<ListedCase> extractListedCases(JsonNode listedCasesJsonNode) throws IOException {
        final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();
        final List<ListedCase> listedCases = new ArrayList<>();
        if (listedCasesJsonNode != null && !listedCasesJsonNode.isMissingNode()) {
            final ArrayNode arrayNode = (ArrayNode) listedCasesJsonNode;
            for (int index = 0; index < arrayNode.size(); index++) {
                final JsonNode listedCase = arrayNode.get(index);
                listedCases.add(mapper.treeToValue(listedCase, ListedCase.class));
            }
        }
        return listedCases;
    }
}
