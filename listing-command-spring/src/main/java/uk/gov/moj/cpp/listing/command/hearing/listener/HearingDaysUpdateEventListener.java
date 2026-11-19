package uk.gov.moj.cpp.listing.command.hearing.listener;

import org.springframework.stereotype.Component;
import uk.gov.moj.cpp.listing.command.hearing.flow.EventHandlers;
import uk.gov.moj.cpp.listing.command.hearing.flow.ListenerPort;
import org.springframework.beans.factory.annotation.Qualifier;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;
import static uk.gov.moj.cpp.listing.command.hearing.listener.HearingDayUtil.getNotCancelledHearingDays;

import uk.gov.justice.listing.events.CrownHearingMigratedToCourtschedule;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDayCourtSchedule;
import uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated;
import uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.moj.cpp.listing.domain.json.JsonObjectToObjectConverter;
import uk.gov.moj.cpp.listing.command.hearing.flow.RaisedEvent;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;

@Component
public class HearingDaysUpdateEventListener implements ListenerPort {

    private static final String HEARING_DAYS = "hearingDays";
    private static final String NON_DEFAULT_DAYS = "nonDefaultDays";
    private final HearingRepository hearingRepository;
    private final ObjectMapper objectMapper;
    private final JsonObjectToObjectConverter jsonObjectToObjectConverter;
    private final HearingSearchSyncService hearingSearchSyncService;

    public HearingDaysUpdateEventListener(final HearingRepository hearingRepository, @Qualifier(ListenerConfiguration.VIEWSTORE_OBJECT_MAPPER) final ObjectMapper objectMapper, final JsonObjectToObjectConverter jsonObjectToObjectConverter, final HearingSearchSyncService hearingSearchSyncService) {
        this.hearingRepository = hearingRepository;
        this.objectMapper = objectMapper;
        this.jsonObjectToObjectConverter = jsonObjectToObjectConverter;
        this.hearingSearchSyncService = hearingSearchSyncService;
    }

    @Override
    public void register(final EventHandlers listeners) {
        listeners
                .on(HearingDaysWithoutCourtCentreCorrected.class, event -> uncheckedIo(() -> hearingDaysWithoutCourtCentreCorrected(event)))
                .on(HearingDayCourtScheduleUpdated.class, event -> uncheckedIo(() -> hearingDayCourtScheduleUpdated(event)))
                .on(CrownHearingMigratedToCourtschedule.class, event -> uncheckedIo(() -> crownHearingMigratedToCourtSchedule(event)));
    }

    public void hearingDaysWithoutCourtCentreCorrected(final RaisedEvent<HearingDaysWithoutCourtCentreCorrected> event) throws JsonProcessingException {
        final HearingDaysWithoutCourtCentreCorrected hearingDaysWithoutCourtCentreCorrected = event.payload();
        final List<HearingDay> hearingDays = hearingDaysWithoutCourtCentreCorrected.getHearingDays();
        final UUID hearingId = hearingDaysWithoutCourtCentreCorrected.getId();
        final UUID courtCentreId = hearingDays.get(0).getCourtCentreId();
        final UUID courtRoomId = hearingDays.get(0).getCourtRoomId();
        List<NonDefaultDay> nonDefaultDays = new ArrayList<>();

        final uk.gov.justice.listing.events.Hearing dbHearing = findHearingBy(hearingId);

        correctHearingDaysWithoutCourtCentre(courtCentreId, courtRoomId, dbHearing);

        if (CollectionUtils.isNotEmpty(dbHearing.getNonDefaultDays())) {
            correctNonDefaultDaysWithoutCourtCentre(courtCentreId, courtRoomId, dbHearing);
            nonDefaultDays = dbHearing.getNonDefaultDays();
        }

        final List<HearingDay> nonCancelledHearingDays = getNotCancelledHearingDays(dbHearing.getHearingDays());
        using(hearingRepository)
                .find(hearingId)
                .remove(HEARING_DAYS)
                .remove(NON_DEFAULT_DAYS)
                .putObjectList(HEARING_DAYS, nonCancelledHearingDays)
                .putObjectList(NON_DEFAULT_DAYS, nonDefaultDays)
                .save();

        hearingSearchSyncService.sync(hearingId);
    }
    public void hearingDayCourtScheduleUpdated(RaisedEvent<HearingDayCourtScheduleUpdated> event) throws JsonProcessingException {
        final HearingDayCourtScheduleUpdated hearingDaysEvent = event.payload();
        mergeCourtScheduleIds(hearingDaysEvent.getHearingId(), hearingDaysEvent.getHearingDayCourtSchedules());
    }
    public void crownHearingMigratedToCourtSchedule(RaisedEvent<CrownHearingMigratedToCourtschedule> event) throws JsonProcessingException {
        final CrownHearingMigratedToCourtschedule migratedEvent = event.payload();
        mergeCourtScheduleIds(migratedEvent.getHearingId(), migratedEvent.getHearingDayCourtSchedules());
    }

