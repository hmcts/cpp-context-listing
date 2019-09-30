package uk.gov.moj.cpp.listing.query.view;


import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.domain.CourtListType;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.query.view.hearing.HearingJsonListCoverterFilterEjectCases;

import javax.inject.Inject;
import javax.json.Json;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;
import static javax.json.Json.createArrayBuilder;
import static uk.gov.moj.cpp.listing.persistence.repository.HearingRepository.*;

@SuppressWarnings({"squid:S1192"})
@ServiceComponent(Component.QUERY_VIEW)
public class HearingQueryView {

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

    @Inject
    private HearingRepository repository;

    @Inject
    private HearingJsonListCoverterFilterEjectCases hearingJsonListCoverterFilterEjectCases;

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
                Json.createObjectBuilder()
                        .add(HEARINGS, hearingJsonListCoverterFilterEjectCases.convert(hearings))
                        .build()
        );
    }

    @Handles("listing.range.search.hearings")
    public JsonEnvelope rangeSearchHearings(final JsonEnvelope query) {
        final boolean allocated = query.payloadAsJsonObject().getBoolean(ALLOCATED_QUERY_PARAMETER);
        final String courtCentreId = query.payloadAsJsonObject().getString(COURT_CENTRE_ID, null);
        final String courtRoomId = query.payloadAsJsonObject().getString(COURT_ROOM_ID, null);
        final String authorityId = query.payloadAsJsonObject().getString(AUTHORITY_ID, null);
        final String authorityIdSearchString = getAuthorityIdSearchString(authorityId);
        final String hearingTypeId = query.payloadAsJsonObject().getString(HEARING_TYPE, null);
        final String jurisdictionType = query.payloadAsJsonObject().getString(JURISDICTION_TYPE, null);
        final String startDate = query.payloadAsJsonObject().getString(START_DATE, EARLIEST_SEARCH_DATE);
        final String endDate = query.payloadAsJsonObject().getString(END_DATE, LATEST_SEARCH_DATE);

        LOGGER.info("Query params -  " +
                        "allocated: {}, " +
                        "courtCentreId: {}, " +
                        "courtRoomId: {}, " +
                        "authorityId: {}, " +
                        "hearingTypeId: {}, " +
                        "jurisdictionType: {}, " +
                        "startDate: {}, " +
                        "endDate: {}, ",
                allocated, courtCentreId, courtRoomId, authorityId, hearingTypeId, jurisdictionType, startDate, endDate);

        final List<Hearing> hearings = repository.findHearings(
                allocated,
                courtCentreId,
                courtRoomId,
                authorityIdSearchString,
                hearingTypeId,
                jurisdictionType,
                startDate,
                endDate
        );

        return enveloper.withMetadataFrom(query, "listing.search.hearings").apply(
                Json.createObjectBuilder()
                        .add(HEARINGS, hearingJsonListCoverterFilterEjectCases.convert(hearings))
                        .build()
        );
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
                Hearing matchedHearingsJsonObject = repository.findHearingsForPublicStandardList(Boolean.TRUE, courtCentreId, startDate, endDate);
                return createPublicStandardCourtListJsonEnvelope(query, matchedHearingsJsonObject);
            } else {
                List<Hearing> matchedHearings = repository.findHearingsForAlphabeticalList(Boolean.TRUE, courtCentreId, startDate);
                return createAlphabeticalListJsonEnvelope(query, matchedHearings);
            }
            // Plug in queries for other list types
        } else {
            LOGGER.error("Supplied CourtList type is not valid {} ", listId);
            return createEmptyResponse(query);
        }


    }

    private JsonEnvelope createAlphabeticalListJsonEnvelope(final JsonEnvelope query, final List<Hearing> matchedHearings) {
        return enveloper.withMetadataFrom(query, "listing.search.court.list").apply(
                Json.createObjectBuilder()
                        .add(HEARINGS, hearingJsonListCoverterFilterEjectCases.convertHearingResultForAlphbeticalList(matchedHearings))
                        .build()
        );
    }

    private JsonEnvelope createPublicStandardCourtListJsonEnvelope(final JsonEnvelope query, final Hearing matchedHearingsJsonObject) {
        return enveloper.withMetadataFrom(query, "listing.search.court.list").apply(
                Json.createObjectBuilder()
                        .add(HEARINGS, hearingJsonListCoverterFilterEjectCases.convertHearingResultForPublicList(matchedHearingsJsonObject))
                        .build()
        );
    }

    private JsonEnvelope createEmptyResponse(JsonEnvelope query) {
        return enveloper.withMetadataFrom(query, "listing.search.court.list").apply(
                Json.createObjectBuilder()
                        .add(HEARINGS, createArrayBuilder().build())
                        .build());
    }

    private String getDateTimeAsString(final String date, final String time, final String defaultTime) {
        final String copyTime = Strings.isNullOrEmpty(time) ? defaultTime : time;
        final LocalDate localDate = LocalDate.parse(date);
        final LocalTime localTime = LocalTime.parse(copyTime);
        return localDate.atTime(localTime).toString();
    }

    private String getAuthorityIdSearchString(String authorityId) {
        if (authorityId != null) {
            return String.format(AUTHORITY_ID_SEARCH, authorityId);
        } else {
            return ALL_AUTHORITY_CODES_SEARCH;
        }
    }
}
