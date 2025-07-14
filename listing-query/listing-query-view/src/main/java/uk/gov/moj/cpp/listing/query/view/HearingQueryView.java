package uk.gov.moj.cpp.listing.query.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.CourtApplications;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.entity.query.CaseByDefendant;
import uk.gov.moj.cpp.listing.persistence.repository.CaseByDefendantRepository;
import uk.gov.moj.cpp.listing.persistence.repository.CourtApplicationRepository;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.CourtListPublishStatusJdbcRepository;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtList;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListPrimaryKey;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.PublishedCourtListRepository;
import uk.gov.moj.cpp.listing.query.view.courtlist.CourtListService;
import uk.gov.moj.cpp.listing.query.view.dto.LinkedCase;
import uk.gov.moj.cpp.listing.query.view.dto.ListedCase;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameter;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameterFactory;
import uk.gov.moj.cpp.listing.query.view.dto.SearchCriteria;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingToJsonConverter;
import uk.gov.moj.cpp.listing.query.view.service.JsonNodeReader;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;
import uk.gov.moj.cpp.listing.query.view.service.ProgressionService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.time.LocalDate.parse;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.justice.services.common.converter.LocalDates.from;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.getString;
import static uk.gov.justice.services.messaging.JsonObjects.toJsonArray;
import static uk.gov.moj.cpp.listing.domain.CourtListType.valueFor;
import static uk.gov.moj.cpp.listing.query.view.dto.SearchCriteria.MATCHED_DEFENDANTS;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;
import javax.ws.rs.NotFoundException;


