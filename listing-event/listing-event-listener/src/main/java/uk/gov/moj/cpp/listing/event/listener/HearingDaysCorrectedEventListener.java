package uk.gov.moj.cpp.listing.event.listener;

import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingDaysWithoutCourtCentreCorrected;
import uk.gov.justice.listing.events.NonDefaultDay;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections.CollectionUtils;

@ServiceComponent(Component.EVENT_LISTENER)
public class HearingDaysCorrectedEventListener {

    private static final String HEARING_DAYS = "hearingDays";
    private static final String NON_DEFAULT_DAYS = "nonDefaultDays";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("listing.events.hearing-days-without-court-centre-corrected")
    public void hearingDaysWithoutCourtCentreCorrected(final Envelope<HearingDaysWithoutCourtCentreCorrected> event) throws JsonProcessingException {
        final HearingDaysWithoutCourtCentreCorrected hearingDaysWithoutCourtCentreCorrected =  event.payload();
        final List<HearingDay> hearingDays = hearingDaysWithoutCourtCentreCorrected.getHearingDays();
        final UUID hearingId = hearingDaysWithoutCourtCentreCorrected.getId();
        final Optional<UUID> courtCentreId = hearingDays.get(0).getCourtCentreId();
        final Optional<UUID> courtRoomId = hearingDays.get(0).getCourtRoomId();

        final Hearing dbHearingEntity = hearingRepository.findBy(hearingId);

        final JsonObject dbHearingJsonObject = jsonFromString(objectMapper.writeValueAsString(dbHearingEntity.getProperties()));

        final uk.gov.justice.listing.events.Hearing dbHearing = jsonObjectToObjectConverter.convert(dbHearingJsonObject, uk.gov.justice.listing.events.Hearing.class);

        correctHearingDaysWithoutCourtCentre(courtCentreId, courtRoomId, dbHearing);

        if (CollectionUtils.isNotEmpty(dbHearing.getNonDefaultDays())) {
            correctNonDefaultDaysWithoutCourtCentre(courtCentreId, courtRoomId, dbHearing);
        }

        using(hearingRepository)
                .find(hearingId)
                .remove(HEARING_DAYS)
                .remove(NON_DEFAULT_DAYS)
                .putObjectList(HEARING_DAYS, dbHearing.getHearingDays())
                .putObjectList(NON_DEFAULT_DAYS, dbHearing.getNonDefaultDays())
                .save();
    }

    private void correctHearingDaysWithoutCourtCentre(final Optional<UUID> courtCentreId, final Optional<UUID> courtRoomId, final uk.gov.justice.listing.events.Hearing dbHearing) {
        dbHearing.getHearingDays().replaceAll(hearingDay -> HearingDay.hearingDay()
                .withCourtCentreId(hearingDay.getCourtCentreId().orElse(courtCentreId.get()))
                .withCourtRoomId(hearingDay.getCourtRoomId().orElse(courtRoomId.orElse(null)))
                .withCourtScheduleId(hearingDay.getCourtScheduleId())
                .withDurationMinutes(hearingDay.getDurationMinutes())
                .withEndTime(hearingDay.getEndTime())
                .withHearingDate(hearingDay.getHearingDate())
                .withSequence(hearingDay.getSequence())
                .withStartTime(hearingDay.getStartTime())
                .build());
    }

    private void correctNonDefaultDaysWithoutCourtCentre(final Optional<UUID> courtCentreId, final Optional<UUID> courtRoomId, final uk.gov.justice.listing.events.Hearing dbHearing) {
        dbHearing.getNonDefaultDays().replaceAll(nonDefaultDay -> NonDefaultDay.nonDefaultDay()
                .withCourtScheduleId(nonDefaultDay.getCourtScheduleId())
                .withSession(nonDefaultDay.getSession())
                .withDuration(nonDefaultDay.getDuration())
                .withStartTime(nonDefaultDay.getStartTime())
                .withCourtRoomId(nonDefaultDay.getCourtRoomId())
                .withOucode(nonDefaultDay.getOucode())
                .withRoomId(nonDefaultDay.getRoomId().map(Optional::of).orElse(courtRoomId.map(UUID::toString)))
                .withCourtCentreId(nonDefaultDay.getCourtCentreId().map(Optional::of).orElse(courtCentreId.map(UUID::toString))).build());
    }

    private static JsonObject jsonFromString(final String jsonObjectStr) {
        final JsonReader jsonReader = Json.createReader(new StringReader(jsonObjectStr));
        final JsonObject object = jsonReader.readObject();
        jsonReader.close();
        return object;
    }
}
