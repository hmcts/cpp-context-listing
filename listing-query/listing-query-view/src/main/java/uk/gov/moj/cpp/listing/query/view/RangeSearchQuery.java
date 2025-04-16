package uk.gov.moj.cpp.listing.query.view;

import static java.time.LocalDate.parse;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.PANEL_ADULT_YOUTH;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingIdsResponse;
import uk.gov.moj.cpp.listing.domain.JurisdictionType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.Notes;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameter;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameterFactory;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;
import uk.gov.moj.cpp.listing.query.view.service.NotesService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

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

        if (!weekCommencingStartDate.isEmpty() && !LocalDate.parse(weekCommencingStartDate).getDayOfWeek().equals(DayOfWeek.MONDAY)) {
            weekCommencingStartDate = LocalDate.parse(weekCommencingStartDate).minusDays(1).toString();
            logger.info("WeekCommencingStartDate is not Monday, hence its being adjusted to previous day [{}]  ", weekCommencingStartDate);
        }

        Optional<Boolean> possibleDisqualificationOpt = Optional.empty();
        if (nonNull(query.payloadAsJsonObject().get(POSSIBLE_DISQUALIFICATION_QUERY_PARAMETER))) {
            possibleDisqualificationOpt = Optional.of(query.payloadAsJsonObject().getBoolean(POSSIBLE_DISQUALIFICATION_QUERY_PARAMETER));
        }

        logger.info("Query params -  " +
                        "allocated: {}, " +
                        "courtCentreId: {}, " +
                        "ouCode: {}, " +
                        "courtRoomId: {}, " +
                        "authorityId: {}, " +
                        "hearingTypeId: {}, " +
                        "jurisdictionType: {}, " +
                        "startDate: {}, " +
                        "endDate: {}, " +
                        "weekCommencingStartDate: {}, " +
                        "weekCommencingEndDate: {}, " +
                        "noPagination : {}, " +
                        "possibleDisqualification : {}, " +
                        "businessType : {}, " +
                        "courtSessionOptional : {}, ",
                allocated, courtCentreId, ouCode, courtRoomId, authorityId, hearingTypeId, jurisdictionType, startDate, endDate, weekCommencingStartDate, weekCommencingEndDate, noPagination, possibleDisqualificationOpt, businessType, courtSessionOptional);

        if (courtSessionOptional.isPresent() || businessType.isPresent()) {

            if(isMags(jurisdictionType) && allocated && ouCode != null ) {
                return getCourtSchedulerHearings(query, allocated, ouCode, courtSessionOptional, courtRoomId, startDate, endDate, businessType, PANEL_ADULT_YOUTH, paginationParameter);
            }

            throw new BadRequestException("courtSession or businessType are only relevant to allocated MAGs with ouCode");
        }


        final List<Hearing> hearings = !weekCommencingStartDate.isEmpty()
                ? findHearingsByWeekCommencingRange(allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, weekCommencingStartDate, weekCommencingEndDate, possibleDisqualificationOpt, paginationParameter.getOffSet(), paginationParameter.getPageSize(), noPagination)
                : findHearings(allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, startDate, endDate, paginationParameter.getOffSet(), paginationParameter.getPageSize(), noPagination);


        final Long totalCount = !(hearings.isEmpty()) ? hearings.get(0).getTotalCount() : 0;

        return buildHearingsResponse(query, allocated, courtRoomId, startDate, hearings, totalCount, paginationParameter);
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

    private JsonEnvelope buildHearingsResponse(final JsonEnvelope query, final boolean allocated, final String courtRoomId, final String startDate, final List<Hearing> hearings, final Long totalCount, final PaginationParameter paginationParameter) {
        final List<Notes> notes = notesService.findNotes(allocated, courtRoomId, startDate, hearings);

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                createObjectBuilder().add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
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


     private JsonEnvelope getCourtSchedulerHearings(final JsonEnvelope query , final boolean allocated, final String ouCode, final Optional<String> courtSessionOptional, final String courtRoomId, final String startDate, final String endDate, final Optional<String> businessType, final String panel, final PaginationParameter paginationParameter) {
        final HearingIdsResponse hearingCourtScheduleIds = courtSchedulerServiceAdapter.getCourtSchedulerHearings(ouCode, courtSessionOptional, courtRoomId, startDate, endDate, businessType, panel, paginationParameter.getPageSize(), paginationParameter.getPageNumber());
        logger.info("CourtScheduler Hearings response : {}", hearingCourtScheduleIds);
        final List<Hearing> soredtedHearingList = hearingCourtScheduleIds.getUuids()==null || hearingCourtScheduleIds.getUuids().isEmpty()? emptyList(): enrichAllCourtSchedulerHearingIdsIntoHearings(hearingCourtScheduleIds.getUuids());
        logger.info("getCourtSchedulerHearings found {} hearings", soredtedHearingList.size());
        return buildHearingsResponse(query, allocated, courtRoomId, startDate,soredtedHearingList, hearingCourtScheduleIds.getResults(), paginationParameter);
    }


    private List<Hearing> enrichAllCourtSchedulerHearingIdsIntoHearings(final List<UUID> hearingIds) {

        final List<Hearing> hearings = repository.findAllCourtSchedulerHearingByIds(hearingIds);
        final HashMap<UUID, Hearing> hearingMap = new HashMap<>();
        hearings.forEach(h -> hearingMap.put(h.getId(), h));

        List<Hearing> sortedHearing = new ArrayList<>();
        hearingIds.forEach(id -> {
            final Hearing fromMap = hearingMap.get(id);
            if(fromMap !=null){
                sortedHearing.add(fromMap);
            }
        });

        return sortedHearing;
    }

    private boolean isMags(final String jurisdictionType) {
        return JurisdictionType.MAGISTRATES.name().equalsIgnoreCase(jurisdictionType);
    }

    private Optional<String> extractCourtSession(final JsonEnvelope query) {
        String courtSession = query.payloadAsJsonObject().getString("courtSession", null);
        if (courtSession != null && "any".equalsIgnoreCase(courtSession.toLowerCase().trim())) {
            courtSession = null;
        }
        return Optional.ofNullable(courtSession);
    }
}
