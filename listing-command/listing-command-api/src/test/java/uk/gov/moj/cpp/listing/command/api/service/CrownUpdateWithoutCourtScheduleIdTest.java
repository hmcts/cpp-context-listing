package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
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

    private final UUID hearingId = UUID.randomUUID();
    private final UUID courtCentreId = UUID.randomUUID();
    private final UUID courtRoomId = UUID.randomUUID();
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

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(spiUpdate(TUE, SUN, List.of(FRI)), mock(JsonEnvelope.class)));

        assertThat(rejection.getHttpStatus(), is(HttpStatus.SC_UNPROCESSABLE_ENTITY));
        assertThat(rejection.getErrorCode(), is("NO_AVAILABILITY"));
        assertThat(rejection.getUnavailableDates(), contains(WED.toString()));
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void rejectsWith422WhenTheStartDateHasNoFinalSession() {
        givenConverters();
        givenFinalSessionsOn(WED, THU);

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(spiUpdate(TUE, SUN, List.of(FRI)), mock(JsonEnvelope.class)));

        assertThat(rejection.getUnavailableDates(), contains(TUE.toString()));
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void rejectsWith422WhenTheWindowHasNoSessionAtAll() {
        givenConverters();
        givenFinalSessionsOn();

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(spiUpdate(TUE, SUN, List.of(FRI)), mock(JsonEnvelope.class)));

        assertThat(rejection.getUnavailableDates(), contains(TUE.toString(), WED.toString(), THU.toString()));
    }

    @Test
    void rejectsWith422WhenCourtschedulerBooksNothingAfterAPassingPreCheck() {
        givenConverters();
        givenFinalSessionsOn(TUE, WED, THU, FRI);
        givenBookedBlock();

        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(spiUpdate(TUE, SUN, List.of(FRI)), mock(JsonEnvelope.class)));

        assertThat(rejection.getUnavailableDates(), contains(TUE.toString(), WED.toString(), THU.toString()));
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
    }

    @Test
    void rejectsAWeekendStartDateWith422BeforeLookingAnythingUp() {
        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(spiUpdate(SAT, SAT.plusDays(5), List.of()), mock(JsonEnvelope.class)));

        assertThat(rejection.getErrorCode(), is("NO_AVAILABILITY"));
        assertThat(rejection.getUnavailableDates(), contains(SAT.toString()));
        verify(hearingSlotsService, never()).search(anyMap());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
        verify(courtSchedulerServiceAdapter, never()).crownFallbackSearchAndBook(any(), any(), any(), anyInt(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsASingleDayWeekendStartDateWith422() {
        final CrownMultiDayExtensionException rejection = assertThrows(CrownMultiDayExtensionException.class,
                () -> service.enrichWithCourtSchedules(spiUpdate(SUN, SUN, List.of()), mock(JsonEnvelope.class)));

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

    private JsonObject sessionJson(final LocalDate date) {
        return JsonObjects.createObjectBuilder()
                .add("courtScheduleId", sessionIds.get(date).toString())
                .add("courtRoomId", courtRoomId.toString())
                .add("courtHouseId", courtCentreId.toString())
                .add("sessionDate", date.toString())
                .build();
    }
}
