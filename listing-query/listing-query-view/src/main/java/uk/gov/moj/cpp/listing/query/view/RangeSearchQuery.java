package uk.gov.moj.cpp.listing.query.view;

import static java.lang.Math.toIntExact;
import static java.lang.String.format;
import static java.time.LocalDate.parse;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.EXACT_HEARING_START_DATETIME;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.PANEL_ADULT_YOUTH;
import static uk.gov.moj.cpp.listing.common.service.HearingIdsResponse.EMPTY_HEARING_ID_RESPONSE;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingIdsResponse;
import uk.gov.moj.cpp.listing.common.service.IdResponse;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameter;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameterFactory;
import uk.gov.moj.cpp.listing.query.view.dto.RangeSearchQueryParams;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;

@SuppressWarnings({"squid:S1067", "pmd:NullAssignment", "java:S107"})
public class RangeSearchQuery {

    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String AUTHORITY_ID = "authorityId";
    private static final String HEARING_TYPE = "hearingTypeId";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String OU_CODE = "ouCode";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String HEARINGS = "hearings";
    private static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";
    private static final String EARLIEST_SEARCH_DATE = "1900-01-01";
    private static final String LATEST_SEARCH_DATE = "9999-01-01";
    private static final String TRIAL_HEARING_TYPE_ID = "bf8155e1-90b9-4080-b133-bfbad895d6e4";
    private static final Set<String> hearingTypeIds = new HashSet<>(List.of(TRIAL_HEARING_TYPE_ID));
    private static final String POSSIBLE_DISQUALIFICATION_QUERY_PARAMETER = "possibleDisqualification";
    private static final String STATUS_FINAL = "FINAL";
    private static final String STATUS_DRAFT = "DRAFT";
    static final String COURT_SESSION_OR_BUSINESS_TYPE_UNALLOCATED_NOT_CROWN = "courtSession or businessType can only filter unallocated hearings when jurisdictionType is CROWN";
    static final String COURT_SESSION_OR_BUSINESS_TYPE_WITHOUT_OU_CODE = "courtSession or businessType require ouCode";
    static final String COURT_SESSION_OR_BUSINESS_TYPE_UNALLOCATED_WINDOW_TOO_LARGE = "courtSession or businessType search matches %d unallocated hearing days, more than the %d allowed; narrow the date window";

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private HearingRepository repository;

    @Inject
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    @Inject
    private NotesService notesService;

    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Inject
    private PaginationParameterFactory paginationParameterFactory;

