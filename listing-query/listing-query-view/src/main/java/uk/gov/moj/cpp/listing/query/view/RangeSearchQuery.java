package uk.gov.moj.cpp.listing.query.view;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.ALL_AUTHORITY_CODES_SEARCH;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.AUTHORITY_ID_SEARCH;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.EARLIEST_SEARCH_DATE;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.LATEST_SEARCH_DATE;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.PROSECUTOR_ID_SEARCH;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
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
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, EARLIEST_SEARCH_DATE);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, LATEST_SEARCH_DATE);


        logger.info("Query params -  " +
                        "courtCentreId: {}, " +
                        "courtRoomId: {}, " +
                        "startDate: {}, " +
                        "endDate: {}, "
                , courtCentreId, courtRoomId, startDate, endDate);

        final List<Hearing> hearings = findHearings(true, courtCentreId, courtRoomId, ALL_AUTHORITY_CODES_SEARCH, ALL_AUTHORITY_CODES_SEARCH, null, null, startDate, endDate);

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.range.search.hearings.for.judge"),
                createObjectBuilder().add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings)));
    }



    public JsonEnvelope rangeSearchHearings(final JsonEnvelope query) {
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER, false);
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String authorityIdSearchString = getAuthorityIdSearchString(authorityId);
        final String prosecutorIdSearchString = getProsecutorIdSearchString(authorityId);
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
                findHearingsByWeekCommencingRange(allocated, courtCentreId, courtRoomId, authorityIdSearchString, prosecutorIdSearchString, hearingTypeId, jurisdictionType, weekCommencingStartDate, weekCommencingEndDate) :
                findHearings(allocated, courtCentreId, courtRoomId, authorityIdSearchString, prosecutorIdSearchString, hearingTypeId, jurisdictionType, startDate, endDate);

        return envelopeFrom(metadataFrom(query.metadata()).withName("listing.search.hearings"),
                createObjectBuilder().add(HEARINGS, hearingJsonListConverterFilterEjectCases.convert(hearings)));
    }

    private String getProsecutorIdSearchString(final String authorityId) {
        if (authorityId != null) {
            return format(PROSECUTOR_ID_SEARCH, authorityId);
        } else {
            return ALL_AUTHORITY_CODES_SEARCH;
        }
    }

    private List<Hearing> findHearingsByWeekCommencingRange(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityIdSearchString, final String prosecutorIdSearchString, final String hearingTypeId, final String jurisdictionType, final String weekCommencingDate, final String weekCommencingEndDate) {
        return isFalse(allocated) ?
                repository.findUnallocatedHearingsByWeekCommencingRange(
                        courtCentreId,
                        courtRoomId,
                        authorityIdSearchString,
                        prosecutorIdSearchString,
                        hearingTypeId,
                        jurisdictionType,
                        EARLIEST_SEARCH_DATE,
                        LATEST_SEARCH_DATE,
                        allocated) :
                repository.findHearingsByWeekCommencingRange(
                        courtCentreId,
                        courtRoomId,
                        authorityIdSearchString,
                        prosecutorIdSearchString,
                        hearingTypeId,
                        jurisdictionType,
                        weekCommencingDate,
                        weekCommencingEndDate);
    }

    private List<Hearing> findHearings(final boolean allocated, final String courtCentreId, final String courtRoomId, final String authorityIdSearchString, final String prosecutorIdSearchString, final String hearingTypeId, final String jurisdictionType, final String startDate, final String endDate) {
        return repository.findHearings(
                String.valueOf(allocated),
                ofNullable(courtCentreId).orElse(null),
                ofNullable(courtRoomId).orElse(null),
                authorityIdSearchString,
                prosecutorIdSearchString,
                ofNullable(hearingTypeId).orElse(null),
                ofNullable(jurisdictionType).orElse(null),
                startDate,
                endDate
        );
    }

    private String getAuthorityIdSearchString(String authorityId) {
        if (authorityId != null) {
            return format(AUTHORITY_ID_SEARCH, authorityId);
        }
        return ALL_AUTHORITY_CODES_SEARCH;
    }

}
