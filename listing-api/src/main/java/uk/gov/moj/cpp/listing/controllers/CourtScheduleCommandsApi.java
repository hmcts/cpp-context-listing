package uk.gov.moj.cpp.listing.controllers;

import static uk.gov.moj.cpp.listing.controllers.CommandRequests.accepted;
import static uk.gov.moj.cpp.listing.controllers.HearingCommandRequests.command;

import uk.gov.moj.cpp.listing.command.hearing.api.HearingCommandApi;
import uk.gov.moj.cpp.listing.openapi.api.CourtScheduleCommandsOpenApi;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /update-hearing-day-court-schedule} (MBD refactor P2.3.2, ADR 015; 202) and
 * {@code POST /migrate-crown-hearings-to-courtschedules} (P2.3.3; 202).
 */
@RestController
public class CourtScheduleCommandsApi implements CourtScheduleCommandsOpenApi {

    static final String UPDATE_HEARING_DAY_COURT_SCHEDULE = "listing.update-hearing-day-court-schedule";
    static final String MIGRATE_CROWN_HEARINGS_TO_COURTSCHEDULES = "listing.migrate-crown-hearings-to-courtschedules";

    private final HearingCommandApi api;
    private final HttpServletRequest request;

    public CourtScheduleCommandsApi(final HearingCommandApi api, final HttpServletRequest request) {
        this.api = api;
        this.request = request;
    }

    @Override
    public ResponseEntity<Void> postUpdateHearingDayCourtSchedule(final Map<String, Object> requestBody) {
        api.handleUpdateHearingDayCourtSchedule(command(request, UPDATE_HEARING_DAY_COURT_SCHEDULE, requestBody, Map.of()));
        return accepted();
    }

    @Override
    public ResponseEntity<Void> postMigrateCrownHearingsToCourtSchedules(final Map<String, Object> requestBody) {
        api.handleMigrateCrownHearingsToCourtSchedules(command(request, MIGRATE_CROWN_HEARINGS_TO_COURTSCHEDULES, requestBody, Map.of()));
        return accepted();
    }
}
