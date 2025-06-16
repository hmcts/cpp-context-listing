package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;
import static utils.HearingDayUtil.getNotCancelledHearingDays;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDayCourtScheduleUpdated;
import uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingDaysUpdateEventListener {

    private static final String HEARING_DAYS = "hearingDays";
    private static final String NON_DEFAULT_DAYS = "nonDefaultDays";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;

    @Handles("listing.events.hearing-days-without-court-centre-corrected")
    public void hearingDaysWithoutCourtCentreCorrected(final Envelope<HearingDaysWithoutCourtCentreCorrected> event) throws JsonProcessingException {
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

    @Handles("listing.events.hearing-day-court-schedule-updated")
    public void hearingDayCourtScheduleUpdated(Envelope<HearingDayCourtScheduleUpdated> event) throws JsonProcessingException {
        final HearingDayCourtScheduleUpdated hearingDaysEvent = event.payload();
        final UUID hearingId = hearingDaysEvent.getHearingId();
        final uk.gov.justice.listing.events.Hearing existingHearing = findHearingBy(hearingId);
        final Map<LocalDate, UUID> scheduledHearingDateMap = new HashMap<>();
        hearingDaysEvent.getHearingDayCourtSchedules().forEach(
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
        dbHearing.getNonDefaultDays().replaceAll(nonDefaultDay -> NonDefaultDay.nonDefaultDay()
                .withValuesFrom(nonDefaultDay)
                .withRoomId(null == nonDefaultDay.getRoomId() ? courtRoomId.toString() : nonDefaultDay.getRoomId())
                .withCourtCentreId(null == nonDefaultDay.getCourtCentreId() ? courtCentreId.toString() : nonDefaultDay.getCourtCentreId())
                .build());
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }

}
