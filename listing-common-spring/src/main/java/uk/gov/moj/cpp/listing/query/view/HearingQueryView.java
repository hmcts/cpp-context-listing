package uk.gov.moj.cpp.listing.query.view;

import org.springframework.transaction.annotation.Transactional;

import static uk.gov.moj.cpp.listing.query.view.Payloads.payload;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

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
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createArrayBuilder;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.createObjectBuilder;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.contains;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.moj.cpp.listing.domain.json.LocalDates.from;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.getString;
import static uk.gov.moj.cpp.listing.domain.json.JsonObjects.toJsonArray;
import static uk.gov.moj.cpp.listing.domain.CourtListType.valueFor;
import static uk.gov.moj.cpp.listing.query.view.dto.SearchCriteria.MATCHED_DEFENDANTS;

import uk.gov.justice.listing.event.PublishCourtListType;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ListToJsonArrayConverter;
import uk.gov.moj.cpp.listing.domain.json.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.domain.json.ObjectMapperProducer;
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
import uk.gov.moj.cpp.listing.query.view.dto.csv.HearingCsvData;
import uk.gov.moj.cpp.listing.query.view.hearing.ApplicationTypeFilter;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingToJsonConverter;
import uk.gov.moj.cpp.listing.query.view.service.JsonNodeReader;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;
import uk.gov.moj.cpp.listing.query.view.service.csv.HearingCsvReportService;

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

import uk.gov.moj.cpp.listing.domain.json.JsonObjects;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.persistence.NoResultException;
import uk.gov.moj.cpp.listing.query.view.exception.QueryBadRequestException;
import uk.gov.moj.cpp.listing.query.view.exception.QueryNotFoundException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * P2.1: read-only transaction around each query, as the JEE query ran inside the container's
 * transaction: the ported code reads lazy entity collections (listedCases, courtApplications)
 * after the repository call.
 */
