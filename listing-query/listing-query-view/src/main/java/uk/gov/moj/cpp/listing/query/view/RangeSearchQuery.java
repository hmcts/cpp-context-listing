package uk.gov.moj.cpp.listing.query.view;

import static java.time.LocalDate.parse;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.listing.query.view.dto.PaginationParameterFactory.newPaginationParameter;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameter;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;

@SuppressWarnings({"squid:S1067", "pmd:NullAssignment"})
public class RangeSearchQuery {

    private static final String ALLOCATED_QUERY_PARAMETER = "allocated";
    private static final String COURT_CENTRE_ID = "courtCentreId";
    private static final String COURT_ROOM_ID = "courtRoomId";
    private static final String AUTHORITY_ID = "authorityId";
    private static final String HEARING_TYPE = "hearingTypeId";
    private static final String JURISDICTION_TYPE = "jurisdictionType";
    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final String HEARINGS = "hearings";
    private static final String WEEK_COMMENCING_START_DATE = "weekCommencingStartDate";
    private static final String WEEK_COMMENCING_END_DATE = "weekCommencingEndDate";
    public static final String EARLIEST_SEARCH_DATE = "1900-01-01";
    public static final String LATEST_SEARCH_DATE = "9999-01-01";

    @SuppressWarnings("squid:S1312")
    @Inject
    private Logger logger;

    @Inject
    private HearingRepository repository;

    @Inject
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    @Inject
    private ListToJsonArrayConverter listToJsonArrayConverter;

    public JsonEnvelope rangeSearchHearingsForJudgeList(final JsonEnvelope query) {
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, EARLIEST_SEARCH_DATE);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, LATEST_SEARCH_DATE);

        logger.info("Query params -  " +
                        "courtCentreId: {}, " +
                        "courtRoomId: {}, " +
                        "startDate: {}, " +
                        "endDate: {}, "
                , courtCentreId, courtRoomId, startDate, endDate);

        final List<Hearing> hearings = findHearings(true, courtCentreId, courtRoomId, authorityId, null, null, startDate, endDate);

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.range.search.hearings.for.judge"),
                createObjectBuilder().add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings)));
    }


    public JsonEnvelope rangeSearchHearings(final JsonEnvelope query) {
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER, false);
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String hearingTypeId = query.payloadAsJsonObject().getString(HEARING_TYPE, null);
        final String jurisdictionType = query.payloadAsJsonObject().getString(JURISDICTION_TYPE, null);
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, EARLIEST_SEARCH_DATE);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, LATEST_SEARCH_DATE);
        final String weekCommencingStartDate = trimToEmpty(query.payloadAsJsonObject().getString(WEEK_COMMENCING_START_DATE, null));
        final String weekCommencingEndDate = trimToEmpty(query.payloadAsJsonObject().getString(WEEK_COMMENCING_END_DATE, null));
        final PaginationParameter paginationParameter = newPaginationParameter(query.payloadAsJsonObject());
        final boolean noPagination = query.payloadAsJsonObject().getBoolean("noPagination", false);

        logger.info("Query params -  " +
                        "allocated: {}, " +
                        "courtCentreId: {}, " +
                        "courtRoomId: {}, " +
                        "authorityId: {}, " +
                        "hearingTypeId: {}, " +
                        "jurisdictionType: {}, " +
                        "startDate: {}, " +
                        "endDate: {}, " +
                        "weekCommencingStartDate: {}, " +
                        "weekCommencingEndDate: {}, "
                ,
                allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, startDate, endDate, weekCommencingStartDate, weekCommencingEndDate);

        final List<Hearing> hearings = !weekCommencingStartDate.isEmpty() ?
                findHearingsByWeekCommencingRange(allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, weekCommencingStartDate, weekCommencingEndDate, paginationParameter.getOffSet(), paginationParameter.getPageSize()) :
                findHearings(allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, startDate, endDate, paginationParameter.getOffSet(), paginationParameter.getPageSize(), noPagination);

        final Long totalCount = !(hearings.isEmpty()) ? hearings.get(0).getTotalCount() : 0;
        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                createObjectBuilder().add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                        .add("results", totalCount)
                        .add("pageCount", toPageCount(totalCount, paginationParameter.getPageSize())));
    }

    private long toPageCount(final long totalCount, final Integer pageSize) {
        return totalCount != 0 ? (long) Math.ceil((double) totalCount / (double) pageSize) : 0;
    }

    @SuppressWarnings("squid:S00107")
    private List<Hearing> findHearingsByWeekCommencingRange(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityId, final String hearingTypeId, final String jurisdictionType, final String weekCommencingDate, final String weekCommencingEndDate, final Integer offSet, final Integer pageSize) {
        return isFalse(allocated) ?
                getUnallocatedHearingsByWeekCommencingRange(allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, offSet, pageSize) :
                getHearingsByWeekCommencingRange(courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, weekCommencingDate, weekCommencingEndDate, offSet, pageSize);
    }

    private List<Hearing> getUnallocatedHearingsByWeekCommencingRange(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityId, final String hearingTypeId, final String jurisdictionType, final Integer offSet, final Integer pageSize) {
        return repository.findUnallocatedHearingsByWeekCommencingRange(
                courtCentreId,
                courtRoomId,
                authorityId,
                hearingTypeId,
                jurisdictionType,
                parse(EARLIEST_SEARCH_DATE),
                parse(LATEST_SEARCH_DATE),
                allocated,
                offSet,
                pageSize);
    }

    private List<Hearing> getHearingsByWeekCommencingRange(final String courtCentreId, final String courtRoomId, final String authorityId, final String hearingTypeId, final String jurisdictionType, final String weekCommencingDate, final String weekCommencingEndDate, final Integer offSet, final Integer pageSize) {
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
    private List<Hearing> findHearings(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityId, final String hearingTypeId, final String jurisdictionType, final String startDate, final String endDate, final Integer offSet, final Integer pageSize, final boolean noPagination) {
        if(noPagination){
            return repository.findHearingsWithNoPagination(
                    String.valueOf(allocated),
                    courtCentreId,
                    courtRoomId,
                    authorityId,
                    hearingTypeId,
                    ofNullable(jurisdictionType).orElse(null),
                    parse(startDate),
                    parse(endDate));
        }
        return repository.findHearings(
                String.valueOf(allocated),
                courtCentreId,
                courtRoomId,
                authorityId,
                hearingTypeId,
                ofNullable(jurisdictionType).orElse(null),
                parse(startDate),
                parse(endDate), offSet, pageSize
        );
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
}