    public JsonEnvelope rangeSearchHearingsForJudgeList(final JsonEnvelope query) {
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, EARLIEST_SEARCH_DATE);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, LATEST_SEARCH_DATE);

        logger.info("Query params -  courtCentreId: {}, courtRoomId: {}, startDate: {}, endDate: {} ", courtCentreId, courtRoomId, startDate, endDate);

        final List<Hearing> hearings = findHearings(true, courtCentreId, courtRoomId, authorityId, null, null, startDate, endDate);

        final List<Hearing> allocatedHearings = hearings.stream()
                .filter(hearing -> nonNull(hearing.getAllocated()))
                .filter(hearing -> hearing.getAllocated().equals(true))
                .collect(toList());

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.range.search.hearings.for.judge"),
                createObjectBuilder().add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(allocatedHearings)));
    }


    public JsonEnvelope rangeSearchHearings(final JsonEnvelope query) {
        RangeSearchQueryParams params = rangeQueryParams(query);

        if (!params.weekCommencingStartDate().isEmpty() && !LocalDate.parse(params.weekCommencingStartDate()).getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            final String dayBeforeString = parse(params.weekCommencingStartDate()).minusDays(1).toString();
            params = params.withWeekCommencingStartDate(dayBeforeString);
            logger.info("WeekCommencingStartDate is not Monday, hence its being adjusted to previous day [{}]  ", dayBeforeString);
        }

        if (params.courtSessionOptional().isPresent() || params.businessType().isPresent()) {

            if(isMags(params.jurisdictionType()) && params.allocated() && params.ouCode() != null ) {
                return getCourtSchedulerHearings(query, params.allocated(), params.ouCode(), params.courtSessionOptional(), params.courtRoomId(), params.startDate(), params.endDate(), params.exactHearingStartDateTime(), params.businessType(), Optional.ofNullable(params.jurisdictionType()), null, null, PANEL_ADULT_YOUTH, params.paginationParameter());
            }

            throw new BadRequestException("courtSession or businessType are only relevant to allocated MAGs with ouCode");
        }


        final List<Hearing> hearings = !params.weekCommencingStartDate().isEmpty()
                ? findHearingsByWeekCommencingRange(params.allocated(), params.courtCentreId(), params.courtRoomId(), params.authorityId(), params.hearingTypeId(), params.jurisdictionType(), params.weekCommencingStartDate(), params.weekCommencingEndDate(), params.possibleDisqualificationOpt(), params.paginationParameter().getOffSet(), params.paginationParameter().getPageSize(), params.noPagination())
                : findHearings(params.allocated(), params.courtCentreId(), params.courtRoomId(), params.authorityId(), params.hearingTypeId(), params.jurisdictionType(), params.startDate(), params.endDate(), params.paginationParameter().getOffSet(), params.paginationParameter().getPageSize(), params.noPagination());


        final Long totalCount = !(hearings.isEmpty()) ? hearings.get(0).getTotalCount() : 0;

        return buildHearingsResponse(query, params.allocated(), params.courtRoomId(), params.startDate(), hearings, totalCount, EMPTY_HEARING_ID_RESPONSE, params.paginationParameter());
    }



    public JsonEnvelope searchHearingsForCotr(final JsonEnvelope query) {
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, EARLIEST_SEARCH_DATE);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, LATEST_SEARCH_DATE);

        logger.info("Query params -  " +
                        "courtCentreId: {}, " +
                        "startDate: {}, " +
                        "endDate: {} ",
                courtCentreId, startDate, endDate);

        final List<Hearing> hearings = findHearingsForCotr(hearingTypeIds, courtCentreId, startDate, endDate);

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                createObjectBuilder().add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings)));
    }

    private JsonEnvelope buildHearingsResponse(final JsonEnvelope query,
                                               final boolean allocated,
                                               final String courtRoomId,
                                               final String startDate,
                                               final List<Hearing> hearings,
                                               final Long totalCount,
                                               final HearingIdsResponse hearingIdsResponse,
                                               final PaginationParameter paginationParameter) {
        final List<Notes> notes = notesService.findNotes(allocated, courtRoomId, startDate, hearings);

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                createObjectBuilder().add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings, hearingIdsResponse))
                        .add("notes", listToJsonArrayConverter.convert(notes))
                        .add("results", totalCount)
                        .add("pageCount", toPageCount(totalCount, paginationParameter.getPageSize())));
    }

    private long toPageCount(final long totalCount, final Integer pageSize) {
        return totalCount != 0 ? (long) Math.ceil((double) totalCount / (double) pageSize) : 0;
    }

    @SuppressWarnings("squid:S00107")
    private List<Hearing> findHearingsByWeekCommencingRange(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityId, final String hearingTypeId,
                                                            final String jurisdictionType, final String weekCommencingDate, final String weekCommencingEndDate,
                                                            final Optional<Boolean> possibleDisqualificationOpt, final Integer offSet, final Integer pageSize, boolean noPagination) {
        if (isFalse(allocated)) {
            if (possibleDisqualificationOpt.isPresent() && isTrue(possibleDisqualificationOpt.get())) {
                return getUnallocatedHearingsByWeekCommencingRangeAndPossibleDisqualification(allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, offSet, weekCommencingDate, weekCommencingEndDate, possibleDisqualificationOpt.get(), pageSize);
            }

            return getUnallocatedHearingsByWeekCommencingRange(allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, offSet, weekCommencingDate, weekCommencingEndDate, pageSize);
        } else {
            return getHearingsByWeekCommencingRange(courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, weekCommencingDate, weekCommencingEndDate, offSet, pageSize, noPagination);
        }
    }

    private List<Hearing> getUnallocatedHearingsByWeekCommencingRange(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityId, final String hearingTypeId, final String jurisdictionType, final Integer offSet, final String weekCommencingStartDate, final String weekCommencingEndDate, final Integer pageSize) {
        return repository.findUnallocatedHearingsByWeekCommencingRange(
                getUUID(courtCentreId),
                getUUID(courtRoomId),
                getUUID(authorityId),
                getUUID(hearingTypeId),
                jurisdictionType,
                isNotBlank(weekCommencingStartDate) ? parse(weekCommencingStartDate) : parse(EARLIEST_SEARCH_DATE),
                isNotBlank(weekCommencingEndDate) ? parse(weekCommencingEndDate) : parse(LATEST_SEARCH_DATE),
                allocated,
                offSet,
                pageSize);
    }

    private UUID getUUID(String str) {
        return str == null ? null : UUID.fromString(str);
    }

    private List<Hearing> getUnallocatedHearingsByWeekCommencingRangeAndPossibleDisqualification(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityId,
                                                                                                 final String hearingTypeId, final String jurisdictionType, final Integer offSet, final String weekCommencingStartDate,
                                                                                                 final String weekCommencingEndDate, final boolean possibleDisqualification, final Integer pageSize) {
        return repository.findUnallocatedHearingsByWeekCommencingRangeAndPossibleDisqualification(
                courtCentreId,
                courtRoomId,
                authorityId,
                hearingTypeId,
                jurisdictionType,
                isNotBlank(weekCommencingStartDate) ? parse(weekCommencingStartDate) : parse(EARLIEST_SEARCH_DATE),
                isNotBlank(weekCommencingEndDate) ? parse(weekCommencingEndDate) : parse(LATEST_SEARCH_DATE),
                allocated,
                possibleDisqualification,
                offSet,
                pageSize);
    }

    private List<Hearing> getHearingsByWeekCommencingRange(final String courtCentreId, final String courtRoomId, final String authorityId, final String hearingTypeId, final String jurisdictionType, final String weekCommencingDate, final String weekCommencingEndDate, final Integer offSet, final Integer pageSize, boolean noPagination) {
        if (noPagination) {
            return repository.findHearingsByWeekCommencingRangeWithNoPagination(
                    courtCentreId,
                    courtRoomId,
                    authorityId,
                    hearingTypeId,
                    jurisdictionType,
                    isNotBlank(weekCommencingDate) ? parse(weekCommencingDate) : null,
                    isNotBlank(weekCommencingEndDate) ? parse(weekCommencingEndDate) : null
            );
        }
        return repository.findHearingsByWeekCommencingRange(
                courtCentreId,
                courtRoomId,
                authorityId,
                hearingTypeId,
                jurisdictionType,
                isNotBlank(weekCommencingDate) ? parse(weekCommencingDate) : null,
                isNotBlank(weekCommencingEndDate) ? parse(weekCommencingEndDate) : null,
                offSet,
                pageSize);
    }

    @SuppressWarnings("squid:S00107")
    private List<Hearing> findHearings(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityId,
                                       final String hearingTypeId, final String jurisdictionType, final String startDate, final String endDate,
                                       final Integer offSet, final Integer pageSize, final boolean noPagination) {
        if (noPagination) {
            final List<Hearing> hearings = repository.findHearings(
                    String.valueOf(allocated),
                    courtCentreId,
                    courtRoomId,
                    authorityId,
                    hearingTypeId,
                    ofNullable(jurisdictionType).orElse(null),
                    parse(startDate),
                    parse(endDate));
            return hearings.stream()
                    .filter(hearing -> nonNull(hearing.getAllocated()))
                    .filter(hearing -> hearing.getAllocated().equals(allocated))
                    .collect(toList());

        } else {
            final List<Hearing> hearings = repository.findHearings(
                    allocated,
                    getUUID(courtCentreId),
                    getUUID(courtRoomId),
                    getUUID(authorityId),
                    getUUID(hearingTypeId),
                    ofNullable(jurisdictionType).orElse(null),
                    parse(startDate),
                    parse(endDate), offSet, pageSize);
            return hearings.stream()
                    .filter(hearing -> nonNull(hearing.getAllocated()))
                    .filter(hearing -> hearing.getAllocated().equals(allocated))
                    .collect(toList());
        }

    }

    private List<Hearing> findHearingsForCotr(final Set<String> hearingTypeIds, final String courtCentreId, final String startDate, final String endDate) {
        return repository.findHearingsForCotr(hearingTypeIds, courtCentreId, parse(startDate), parse(endDate));
    }

    @SuppressWarnings("squid:S00107")
    private List<Hearing> findHearings(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityId, final String hearingTypeId, final String jurisdictionType, final String startDate, final String endDate) {
        return repository.findHearings(
                String.valueOf(allocated),
                courtCentreId,
                courtRoomId,
                authorityId,
                hearingTypeId,
                ofNullable(jurisdictionType).orElse(null),
                parse(startDate),
                parse(endDate));
    }


     private JsonEnvelope getCourtSchedulerHearings(final JsonEnvelope query, final boolean allocated, final String ouCode, final Optional<String> courtSessionOptional, final String courtRoomId, final String startDate, final String endDate, final Optional<Instant> startDateTime, final Optional<String> businessType, final Optional<String> jurisdiction, final String status, final String hearingTypeId, final String panel, final PaginationParameter paginationParameter) {
        final HearingIdsResponse hearingIdsResponse = courtSchedulerServiceAdapter.getCourtSchedulerHearings(ouCode, courtSessionOptional, courtRoomId, startDate, endDate, startDateTime, businessType, jurisdiction, status, panel, paginationParameter.getPageSize(), paginationParameter.getPageNumber());
        logger.info("CourtScheduler Hearings response : {}", hearingIdsResponse);
        // hearingType is held only in the listing viewstore (not courtscheduler), so it is filtered
        // here during enrichment. Draft/final selection is applied courtscheduler-side via the status
        // filter, so no allocated/draft filter is applied here. Pagination and the result count are
        // left exactly as before: the caller's pageSize is forwarded to courtscheduler and its results
        // count is passed straight through.
        final List<Hearing> enrichedHearingList = isEmpty(hearingIdsResponse.getUuids())
                ? emptyList() : enrichAllCourtSchedulerHearingIdsIntoHearings(hearingIdsResponse.getUuids(), hearingTypeId);
        logger.info("getCourtSchedulerHearings found {} hearings", hearingIdsResponse.getResults());
        return buildHearingsResponse(query, allocated, courtRoomId, startDate, enrichedHearingList, hearingIdsResponse.getResults(), hearingIdsResponse, paginationParameter);
    }


    private List<Hearing> enrichAllCourtSchedulerHearingIdsIntoHearings(final List<IdResponse> hearingIds, final String hearingTypeId) {

        // De-duplicate hearing ids before hitting the listing viewstore: a multi-day hearing arrives
        // as one IdResponse per day, all sharing the same hearingId.
        final List<UUID> distinctHearingIds = hearingIds.stream().map(IdResponse::hearingId).distinct().toList();
        final List<Hearing> hearings = repository.findAllCourtSchedulerHearingByIds(distinctHearingIds);
        final HashMap<UUID, Hearing> hearingsById = new HashMap<>();
        hearings.forEach(h -> hearingsById.put(h.getId(), h));

        final List<Hearing> enrichedHearings = new ArrayList<>();
        hearingIds.forEach(id -> {
            final Hearing base = hearingsById.get(id.hearingId());
            // hearingType is not known to courtscheduler, so it is filtered listing-side against the
            // viewstore. Draft/allocated filtering is handled courtscheduler-side (the isDraft flag).
            if (base != null && matchesHearingType(base, hearingTypeId)) {
                // Give each hearing-day its own Hearing (deep-copied properties) so a multi-day
                // hearing renders one distinct row per day rather than collapsing onto the last day.
                final Hearing perDay = new Hearing(base);
                perDay.setHearingDate(id.hearingDate());
                perDay.setHearingDayCount(id.hearingDayCount());
                perDay.setHearingDayPosition(id.hearingDayPosition());
                enrichedHearings.add(perDay);
            }
        });

        return enrichedHearings;
    }

    private static boolean matchesHearingType(final Hearing hearing, final String hearingTypeId) {
        return hearingTypeId == null
                || (hearing.getTypeId() != null && hearingTypeId.equals(hearing.getTypeId().toString()));
    }

    private boolean isMags(final String jurisdictionType) {
        return JurisdictionType.MAGISTRATES.name().equalsIgnoreCase(jurisdictionType);
    }

    private boolean isCrown(final String jurisdictionType) {
        return JurisdictionType.CROWN.name().equalsIgnoreCase(jurisdictionType);
    }

    private Optional<String> extractCourtSession(final JsonEnvelope query) {
        String courtSession = query.payloadAsJsonObject().getString("courtSession", null);
        if (courtSession != null && "any".equalsIgnoreCase(courtSession.toLowerCase().trim())) {
            courtSession = null;
        }
        return Optional.ofNullable(courtSession);
    }


    public JsonEnvelope rangeSearchCourtCalendar(final JsonEnvelope query) {
        final RangeSearchQueryParams params = rangeQueryParams(query);

        if (params.courtSessionOptional().isPresent() || params.businessType().isPresent()) {
            return getCourtSchedulerHearingsForCourtCalendar(query, params);
        }

        List<Hearing> hearings ;

        if(!params.weekCommencingStartDate().isEmpty()){
            hearings = findHearingsByWeekCommencingRange(params.allocated(), params.courtCentreId(), params.courtRoomId(), params.authorityId(), params.hearingTypeId(), params.jurisdictionType(), params.weekCommencingStartDate(), params.weekCommencingEndDate(), params.possibleDisqualificationOpt(), params.paginationParameter().getOffSet(), params.paginationParameter().getPageSize(), params.noPagination());
        } else if(params.allocated()){
            final Instant exactHearingStartDateTime = params.exactHearingStartDateTime().orElse(null);
            hearings = findAllocatedHearingsForCourtCalendar(params.courtCentreId(), params.courtRoomId(), params.authorityId(), params.hearingTypeId(), params.jurisdictionType(), params.startDate(), params.endDate(), exactHearingStartDateTime, params.paginationParameter().getOffSet(), params.paginationParameter().getPageSize());
        } else {
            hearings = findHearings(params.allocated(), params.courtCentreId(), params.courtRoomId(), params.authorityId(), params.hearingTypeId(), params.jurisdictionType(), params.startDate(), params.endDate(), params.paginationParameter().getOffSet(), params.paginationParameter().getPageSize(), params.noPagination());
        }

        final Long totalCount = !(hearings.isEmpty()) ? hearings.get(0).getTotalCount() : 0;

        return buildHearingsResponse(query, params.allocated(), params.courtRoomId(), params.startDate(), hearings, totalCount, EMPTY_HEARING_ID_RESPONSE, params.paginationParameter());
    }

    /**
     * businessType and courtSession are court-schedule session attributes known only to courtscheduler,
     * so a court-calendar search filtered on either is answered from courtscheduler by session status:
     * allocated hearings sit in FINAL sessions, unallocated CROWN hearings in DRAFT sessions. Unallocated
     * MAGS hearings hold no session to filter on, and without ouCode courtscheduler would search every
     * court, so both are rejected rather than silently returning unfiltered results.
     */
    private JsonEnvelope getCourtSchedulerHearingsForCourtCalendar(final JsonEnvelope query, final RangeSearchQueryParams params) {
        if (!params.allocated() && !isCrown(params.jurisdictionType())) {
            throw new BadRequestException(COURT_SESSION_OR_BUSINESS_TYPE_UNALLOCATED_NOT_CROWN);
        }
        if (params.ouCode() == null) {
            throw new BadRequestException(COURT_SESSION_OR_BUSINESS_TYPE_WITHOUT_OU_CODE);
        }

        // The unallocated court calendar searches by week-commencing window rather than startDate/endDate.
        final boolean weekCommencing = !params.weekCommencingStartDate().isEmpty();
        final String startDate = weekCommencing ? params.weekCommencingStartDate() : params.startDate();
        final String endDate = weekCommencing && !params.weekCommencingEndDate().isEmpty() ? params.weekCommencingEndDate() : params.endDate();

        if (params.allocated()) {
            return getCourtSchedulerHearings(query, true, params.ouCode(), params.courtSessionOptional(), params.courtRoomId(), startDate, endDate, params.exactHearingStartDateTime(), params.businessType(), Optional.ofNullable(params.jurisdictionType()), STATUS_FINAL, params.hearingTypeId(), PANEL_ADULT_YOUTH, params.paginationParameter());
        }
        return getUnallocatedCourtSchedulerHearings(query, params, startDate, endDate);
    }

    /**
     * The unallocated court calendar lists one row per hearing carrying all of its hearing days, as the
     * viewstore search does. courtscheduler returns, and pages over, one row per hearing day, so every DRAFT
     * hearing day in the window is fetched, collapsed to distinct hearings in courtscheduler order, and the
     * hearings are counted and paged here. The allocated calendar keeps one row per hearing day. A window
     * holding more DRAFT hearing days than the range-search page size cap is rejected rather than truncated.
     */
    private JsonEnvelope getUnallocatedCourtSchedulerHearings(final JsonEnvelope query, final RangeSearchQueryParams params,
                                                              final String startDate, final String endDate) {
        final PaginationParameter paginationParameter = params.paginationParameter();
        HearingIdsResponse draftHearingDays = getDraftCourtSchedulerHearingDays(params, startDate, endDate, paginationParameter.getPageSize());
        if (draftHearingDays.getResults() > draftHearingDays.getUuids().size()) {
            final long maxHearingDays = paginationParameterFactory.getMaxPageSize();
            if (draftHearingDays.getResults() > maxHearingDays) {
                throw new BadRequestException(format(COURT_SESSION_OR_BUSINESS_TYPE_UNALLOCATED_WINDOW_TOO_LARGE, draftHearingDays.getResults(), maxHearingDays));
            }
            draftHearingDays = getDraftCourtSchedulerHearingDays(params, startDate, endDate, toIntExact(draftHearingDays.getResults()));
        }
        final List<UUID> hearingIds = draftHearingDays.getUuids().stream().map(IdResponse::hearingId).distinct().toList();
        final List<Hearing> hearings = findHearingsInOrder(hearingIds, params.hearingTypeId());
        final List<Hearing> pageOfHearings = hearings.stream()
                .skip(paginationParameter.getOffSet())
                .limit(paginationParameter.getPageSize())
                .toList();
        logger.info("getUnallocatedCourtSchedulerHearings found {} hearings in {} DRAFT hearing days", hearings.size(), draftHearingDays.getResults());
        return buildHearingsResponse(query, false, params.courtRoomId(), startDate, pageOfHearings, (long) hearings.size(), EMPTY_HEARING_ID_RESPONSE, paginationParameter);
    }

    private HearingIdsResponse getDraftCourtSchedulerHearingDays(final RangeSearchQueryParams params, final String startDate, final String endDate, final int pageSize) {
        return courtSchedulerServiceAdapter.getCourtSchedulerHearings(params.ouCode(), params.courtSessionOptional(), params.courtRoomId(), startDate, endDate, params.exactHearingStartDateTime(), params.businessType(), Optional.ofNullable(params.jurisdictionType()), STATUS_DRAFT, PANEL_ADULT_YOUTH, pageSize, 1);
    }

    private List<Hearing> findHearingsInOrder(final List<UUID> hearingIds, final String hearingTypeId) {
        if (hearingIds.isEmpty()) {
            return emptyList();
        }
        final Map<UUID, Hearing> hearingsById = repository.findAllCourtSchedulerHearingByIds(hearingIds).stream()
                .collect(toMap(Hearing::getId, identity(), (first, duplicate) -> first));
        return hearingIds.stream()
                .map(hearingsById::get)
                .filter(Objects::nonNull)
                .filter(hearing -> matchesHearingType(hearing, hearingTypeId))
                .toList();
    }

    private RangeSearchQueryParams rangeQueryParams(final JsonEnvelope query) {
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER, false);
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String hearingTypeId = query.payloadAsJsonObject().getString(HEARING_TYPE, null);
        final String jurisdictionType = query.payloadAsJsonObject().getString(JURISDICTION_TYPE, null);
        final String ouCode = query.payloadAsJsonObject().getString(OU_CODE, null);
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, EARLIEST_SEARCH_DATE);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, LATEST_SEARCH_DATE);
        String weekCommencingStartDate = trimToEmpty(query.payloadAsJsonObject().getString(WEEK_COMMENCING_START_DATE, null));
        final String weekCommencingEndDate = trimToEmpty(query.payloadAsJsonObject().getString(WEEK_COMMENCING_END_DATE, null));
        final PaginationParameter paginationParameter = paginationParameterFactory.newPaginationParameter(query.payloadAsJsonObject());
        final boolean noPagination = query.payloadAsJsonObject().getBoolean("noPagination", false);
        Optional<String> businessType = Optional.ofNullable(query.payloadAsJsonObject().getString("businessType", null));
        Optional<String> courtSessionOptional = extractCourtSession(query);
        Optional<Boolean> possibleDisqualificationOpt = Optional.empty();
        if (nonNull(query.payloadAsJsonObject().get(POSSIBLE_DISQUALIFICATION_QUERY_PARAMETER))) {
            possibleDisqualificationOpt = Optional.of(query.payloadAsJsonObject().getBoolean(POSSIBLE_DISQUALIFICATION_QUERY_PARAMETER));
        }
        Optional<Instant> exactHearingStartDateTimeOpt = Optional.empty();
        String exactHearingStartDateTimeStr = query.payloadAsJsonObject().getString(EXACT_HEARING_START_DATETIME, null);
        if (exactHearingStartDateTimeStr != null) {
            try {
                exactHearingStartDateTimeOpt = Optional.of(Instant.parse(exactHearingStartDateTimeStr));
            } catch (Exception e) {
                logger.warn("Invalid exact hearing startDateTime format: {}. Expected ISO format with UTC timezone (yyyy-MM-ddTHH:mm:ssZ)", exactHearingStartDateTimeStr);
                throw new BadRequestException("Invalid startDateTime format %s".formatted(exactHearingStartDateTimeStr));
            }
        }
        RangeSearchQueryParams params = new RangeSearchQueryParams(allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, ouCode, startDate, endDate, weekCommencingStartDate, weekCommencingEndDate, paginationParameter, noPagination, businessType, courtSessionOptional, possibleDisqualificationOpt, exactHearingStartDateTimeOpt);

        logger.info("Query params: {} ", params);

        return params;
    }

    private List<Hearing> findAllocatedHearingsForCourtCalendar(final String courtCentreId, final String courtRoomId, final String authorityId,
                                       final String hearingTypeId, final String jurisdictionType, final String startDate, final String endDate, final Instant startDateTime,
                                       final Integer offSet, final Integer pageSize) {

            return repository.findAllocatedHearingsForCourtCalendar(
                    getUUID(courtCentreId),
                    getUUID(courtRoomId),
                    getUUID(authorityId),
                    getUUID(hearingTypeId),
                    ofNullable(jurisdictionType).orElse(null),
                    parse(startDate),
                    parse(endDate), startDateTime, offSet, pageSize);
   }
}
