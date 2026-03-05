package uk.gov.moj.cpp.listing.common.utils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.BST;
import static uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils.UTC;

import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.moj.cpp.listing.common.util.NonDefaultDayConverter;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NonDefaultDayConverterTest {

    @Test
    public void shouldConvertNonDefaultDaysCoreToCommand() {
        // Given
        List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays = new ArrayList<>();
        String courtCentreId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        String courtScheduleId = UUID.randomUUID().toString();
        ZonedDateTime startTime = ZonedDateTime.of(LocalDateTime.of(2024, 9, 12, 9, 0), BST).withZoneSameInstant(UTC);

        uk.gov.justice.core.courts.NonDefaultDay nonDefaultDay = uk.gov.justice.core.courts.NonDefaultDay.nonDefaultDay()
                .withCourtCentreId(courtCentreId)
                .withRoomId(roomId)
                .withCourtScheduleId(courtScheduleId)
                .withStartTime(startTime)
                .withDuration(30)
                .withSession("MORNING")
                .withOucode("TEST_OUCODE")
                .build();
        nonDefaultDays.add(nonDefaultDay);

        // When
        List<NonDefaultDay> result = NonDefaultDayConverter.convertNonDefaultDaysCoreToCommand(nonDefaultDays);

        // Then
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCourtCentreId(), is(courtCentreId));
        assertThat(result.get(0).getRoomId(), is(roomId));
        assertThat(result.get(0).getCourtScheduleId(), is(courtScheduleId));
        assertThat(result.get(0).getStartTime(), is(startTime));
        assertThat(result.get(0).getDuration(), is(30));
        assertThat(result.get(0).getSession(), is("MORNING"));
        assertThat(result.get(0).getOucode(), is("TEST_OUCODE"));
    }

    @Test
    public void shouldReturnEmptyListWhenNoNonDefaultDaysProvided() {
        // Given
        List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays = Collections.emptyList();

        // When
        List<NonDefaultDay> result = NonDefaultDayConverter.convertNonDefaultDaysCoreToCommand(nonDefaultDays);

        // Then
        assertThat(result, is(empty()));
    }

    @Test
    public void shouldConvertCommandNonDefaultDaysToHearingDays() {
        // Given
        List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays = new ArrayList<>();
        String courtCentreId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        String courtScheduleId = UUID.randomUUID().toString();
        ZonedDateTime startTime = ZonedDateTime.of(LocalDateTime.of(2024, 9, 12, 9, 0), BST).withZoneSameInstant(UTC);

        uk.gov.justice.core.courts.NonDefaultDay nonDefaultDay = uk.gov.justice.core.courts.NonDefaultDay.nonDefaultDay()
                .withCourtCentreId(courtCentreId)
                .withRoomId(roomId)
                .withCourtScheduleId(courtScheduleId)
                .withStartTime(startTime)
                .withDuration(30)
                .build();
        nonDefaultDays.add(nonDefaultDay);

        // When
        List<HearingDay> result = NonDefaultDayConverter.convertCoreNonDefaultDaysToHearingDays(nonDefaultDays);

        // Then
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCourtCentreId(), is(UUID.fromString(courtCentreId)));
        assertThat(result.get(0).getCourtRoomId(), is(UUID.fromString(roomId)));
        assertThat(result.get(0).getCourtScheduleId(), is(UUID.fromString(courtScheduleId)));
        assertThat(result.get(0).getDurationMinutes(), is(30));
        assertThat(result.get(0).getHearingDate(), is(startTime.toLocalDate()));
    }

    @Test
    public void shouldReturnEmptyListWhenNoNonDefaultDaysProvidedForHearingDays() {
        // Given
        List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays = Collections.emptyList();

        // When
        List<HearingDay> result = NonDefaultDayConverter.convertCoreNonDefaultDaysToHearingDays(nonDefaultDays);

        // Then
        assertThat(result, is(empty()));
    }

    @Test
    public void shouldConvertBookedSlotsToHearingDays() {
        // Given
        List<RotaSlot> bookedSlots = new ArrayList<>();
        String courtCentreId = UUID.randomUUID().toString();
        String roomId = UUID.randomUUID().toString();
        String courtScheduleId = UUID.randomUUID().toString();
        ZonedDateTime startTime = ZonedDateTime.of(LocalDateTime.of(2024, 9, 12, 9, 0), BST).withZoneSameInstant(UTC);

        RotaSlot slot = RotaSlot.rotaSlot()
                .withCourtCentreId(courtCentreId)
                .withRoomId(roomId)
                .withCourtScheduleId(courtScheduleId)
                .withStartTime(startTime)
                .withDuration(30)
                .build();
        bookedSlots.add(slot);

        // When
        List<HearingDay> result = NonDefaultDayConverter.convertBookedSlotsToHearingDays(bookedSlots);

        // Then
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getCourtCentreId(), is(UUID.fromString(courtCentreId)));
        assertThat(result.get(0).getCourtRoomId(), is(UUID.fromString(roomId)));
        assertThat(result.get(0).getCourtScheduleId(), is(UUID.fromString(courtScheduleId)));
        assertThat(result.get(0).getDurationMinutes(), is(30));
        assertThat(result.get(0).getHearingDate(), is(startTime.toLocalDate()));
        assertThat(result.get(0).getStartTime(), is(startTime));
    }

    @Test
    public void shouldReturnEmptyListWhenNoBookedSlotsProvided() {
        // Given
        List<RotaSlot> bookedSlots = Collections.emptyList();

        // When
        List<HearingDay> result = NonDefaultDayConverter.convertBookedSlotsToHearingDays(bookedSlots);

        // Then
        assertThat(result, is(empty()));
    }
} 