package uk.gov.moj.cpp.listing.query.view.service;

import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsLast;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.COURT_ROOM_ID;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.HEARING_SLOTS;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.OU_CODE;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.PAGE_NUMBER;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.PAGE_SIZE;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.PANEL_ADULT_YOUTH;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.SESSION_END_DATE;
import static uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter.SESSION_START_DATE;

import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.persistence.entity.query.JudiciaryHearingDay;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splits the judiciary sitting in a court room on a date into AM and PM sessions (CHD-3323).
 * The session of a hearing day comes from its Court Scheduler session (AM, PM or AD = both);
 * when that is unknown the hearing day start time in UK local time decides.
 */
@ApplicationScoped
public class JudiciarySessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JudiciarySessionService.class);

    private static final ZoneId UK_ZONE = ZoneId.of("Europe/London");
    private static final String PANEL = "panel";
    private static final String SHOW_OVERBOOKED_SLOTS = "showOverbookedSlots";
    private static final String COURT_SCHEDULE_ID = "courtScheduleId";
    private static final String COURT_SESSION = "courtSession";
    private static final String AM = "AM";
    private static final String PM = "PM";
    private static final String ALL_DAY = "AD";
    private static final String COURT_SCHEDULER_PAGE_NUMBER = "1";
    private static final String COURT_SCHEDULER_PAGE_SIZE = "100";

    @Inject
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;

    @Inject
    @Value(key = "listing.query.judiciary.pmSessionStartTime", defaultValue = "13:00")
    private String pmSessionStartTime;

    public JudiciarySessions splitIntoSessions(final List<JudiciaryHearingDay> hearingDays,
                                               final Optional<String> ouCode,
                                               final String courtRoomId,
                                               final LocalDate searchDate) {
        final Map<String, String> sessionsByCourtScheduleId = findCourtSessions(hearingDays, ouCode, courtRoomId, searchDate);
        final LocalTime pmStart = LocalTime.parse(pmSessionStartTime);

        final List<JudiciaryHearingDay> amHearingDays = new ArrayList<>();
        final List<JudiciaryHearingDay> pmHearingDays = new ArrayList<>();
        hearingDays.forEach(hearingDay -> {
            final String session = Optional.ofNullable(hearingDay.courtScheduleId())
                    .map(sessionsByCourtScheduleId::get)
                    .orElseGet(() -> sessionFromStartTime(hearingDay.startTime(), pmStart));
            if (AM.equals(session) || ALL_DAY.equals(session)) {
                amHearingDays.add(hearingDay);
            }
            if (PM.equals(session) || ALL_DAY.equals(session)) {
                pmHearingDays.add(hearingDay);
            }
        });

        return new JudiciarySessions(orderedJudicialIds(amHearingDays), orderedJudicialIds(pmHearingDays));
    }

    private Map<String, String> findCourtSessions(final List<JudiciaryHearingDay> hearingDays,
                                                  final Optional<String> ouCode,
                                                  final String courtRoomId,
                                                  final LocalDate searchDate) {
        final boolean anyCourtScheduleId = hearingDays.stream().map(JudiciaryHearingDay::courtScheduleId).anyMatch(Objects::nonNull);
        if (!anyCourtScheduleId) {
            return Map.of();
        }
        if (ouCode.isEmpty()) {
            LOGGER.warn("No ouCode for court room {}, placing judiciary by hearing start time", courtRoomId);
            return Map.of();
        }

        final Map<String, String> queryParams = new HashMap<>();
        queryParams.put(OU_CODE, ouCode.get());
        queryParams.put(COURT_ROOM_ID, courtRoomId);
        queryParams.put(SESSION_START_DATE, searchDate.toString());
        queryParams.put(SESSION_END_DATE, searchDate.toString());
        queryParams.put(PANEL, PANEL_ADULT_YOUTH);
        queryParams.put(SHOW_OVERBOOKED_SLOTS, Boolean.TRUE.toString());
        queryParams.put(PAGE_NUMBER, COURT_SCHEDULER_PAGE_NUMBER);
        queryParams.put(PAGE_SIZE, COURT_SCHEDULER_PAGE_SIZE);

        try {
            final Response response = courtSchedulerServiceAdapter.hearingSlotsSearch(queryParams);
            if (response.getStatus() != Response.Status.OK.getStatusCode() ||!(response.getEntity() instanceof JsonObject payload) || !payload.containsKey(HEARING_SLOTS)) {
                LOGGER.warn("Court Scheduler hearing slots search returned status {} for court room {} on {}, placing judiciary by hearing start time",
                        response.getStatus(), courtRoomId, searchDate);
                return Map.of();
            }
            final Map<String, String> sessionsByCourtScheduleId = new HashMap<>();
            payload.getJsonArray(HEARING_SLOTS).getValuesAs(JsonObject.class).stream()
                    .filter(slot -> nonNull(slot.getString(COURT_SCHEDULE_ID, null)) && nonNull(slot.getString(COURT_SESSION, null)))
                    .forEach(slot -> sessionsByCourtScheduleId.put(slot.getString(COURT_SCHEDULE_ID), slot.getString(COURT_SESSION)));
            return sessionsByCourtScheduleId;
        } catch (final RuntimeException e) {
            LOGGER.warn("Court Scheduler hearing slots search failed for court room {} on {}, placing judiciary by hearing start time",
                    courtRoomId, searchDate, e);
            return Map.of();
        }
    }

    private static String sessionFromStartTime(final Instant startTime, final LocalTime pmStart) {
        if (isNull(startTime)) {
            return null;
        }
        return startTime.atZone(UK_ZONE).toLocalTime().isBefore(pmStart) ? AM : PM;
    }

    /**
     * De-duplicates by judicial id, most distinct hearings first, then earliest start time.
     */
    private static List<String> orderedJudicialIds(final List<JudiciaryHearingDay> hearingDays) {
        final Comparator<Collection<JudiciaryHearingDay>> byHearingCountDescending =
                comparing((Collection<JudiciaryHearingDay> days) -> days.stream().map(JudiciaryHearingDay::hearingId).distinct().count()).reversed();
        final Comparator<Collection<JudiciaryHearingDay>> byEarliestStartTime =
                comparing(days -> days.stream().map(JudiciaryHearingDay::startTime).filter(Objects::nonNull).min(naturalOrder()).orElse(null),
                        nullsLast(naturalOrder()));

        return hearingDays.stream()
                .collect(groupingBy(JudiciaryHearingDay::judicialId))
                .entrySet().stream()
                .sorted(Map.Entry.<String, List<JudiciaryHearingDay>>comparingByValue(byHearingCountDescending.thenComparing(byEarliestStartTime))
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .toList();
    }

    public record JudiciarySessions(List<String> amJudicialIds, List<String> pmJudicialIds) {
    }
}