    private void mergeCourtScheduleIds(final UUID hearingId, final List<HearingDayCourtSchedule> hearingDayCourtSchedules) throws JsonProcessingException {
        final uk.gov.justice.listing.events.Hearing existingHearing = findHearingBy(hearingId);
        final Map<LocalDate, UUID> scheduledHearingDateMap = new HashMap<>();
        hearingDayCourtSchedules.forEach(
                hd -> scheduledHearingDateMap.put(hd.getHearingDate(), hd.getCourtScheduleId()));

        updateHearingDays(existingHearing, scheduledHearingDateMap);
        updateNonDefaultDays(existingHearing, scheduledHearingDateMap);

        using(hearingRepository)
                .find(hearingId)
                .remove(HEARING_DAYS)
                .remove(NON_DEFAULT_DAYS)
                .putObjectList(HEARING_DAYS, getNotCancelledHearingDays(existingHearing.getHearingDays()))
                .putObjectList(NON_DEFAULT_DAYS, nonNull(existingHearing.getNonDefaultDays()) ? existingHearing.getNonDefaultDays() : emptyList())
                .save();
        hearingSearchSyncService.sync(hearingId);
    }

    private void updateNonDefaultDays(uk.gov.justice.listing.events.Hearing existingHearing,
                                      Map<LocalDate, UUID> scheduledHearingDateMap) {
        if (nonNull(existingHearing.getNonDefaultDays())) {
            existingHearing.getNonDefaultDays().replaceAll(nd -> {
                UUID scheduleIdFromCourtScheduler = scheduledHearingDateMap.get(nd.getStartTime().toLocalDate());
                if (scheduleIdFromCourtScheduler != null && !scheduleIdFromCourtScheduler.toString().equals(nd.getCourtScheduleId())) {
                    return NonDefaultDay.nonDefaultDay()
                            .withValuesFrom(nd)
                            .withCourtScheduleId(scheduleIdFromCourtScheduler.toString())
                            .build();
                }
                return nd;
            });
        }
    }

    private void updateHearingDays(uk.gov.justice.listing.events.Hearing existingHearing,
                                   Map<LocalDate, UUID> scheduledHearingDateMap) {
        if (nonNull(existingHearing.getHearingDays())) {
            existingHearing.getHearingDays().replaceAll(hd -> {
                UUID scheduleIdFromCourtScheduler = scheduledHearingDateMap.get(hd.getHearingDate());
                if (scheduleIdFromCourtScheduler != null && !scheduleIdFromCourtScheduler.equals(hd.getCourtScheduleId())) {
                    return HearingDay.hearingDay()
                            .withValuesFrom(hd)
                            .withCourtScheduleId(scheduleIdFromCourtScheduler)
                            .build();
                }
                return hd;
            });
        }
    }

    private uk.gov.justice.listing.events.Hearing findHearingBy(final UUID hearingId) throws JsonProcessingException {
        final Hearing existingHearingEntity = hearingRepository.findBy(hearingId);
        final JsonObject existingHearingJsonObject =
                jsonFromString(objectMapper.writeValueAsString(existingHearingEntity.getProperties()));
        final uk.gov.justice.listing.events.Hearing existingHearing =
                jsonObjectToObjectConverter.convert(existingHearingJsonObject, uk.gov.justice.listing.events.Hearing.class);

        return existingHearing;
    }

    private void correctHearingDaysWithoutCourtCentre(final UUID courtCentreId, final UUID courtRoomId, final uk.gov.justice.listing.events.Hearing dbHearing) {
        dbHearing.getHearingDays().replaceAll(hearingDay -> HearingDay.hearingDay()
                .withValuesFrom(hearingDay)
                .withCourtCentreId(nonNull(hearingDay.getCourtCentreId()) ? hearingDay.getCourtCentreId() : courtCentreId)
                .withCourtRoomId(nonNull(hearingDay.getCourtRoomId()) ? hearingDay.getCourtRoomId() : courtRoomId)
                .build());
    }

    private void correctNonDefaultDaysWithoutCourtCentre(final UUID courtCentreId, final UUID courtRoomId, final uk.gov.justice.listing.events.Hearing dbHearing) {
        dbHearing.getNonDefaultDays().replaceAll(nonDefaultDay -> {
            final String roomId = determineRoomId(nonDefaultDay, courtRoomId);
            final String courtCentreIdValue = determineCourtCentreId(nonDefaultDay, courtCentreId);
            
            return NonDefaultDay.nonDefaultDay()
                    .withValuesFrom(nonDefaultDay)
                    .withRoomId(roomId)
                    .withCourtCentreId(courtCentreIdValue)
                    .build();
        });
    }

    private String determineRoomId(final NonDefaultDay nonDefaultDay, final UUID courtRoomId) {
        if (nonDefaultDay.getRoomId() != null) {
            return nonDefaultDay.getRoomId();
        }
        return nonNull(courtRoomId) ? courtRoomId.toString() : null;
    }

    private String determineCourtCentreId(final NonDefaultDay nonDefaultDay, final UUID courtCentreId) {
        if (nonDefaultDay.getCourtCentreId() != null) {
            return nonDefaultDay.getCourtCentreId();
        }
        return nonNull(courtCentreId) ? courtCentreId.toString() : null;
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }


    /** The JEE handler declared a checked exception: it failed the event, as a runtime exception does here. */
    private static void uncheckedIo(final IoAction action) {
        try {
            action.run();
        } catch (final java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    private interface IoAction {
        void run() throws java.io.IOException;
    }
}