@SuppressWarnings({"squid:S1192", "squid:S00107", "squid:S1166"})
@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
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
    private static final UUID NO_MATCH_MASTER_DEFENDANT_ID = new UUID(0, 0);  // nil UUID no-op placeholder: keeps the IN clause valid while matching no rows
    private static final String MATCHED_DEFENDANT_IDS = "matchedDefendantIds";
    private static final String CASE_URN_FOR_LINKED_CASES = "caseUrnForLinkedCases";
    private static final String RETURN_ALL_HEARINGS = "returnAllHearings";
    private static final String PROSECUTION_CASES = "prosecutionCases";
    private static final String URN = "urn";
    private static final String WOFD_HEARING_TYPE_ID = "638ced9d-3f95-4e99-b27b-47fa5a2c6add";
    private static final String PCB_HEARING_TYPE_ID = "3a2d160f-363b-4360-96e1-0007a400a64c";


    private final HearingRepository repository;

    private final CourtApplicationRepository courtApplicationRepository;

    private final PublishedCourtListRepository publishedCourtListRepository;

    private final PublishedCourtListToJsonConverter publishedCourtListToJsonConverter;

    private final CourtListPublishStatusJdbcRepository courtListRepository;

    private final HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    private final RangeSearchQuery rangeSearchQuery;

    private final CourtListService courtListService;


    private final NotesService notesService;

    private final ListToJsonArrayConverter<Notes> listToJsonArrayConverter;

    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final CaseByDefendantRepository caseByDefendantRepository;

    private final StringToJsonObjectConverter stringToJsonObjectConverter;

    private final PaginationParameterFactory paginationParameterFactory;

    private final ApplicationTypeFilter applicationTypeFilter;

    private final HearingCsvReportService hearingCsvReportService;

    public static final String TYPE = "type";

    public static final String LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS = "listing.allocated.and.unallocated.hearings";


    public JsonObject searchHearings(final JsonObject query, final UUID userId) {
        final boolean allocated = query.getBoolean(ALLOCATED_QUERY_PARAMETER);
        final String courtCentreId = query.getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.getString(COURT_ROOM_ID, null);
        final String authorityId = query.getString(AUTHORITY_ID, null);
        final String hearingTypeId = query.getString(HEARING_TYPE, null);
        final String jurisdictionType = getString(query, JURISDICTION_TYPE).orElse(null);
        final String searchDate = query.getString(SEARCH_DATE);
        final String startTime = getDateTimeAsString(searchDate, query.getString(START_TIME, MIN.toString()), MIN.toString());
        final String endTime = getDateTimeAsString(searchDate, query.getString(END_TIME, MAX.toString()), MAX.toString());
        final String startTimeForMatched = getDateTimeAsString(searchDate, query.getString(START_TIME, LocalTime.now().toString()), LocalTime.now().toString());

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

        final List<Hearing> filteredHearings = applicationTypeFilter.filter(userId, hearings);


        LOGGER.info("number of records from query -  {}", filteredHearings.size());

        final List<Notes> notes = notesService.findNotes(allocated, courtRoomId, searchDate, filteredHearings);

        return payload(createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convertForSearchHearing(filteredHearings,
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

    public JsonObject searchUnscheduledHearings(final JsonObject query) {
        final String typeOfListQueryParam = query.getString(TYPE_OF_LIST, null);
        final String caseUrnQueryParam = query.getString(CASE_URN, null);
        final String courtCentreIdQueryParam = query.getString(COURT_CENTRE_IDS, null);
        final PaginationParameter paginationParameter = paginationParameterFactory.newPaginationParameter(query);

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
        return createObjectBuilder()
                .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                .add("results", totalCount)
                .add("pageCount", toPageCount(totalCount, paginationParameter.getPageSize()))
                .build();
    }

    private long toPageCount(final long totalCount, final Integer pageSize) {
        return (long) Math.ceil((double) totalCount / (double) pageSize) ;
    }

    public JsonObject searchAllocatedAndUnallocatedHearings(final JsonObject query) {
        LOGGER.info("Event: {}", LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS);
        final List<Hearing> hearings;
        final String caseIdQueryParam = query.getString(CASE_ID, null);
        final String applicationIdQueryParam = query.getString(APPLICATION_ID, null);
        LOGGER.info("Event: {}, Case Id: {}, Application Id: {}", LISTING_ALLOCATED_AND_UNALLOCATED_HEARINGS, caseIdQueryParam, applicationIdQueryParam);
        if (caseIdQueryParam == null && applicationIdQueryParam == null) {
            return payload(createObjectBuilder()
                            .add(HEARINGS, JsonObjects.createArrayBuilder().build())
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

        return payload(createObjectBuilder()
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

    public JsonObject searchAvailableHearings(final JsonObject query) throws IOException {
        final String hearingId = query.getString(HEARING_ID, null);
        final String caseUrnsQueryParam = query.getString(CASE_URN, null);
        final String searchCriteriaQueryParam = query.getString(SEARCH_CRITERIA, null);
        final String jurisdictionType = query.getString(JURISDICTION_TYPE, null);
        final String matchingDefendantIdsParam = query.getString(MATCHED_DEFENDANT_IDS, null);
        final String caseUrnForLinkedCases = query.getString(CASE_URN_FOR_LINKED_CASES, null);
        final boolean allocated = true;
        final Boolean returnAllHearings = query.getBoolean(RETURN_ALL_HEARINGS, false);

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
                    toMasterDefendantUuidSet(masterDefendantSet),
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

        return payload(createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                        .add(NOTES, listToJsonArrayConverter.convert(notes)));
    }

    public JsonObject rangeSearchHearingsForJudge(final JsonObject query) {
        return rangeSearchQuery.rangeSearchHearingsForJudgeList(query);
    }

    public JsonObject searchHearingsWithAnyAllocationState(final JsonObject query) {
        final String caseUrnQueryParam = query.getString(CASE_URN).toUpperCase();
        final String startDateQueryParam = query.getString(START_DATE);
        final LocalDate startDate = LocalDate.parse(startDateQueryParam);

        LOGGER.info("\n Query params - caseUrn : {}, startDate: {} ", caseUrnQueryParam, startDate);

        final List<Hearing> hearings = repository.findHearingsByCaseUrnAndAnyAllocationState(caseUrnQueryParam, startDate);

        return createObjectBuilder()
                .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                .build();
    }

    public JsonObject rangeSearchHearings(final JsonObject query, final UUID userId) {
        final JsonObject responseEnvelope = rangeSearchQuery.rangeSearchHearings(query);
        return filterHearingsBasedOnApplicationType(userId, responseEnvelope);

    }

    public JsonObject rangeSearchHearingsForCourtCalendar(final JsonObject query, final UUID userId) {
        final JsonObject responseEnvelope =  rangeSearchQuery.rangeSearchCourtCalendar(query);
        return filterHearingsBasedOnApplicationType(userId, responseEnvelope);
    }

    private JsonObject filterHearingsBasedOnApplicationType(final UUID userId, final JsonObject responseEnvelope) {
        final JsonObject responsePayload = responseEnvelope;

        if (!responsePayload.containsKey(HEARINGS) || responsePayload.getJsonArray(HEARINGS).isEmpty()) {
            return responseEnvelope;
        }

        final JsonArray hearingsArray = responsePayload.getJsonArray(HEARINGS);

        final JsonArray filteredHearingsArray = applicationTypeFilter.filter(userId, hearingsArray);

        // Rebuild the response envelope with filtered hearings, keeping other fields unchanged
        final JsonObjectBuilder responseBuilder = createObjectBuilder();

        // Copy all existing fields from the original response
        responsePayload.forEach((key, value) -> {
            if (!HEARINGS.equals(key)) {
                responseBuilder.add(key, value);
            }
        });

        responseBuilder.add(HEARINGS, filteredHearingsArray);

        return payload(responseBuilder.build());
    }

    public JsonObject searchHearingsForCotr(final JsonObject query) {
        return rangeSearchQuery.searchHearingsForCotr(query);
    }

    public JsonObject getCourtListContent(final JsonObject query) {

        final String courtCentreId = query.getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.getString(COURT_ROOM_ID, null);
        final LocalDate startDate = ofNullable(query.getString(START_DATE, null)).map(LocalDate::parse).orElse(null);
        final LocalDate endDate = ofNullable(query.getString(END_DATE, null)).map(LocalDate::parse).orElse(null);
        final String listId = query.getString(LIST_ID);
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

        Set<String> excludedHearingTypeIds = Set.of(WOFD_HEARING_TYPE_ID, PCB_HEARING_TYPE_ID);

        if (listType.get().equals(CourtListType.ALPHABETICAL)) {
            final List<Hearing> matchedHearings = repository.findHearingsForAlphabeticalList(TRUE, courtCentreId, startDate, excludedHearingTypeIds);
            return createAlphabeticalListJsonEnvelope(query, matchedHearings);
        }
        // Plug in queries for other list types
        final Hearing matchedHearingsJsonObject = repository.findHearingsForPublicStandardList(TRUE, courtCentreId, startDate, endDate, excludedHearingTypeIds);
        return createPublicStandardCourtListJsonEnvelope(query, matchedHearingsJsonObject);
    }

    public JsonObject retrieveCourtList(final JsonObject queryEnvelope) {

        final JsonObject queryPayload = queryEnvelope;

        final JsonObject courtListResponsePayload = (queryPayload.containsKey("published") &&
                queryPayload.getBoolean("published"))
                ? getPublishedCourtListResponsePayload(queryPayload)
                : getUnpublishedCourtListResponsePayload(queryPayload);

        return payload(courtListResponsePayload);
    }

    public JsonObject getCasesByDefendantAndHearingDate(List<UUID> caseIds, List<UUID> defendants, String hearingDate) {
        final List<CaseByDefendant> caseList = caseByDefendantRepository.getCasesByDefendantAndHearingDate(caseIds, defendants, parse(hearingDate));
        return payload(createCasesByDefendantAndHearingDateResponse(caseList));
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

    private JsonObject getUnpublishedCourtListResponsePayload(final JsonObject queryPayload) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(format("getUnpublishedCourtListResponsePayload payLoad = %s", queryPayload));
        }
        final String endDate = trimToEmpty(queryPayload.getString("endDate", ""));
        return courtListService.retrieveUnPublishedCourtList(
                fromString(queryPayload.getString("courtCentreId")),
                uk.gov.moj.cpp.listing.domain.xhibit.PublishCourtListType.valueOf(queryPayload.getString("publishCourtListType")),
                parse(queryPayload.getString("startDate")),
                StringUtils.isNotBlank(endDate) ? endDate : StringUtils.EMPTY
        );
    }

    @SuppressWarnings("WeakerAccess")
    public JsonObject getPublishedCourtLists(final JsonObject query) {

        final JsonObject queryPayload = query;

        final List<PublishedCourtList> publishedCourtLists = queryPayload.containsKey(COURT_CENTRE_ID)
                ? getPublishedCourtList(queryPayload)
                : publishedCourtListRepository.findAll();

        return payload(publishedCourtListToJsonConverter.convert(publishedCourtLists));
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

    public JsonObject getCourtListPublishStatus(final JsonObject query) {
        final String courtCentreId = query.getString(COURT_CENTRE_ID);
        final String publishCourtListTypes = query.getString(PUBLISH_COURT_LIST_TYPES);
        final LocalDate publishDate = from(query.getString(PUBLISH_DATE));
        final boolean weekCommencing = query.getBoolean(WEEK_COMMENCING, false);

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

        return payload(createObjectBuilder().add("publishCourtListStatuses", courtListPublishStatuses));
    }


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
    public JsonObject getHearingById(final JsonObject query) {
        final UUID hearingId = extractUUID(query);
        try {
            final Hearing hearing = repository.findBy(hearingId);
            if (hearing == null) {
                throw new QueryNotFoundException("There is no Hearing for this ID: " + hearingId);
            }
            return payload(HearingToJsonConverter.convert(hearing));
        } catch (NoResultException e) {
            throw new QueryNotFoundException("There is no Hearing for this ID: " + hearingId);
        }
    }

    private UUID extractUUID(final JsonObject query) {
        return fromString(query.getString(ID, null));
    }

    private JsonObject createAlphabeticalListJsonEnvelope(final JsonObject query, final List<Hearing> matchedHearings) {
        return payload(createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convertHearingResultForAlphabeticalList(matchedHearings)));
    }

    private JsonObject createPublicStandardCourtListJsonEnvelope(final JsonObject query, final Hearing matchedHearingsJsonObject) {
        return payload(createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convertHearingResultForPublicList(matchedHearingsJsonObject)));
    }

    private JsonObject createEmptyResponse(JsonObject query) {
        return payload(createObjectBuilder()
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

    /**
     * Converts the string master defendant id set to UUIDs for the tuned
     * {@code findHearings} overload, which compares against the uuid column directly
     * (no varchar cast). The empty-string no-op placeholder maps to the nil UUID.
     */
    private Set<UUID> toMasterDefendantUuidSet(final Set<String> masterDefendantSet) {
        return masterDefendantSet.stream()
                .map(id -> EMPTY_STRING.equals(id) ? NO_MATCH_MASTER_DEFENDANT_ID : fromString(id))
                .collect(toSet());
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


    public String generateHearingCsvReport(final JsonObject query, final UUID userId) {
        final String courtCentreId = query.getString(COURT_CENTRE_ID);
        final String startDateStr = query.getString(START_DATE);

        LocalDate startDate ;
        try{
            startDate =  LocalDate.parse(startDateStr);
        }catch (Exception e){
            throw new QueryBadRequestException("Invalid start date: " + startDateStr);
        }
        final Integer numberOfWeeks = query.getInt("numberOfWeeks", 2);

        LOGGER.info("Generating CSV report for courtCentreId: {}, startDate: {}, numberOfWeeks: {}",
                   courtCentreId, startDate, numberOfWeeks);

        final List<HearingCsvData> csvData = hearingCsvReportService.findHearingsForCsvReport(courtCentreId, startDate, numberOfWeeks, paginationParameterFactory.getMaxPageSize());

        if (csvData.isEmpty()) {
            LOGGER.warn("No hearing data found for CSV report");
           // return "";
        }

        return hearingCsvReportService.generateCsvContent(csvData, userId);
    }


}
