package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.SelectedCourtCentre;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory;
import uk.gov.moj.cpp.listing.command.api.util.SlotsToJsonStringConverter;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackResult;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownFallbackSource;
import uk.gov.moj.cpp.listing.common.crownfallback.CrownMultiDayExtensionException;
import uk.gov.moj.cpp.listing.common.service.CourtSchedulerServiceAdapter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.CourtSchedule;
import uk.gov.moj.cpp.listing.domain.ListUpdateHearing;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * SPRDT-1539: a CROWN update-hearing-for-listing with a courtroom and no courtScheduleId (the payload
 * the SPI helper sends) books one anchored crown.search.and.book block over the window's weekdays —
 * never a per-day MAGS search-and-book, whose NCFL-only matcher left the hearing unallocated.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrownUpdateWithoutCourtScheduleIdTest {

    private static final LocalDate TUE = LocalDate.of(2026, 11, 10);
    private static final LocalDate WED = TUE.plusDays(1);
    private static final LocalDate THU = TUE.plusDays(2);
    private static final LocalDate FRI = TUE.plusDays(3);
    private static final LocalDate SAT = TUE.plusDays(4);
    private static final LocalDate SUN = TUE.plusDays(5);
    private static final String OU_CODE = "C01CY00";

    @InjectMocks
    private CourtScheduleEnrichmentService service;
    @Mock
    private HearingSlotsService hearingSlotsService;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Mock
    private SlotsToJsonStringConverter slotsToJsonStringConverter;
    @Mock
    private CourtSchedulerServiceAdapter courtSchedulerServiceAdapter;
    @Mock
    private CourtCentreFactory courtCentreFactory;
    @Mock
    private HearingLookupService hearingLookupService;

    private final UUID hearingId = UUID.randomUUID();
    private final UUID courtCentreId = UUID.randomUUID();
    private final UUID courtRoomId = UUID.randomUUID();
    private final UUID otherCourtRoomId = UUID.randomUUID();
    private final Map<LocalDate, UUID> sessionIds = Map.of(
            TUE, UUID.randomUUID(), WED, UUID.randomUUID(), THU, UUID.randomUUID(), FRI, UUID.randomUUID());

    @AfterEach
    void neverUsesTheMagsSearchAndBook() {
        verify(hearingSlotsService, never()).searchBookSlots(anyMap());
    }

    @Test
    void booksOneCrownBlockOverTheWeekdaysAndSkipsTheWeekendForTheSpiPayload() {
        // F1 shape: Tue..Sun, Friday non-sitting, second nonDefaultDay on Sunday, raw durations 1 and 2.
        givenConverters();
        givenFinalSessionsOn(TUE, WED, THU, FRI);
        givenBookedBlock(TUE, WED, THU, FRI);
        givenListedInSessions();

        final UpdateHearingForListing result = service.enrichWithCourtSchedules(spiUpdate(TUE, SUN, List.of(FRI)), mock(JsonEnvelope.class));

        final Map<String, String> lookup = capture(Lookup.SEARCH);
        assertThat(lookup.get("status"), is("FINAL"));
        assertThat(lookup.get("jurisdiction"), is("CROWN"));
        assertThat(lookup.get("courtSession"), is("AD"));
        assertThat(lookup.get("courtRoomId"), is(courtRoomId.toString()));
        assertThat(lookup.get("ouCode"), is(OU_CODE));
        assertThat(lookup.get("sessionStartDate"), is(TUE.toString()));
        assertThat(lookup.get("sessionEndDate"), is(FRI.toString()));

        final Map<String, String> booking = capture(Lookup.BOOK);
        assertThat(booking.get("courtScheduleId"), is(sessionIds.get(TUE).toString()));
        assertThat(booking.get("durationInMinutes"), is("1440"));
        assertThat(booking.get("hearingDate"), is(TUE.toString()));
        assertThat(booking.get("endDate"), is(FRI.toString()));
        assertThat(booking.get("courtRoomId"), is(courtRoomId.toString()));
        assertThat(booking.get("earliestHearingTime"), startsWith("2026-11-10T10:00"));

        assertThat(result.getHearingDays().stream().map(HearingDay::getHearingDate).toList(), contains(TUE, WED, THU));
        assertThat(result.getHearingDays().stream().map(HearingDay::getCourtScheduleId).toList(),
                contains(sessionIds.get(TUE), sessionIds.get(WED), sessionIds.get(THU)));
        result.getHearingDays().forEach(day -> {
            assertThat(day.getCourtRoomId(), is(courtRoomId));
            assertThat(day.getIsDraft(), is(false));
            assertThat(day.getDurationMinutes(), is(360));
        });
        // The Tuesday nonDefaultDay start time is kept.
        assertThat(result.getHearingDays().get(0).getStartTime(), is(ZonedDateTime.parse("2026-11-10T10:00:00Z")));
    }

    @Test
    void sizesAnAllWeekdayWindowAtOneCourtDayPerWeekday() {
        givenConverters();
        givenFinalSessionsOn(TUE, WED, THU);
        givenBookedBlock(TUE, WED, THU);
        givenListedInSessions();

        final UpdateHearingForListing result = service.enrichWithCourtSchedules(spiUpdate(TUE, THU, List.of()), mock(JsonEnvelope.class));

        final Map<String, String> booking = capture(Lookup.BOOK);
        assertThat(booking.get("durationInMinutes"), is("1080"));
        assertThat(booking.get("endDate"), is(THU.toString()));
        assertThat(result.getHearingDays().stream().map(HearingDay::getHearingDate).toList(), contains(TUE, WED, THU));
    }

    @Test
    void doesNotRequireASessionOnANonSittingWeekday() {
        givenConverters();
        givenFinalSessionsOn(TUE, WED, THU);
        givenBookedBlock(TUE, WED, THU, FRI);
        givenListedInSessions();

        final UpdateHearingForListing result = service.enrichWithCourtSchedules(spiUpdate(TUE, SUN, List.of(FRI)), mock(JsonEnvelope.class));

        assertThat(result.getHearingDays().stream().map(HearingDay::getHearingDate).toList(), contains(TUE, WED, THU));
    }

    @Test
    void rejectsWith422WhenAMustSitWeekdayHasNoFinalSession() {
        givenConverters();
        givenFinalSessionsOn(TUE, THU, FRI);

        final UpdateHearingForListing update = spiUpdate(TUE, SUN, List.of(FRI));
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(update, envelope));

        assertThat(rejection.getHttpStatus(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(rejection.getErrorCode(), is("NO_AVAILABILITY"));
        assertThat(rejection.getUnavailableDates(), contains(WED.toString()));
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void rejectsWith422WhenTheStartDateHasNoFinalSession() {
        givenConverters();
        givenFinalSessionsOn(WED, THU);

        final UpdateHearingForListing update = spiUpdate(TUE, SUN, List.of(FRI));
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(update, envelope));

        assertThat(rejection.getUnavailableDates(), contains(TUE.toString()));
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void rejectsWith422WhenTheWindowHasNoSessionAtAll() {
        givenConverters();
        givenFinalSessionsOn();

        final UpdateHearingForListing update = spiUpdate(TUE, SUN, List.of(FRI));
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(update, envelope));

        assertThat(rejection.getUnavailableDates(), contains(TUE.toString(), WED.toString(), THU.toString()));
    }

    @Test
    void rejectsWith422WhenCourtschedulerBooksNothingAfterAPassingPreCheck() {
        givenConverters();
        givenFinalSessionsOn(TUE, WED, THU, FRI);
        givenBookedBlock();

        final UpdateHearingForListing update = spiUpdate(TUE, SUN, List.of(FRI));
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(update, envelope));

        assertThat(rejection.getUnavailableDates(), contains(TUE.toString(), WED.toString(), THU.toString()));
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
    }

    @Test
    void rejectsAWeekendStartDateWith422BeforeLookingAnythingUp() {
        final UpdateHearingForListing update = spiUpdate(SAT, SAT.plusDays(5), List.of());
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(update, envelope));

        assertThat(rejection.getErrorCode(), is("NO_AVAILABILITY"));
        assertThat(rejection.getUnavailableDates(), contains(SAT.toString()));
        verify(hearingSlotsService, never()).search(anyMap());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
        verify(courtSchedulerServiceAdapter, never()).crownFallbackSearchAndBook(any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsASingleDayWeekendStartDateWith422() {
        final UpdateHearingForListing update = spiUpdate(SUN, SUN, List.of());
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(update, envelope));

        assertThat(rejection.getUnavailableDates(), contains(SUN.toString()));
        verify(courtSchedulerServiceAdapter, never()).crownFallbackSearchAndBook(any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void looksTheOuCodeUpInReferenceDataWhenThePayloadHasNone() {
        givenConverters();
        givenFinalSessionsOn(TUE, WED, THU);
        givenBookedBlock(TUE, WED, THU);
        givenListedInSessions();
        final JsonEnvelope envelope = mock(JsonEnvelope.class);
        final uk.gov.justice.listing.commands.CourtCentreDetails centre = mock(uk.gov.justice.listing.commands.CourtCentreDetails.class);
        when(centre.getOucode()).thenReturn("C01XX00");
        when(courtCentreFactory.getCourtCentre(courtCentreId, envelope)).thenReturn(centre);

        final UpdateHearingForListing withoutSelectedCentre = UpdateHearingForListing.updateHearingForListing()
                .withValuesFrom(spiUpdate(TUE, THU, List.of()))
                .withSelectedCourtCentre(null)
                .build();
        service.enrichWithCourtSchedules(withoutSelectedCentre, envelope);

        assertThat(capture(Lookup.SEARCH).get("ouCode"), is("C01XX00"));
    }

    @Test
    void listSideBooksOneCrownBlockAnchoredOnTheFirstDayInsteadOfTheMagsLoop() {
        // SPRDT-1539 list side: a multi-day CROWN list payload with a room and no courtScheduleId.
        givenConverters();
        givenFinalSessionsOn(TUE);
        givenBookedBlock(TUE, WED, THU);
        givenListedInSessions();
        final uk.gov.justice.listing.commands.HearingListingNeeds hearing = uk.gov.justice.listing.commands.HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListedStartDateTime(ZonedDateTime.parse(TUE + "T10:00:00Z"))
                .withCourtCentre(uk.gov.justice.core.courts.CourtCentre.courtCentre()
                        .withId(courtCentreId).withRoomId(courtRoomId).withCode(OU_CODE).build())
                .withHearingDays(List.of(TUE, WED, THU).stream()
                        .map(day -> HearingDay.hearingDay().withHearingDate(day)
                                .withStartTime(ZonedDateTime.parse(day + "T10:00:00Z")).withDurationMinutes(360).build())
                        .toList())
                .build();

        final uk.gov.justice.listing.commands.HearingListingNeeds result = service.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        final Map<String, String> lookup = capture(Lookup.SEARCH);
        assertThat(lookup.get("sessionStartDate"), is(TUE.toString()));
        assertThat(lookup.get("sessionEndDate"), is(TUE.toString()));
        final Map<String, String> booking = capture(Lookup.BOOK);
        assertThat(booking.get("courtScheduleId"), is(sessionIds.get(TUE).toString()));
        assertThat(booking.get("durationInMinutes"), is("1080"));
        assertThat(result.getHearingDays().stream().map(HearingDay::getCourtScheduleId).toList(),
                contains(sessionIds.get(TUE), sessionIds.get(WED), sessionIds.get(THU)));
    }

    // ─── option 1: an allocated block keeps its per-day courtrooms ─────────────

    @Test
    void resizesTheHeldBlockWithoutAnAnchorOrPreCheckAndKeepsPerDayCourtrooms() {
        // Tue..Thu allocated in the main room with Wednesday moved to another room through
        // change-court-room-for-multiday-hearing; the update extends the block to Friday.
        givenConverters();
        givenStoredHearing("CROWN", true, courtRoomId, roomsByDate(TUE, courtRoomId, WED, otherCourtRoomId, THU, courtRoomId));
        givenBookedBlockInRooms(roomsByDate(TUE, courtRoomId, WED, otherCourtRoomId, THU, courtRoomId, FRI, courtRoomId));
        givenListedInSessions();
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        final UpdateHearingForListing result = service.enrichWithCourtSchedules(spiUpdate(TUE, FRI, List.of()), envelope);

        verify(hearingLookupService).findHearing(hearingId, envelope);
        verify(hearingSlotsService, never()).search(anyMap());
        final Map<String, String> booking = capture(Lookup.BOOK);
        assertThat("no anchor (dropped from the request body)", booking.get("courtScheduleId"), nullValue());
        assertThat(booking.get("hearingDate"), is(TUE.toString()));
        assertThat(booking.get("endDate"), is(FRI.toString()));
        assertThat(booking.get("durationInMinutes"), is("1440"));
        assertThat(booking.get("courtRoomId"), is(courtRoomId.toString()));
        assertThat(result.getHearingDays().stream().map(HearingDay::getHearingDate).toList(), contains(TUE, WED, THU, FRI));
        assertThat(result.getHearingDays().stream().map(HearingDay::getCourtRoomId).toList(),
                contains(courtRoomId, otherCourtRoomId, courtRoomId, courtRoomId));
    }

    @Test
    void resizesTheHeldBlockWithoutLookingUpSessionsWhenTheWindowIsUnchanged() {
        givenConverters();
        givenStoredHearing("CROWN", true, courtRoomId, roomsByDate(TUE, otherCourtRoomId, WED, courtRoomId, THU, courtRoomId));
        givenBookedBlockInRooms(roomsByDate(TUE, otherCourtRoomId, WED, courtRoomId, THU, courtRoomId));
        givenListedInSessions();

        final UpdateHearingForListing result = service.enrichWithCourtSchedules(spiUpdate(TUE, THU, List.of()), mock(JsonEnvelope.class));

        verify(hearingSlotsService, never()).search(anyMap());
        assertThat("no anchor", capture(Lookup.BOOK).get("courtScheduleId"), nullValue());
        assertThat(result.getHearingDays().stream().map(HearingDay::getCourtRoomId).toList(),
                contains(otherCourtRoomId, courtRoomId, courtRoomId));
    }

    @Test
    void rejectsWith422WhenCourtschedulerReturnsNothingForTheHeldBlock() {
        givenConverters();
        givenStoredHearing("CROWN", true, courtRoomId, roomsByDate(TUE, courtRoomId, WED, courtRoomId));
        givenBookedBlock();

        final UpdateHearingForListing update = spiUpdate(TUE, THU, List.of());
        final JsonEnvelope envelope = mock(JsonEnvelope.class);

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(update, envelope));

        assertThat(rejection.getUnavailableDates(), contains(TUE.toString(), WED.toString(), THU.toString()));
        verify(hearingSlotsService, never()).search(anyMap());
    }

    @Test
    void movesAnAllocatedMagistratesHearingIntoTheCrownRoomWithAnAnchor() {
        // The SPI journey: the stored hearing is an allocated MAGISTRATES hearing in the same room id.
        givenConverters();
        givenStoredHearing("MAGISTRATES", true, courtRoomId, roomsByDate(TUE, courtRoomId));
        givenFinalSessionsOn(TUE, WED, THU);
        givenBookedBlock(TUE, WED, THU);
        givenListedInSessions();

        service.enrichWithCourtSchedules(spiUpdate(TUE, THU, List.of()), mock(JsonEnvelope.class));

        assertThat(capture(Lookup.BOOK).get("courtScheduleId"), is(sessionIds.get(TUE).toString()));
    }

    @Test
    void movesEveryDayToANewMainCourtroomWithAnAnchor() {
        givenConverters();
        givenStoredHearing("CROWN", true, otherCourtRoomId, roomsByDate(TUE, otherCourtRoomId, WED, otherCourtRoomId, THU, otherCourtRoomId));
        givenFinalSessionsOn(TUE, WED, THU);
        givenBookedBlock(TUE, WED, THU);
        givenListedInSessions();

        final UpdateHearingForListing result = service.enrichWithCourtSchedules(spiUpdate(TUE, THU, List.of()), mock(JsonEnvelope.class));

        assertThat(capture(Lookup.SEARCH).get("courtRoomId"), is(courtRoomId.toString()));
        assertThat(capture(Lookup.BOOK).get("courtScheduleId"), is(sessionIds.get(TUE).toString()));
        result.getHearingDays().forEach(day -> assertThat(day.getCourtRoomId(), is(courtRoomId)));
    }

    @Test
    void movesWithAnAnchorWhenTheUpdateChangesTheFirstDay() {
        givenConverters();
        givenStoredHearing("CROWN", true, courtRoomId, roomsByDate(WED, courtRoomId, THU, courtRoomId));
        givenFinalSessionsOn(TUE, WED, THU);
        givenBookedBlock(TUE, WED, THU);
        givenListedInSessions();

        service.enrichWithCourtSchedules(spiUpdate(TUE, THU, List.of()), mock(JsonEnvelope.class));

        assertThat(capture(Lookup.SEARCH).get("sessionStartDate"), is(TUE.toString()));
        assertThat(capture(Lookup.BOOK).get("courtScheduleId"), is(sessionIds.get(TUE).toString()));
    }

    @Test
    void movesAnUnallocatedCrownHearingOntoFinalSessionsWithAnAnchor() {
        givenConverters();
        givenStoredHearing("CROWN", false, courtRoomId, roomsByDate(TUE, courtRoomId, WED, courtRoomId, THU, courtRoomId));
        givenFinalSessionsOn(TUE, WED, THU);
        givenBookedBlock(TUE, WED, THU);
        givenListedInSessions();

        service.enrichWithCourtSchedules(spiUpdate(TUE, THU, List.of()), mock(JsonEnvelope.class));

        assertThat(capture(Lookup.BOOK).get("courtScheduleId"), is(sessionIds.get(TUE).toString()));
    }

    @Test
    void booksAsANewBlockWhenTheViewstoreLookupFails() {
        givenConverters();
        when(hearingLookupService.findHearing(eq(hearingId), any())).thenThrow(new IllegalStateException("viewstore down"));
        givenFinalSessionsOn(TUE, WED, THU);
        givenBookedBlock(TUE, WED, THU);
        givenListedInSessions();

        service.enrichWithCourtSchedules(spiUpdate(TUE, THU, List.of()), mock(JsonEnvelope.class));

        assertThat(capture(Lookup.BOOK).get("courtScheduleId"), is(sessionIds.get(TUE).toString()));
    }

    // ─── a window with one weekday ─────────────────────────────────────────────

    @Test
    void booksAFridayToSundayWindowAsTheFridaySingleDayWithoutTheWeekendMinutes() {
        // Fri..Sun holds one weekday. As a block it reached courtscheduler as 360 minutes ending on the
        // Friday, which courtscheduler reads as a single day and rejects for the missing source (400),
        // surfacing as a false 422. The SPI payload also carries Sat and Sun days of 360 minutes each.
        final UUID bookedSessionId = UUID.randomUUID();
        when(courtSchedulerServiceAdapter.crownFallbackSearchAndBook(any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new CrownFallbackResult(hearingId, bookedSessionId, courtRoomId, FRI,
                        ZonedDateTime.parse(FRI + "T10:00:00Z"), ZonedDateTime.parse(FRI + "T16:00:00Z"),
                        360, false, "LGT", "CROWN_FB_UPDATE", false));

        final UpdateHearingForListing result = service.enrichWithCourtSchedules(spiUpdate(FRI, SUN, List.of()), mock(JsonEnvelope.class));

        verify(courtSchedulerServiceAdapter).crownFallbackSearchAndBook(eq(hearingId), eq(courtCentreId), eq(FRI), eq(360),
                eq(Optional.of(courtRoomId)), any(), eq(CrownFallbackSource.UPDATE_HEARING_FOR_LISTING), any(), any(), any());
        verify(hearingSlotsService, never()).search(anyMap());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
        assertThat(result.getHearingDays().stream().map(HearingDay::getHearingDate).toList(), contains(FRI));
        assertThat(result.getHearingDays().get(0).getCourtScheduleId(), is(bookedSessionId));
        assertThat(result.getHearingDays().get(0).getCourtRoomId(), is(courtRoomId));
        assertThat(result.getHearingDays().get(0).getDurationMinutes(), is(360));
        assertThat("the Sunday nonDefaultDay is dropped", result.getNonDefaultDays().size(), is(1));
    }

    @Test
    void keepsAFridayToMondayWindowOnTheBlockRoute() {
        givenConverters();
        givenFinalSessionsOn(FRI);
        givenBookedBlock(FRI);
        givenListedInSessions();
        final LocalDate monday = FRI.plusDays(3);

        service.enrichWithCourtSchedules(spiUpdate(FRI, monday, List.of(monday)), mock(JsonEnvelope.class));

        final Map<String, String> booking = capture(Lookup.BOOK);
        assertThat(booking.get("durationInMinutes"), is("720"));
        assertThat(booking.get("endDate"), is(monday.toString()));
        verify(courtSchedulerServiceAdapter, never()).crownFallbackSearchAndBook(any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any());
    }

    // ─── fixtures ──────────────────────────────────────────────────────────────

    private enum Lookup { SEARCH, BOOK }

    @SuppressWarnings("unchecked")
    private Map<String, String> capture(final Lookup lookup) {
        final ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        if (lookup == Lookup.SEARCH) {
            verify(hearingSlotsService).search(captor.capture());
        } else {
            verify(hearingSlotsService).multiDaySearchAndBook(captor.capture());
        }
        return captor.getValue();
    }

    /** The SPI helper's payload (cpp-apitests main ListingHelper.setDataForHearing), after enrichment. */
    private UpdateHearingForListing spiUpdate(final LocalDate start, final LocalDate end, final List<LocalDate> nonSittingDays) {
        final List<HearingDay> days = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            if (!nonSittingDays.contains(day)) {
                days.add(HearingDay.hearingDay()
                        .withHearingDate(day)
                        .withStartTime(ZonedDateTime.parse(day + "T09:00:00Z"))
                        .withDurationMinutes(360)
                        .withCourtCentreId(courtCentreId)
                        .withCourtRoomId(courtRoomId)
                        .build());
            }
        }
        return UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre()
                        .withId(courtCentreId)
                        .withOuCode(OU_CODE)
                        .withCourtRoomId(courtRoomId)
                        .build())
                .withStartDate(start)
                .withEndDate(end)
                .withNonSittingDays(nonSittingDays)
                .withHearingDays(days)
                .withNonDefaultDays(List.of(
                        NonDefaultDay.nonDefaultDay()
                                .withStartTime(ZonedDateTime.parse(start + "T10:00:00Z"))
                                .withDuration(1)
                                .build(),
                        NonDefaultDay.nonDefaultDay()
                                .withStartTime(ZonedDateTime.parse(end + "T09:30:00Z"))
                                .withDuration(2)
                                .build()))
                .build();
    }

    private void givenConverters() {
        when(objectToJsonObjectConverter.convert(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenAnswer(inv -> {
            final JsonObject json = inv.getArgument(0);
            final CourtSchedule session = new CourtSchedule();
            session.setCourtScheduleId(json.getString("courtScheduleId"));
            session.setCourtRoomId(json.getString("courtRoomId"));
            session.setCourtHouseId(json.getString("courtHouseId"));
            session.setSessionDate(LocalDate.parse(json.getString("sessionDate")));
            session.setDraft(false);
            session.setHearingStartTime(json.getString("sessionDate") + "T09:00:00Z");
            return session;
        });
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class))).thenAnswer(inv -> {
            final JsonObject json = inv.getArgument(0);
            final ListUpdateHearing listed = new ListUpdateHearing();
            listed.setCourtScheduleId(json.getString("courtScheduleId"));
            listed.setHearingStartTime(json.getString("hearingStartTime"));
            listed.setDuration(json.getInt("duration"));
            return listed;
        });
    }

    private void givenFinalSessionsOn(final LocalDate... dates) {
        final JsonArrayBuilder slots = JsonObjects.createArrayBuilder();
        for (final LocalDate date : dates) {
            slots.add(sessionJson(date));
        }
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(JsonObjects.createObjectBuilder().add("hearingSlots", slots).build());
        when(hearingSlotsService.search(anyMap())).thenReturn(response);
    }

    private void givenBookedBlock(final LocalDate... dates) {
        final JsonArrayBuilder sessions = JsonObjects.createArrayBuilder();
        for (final LocalDate date : dates) {
            sessions.add(sessionJson(date));
        }
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(JsonObjects.createObjectBuilder().add("sessions", sessions).build());
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(response);
    }

    private void givenListedInSessions() {
        final JsonArrayBuilder hearings = JsonObjects.createArrayBuilder();
        sessionIds.forEach((date, id) -> hearings.add(JsonObjects.createObjectBuilder()
                .add("courtScheduleId", id.toString())
                .add("hearingStartTime", date + "T09:00:00Z")
                .add("duration", 360)));
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(JsonObjects.createObjectBuilder().add("hearings", hearings).build());
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(response);
        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().build());
    }

    private static Map<LocalDate, UUID> roomsByDate(final Object... dateRoomPairs) {
        final Map<LocalDate, UUID> rooms = new TreeMap<>();
        for (int i = 0; i < dateRoomPairs.length; i += 2) {
            rooms.put((LocalDate) dateRoomPairs[i], (UUID) dateRoomPairs[i + 1]);
        }
        return rooms;
    }

    /** The listing.search.hearing projection of the hearing as stored before the update. */
    private void givenStoredHearing(final String jurisdiction, final boolean allocated, final UUID mainCourtRoomId,
                                    final Map<LocalDate, UUID> heldRoomByDate) {
        final JsonArrayBuilder days = JsonObjects.createArrayBuilder();
        heldRoomByDate.forEach((date, room) -> days.add(JsonObjects.createObjectBuilder()
                .add("hearingDate", date.toString())
                .add("courtScheduleId", sessionIds.get(date).toString())
                .add("courtCentreId", courtCentreId.toString())
                .add("courtRoomId", room.toString())
                .add("isCancelled", false)));
        when(hearingLookupService.findHearing(eq(hearingId), any())).thenReturn(Optional.of(JsonObjects.createObjectBuilder()
                .add("id", hearingId.toString())
                .add("jurisdictionType", jurisdiction)
                .add("allocated", allocated)
                .add("courtCentreId", courtCentreId.toString())
                .add("courtRoomId", mainCourtRoomId.toString())
                .add("hearingDays", days)
                .build()));
    }

    private void givenBookedBlockInRooms(final Map<LocalDate, UUID> roomByDate) {
        final JsonArrayBuilder sessions = JsonObjects.createArrayBuilder();
        roomByDate.forEach((date, room) -> sessions.add(JsonObjects.createObjectBuilder()
                .add("courtScheduleId", sessionIds.get(date).toString())
                .add("courtRoomId", room.toString())
                .add("courtHouseId", courtCentreId.toString())
                .add("sessionDate", date.toString())));
        final Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(JsonObjects.createObjectBuilder().add("sessions", sessions).build());
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(response);
    }

    private JsonObject sessionJson(final LocalDate date) {
        return JsonObjects.createObjectBuilder()
                .add("courtScheduleId", sessionIds.get(date).toString())
                .add("courtRoomId", courtRoomId.toString())
                .add("courtHouseId", courtCentreId.toString())
                .add("sessionDate", date.toString())
                .build();
    }
}