@SuppressWarnings({"squid:S1192", "squid:S00107", "squid:S1166"})
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
    private static final String NOTES = "notes";
    private static final String ID = "id";
    private static final String CASE_URN = "caseUrn";
    private static final String CASE_ID = "caseId";
    private static final String APPLICATION_ID = "applicationId";
    private static final String HEARING_ID = "hearingId";
    private static final String TYPE_OF_LIST = "typeOfList";
    private static final String COURT_CENTRE_IDS = "courtCentreIds";
    private static final String SEARCH_CRITERIA = "searchCriteria";
    private static final String NAME_LISTING_SEARCH_HEARING = "listing.search.hearing";
    private static final String EMPTY_STRING = "";  // It is needed as jsonb query cannot handle null as per our query condition
    private static final String MATCHED_DEFENDANT_IDS = "matchedDefendantIds";
    private static final String CASE_URN_FOR_LINKED_CASES = "caseUrnForLinkedCases";
    private static final String RETURN_ALL_HEARINGS = "returnAllHearings";
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String URN = "urn";


    @Inject
    private HearingRepository repository;

    @Inject
    private CourtApplicationRepository courtApplicationRepository;

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

    @Inject
    private NotesService notesService;

    @Inject
    private ListToJsonArrayConverter<Notes> listToJsonArrayConverter;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private CaseByDefendantRepository caseByDefendantRepository;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private PaginationParameterFactory paginationParameterFactory;

    public static final String TYPE = "type";

    public static final String LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS = "listing.allocated.and.unallocated.hearings";


    @Handles("listing.search.hearings")
    public JsonEnvelope searchHearings(final JsonEnvelope query) {
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER);
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String hearingTypeId = query.payloadAsJsonObject().getString(HEARING_TYPE, null);
        final String jurisdictionType = getString(query.payloadAsJsonObject(), JURISDICTION_TYPE).orElse(null);
        final String searchDate = query.payloadAsJsonObject().getString(SEARCH_DATE);
        final String startTime = getDateTimeAsString(searchDate, query.payloadAsJsonObject().getString(START_TIME, MIN.toString()), MIN.toString());
        final String endTime = getDateTimeAsString(searchDate, query.payloadAsJsonObject().getString(END_TIME, MAX.toString()), MAX.toString());
        final String startTimeForMatched = getDateTimeAsString(searchDate, query.payloadAsJsonObject().getString(START_TIME, LocalTime.now().toString()), LocalTime.now().toString());

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
                authorityId,
                hearingTypeId,
                jurisdictionType,
                LocalDate.parse(searchDate),
                ZonedDateTime.of(LocalDateTime.parse(startTime), UTC),
                ZonedDateTime.of(LocalDateTime.parse(endTime), UTC)
        );

        LOGGER.info("number of records from query -  {}", hearings.size());

        final List<Notes> notes = notesService.findNotes(allocated, courtRoomId, searchDate, hearings);

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convertForSearchHearing(hearings,
                                getHearingDayMatchedCriteriaMap(courtCentreId,
                                        courtRoomId, searchDate, startTimeForMatched, endTime)))
                        .add(NOTES, listToJsonArrayConverter.convert(notes))
                        .build()
        );
    }

    private Map<String, String> getHearingDayMatchedCriteriaMap(final String courtCentreId, final String courtRoomId, final String searchDate, final String startTime, final String endTime) {
        final Map<String, String> hearingDayMatchedCriteriaMap = new HashMap<>();
        hearingDayMatchedCriteriaMap.put(COURT_CENTRE_ID, courtCentreId);
        hearingDayMatchedCriteriaMap.put(COURT_ROOM_ID, courtRoomId);
        hearingDayMatchedCriteriaMap.put(SEARCH_DATE, searchDate);
        hearingDayMatchedCriteriaMap.put(START_TIME, startTime);
        hearingDayMatchedCriteriaMap.put(END_TIME, endTime);
        return hearingDayMatchedCriteriaMap;
    }

    @Handles("listing.unscheduled.search.hearings")
    public Envelope<JsonObject> searchUnscheduledHearings(final JsonEnvelope query) {
        final String typeOfListQueryParam = query.payloadAsJsonObject().getString(TYPE_OF_LIST, null);
        final String caseUrnQueryParam = query.payloadAsJsonObject().getString(CASE_URN, null);
        final String courtCentreIdQueryParam = query.payloadAsJsonObject().getString(COURT_CENTRE_IDS, null);
        final PaginationParameter paginationParameter = paginationParameterFactory.newPaginationParameter(query.payloadAsJsonObject());

        LOGGER.info("listing.unscheduled.search.hearings Query params -  " +
                        "caseUrn: {}, " +
                        "typeOfList: {}, " +
                        "courtCentreId: {}, " +
                        "pageSize: {},  " +
                        "pageNumber: {} ",
                caseUrnQueryParam, typeOfListQueryParam, courtCentreIdQueryParam, paginationParameter.getPageSize(), paginationParameter.getPageNumber());

        final List<Hearing> hearings;

        if (!isNullOrEmpty(courtCentreIdQueryParam)) {
            final Set<String> courtCentreIdSet = Stream.of(courtCentreIdQueryParam.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (!courtCentreIdSet.isEmpty()) {
                hearings = repository.findHearings(caseUrnQueryParam, typeOfListQueryParam, courtCentreIdSet, paginationParameter.getOffSet(), paginationParameter.getPageSize());
            } else {
                hearings = emptyList();
            }
        } else {
                hearings = repository.findHearings(caseUrnQueryParam, typeOfListQueryParam, paginationParameter.getOffSet(), paginationParameter.getPageSize());
        }

        final Long totalCount = !(hearings.isEmpty()) ? hearings.get(0).getTotalCount() : 0;
        return envelop(createObjectBuilder()
                .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                .add("results", totalCount)
                .add("pageCount", toPageCount(totalCount, paginationParameter.getPageSize()))
                .build())
                .withName("listing.unscheduled.search.hearings")
                .withMetadataFrom(query);
    }

    private long toPageCount(final long totalCount, final Integer pageSize) {
        return (long) Math.ceil((double) totalCount / (double) pageSize) ;
    }

    @Handles(LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS)
    public JsonEnvelope searchAllocatedAndUnallocatedHearings(final JsonEnvelope query) {
        LOGGER.info("Event: {}", LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS);
        final List<Hearing> hearings;
        final String caseIdQueryParam = query.payloadAsJsonObject().getString(CASE_ID, null);
        final String applicationIdQueryParam = query.payloadAsJsonObject().getString(APPLICATION_ID, null);
        LOGGER.info("Event: {}, Case Id: {}, Application Id: {}", LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS, caseIdQueryParam, applicationIdQueryParam);
        if (caseIdQueryParam == null && applicationIdQueryParam == null) {
            return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                    createObjectBuilder()
                            .add(HEARINGS, Json.createArrayBuilder().build())
            );
        }
        if (nonNull(applicationIdQueryParam)) {
            hearings = repository.findAllocatedAndUnallocatedHearingsByCaseId(caseIdQueryParam, applicationIdQueryParam);
            final List<CourtApplications> childApplications = courtApplicationRepository.findByParentApplicationId(UUID.fromString(applicationIdQueryParam));
            if (isNotEmpty(childApplications)) {
                final Set<UUID> hearingIds = hearings.stream().map(Hearing::getId).collect(toSet());
                hearings.addAll(childApplications
                        .stream()
                        .map(childApplication -> repository.findAllocatedAndUnallocatedHearingsByCaseId(caseIdQueryParam, childApplication.getApplicationId().toString()))
                        .flatMap(Collection::stream)
                        .filter(e -> hearingIds.add(e.getId()))
                        .toList());
            }
        } else {
            hearings = repository.findAllocatedAndUnallocatedHearingsByCaseId(caseIdQueryParam);
        }
        LOGGER.info("Event: {}, number of hearing records from query -  {}", LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS, hearings.size());

        final List<Hearing> hearingsToRemove = new ArrayList<>();
        if (caseIdQueryParam != null) {
            removedReViewHearings(hearings, hearingsToRemove);
            LOGGER.info("Event: {}, number of hearing in Review to be removed - {}", LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS, hearingsToRemove.size());
        }
        hearingsToRemove.forEach(hearings::remove);

        LOGGER.info("Event: {}, number of hearing to return - {}", LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS, hearings.size());

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
        );
    }

    private void removedReViewHearings(final List<Hearing> hearings, final List<Hearing> hearingsToRemove) {
        hearings.forEach(hearing -> {
            if (hearing.getListedCases() != null) {
                hearing.getListedCases().forEach(listedCase -> {
                    final JsonNodeReader reader = JsonNodeReader.read(hearing.getProperties());
                    if (nonNull(reader.get(TYPE))) {
                        final String reviewType = reader.get(TYPE).getText("description");
                        if (nonNull(hearing.getCourtApplications())  && !hearing.getCourtApplications().isEmpty() && "Review".equals(reviewType)) {
                            hearingsToRemove.add(hearing);
                        }
                    }
                });
            }
        });
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
        final Boolean returnAllHearings = query.payloadAsJsonObject().getBoolean(RETURN_ALL_HEARINGS, false);

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

        if (hearingId != null) {
            // search hearing by incoming hearing id
            final Hearing hearing = repository.findBy(fromString(hearingId));

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
                        "caseUrnForLinkedCases: {}"+
                        "returnAllHearings: {}",
                allocated, jurisdictionTypeSet, hearingId, caseUrnSet, masterDefendantSet, linkedCaseUrnSet, caseUrnForLinkedCases,returnAllHearings);

        final List<Hearing> hearings;

        if (returnAllHearings) {
            hearings = repository.findHearings(
                    jurisdictionTypeSet,
                    hearingId,
                    caseUrnSet,
                    masterDefendantSet,
                    linkedCaseUrnSet,
                    caseUrnForLinkedCases,
                    now()
            );
        } else {
            hearings = repository.findHearings(
                    allocated,
                    jurisdictionTypeSet,
                    hearingId,
                    caseUrnSet,
                    masterDefendantSet,
                    linkedCaseUrnSet,
                    caseUrnForLinkedCases,
                    now()
            );
        }

        final List<Notes> notes = notesService.findNotes(true, null, null, hearings);

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                        .add(NOTES, listToJsonArrayConverter.convert(notes)));
    }

    @Handles("listing.range.search.hearings.for.judge.list")
    public JsonEnvelope rangeSearchHearingsForJudge(final JsonEnvelope query) {
        return rangeSearchQuery.rangeSearchHearingsForJudgeList(query);
    }

    @Handles("listing.any-allocation.search.hearings")
    public Envelope<JsonObject> searchHearingsWithAnyAllocationState(final JsonEnvelope query) {
        final String caseUrnQueryParam = query.payloadAsJsonObject().getString(CASE_URN).toUpperCase();

        LOGGER.info("\n Query params - caseUrn : {} ", caseUrnQueryParam);

        final List<Hearing> hearings = repository.findHearingsByCaseUrnAndAnyAllocationState(caseUrnQueryParam);

        return Enveloper.envelop(createObjectBuilder()
                .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                .build()).withName("listing.any-allocation.search.hearings").withMetadataFrom(query);
    }

    @Handles("listing.range.search.hearings")
    public JsonEnvelope rangeSearchHearings(final JsonEnvelope query) {
        return rangeSearchQuery.rangeSearchHearings(query);
    }

    public JsonEnvelope rangeSearchHearingsForCourtCalendar(final JsonEnvelope query) {
        return rangeSearchQuery.rangeSearchCourtCalendar(query);
    }

    public JsonEnvelope searchHearingsForCotr(final JsonEnvelope query) {
        return rangeSearchQuery.searchHearingsForCotr(query);
    }

    @Handles("listing.search.court.list")
    public JsonEnvelope getCourtListContent(final JsonEnvelope query) {

        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final LocalDate startDate = ofNullable(query.payloadAsJsonObject().getString(START_DATE, null)).map(LocalDate::parse).orElse(null);
        final LocalDate endDate = ofNullable(query.payloadAsJsonObject().getString(END_DATE, null)).map(LocalDate::parse).orElse(null);
        final String listId = query.payloadAsJsonObject().getString(LIST_ID);
        LOGGER.info("Parameters -  " +
                        COURT_CENTRE_ID + " : {}, " +
                        COURT_ROOM_ID + " : {}, " +
                        START_DATE + " : {}, " +
                        END_DATE + " : {}, " +
                        LIST_ID + " : {}, ",
                courtCentreId, courtRoomId, startDate, endDate, listId);
        final Optional<CourtListType> listType = valueFor(listId);
        if (!listType.isPresent()) {
            LOGGER.error("Supplied CourtList type is not valid {} ", listId);
            return createEmptyResponse(query);
        }

        if (listType.get().equals(CourtListType.ALPHABETICAL)) {
            final List<Hearing> matchedHearings = repository.findHearingsForAlphabeticalList(TRUE, courtCentreId, startDate);
            return createAlphabeticalListJsonEnvelope(query, matchedHearings);
        }
        // Plug in queries for other list types
        final Hearing matchedHearingsJsonObject = repository.findHearingsForPublicStandardList(TRUE, courtCentreId, startDate, endDate);
        return createPublicStandardCourtListJsonEnvelope(query, matchedHearingsJsonObject);
    }

    @Handles("listing.courtlist")
    public JsonEnvelope retrieveCourtList(final JsonEnvelope queryEnvelope) {

        final JsonObject queryPayload = queryEnvelope.payloadAsJsonObject();

        final JsonObject courtListResponsePayload = (queryPayload.containsKey("published") &&
                queryPayload.getBoolean("published"))
                ? getPublishedCourtListResponsePayload(queryPayload)
                : getUnpublishedCourtListResponsePayload(queryEnvelope, queryPayload);

        return envelopeFrom(metadataFrom(queryEnvelope.metadata()).withName("listing.courtlist"), courtListResponsePayload);
    }

    public JsonEnvelope getCasesByDefendantAndHearingDate(List<UUID> caseIds, List<UUID> defendants, String hearingDate, final JsonEnvelope query) {
        final List<CaseByDefendant> caseList = caseByDefendantRepository.getCasesByDefendantAndHearingDate(caseIds, defendants, parse(hearingDate));
        return envelopeFrom(query.metadata(), createCasesByDefendantAndHearingDateResponse(caseList));
    }

    private JsonObject createCasesByDefendantAndHearingDateResponse(final List<CaseByDefendant> caseList){
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        caseList.forEach(prosecutionCase->{
            final JsonObjectBuilder objectBuilder = createObjectBuilder();
            objectBuilder.add(CASE_ID, prosecutionCase.getCaseId().toString());
            if (nonNull(prosecutionCase.getUrn())) {
                objectBuilder.add(URN, prosecutionCase.getUrn());
            }
            arrayBuilder.add(objectBuilder.build());
        });
        return createObjectBuilder().add(PROSECUTION_CASES, arrayBuilder.build()).build();
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

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.publishedcourtlist"),
                publishedCourtListToJsonConverter.convert(publishedCourtLists));
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
        final LocalDate publishDate = from(query.payloadAsJsonObject().getString(PUBLISH_DATE));
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

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.court.list.publish.status"), createObjectBuilder().add("publishCourtListStatuses", courtListPublishStatuses));
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
        final UUID hearingId = extractUUID(query);
        try {
            final Hearing hearing = repository.findBy(hearingId);
            if (hearing == null) {
                throw new NotFoundException("There is no Hearing for this ID: " + hearingId);
            }
            return envelopeFrom(metadataFrom(query.metadata()).withName(NAME_LISTING_SEARCH_HEARING),
                    HearingToJsonConverter.convert(hearing));
        } catch (NoResultException e) {
            throw new NotFoundException("There is no Hearing for this ID: " + hearingId);
        }
    }

    private UUID extractUUID(final JsonEnvelope query) {
        return fromString(query.payloadAsJsonObject().getString(ID, null));
    }

    private JsonEnvelope createAlphabeticalListJsonEnvelope(final JsonEnvelope query, final List<Hearing> matchedHearings) {
        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.court.list"),
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convertHearingResultForAlphabeticalList(matchedHearings)));
    }

    private JsonEnvelope createPublicStandardCourtListJsonEnvelope(final JsonEnvelope query, final Hearing matchedHearingsJsonObject) {
        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.court.list"),
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convertHearingResultForPublicList(matchedHearingsJsonObject)));
    }

    private JsonEnvelope createEmptyResponse(JsonEnvelope query) {
        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.court.list"),
                createObjectBuilder()
                        .add(HEARINGS, createArrayBuilder().build()));

    }

    private String getDateTimeAsString(final String date, final String time, final String defaultTime) {
        final String copyTime = isNullOrEmpty(time) ? defaultTime : time;
        final LocalDate localDate = parse(date);
        final LocalTime localTime = LocalTime.parse(copyTime);
        return localDate.atTime(localTime).toString();
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
