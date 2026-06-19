package uk.gov.moj.cpp.listing.command.api.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds;
import static uk.gov.justice.listing.commands.UpdateHearingForListing.updateHearingForListing;

import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NonDefaultDay;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.HearingTypeFactory;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingDurationEnrichmentServiceTest {

    private static final String HEARING_TYPE_ID = randomUUID().toString();
    private static final int DURATION_BY_HEARING_TYPE = 30;
    private static final UUID HEARING_ID = randomUUID();
    private static final int MINUTES_IN_DAY = 360;

    @Mock
    private HearingTypeFactory hearingTypeFactory;

    @InjectMocks
    private HearingDurationEnrichmentService hearingDurationEnrichmentService;

    private Map<String, Integer> hearingTypesIdDurationMap;
    private JsonEnvelope envelope;

    @BeforeEach
    void setUp() {
        hearingTypesIdDurationMap = new HashMap<>();
        hearingTypesIdDurationMap.put(HEARING_TYPE_ID, DURATION_BY_HEARING_TYPE);
        envelope = mock(JsonEnvelope.class);
        lenient().when(hearingTypeFactory.getHearingTypesIdDurationMap(any())).thenReturn(hearingTypesIdDurationMap);
    }

    @Test
    public void shouldEnrichHearingDurationWhenEstimatedMinutesIsNotProvided() {
        // Given
        HearingListingNeeds hearing = hearingListingNeeds()
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withEstimatedMinutes(null)
                .build();


        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getEstimatedMinutes());
    }

    @Test
    public void shouldEnrichHearingDurationWhenEstimatedMinutesIsZero() {
        // Given
        HearingListingNeeds hearing = hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withEstimatedMinutes(0)
                .build();


        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getEstimatedMinutes());
    }

    @Test
    public void shouldEnrichHearingDurationWhenEstimatedMinutesIsOneMinute() {
        // Given
        HearingListingNeeds hearing = hearingListingNeeds()
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withEstimatedMinutes(1)
                .build();


        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getEstimatedMinutes());
    }

    @Test
    public void shouldNotChangeDurationIfEstimatedMinutesIsValid() {
        // Given
        int validDuration = 60;
        HearingListingNeeds hearing = hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withEstimatedMinutes(validDuration)
                .build();

        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(validDuration, result.getEstimatedMinutes());
    }

    @Test
    public void shouldEnrichDurationIfBookedSlotsHaveNoDuration() {
        // Given
        RotaSlot bookedSlot1 = RotaSlot.rotaSlot().withDuration(null).build();

        HearingListingNeeds hearing = hearingListingNeeds()
                .withId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withBookedSlots(Arrays.asList(bookedSlot1))
                .build();

        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getBookedSlots().get(0).getDuration());
    }

    @Test
    public void shouldAdjustDurationForMultipleSlotsForMultiday() {
        // Given
        RotaSlot bookedSlot1 = RotaSlot.rotaSlot()
                .withDuration(0)
                .build();
        RotaSlot bookedSlot2 = RotaSlot.rotaSlot()
                .withDuration(1)
                .build();

        HearingListingNeeds hearing = hearingListingNeeds()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withEstimatedMinutes(20)
                .withBookedSlots(Arrays.asList(bookedSlot1, bookedSlot2))
                .build();

        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(MINUTES_IN_DAY, result.getBookedSlots().get(0).getDuration());
        assertEquals(MINUTES_IN_DAY, result.getBookedSlots().get(1).getDuration());
        assertEquals(2 * MINUTES_IN_DAY, result.getEstimatedMinutes());

    }

    @Test
    public void shouldAdjustDurationForMultipleNonDefaultDaysForMultiDay() {
        // Given
        NonDefaultDay nonDefaultDay1 = NonDefaultDay.nonDefaultDay()
                .withDuration(0)
                .withCourtCentreId(randomUUID().toString())
                .withRoomId(randomUUID().toString())
                .withStartTime(ZonedDateTime.now())
                .build();
        NonDefaultDay nonDefaultDay2 = NonDefaultDay.nonDefaultDay()
                .withDuration(1)
                .withCourtCentreId(randomUUID().toString())
                .withRoomId(randomUUID().toString())
                .withStartTime(ZonedDateTime.now())
                .build();

        HearingListingNeeds hearing = hearingListingNeeds()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withEstimatedMinutes(20)
                .withNonDefaultDays(Arrays.asList(nonDefaultDay1, nonDefaultDay2))
                .build();

        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(MINUTES_IN_DAY, result.getNonDefaultDays().get(0).getDuration());
        assertEquals(MINUTES_IN_DAY, result.getNonDefaultDays().get(1).getDuration());
        assertEquals(2 * MINUTES_IN_DAY, result.getEstimatedMinutes());

    }

    @Test
    public void shouldAdjustDurationForMultipleNonDefaultDaysForMultiDayUpdate() {
        // Given
        uk.gov.justice.listing.commands.NonDefaultDay nonDefaultDay1 = uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                .withDuration(0)
                .build();
        uk.gov.justice.listing.commands.NonDefaultDay nonDefaultDay2 = uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                .withDuration(1)
                .build();

        UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(HEARING_ID)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withNonDefaultDays(Arrays.asList(nonDefaultDay1, nonDefaultDay2))
                .build();

        // When
        UpdateHearingForListing result = hearingDurationEnrichmentService.enrichWithDurationForUpdate(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(MINUTES_IN_DAY, result.getNonDefaultDays().get(0).getDuration());
        assertEquals(MINUTES_IN_DAY, result.getNonDefaultDays().get(1).getDuration());
    }

    @Test
    public void shouldEnrichDurationIfBookedSlotsHaveZeroDuration() {
        // Given
        RotaSlot bookedSlot1 = RotaSlot.rotaSlot()
                .withDuration(0)
                .build();

        HearingListingNeeds hearing = hearingListingNeeds()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withEstimatedMinutes(20)
                .withBookedSlots(Arrays.asList(bookedSlot1))
                .build();

        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getBookedSlots().get(0).getDuration());
    }

    @Test
    public void shouldEnrichDurationIfBookedSlotsHaveOneMinuteDuration() {
        // Given
        RotaSlot bookedSlot1 = RotaSlot.rotaSlot()
                .withDuration(1)
                .build();

        HearingListingNeeds hearing = hearingListingNeeds()
                .withId(HEARING_ID)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withEstimatedMinutes(20)
                .withBookedSlots(Collections.singletonList(bookedSlot1))
                .build();

        // When
        HearingListingNeeds result = hearingDurationEnrichmentService.enrichWithDurations(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getBookedSlots().get(0).getDuration());
    }

    @Test
    public void shouldEnrichDurationIfNonDefaultDaysHaveNoDuration() {
        // Given
        uk.gov.justice.listing.commands.NonDefaultDay nonDefaultDay1 = uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                .withDuration(null)
                .build();

        UpdateHearingForListing hearing = updateHearingForListing()
                .withHearingId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withNonDefaultDays(Arrays.asList(nonDefaultDay1))
                .build();


        // When
        UpdateHearingForListing result = hearingDurationEnrichmentService.enrichWithDurationForUpdate(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getNonDefaultDays().get(0).getDuration());
    }

    @Test
    public void shouldEnrichDurationIfNonDefaultDaysHaveZeroDuration() {
        // Given
        uk.gov.justice.listing.commands.NonDefaultDay nonDefaultDay = uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                .withDuration(0)
                .build();

        UpdateHearingForListing hearing = updateHearingForListing()
                .withHearingId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withNonDefaultDays(Collections.singletonList(nonDefaultDay))
                .build();

        // When
        UpdateHearingForListing result = hearingDurationEnrichmentService.enrichWithDurationForUpdate(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getNonDefaultDays().get(0).getDuration());
    }

    @Test
    public void shouldEnrichDurationIfNonDefaultDaysHaveOneMinuteDuration() {
        // Given
        uk.gov.justice.listing.commands.NonDefaultDay nonDefaultDay = uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                .withDuration(1)
                .build();

        UpdateHearingForListing hearing = updateHearingForListing()
                .withHearingId(HEARING_ID)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withType(HearingType.hearingType().withId(UUID.fromString(HEARING_TYPE_ID)).build())
                .withNonDefaultDays(Collections.singletonList(nonDefaultDay))
                .build();


        // When
        UpdateHearingForListing result = hearingDurationEnrichmentService.enrichWithDurationForUpdate(hearing, envelope);

        // Then
        assertNotNull(result);
        assertEquals(DURATION_BY_HEARING_TYPE, result.getNonDefaultDays().get(0).getDuration());
    }


}