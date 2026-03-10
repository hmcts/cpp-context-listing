package uk.gov.moj.cpp.listing.command.api.service;


import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_LOCAL_DATE;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_UTC_DATE_TIME;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.FUTURE_ZONED_DATE_TIME;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.INTEGER;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.NonDefaultDay;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.courtcentre.HearingTypeFactory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HearingDaysEnrichmentServiceTest {

    private static final int DEFAULT_DURATION = 20;
    @InjectMocks
    private HearingDaysEnrichmentService hearingDaysEnrichmentService;
    @Mock
    private HearingTypeFactory hearingTypeFactory;
    @Mock
    private HearingDurationEnrichmentService hearingDurationEnrichmentService;
    @Mock
    private CourtScheduleEnrichmentService courtScheduleEnrichmentService;
    @Mock
    private JsonEnvelope jsonEnvelope;
    private RotaSlot defaultSlot;
    private HearingDay defaultHearingDay;
    private NonDefaultDay defaultNonDefaultDay;
    private uk.gov.justice.listing.commands.NonDefaultDay defaultCommandNonDefaultDay;

    @BeforeEach
    void setUp() {
        defaultSlot = RotaSlot.rotaSlot()
                .withCourtCentreId(randomUUID().toString())
                .withCourtRoomId(INTEGER.next())
                .withCourtScheduleId(randomUUID().toString())
                .withDuration(DEFAULT_DURATION)
                .withOucode(STRING.next())
                .withRoomId(randomUUID().toString())
                .withSession(STRING.next())
                .withStartTime(FUTURE_UTC_DATE_TIME.next()).build();
        defaultHearingDay = HearingDay.hearingDay()
                .withCourtCentreId(randomUUID())
                .withCourtRoomId(randomUUID())
                .withHearingDate(FUTURE_LOCAL_DATE.next())
                .withStartTime(FUTURE_ZONED_DATE_TIME.next())
                .withCourtScheduleId(randomUUID())
                .withDurationMinutes(DEFAULT_DURATION)
                .build();

        defaultNonDefaultDay = NonDefaultDay.nonDefaultDay()
                .withCourtCentreId(randomUUID().toString())
                .withCourtRoomId(INTEGER.next())
                .withOucode(STRING.next())
                .withRoomId(randomUUID().toString())
                .withStartTime(FUTURE_UTC_DATE_TIME.next())
                .withDuration(DEFAULT_DURATION)
                .withCourtScheduleId(randomUUID().toString())
                .withSession(STRING.next())
                .build();

        defaultCommandNonDefaultDay = uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                .withCourtCentreId(randomUUID().toString())
                .withCourtRoomId(INTEGER.next())
                .withOucode(STRING.next())
                .withRoomId(randomUUID().toString())
                .withStartTime(FUTURE_UTC_DATE_TIME.next())
                .withDuration(DEFAULT_DURATION)
                .withCourtScheduleId(randomUUID().toString())
                .withSession(STRING.next())
                .build();
        // Common setup if needed
    }

    @Test
    public void shouldEnrichHearingDaysFromHearingDays() {
        // Given
        final List<HearingDay> hearingDays = Arrays.asList(createHearingDay(LocalDate.now()), createHearingDay(LocalDate.now().plusDays(1)));
        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withId(randomUUID())
                .withHearingDays(hearingDays)
                .withCourtCentre(CourtCentre.courtCentre().withRoomId(randomUUID()).build())
                .build();

        // When
        HearingListingNeeds enrichedHearing = hearingDaysEnrichmentService.enrichHearings(hearing, jsonEnvelope);

        // Then
        assertNotNull(enrichedHearing);
        assertEquals(JurisdictionType.MAGISTRATES, enrichedHearing.getJurisdictionType());
        assertNotNull(enrichedHearing.getHearingDays());
        assertFalse(enrichedHearing.getHearingDays().isEmpty());
        enrichedHearing.getHearingDays().forEach(hearingDay -> assertNotNull(hearingDay.getCourtScheduleId()));

        assertEquals(2, enrichedHearing.getHearingDays().size());

    }

    @Test
    public void shouldEnrichHearingsForMagistratesJurisdiction() {
        // Given
        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withId(randomUUID())
                .withListedStartDateTime(FUTURE_ZONED_DATE_TIME.next())
                .withCourtCentre(CourtCentre.courtCentre().withRoomId(randomUUID()).build())
                .build();

        // When
        HearingListingNeeds enrichedHearing = hearingDaysEnrichmentService.enrichHearings(hearing, jsonEnvelope);

        // Then
        assertNotNull(enrichedHearing);
        assertEquals(JurisdictionType.MAGISTRATES, enrichedHearing.getJurisdictionType());
    }

    @Test
    public void shouldEnrichHearingsForCrownJurisdiction() {
        // Given
        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(randomUUID())
                .withListedStartDateTime(FUTURE_ZONED_DATE_TIME.next())
                .withCourtCentre(CourtCentre.courtCentre().withRoomId(randomUUID()).build())
                .build();

        // When
        HearingListingNeeds enrichedHearing = hearingDaysEnrichmentService.enrichHearings(hearing, jsonEnvelope);

        // Then
        assertNotNull(enrichedHearing);
        assertEquals(JurisdictionType.CROWN, enrichedHearing.getJurisdictionType());
    }

    @Test
    public void shouldEnrichSingleHearingForMagistratesJurisdiction() {
        LocalDate date = LocalDate.now();
        // Given
        UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withHearingId(randomUUID())
                .withStartDate(date)
                .withEndDate(date)
                .withNonSittingDays(emptyList())
                .withNonDefaultDays(emptyList())
                .withHearingDays(Arrays.asList(
                        createHearingDay(LocalDate.now()),
                        createHearingDay(LocalDate.now().plusDays(1))))
                .build();

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(hearing, jsonEnvelope);

        // Then
        assertNotNull(enrichedHearing);
        assertEquals(JurisdictionType.MAGISTRATES, enrichedHearing.getJurisdictionType());
        enrichedHearing.getHearingDays().forEach(hearingDay -> assertNotNull(hearingDay.getCourtScheduleId()));
    }

    @Test
    public void shouldEnrichSingleHearingForCrownJurisdiction() {
        // Given
        UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingId(randomUUID())
                .withNonDefaultDays(Arrays.asList(
                        createCommandNonDefaultDay(LocalDate.now(), "AM"),
                        createCommandNonDefaultDay(LocalDate.now().plusDays(1), "PM")))
                .withStartDate(FUTURE_LOCAL_DATE.next())
                .withEndDate(FUTURE_LOCAL_DATE.next().plusDays(2))
                .withNonSittingDays(List.of(LocalDate.now().plusDays(3)))
                .withHearingId(randomUUID())
                .build();

        // When
        UpdateHearingForListing enrichedHearing = hearingDaysEnrichmentService.enrichHearing(hearing, jsonEnvelope);

        // Then
        assertNotNull(enrichedHearing);
        assertEquals(JurisdictionType.CROWN, enrichedHearing.getJurisdictionType());
    }

    @Test
    public void shouldEnrichHearingDaysFromBookedSlots() {
        // Given
        List<RotaSlot> bookedSlots = Arrays.asList(
                createRotaSlot(LocalDate.now().plusDays(1), "AM"),
                createRotaSlot(LocalDate.now().plusDays(2), "AM")
        );

        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withId(randomUUID())
                .withBookedSlots(bookedSlots)
                .withCourtCentre(CourtCentre.courtCentre().withRoomId(randomUUID()).build())
                .build();

        // When
        HearingListingNeeds enrichedHearing = hearingDaysEnrichmentService.enrichHearings(hearing, jsonEnvelope);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertFalse(enrichedHearing.getHearingDays().isEmpty());
        enrichedHearing.getHearingDays().forEach(hearingDay -> assertNotNull(hearingDay.getCourtScheduleId()));

        assertEquals(2, enrichedHearing.getHearingDays().size());
    }

    @Test
    public void shouldEnrichHearingDaysFromNonDefaultDays() {
        // Given
        List<uk.gov.justice.core.courts.NonDefaultDay> nonDefaultDays = Arrays.asList(
                createNonDefaultDay(LocalDate.now(), "AM"),
                createNonDefaultDay(LocalDate.now().plusDays(1), "AM")
        );

        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withId(randomUUID())
                .withNonDefaultDays(nonDefaultDays)
                .withCourtCentre(CourtCentre.courtCentre().withRoomId(randomUUID()).build())
                .build();

        // When
        HearingListingNeeds enrichedHearing = hearingDaysEnrichmentService.enrichHearings(hearing, jsonEnvelope);

        // Then
        assertNotNull(enrichedHearing);
        assertNotNull(enrichedHearing.getHearingDays());
        assertFalse(enrichedHearing.getHearingDays().isEmpty());
        assertEquals(2, enrichedHearing.getHearingDays().size());
        enrichedHearing.getHearingDays().forEach(hearingDay -> assertNotNull(hearingDay.getCourtScheduleId()));
    }

    @Test
    public void shouldHandleNullJurisdictionType() {
        // Given
        HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withId(randomUUID())
                .build();

        // When
        HearingListingNeeds enrichedHearing = hearingDaysEnrichmentService.enrichHearings(hearing, jsonEnvelope);

        // Then
        assertNotNull(enrichedHearing);
        assertTrue(enrichedHearing.getHearingDays() == null || enrichedHearing.getHearingDays().isEmpty());
    }

    private HearingDay createHearingDay(LocalDate date) {
        return HearingDay.hearingDay()
                .withValuesFrom(defaultHearingDay)
                .withHearingDate(date)
                .withCourtScheduleId(randomUUID())
                .build();
    }

    private NonDefaultDay createNonDefaultDay(LocalDate date, String session) {
        return NonDefaultDay.nonDefaultDay()
                .withValuesFrom(defaultNonDefaultDay)
                .withStartTime(createStartTime(date, session))
                .build();
    }

    private uk.gov.justice.listing.commands.NonDefaultDay createCommandNonDefaultDay(LocalDate date, String session) {
        return uk.gov.justice.listing.commands.NonDefaultDay.nonDefaultDay()
                .withValuesFrom(defaultCommandNonDefaultDay)
                .withStartTime(createStartTime(date, session))
                .build();
    }

    private RotaSlot createRotaSlot(LocalDate date, String session) {
        return RotaSlot.rotaSlot().withValuesFrom(defaultSlot).withStartTime(createStartTime(date, session)).build();
    }

    private ZonedDateTime createStartTime(LocalDate date, String session) {
        return "PM".equalsIgnoreCase(session) ?
                date.atTime(13, 0).atZone(ZoneId.of("UTC")) :
                date.atTime(9, 0).atZone(ZoneId.of("UTC"));
    }
} 