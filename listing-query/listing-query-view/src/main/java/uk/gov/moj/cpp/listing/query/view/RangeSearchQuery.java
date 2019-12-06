package uk.gov.moj.cpp.listing.query.view;

import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.ALL_AUTHORITY_CODES_SEARCH;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.AUTHORITY_ID_SEARCH;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.EARLIEST_SEARCH_DATE;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.LATEST_SEARCH_DATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListConverterFilterEjectCases;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;

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


    @SuppressWarnings("squid:S1312")
    private Logger logger;

    @Inject
    private Enveloper enveloper;

    @Inject
    private HearingRepository repository;

    @Inject
    private HearingJsonListConverterFilterEjectCases hearingJsonListConverterFilterEjectCases;

    public JsonEnvelope rangeSearchHearings(final JsonEnvelope query) {
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER, false);
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String authorityIdSearchString = getAuthorityIdSearchString(authorityId);
        final String hearingTypeId = query.payloadAsJsonObject().getString(HEARING_TYPE, null);
        final String jurisdictionType = query.payloadAsJsonObject().getString(JURISDICTION_TYPE, null);
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, EARLIEST_SEARCH_DATE);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, LATEST_SEARCH_DATE);
        final String weekCommencingStartDate = trimToEmpty(query.payloadAsJsonObject().getString(WEEK_COMMENCING_START_DATE, null));
        final String weekCommencingEndDate = trimToEmpty(query.payloadAsJsonObject().getString(WEEK_COMMENCING_END_DATE, null));

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
                findHearingsByWeekCommencingRange(allocated, courtCentreId, courtRoomId, authorityIdSearchString, hearingTypeId, jurisdictionType, weekCommencingStartDate, weekCommencingEndDate) :
                findHearings(allocated, courtCentreId, courtRoomId, authorityIdSearchString, hearingTypeId, jurisdictionType, startDate, endDate);

        return enveloper.withMetadataFrom(query, "listing.search.hearings").apply(
                createObjectBuilder()
                        .add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings))
                        .build()
        );

    }

    private List<Hearing> findHearingsByWeekCommencingRange(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityIdSearchString, final String hearingTypeId, final String jurisdictionType, final String weekCommencingDate, final String weekCommencingEndDate) {
        return isFalse(allocated) ?
                repository.findUnallocatedHearingsByWeekCommencingRange(
                        courtCentreId,
                        courtRoomId,
                        authorityIdSearchString,
                        hearingTypeId,
                        jurisdictionType,
                        EARLIEST_SEARCH_DATE,
                        LATEST_SEARCH_DATE,
                        allocated) :
                repository.findHearingsByWeekCommencingRange(
                        courtCentreId,
                        courtRoomId,
                        authorityIdSearchString,
                        hearingTypeId,
                        jurisdictionType,
                        weekCommencingDate,
                        weekCommencingEndDate);
    }

    private List<Hearing> findHearings(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityIdSearchString, final String hearingTypeId, final String jurisdictionType, final String startDate, final String endDate) {
        return repository.findHearings(
                allocated,
                courtCentreId,
                courtRoomId,
                authorityIdSearchString,
                hearingTypeId,
                jurisdictionType,
                startDate,
                endDate
        );
    }

    private String getAuthorityIdSearchString(String authorityId) {
        if (authorityId != null) {
            return String.format(AUTHORITY_ID_SEARCH, authorityId);
        }
        return ALL_AUTHORITY_CODES_SEARCH;
    }

}
