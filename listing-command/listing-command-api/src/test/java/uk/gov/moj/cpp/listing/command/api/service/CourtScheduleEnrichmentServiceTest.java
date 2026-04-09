package uk.gov.moj.cpp.listing.command.api.service;

import uk.gov.justice.services.messaging.JsonObjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.api.util.FileUtil.givenPayload;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.core.courts.WeekCommencingDate;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.commands.NonDefaultDay;
import uk.gov.justice.listing.commands.UpdateHearingForListing;
import uk.gov.justice.listing.courts.SelectedCourtCentre;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.util.SlotsToJsonStringConverter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.CourtSchedule;
import uk.gov.moj.cpp.listing.domain.HearingSlotSearchResponse;
import uk.gov.moj.cpp.listing.domain.ListUpdateHearing;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtScheduleEnrichmentServiceTest {
    @InjectMocks
    private CourtScheduleEnrichmentService courtScheduleEnrichmentService;
    @Mock
    private HearingSlotsService hearingSlotsService;
    @Mock
    private Response response;
    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;
    @Mock
    private JsonObjectToObjectConverter jsonObjectConverter;
    @Mock
    private SlotsToJsonStringConverter slotsToJsonStringConverter;

    @Test
    void searchAndBookShouldReturnBookedHearingSlots() {
        final String hearingId = "5416c10a-0cf1-49d5-a7c9-5761ff3bdf2c";
        String ouCode = "OU12345";
        String hearingSessionDate = LocalDate.now().toString();
        String courtRoomId = UUID.randomUUID().toString();
        String hearingSessionDateSearchCutOff = LocalDate.now().plusDays(7).toString();
        String sessionStartTime = LocalDate.now().toString();
        Integer durationInMinutes = 20;

        final JsonObject searchBookResponse = givenPayload("/courtscheduler.search.book.hearing.slots.json");

        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(searchBookResponse);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(searchBookResponse);

        final HearingSlotSearchResponse hearingSlotSearchResponse = courtScheduleEnrichmentService.
                searchAndBookSlots(hearingId, ouCode, hearingSessionDate, courtRoomId, hearingSessionDateSearchCutOff, sessionStartTime, durationInMinutes, true);

        assertThat(hearingSlotSearchResponse.courtScheduleId(), is("23681024-8eac-4890-8c44-4651ad48cb24"));
        assertThat(hearingSlotSearchResponse.sessionStartTime(), is("2020-05-26T09:00:000Z"));
        assertThat(hearingSlotSearchResponse.hearingId(), is(hearingId));
    }

    @Test
    void enrichShouldAddMultiDayParamsOnSearch() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now();
        final LocalDate day2 = day1.plusDays(1);

        final HearingDay hd1 = HearingDay.hearingDay()
                .withCourtRoomId(courtRoomId)
                .withHearingDate(day1)
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(30)
                .build();

        final HearingDay hd2 = HearingDay.hearingDay()
                .withCourtRoomId(courtRoomId)
                .withHearingDate(day2)
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC).plusHours(1))
                .withDurationMinutes(30)
                .build();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withCourtRoomId(courtRoomId)
                .withStartDate(day1)
                .withHearingDays(Arrays.asList(hd1, hd2))
                .build();

        final String bookedCourtScheduleId = UUID.randomUUID().toString();
        final JsonObject searchJson = JsonObjects.createObjectBuilder()
                .add("hearingSlots", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", bookedCourtScheduleId)
                                .add("courtRoomId", courtRoomId.toString())
                                .add("sessionStartTime", "2020-01-01T09:00:00Z")))
                .build();

        final Response searchResponse = mock(Response.class);
        when(searchResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(searchResponse.getEntity()).thenReturn(searchJson);
        when(hearingSlotsService.search(anyMap())).thenReturn(searchResponse);
        when(objectToJsonObjectConverter.convert(searchJson)).thenReturn(searchJson);

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(bookedCourtScheduleId).build());

        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", bookedCourtScheduleId)
                                .add("hearingStartTime", "2020-01-01T09:00:00Z")
                                .add("duration", 30)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        @SuppressWarnings("unchecked")
        final org.mockito.ArgumentCaptor<java.util.Map<String, String>> mapCaptor =
                org.mockito.ArgumentCaptor.forClass(java.util.Map.class);

        courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService, times(2)).search(mapCaptor.capture());

        for (java.util.Map<String, String> qp : mapCaptor.getAllValues()) {
            assertThat(qp.get("courtSession"), is("AD"));
            assertThat(qp.get("showOverbookedSlots"), is(Boolean.TRUE.toString()));
            assertThat(qp.get("isSlotBased"), is(Boolean.FALSE.toString()));
        }
    }

    @Test
    void enrichShouldNotIncludeStartTimeForMultiDaySearch() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now();
        final LocalDate day2 = day1.plusDays(1);

        final HearingDay hd1 = HearingDay.hearingDay()
                .withCourtRoomId(courtRoomId)
                .withHearingDate(day1)
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(30)
                .build();

        final HearingDay hd2 = HearingDay.hearingDay()
                .withCourtRoomId(courtRoomId)
                .withHearingDate(day2)
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC).plusHours(1))
                .withDurationMinutes(30)
                .build();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withCourtRoomId(courtRoomId)
                .withStartDate(day1)
                .withHearingDays(Arrays.asList(hd1, hd2))
                .build();

        final String bookedCourtScheduleId = UUID.randomUUID().toString();
        final JsonObject searchJson = JsonObjects.createObjectBuilder()
                .add("hearingSlots", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", bookedCourtScheduleId)
                                .add("courtRoomId", courtRoomId.toString())
                                .add("sessionStartTime", "2020-01-01T09:00:00Z")))
                .build();

        final Response searchResponse = mock(Response.class);
        when(searchResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(searchResponse.getEntity()).thenReturn(searchJson);
        when(hearingSlotsService.search(anyMap())).thenReturn(searchResponse);
        when(objectToJsonObjectConverter.convert(searchJson)).thenReturn(searchJson);

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(bookedCourtScheduleId).build());

        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", bookedCourtScheduleId)
                                .add("hearingStartTime", "2020-01-01T09:00:00Z")
                                .add("duration", 30)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        @SuppressWarnings("unchecked")
        final org.mockito.ArgumentCaptor<java.util.Map<String, String>> mapCaptor =
                org.mockito.ArgumentCaptor.forClass(java.util.Map.class);

        courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService, times(2)).search(mapCaptor.capture());
        for (java.util.Map<String, String> qp : mapCaptor.getAllValues()) {
            assertThat(qp.get("courtSession"), is("AD"));
            assertThat(qp.get("isSlotBased"), is(Boolean.FALSE.toString()));
            assertThat("hearingStartTime should not be sent for multi-day search",
                    qp.containsKey(CourtScheduleEnrichmentService.HEARING_START_TIME), is(false));
        }
    }

    // ─── CROWN needsCourtScheduleEnrichment tests ────────────────────────

    @Test
    void shouldReturnTrueForCrownFixedDateWithCourtScheduleId() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(UUID.randomUUID())
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(true));
    }

    @Test
    void shouldReturnFalseForCrownWeekCommencing() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(LocalDate.now().toString())
                        .withDuration(1)
                        .build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(UUID.randomUUID())
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(false));
    }

    @Test
    void shouldReturnFalseForCrownWithNoCourtScheduleIds() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(false));
    }

    @Test
    void shouldReturnFalseForCrownWithEmptyHearingDays() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.emptyList())
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(false));
    }

    @Test
    void shouldNeedEnrichmentForCrownAllocationCandidateWithoutCourtScheduleIds() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListedStartDateTime(ZonedDateTime.now())
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(UUID.randomUUID()).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(true));
    }

    @Test
    void shouldNotNeedEnrichmentForCrownWeekCommencingEvenIfAllocationCandidate() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListedStartDateTime(ZonedDateTime.now())
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(UUID.randomUUID()).build())
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(LocalDate.now().toString())
                        .withDuration(1)
                        .build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(false));
    }

    // ─── CROWN single-day enrichment tests ───────────────────────────────

    @Test
    void shouldEnrichCrownSingleDayWithNonDraftSession() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds
        final CourtSchedule cs = new CourtSchedule();
        cs.setCourtScheduleId(courtScheduleId.toString());
        cs.setSessionDate(sessionDate);
        cs.setCourtRoomId(courtRoomId.toString());
        cs.setCourtHouseId(courtHouseId.toString());
        cs.setDraft(false);
        cs.setHearingStartTime("2026-03-16T10:00:00Z");

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("isDraft", false)))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        // Mock listHearingInCourtSessions
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("hearingStartTime", "2026-03-16T10:00:00Z")
                                .add("duration", 240)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        // listHearingInCourtSessions should be called for non-draft sessions
        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        assertThat(result.getHearingDays().size(), is(1));
        assertThat(result.getHearingDays().get(0).getCourtScheduleId().toString(), is(courtScheduleId.toString()));
    }

    @Test
    void shouldNotCallListHearingWhenCrownSingleDaySessionIsDraft() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds - isDraft=true
        final CourtSchedule cs = new CourtSchedule();
        cs.setCourtScheduleId(courtScheduleId.toString());
        cs.setSessionDate(sessionDate);
        cs.setCourtRoomId(courtRoomId.toString());
        cs.setCourtHouseId(courtHouseId.toString());
        cs.setDraft(true);
        cs.setHearingStartTime("2026-03-16T10:00:00Z");

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("isDraft", true)))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        // listHearingInCourtSessions should NOT be called for draft sessions
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        // HearingDays should carry isDraft=true from sanity check
        assertThat(result.getHearingDays().size(), is(1));
        assertThat(result.getHearingDays().get(0).getIsDraft(), is(true));
    }

    @Test
    void shouldSkipCrownEnrichmentWhenWeekCommencing() {
        final UUID hearingId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(LocalDate.now().toString())
                        .withDuration(1)
                        .build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(UUID.randomUUID())
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        // No court scheduler calls should be made
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
        // Hearing should be returned unchanged
        assertThat(result.getId(), is(hearingId));
    }

    @Test
    void shouldSearchAndBookForCrownListWithoutCourtScheduleIdsWhenAllocationCandidate() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withListedStartDateTime(ZonedDateTime.now().plusDays(5))
                .withEndDate(LocalDate.now().plusDays(5).toString())
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withStartTime(ZonedDateTime.now().plusDays(5).withHour(10).withMinute(0))
                                .withDurationMinutes(240)
                                .build()))
                .withEstimatedMinutes(240)
                .build();

        final JsonObject searchBookResponse = givenPayload("/courtscheduler.search.book.hearing.slots.json");
        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(searchBookResponse);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        // searchAndBook should be called for Crown without courtScheduleIds
        verify(hearingSlotsService).searchBookSlots(anyMap());
        assertThat(result.getHearingDays().size(), is(1));
        assertThat(result.getHearingDays().get(0).getCourtScheduleId().toString(), is("23681024-8eac-4890-8c44-4651ad48cb24"));
    }

    // ─── CROWN multi-day enrichment tests ────────────────────────────────

    @Test
    void shouldEnrichCrownMultiDayWithConsecutiveSessions() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtScheduleId3 = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now().plusDays(5);

        // Multi-day: estimatedMinutes > 360
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(1080)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId1)
                                .withHearingDate(day1)
                                .withDurationMinutes(360)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook
        final CourtSchedule cs1 = buildCourtSchedule(courtScheduleId1, courtRoomId, courtHouseId, day1, false);
        final CourtSchedule cs2 = buildCourtSchedule(courtScheduleId2, courtRoomId, courtHouseId, day1.plusDays(1), false);
        final CourtSchedule cs3 = buildCourtSchedule(courtScheduleId3, courtRoomId, courtHouseId, day1.plusDays(2), false);

        final JsonObject multiDayResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs1))
                        .add(buildCsJson(cs2))
                        .add(buildCsJson(cs3)))
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(multiDayResponseJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class)))
                .thenReturn(cs1, cs2, cs3);

        // Mock listHearingInCourtSessions for booking
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId1, "2026-03-16T10:00:00Z", 360))
                        .add(buildListHearingJson(courtScheduleId2, "2026-03-17T10:00:00Z", 360))
                        .add(buildListHearingJson(courtScheduleId3, "2026-03-18T10:00:00Z", 360)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder()
                        .add(courtScheduleId1.toString())
                        .add(courtScheduleId2.toString())
                        .add(courtScheduleId3.toString())
                        .build());

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        verify(hearingSlotsService).multiDaySearchAndBook(anyMap());
        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        assertThat(result.getHearingDays().size(), is(3));
    }

    @Test
    void shouldReturnUnchangedWhenMultiDaySearchReturnsEmpty() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(1080)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(360)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook returning empty
        final JsonObject emptyResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder())
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(emptyResponseJson);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        // Original hearingDays should be preserved
        assertThat(result.getHearingDays().size(), is(1));
    }

    @Test
    void shouldNotCallListHearingWhenMultiDaySessionsHaveDraft() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtScheduleId3 = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(1080)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId1)
                                .withHearingDate(day1)
                                .withDurationMinutes(360)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook - one session is draft
        final CourtSchedule cs1 = buildCourtSchedule(courtScheduleId1, courtRoomId, courtHouseId, day1, false);
        final CourtSchedule cs2 = buildCourtSchedule(courtScheduleId2, courtRoomId, courtHouseId, day1.plusDays(1), true);  // draft!
        final CourtSchedule cs3 = buildCourtSchedule(courtScheduleId3, courtRoomId, courtHouseId, day1.plusDays(2), false);

        final JsonObject multiDayResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs1))
                        .add(buildCsJson(cs2))
                        .add(buildCsJson(cs3)))
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(multiDayResponseJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class)))
                .thenReturn(cs1, cs2, cs3);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        // listHearingInCourtSessions should NOT be called when any session is draft
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        // Expanded hearingDays should be returned with isDraft from sessions
        assertThat(result.getHearingDays().size(), is(3));
    }

    // ─── CROWN update hearing enrichment tests ───────────────────────────

    @Test
    void shouldSkipCrownUpdateEnrichmentWhenNoCourtScheduleIds() {
        final UUID hearingId = UUID.randomUUID();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        // No court scheduler calls
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        assertThat(result.getHearingId(), is(hearingId));
    }

    @Test
    void shouldSearchAndBookForCrownUpdateWhenAllocationCandidateWithoutCourtScheduleIds() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withStartDate(LocalDate.now().plusDays(5))
                .withEndDate(LocalDate.now().plusDays(5))
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(240)
                                .withCourtRoomId(courtRoomId)
                                .withCourtCentreId(courtCentreId)
                                .build()))
                .build();

        final JsonObject searchBookResponse = givenPayload("/courtscheduler.search.book.hearing.slots.json");
        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(searchBookResponse);

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService).searchBookSlots(anyMap());
        // listHearingInCourtSessions should NOT be called — update searchAndBook returns directly
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        assertThat(result.getHearingDays().size(), is(1));
        // courtScheduleId should be set from searchAndBook response
        assertThat(result.getHearingDays().get(0).getCourtScheduleId().toString(), is("23681024-8eac-4890-8c44-4651ad48cb24"));
        // courtRoomId should be PRESERVED from the original hearing day, not overwritten by searchAndBook
        assertThat(result.getHearingDays().get(0).getCourtRoomId(), is(courtRoomId));
    }

    @Test
    void shouldReturnUnchangedWhenCrownUpdateSearchAndBookReturnsEmpty() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withStartDate(LocalDate.now().plusDays(5))
                .withEndDate(LocalDate.now().plusDays(5))
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // searchAndBook returns empty response
        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        // Return empty hearingSlots object
        when(objectToJsonObjectConverter.convert(any())).thenReturn(
                javax.json.Json.createObjectBuilder().add("hearingSlots", javax.json.Json.createObjectBuilder()).build());

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService).searchBookSlots(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        // Should return unchanged hearing when searchAndBook returns empty
        assertThat(result.getHearingId(), is(hearingId));
        assertThat(result.getHearingDays().get(0).getCourtScheduleId(), is(org.hamcrest.CoreMatchers.nullValue()));
    }

    @Test
    void shouldEnrichCrownUpdateSingleDay() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds - non-draft
        final CourtSchedule cs = new CourtSchedule();
        cs.setCourtScheduleId(courtScheduleId.toString());
        cs.setSessionDate(sessionDate);
        cs.setCourtRoomId(courtRoomId.toString());
        cs.setCourtHouseId(courtHouseId.toString());
        cs.setDraft(false);
        cs.setHearingStartTime("2026-03-16T10:00:00Z");

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        // Mock listHearingInCourtSessions
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId, "2026-03-16T10:00:00Z", 240)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        assertThat(result.getHearingDays().size(), is(1));
    }

    @Test
    void shouldSetIsDraftOnCrownUpdateWhenDraftSessions() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds - isDraft=true
        final CourtSchedule cs = new CourtSchedule();
        cs.setCourtScheduleId(courtScheduleId.toString());
        cs.setSessionDate(sessionDate);
        cs.setCourtRoomId(courtRoomId.toString());
        cs.setCourtHouseId(courtHouseId.toString());
        cs.setDraft(true);
        cs.setHearingStartTime("2026-03-16T10:00:00Z");

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        assertThat(result.getHearingDays().size(), is(1));
        assertThat(result.getHearingDays().get(0).getIsDraft(), is(true));
    }

    // ─── Sanity check tests ──────────────────────────────────────────────

    @Test
    void shouldUseSchedulerDateWhenHearingDateMismatches() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate hearingDate = LocalDate.of(2026, 3, 16);
        final LocalDate schedulerDate = LocalDate.of(2026, 3, 17);  // different!

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(hearingDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Session has different date than hearingDay
        final CourtSchedule cs = new CourtSchedule();
        cs.setCourtScheduleId(courtScheduleId.toString());
        cs.setSessionDate(schedulerDate);  // scheduler wins
        cs.setCourtRoomId(courtRoomId.toString());
        cs.setCourtHouseId(courtHouseId.toString());
        cs.setDraft(true);  // draft so we skip list call and can inspect sanity-checked days
        cs.setHearingStartTime("2026-03-17T10:00:00Z");

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        // Scheduler date should win
        assertThat(result.getHearingDays().get(0).getHearingDate(), is(schedulerDate));
    }

    // ─── CROWN update multi-day enrichment tests ───────────────────────

    @Test
    void shouldEnrichCrownUpdateMultiDay() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now().plusDays(5);

        // Multi-day: totalDuration > 360
        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Arrays.asList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId1)
                                .withHearingDate(day1)
                                .withDurationMinutes(360)
                                .build(),
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId2)
                                .withHearingDate(day1.plusDays(1))
                                .withDurationMinutes(360)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook
        final CourtSchedule cs1 = buildCourtSchedule(courtScheduleId1, courtRoomId, courtHouseId, day1, false);
        final CourtSchedule cs2 = buildCourtSchedule(courtScheduleId2, courtRoomId, courtHouseId, day1.plusDays(1), false);

        final JsonObject multiDayResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs1))
                        .add(buildCsJson(cs2)))
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(multiDayResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class)))
                .thenReturn(cs1, cs2);

        // Mock listHearingInCourtSessions
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId1, "2026-03-16T10:00:00Z", 360))
                        .add(buildListHearingJson(courtScheduleId2, "2026-03-17T10:00:00Z", 360)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder()
                        .add(courtScheduleId1.toString())
                        .add(courtScheduleId2.toString())
                        .build());

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService).multiDaySearchAndBook(anyMap());
        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        assertThat(result.getHearingDays().size(), is(2));
    }

    @Test
    void shouldReturnUnchangedWhenCrownUpdateMultiDaySearchReturnsEmpty() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(720)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook returning empty
        final JsonObject emptyResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder())
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(emptyResponseJson);

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        assertThat(result.getHearingId(), is(hearingId));
    }

    @Test
    void shouldNotCallListHearingWhenCrownUpdateMultiDaySessionsHaveDraft() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now().plusDays(5);

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId1)
                                .withHearingDate(day1)
                                .withDurationMinutes(720)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook - one session is draft
        final CourtSchedule cs1 = buildCourtSchedule(courtScheduleId1, courtRoomId, courtHouseId, day1, false);
        final CourtSchedule cs2 = buildCourtSchedule(courtScheduleId2, courtRoomId, courtHouseId, day1.plusDays(1), true);

        final JsonObject multiDayResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs1))
                        .add(buildCsJson(cs2)))
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(multiDayResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class)))
                .thenReturn(cs1, cs2);

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        assertThat(result.getHearingDays().size(), is(2));
    }

    @Test
    void shouldSkipCrownUpdateWhenHearingDaysEmpty() {
        final UUID hearingId = UUID.randomUUID();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.emptyList())
                .build();

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
        assertThat(result.getHearingId(), is(hearingId));
    }

    @Test
    void shouldKeepExistingJudiciaryOnCrownUpdate() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID judicialId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final uk.gov.justice.core.courts.JudicialRole existingJudiciary = uk.gov.justice.core.courts.JudicialRole.judicialRole()
                .withJudicialId(judicialId)
                .build();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withJudiciary(Collections.singletonList(existingJudiciary))
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds - non-draft
        final CourtSchedule cs = buildCourtSchedule(courtScheduleId, courtRoomId, courtHouseId, sessionDate, false);

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs)))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        // Mock listHearingInCourtSessions
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId, "2026-03-16T10:00:00Z", 240)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        // Existing judiciary should be preserved
        assertThat(result.getJudiciary().size(), is(1));
        assertThat(result.getJudiciary().get(0).getJudicialId(), is(judicialId));
    }

    // ─── fetchCourtSchedulesByIds error paths ────────────────────────────

    @Test
    void shouldReturnEmptyWhenFetchCourtSchedulesByIdsFailsResponse() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(UUID.randomUUID()).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds returning error status
        final Response errorResponse = mock(Response.class);
        when(errorResponse.getStatus()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(errorResponse);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        // Should return unchanged since sessions is empty
        assertThat(result.getHearingDays().size(), is(1));
    }

    @Test
    void shouldReturnEmptyWhenFetchCourtSchedulesByIdsReturnsNullResponse() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(UUID.randomUUID()).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds returning null JSON
        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(null);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        assertThat(result.getHearingDays().size(), is(1));
    }

    // ─── multiDaySearchAndBook error paths ───────────────────────────────

    @Test
    void shouldReturnUnchangedWhenMultiDaySearchAndBookFailsResponse() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(1080)
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(UUID.randomUUID()).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(360)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook returning error status
        final Response errorResponse = mock(Response.class);
        when(errorResponse.getStatus()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(errorResponse);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        assertThat(result.getHearingDays().size(), is(1));
    }

    @Test
    void shouldReturnUnchangedWhenMultiDaySearchAndBookReturnsNullJson() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(1080)
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(UUID.randomUUID()).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(360)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook returning null json
        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(null);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        assertThat(result.getHearingDays().size(), is(1));
    }

    // ─── needsCourtScheduleEnrichment static tests ───────────────────────

    @Test
    void shouldNeedEnrichmentForMagistratesWithBookingReference() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withId(UUID.randomUUID())
                .withBookingReference(UUID.randomUUID())
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(true));
    }

    @Test
    void shouldNotNeedEnrichmentForCrownWithoutCourtScheduleIds() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(UUID.randomUUID())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(false));
    }

    @Test
    void shouldNotNeedEnrichmentForUnknownJurisdiction() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(UUID.randomUUID())
                                .withHearingDate(LocalDate.now())
                                .build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(false));
    }

    @Test
    void shouldNeedEnrichmentForCrownWithCourtScheduleIds() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(UUID.randomUUID())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(UUID.randomUUID())
                                .withHearingDate(LocalDate.now())
                                .withDurationMinutes(240)
                                .build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(true));
    }

    // ─── isCandidateForAllocation tests ──────────────────────────────────

    @Test
    void shouldBeAllocationCandidateWithStartDateTimeAndCourtRoom() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withId(UUID.randomUUID())
                .withListedStartDateTime(ZonedDateTime.now())
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(UUID.randomUUID()).build())
                .build();

        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(hearing), is(true));
    }

    @Test
    void shouldNotBeAllocationCandidateWithoutStartDateTime() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withId(UUID.randomUUID())
                .withCourtCentre(CourtCentre.courtCentre().withId(UUID.randomUUID()).withRoomId(UUID.randomUUID()).build())
                .build();

        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(hearing), is(false));
    }

    @Test
    void shouldBeUpdateAllocationCandidateWithStartDateAndCourtRoom() {
        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withStartDate(LocalDate.now())
                .withCourtRoomId(UUID.randomUUID())
                .build();

        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(update), is(true));
    }

    @Test
    void shouldNotBeUpdateAllocationCandidateWithoutCourtRoom() {
        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withStartDate(LocalDate.now())
                .build();

        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(update), is(false));
    }

    // ─── sanityCheckAndEnrichCrown edge cases ────────────────────────────

    @Test
    void shouldPassThroughHearingDayWithNoCourtScheduleId() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        // One hearingDay with courtScheduleId, one without
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Arrays.asList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(120)
                                .build(),
                        HearingDay.hearingDay()
                                .withHearingDate(sessionDate.plusDays(1))
                                .withDurationMinutes(120)
                                .build()))
                .build();

        final CourtSchedule cs = buildCourtSchedule(courtScheduleId, courtRoomId, courtHouseId, sessionDate, true);

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs)))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        // Both days should be present; one enriched, one passed through
        assertThat(result.getHearingDays().size(), is(2));
    }

    @Test
    void searchAndBookShouldReturnBookedHearingSlotsWithJudiciaries() {
        final String hearingId = "5416c10a-0cf1-49d5-a7c9-5761ff3bdf2c";
        final String ouCode = "OU12345";
        final String hearingSessionDate = LocalDate.now().toString();
        final String courtRoomId = UUID.randomUUID().toString();
        final String hearingSessionDateSearchCutOff = LocalDate.now().plusDays(7).toString();
        final String sessionStartTime = LocalDate.now().toString();
        final Integer durationInMinutes = 20;

        final JsonObject searchBookResponse = givenPayload("/courtscheduler.search.book.hearing.slots.with.judiciaries.json");

        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(searchBookResponse);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(searchBookResponse);

        final HearingSlotSearchResponse hearingSlotSearchResponse = courtScheduleEnrichmentService
                .searchAndBookSlots(hearingId, ouCode, hearingSessionDate, courtRoomId, hearingSessionDateSearchCutOff, sessionStartTime, durationInMinutes, true);

        assertThat(hearingSlotSearchResponse.courtScheduleId(), is("23681024-8eac-4890-8c44-4651ad48cb24"));
        assertThat(hearingSlotSearchResponse.judiciaries().size(), is(2));
        assertThat(hearingSlotSearchResponse.judiciaries().get(0).getJudicialId().toString(), is("a1b2c3d4-e5f6-7890-abcd-ef1234567890"));
        assertThat(hearingSlotSearchResponse.judiciaries().get(0).getJudicialRoleType().getJudiciaryType(), is("CIRCUIT_JUDGE"));
        assertThat(hearingSlotSearchResponse.judiciaries().get(0).getIsBenchChairman().orElse(false), is(true));
        assertThat(hearingSlotSearchResponse.judiciaries().get(0).getIsDeputy().orElse(true), is(false));
        assertThat(hearingSlotSearchResponse.judiciaries().get(1).getJudicialId().toString(), is("b2c3d4e5-f6a7-8901-bcde-f12345678901"));
        assertThat(hearingSlotSearchResponse.judiciaries().get(1).getJudicialRoleType().getJudiciaryType(), is("RECORDER"));
        assertThat(hearingSlotSearchResponse.judiciaries().get(1).getIsDeputy().orElse(false), is(true));
    }

    // ─── MAGISTRATES update hearing judiciary enrichment tests ─────────

    @Test
    void shouldPreserveMagistratesExistingJudiciaryOnUpdateHearing() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID judicialId = UUID.randomUUID();

        final uk.gov.justice.core.courts.JudicialRole existingJudiciary = uk.gov.justice.core.courts.JudicialRole.judicialRole()
                .withJudicialId(judicialId)
                .build();

        final HearingDay hearingDay = HearingDay.hearingDay()
                .withCourtScheduleId(courtScheduleId)
                .withCourtRoomId(courtRoomId)
                .withHearingDate(LocalDate.now().plusDays(3))
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(30)
                .build();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withJudiciary(Collections.singletonList(existingJudiciary))
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withCourtRoomId(courtRoomId)
                .withStartDate(LocalDate.now().plusDays(3))
                .withHearingDays(Collections.singletonList(hearingDay))
                .build();

        // Mock slotsToJsonStringConverter
        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        // Mock listHearingInCourtSessions — the response has NO judiciaries
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("hearingStartTime", "2026-03-16T10:00:00Z")
                                .add("duration", 30)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        // Existing judiciary should be preserved even though response has none
        assertThat(result.getJudiciary().size(), is(1));
        assertThat(result.getJudiciary().get(0).getJudicialId(), is(judicialId));
    }

    @Test
    void shouldEnrichMagistratesUpdateHearingWithJudiciaryFromListResponse() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID judicialId = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

        final HearingDay hearingDay = HearingDay.hearingDay()
                .withCourtScheduleId(courtScheduleId)
                .withCourtRoomId(courtRoomId)
                .withHearingDate(LocalDate.now().plusDays(3))
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(30)
                .build();

        // No existing judiciary on the update
        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withCourtRoomId(courtRoomId)
                .withStartDate(LocalDate.now().plusDays(3))
                .withHearingDays(Collections.singletonList(hearingDay))
                .build();

        // Mock slotsToJsonStringConverter
        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        // Mock listHearingInCourtSessions — response includes judiciaries
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("hearingStartTime", "2026-03-16T10:00:00Z")
                                .add("duration", 30)
                                .add("judiciaries", JsonObjects.createArrayBuilder()
                                        .add(JsonObjects.createObjectBuilder()
                                                .add("judiciaryId", judicialId.toString())
                                                .add("judiciaryType", "MAGISTRATE")
                                                .add("benchChairman", true)
                                                .add("deputy", false)))))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        // Judiciary should be populated from the list response, converted from domain to core model
        assertThat(result.getJudiciary().size(), is(1));
        assertThat(result.getJudiciary().get(0).getJudicialId(), is(judicialId));
        assertThat(result.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType(), is("MAGISTRATE"));
        assertThat(result.getJudiciary().get(0).getIsBenchChairman(), is(true));
        assertThat(result.getJudiciary().get(0).getIsDeputy(), is(false));
    }

    @Test
    void shouldNotSetJudiciaryWhenNeitherExistingNorEnrichedPresent() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final HearingDay hearingDay = HearingDay.hearingDay()
                .withCourtScheduleId(courtScheduleId)
                .withCourtRoomId(courtRoomId)
                .withHearingDate(LocalDate.now().plusDays(3))
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(30)
                .build();

        // No existing judiciary on the update
        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withCourtRoomId(courtRoomId)
                .withStartDate(LocalDate.now().plusDays(3))
                .withHearingDays(Collections.singletonList(hearingDay))
                .build();

        // Mock slotsToJsonStringConverter
        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        // Mock listHearingInCourtSessions — no judiciaries in response
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("hearingStartTime", "2026-03-16T10:00:00Z")
                                .add("duration", 30)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        // No judiciary should be set — neither existing nor enriched
        assertThat(result.getJudiciary() == null || result.getJudiciary().isEmpty(), is(true));
    }

    @Test
    void shouldReturnUnchangedWhenCrownMultiDayHasNoCourtScheduleIdOnAnyDay() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(720)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Arrays.asList(
                        HearingDay.hearingDay()
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(360)
                                .build(),
                        HearingDay.hearingDay()
                                .withHearingDate(sessionDate.plusDays(1))
                                .withDurationMinutes(360)
                                .build()))
                .build();

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        assertThat(result.getHearingDays().size(), is(2));
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(any());
    }

    // ─── getFirstAvailableSlot error paths ────────────────────────────────

    @Test
    void shouldThrowWhenGetFirstAvailableSlotSearchFails() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final LocalDate hearingDate = LocalDate.now().plusDays(5);

        // HearingDay with NULL courtScheduleId triggers getFirstAvailableSlot
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(hearingDate)
                .withDurationMinutes(120)
                .withStartTime(hearingDate.atTime(10, 0).atZone(ZoneOffset.UTC))
                .withCourtRoomId(courtRoomId)
                .build();

        final UpdateHearingForListing updateHearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withHearingDays(Collections.singletonList(hearingDay))
                .build();

        final Response failedResponse = mock(Response.class);
        when(failedResponse.getStatus()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(failedResponse.hasEntity()).thenReturn(true);
        when(failedResponse.getEntity()).thenReturn("Server Error");
        when(hearingSlotsService.search(anyMap())).thenReturn(failedResponse);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                courtScheduleEnrichmentService.enrichWithCourtSchedules(updateHearing, mock(JsonEnvelope.class)));
    }

    @Test
    void shouldThrowWhenGetFirstAvailableSlotReturnsEmptySlots() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final LocalDate hearingDate = LocalDate.now().plusDays(5);

        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(hearingDate)
                .withDurationMinutes(120)
                .withStartTime(hearingDate.atTime(10, 0).atZone(ZoneOffset.UTC))
                .withCourtRoomId(courtRoomId)
                .build();

        final UpdateHearingForListing updateHearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withHearingDays(Collections.singletonList(hearingDay))
                .build();

        final JsonObject emptyResponseJson = JsonObjects.createObjectBuilder()
                .add("hearingSlots", JsonObjects.createArrayBuilder())
                .build();

        final Response emptyResponse = mock(Response.class);
        when(emptyResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(emptyResponse.getEntity()).thenReturn(emptyResponseJson);
        when(hearingSlotsService.search(anyMap())).thenReturn(emptyResponse);
        when(objectToJsonObjectConverter.convert(emptyResponseJson)).thenReturn(emptyResponseJson);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () ->
                courtScheduleEnrichmentService.enrichWithCourtSchedules(updateHearing, mock(JsonEnvelope.class)));
    }

    // ─── populateJudiciaryInfoFromSlots edge case tests ─────────────────

    @Test
    void shouldReturnNoJudiciaryWhenListResponseFails() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        // HearingDay with courtScheduleId — bypasses getFirstAvailableSlot
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withCourtScheduleId(courtScheduleId)
                .withCourtRoomId(courtRoomId)
                .withHearingDate(LocalDate.now().plusDays(3))
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(30)
                .build();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withCourtRoomId(courtRoomId)
                .withStartDate(LocalDate.now().plusDays(3))
                .withHearingDays(Collections.singletonList(hearingDay))
                .build();

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        // Mock listHearingInCourtSessions returning 500
        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);

        // combineSearchAndBookResponseAndListResponse throws RuntimeException when response is not success
        // so populateJudiciaryInfoFromSlots is never reached — the failed list response causes an exception
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class)));
    }

    @Test
    void shouldReturnNoJudiciaryWhenResponseHasNoJudiciariesKey() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        // HearingDay with courtScheduleId — bypasses getFirstAvailableSlot
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withCourtScheduleId(courtScheduleId)
                .withCourtRoomId(courtRoomId)
                .withHearingDate(LocalDate.now().plusDays(3))
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(30)
                .build();

        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withSelectedCourtCentre(SelectedCourtCentre.selectedCourtCentre().withOuCode("OU123").build())
                .withCourtRoomId(courtRoomId)
                .withStartDate(LocalDate.now().plusDays(3))
                .withHearingDays(Collections.singletonList(hearingDay))
                .build();

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        // Mock listHearingInCourtSessions — response is 200 but hearing has NO "judiciaries" key
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("hearingStartTime", "2026-03-16T10:00:00Z")
                                .add("duration", 30)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(update, mock(JsonEnvelope.class));

        // populateJudiciaryInfoFromSlots finds no "judiciaries" key in the hearing object, returns empty list
        // No judiciary should be set on the result
        assertThat(result.getJudiciary() == null || result.getJudiciary().isEmpty(), is(true));
    }

    // ─── Helper methods ──────────────────────────────────────────────────

    private CourtSchedule buildCourtSchedule(UUID courtScheduleId, UUID courtRoomId, UUID courtHouseId, LocalDate sessionDate, boolean isDraft) {
        final CourtSchedule cs = new CourtSchedule();
        cs.setCourtScheduleId(courtScheduleId.toString());
        cs.setCourtRoomId(courtRoomId.toString());
        cs.setCourtHouseId(courtHouseId.toString());
        cs.setSessionDate(sessionDate);
        cs.setDraft(isDraft);
        cs.setHearingStartTime(sessionDate + "T10:00:00Z");
        return cs;
    }

    private JsonObject buildCsJson(CourtSchedule cs) {
        return JsonObjects.createObjectBuilder()
                .add("courtScheduleId", cs.getCourtScheduleId())
                .add("courtRoomId", cs.getCourtRoomId())
                .add("courtHouseId", cs.getCourtHouseId())
                .add("sessionDate", cs.getSessionDate().toString())
                .add("isDraft", cs.isDraft())
                .build();
    }

    private JsonObject buildListHearingJson(UUID courtScheduleId, String hearingStartTime, int duration) {
        return JsonObjects.createObjectBuilder()
                .add("courtScheduleId", courtScheduleId.toString())
                .add("hearingStartTime", hearingStartTime)
                .add("duration", duration)
                .build();
    }

    // ─── calculateAggregatedDuration tests ───────────────────────────────

    @Test
    void shouldCalculateDurationFromHearingDays() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withEstimatedMinutes(100)
                .withHearingDays(Arrays.asList(
                        HearingDay.hearingDay().withDurationMinutes(360).build(),
                        HearingDay.hearingDay().withDurationMinutes(360).build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing), is(720));
    }

    @Test
    void shouldCalculateDurationFromNonDefaultDaysWhenNoHearingDays() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withEstimatedMinutes(100)
                .withNonDefaultDays(Arrays.asList(
                        new uk.gov.justice.core.courts.NonDefaultDay(null, null, null, 180, null, null, null, null),
                        new uk.gov.justice.core.courts.NonDefaultDay(null, null, null, 180, null, null, null, null)))
                .build();

        assertThat(CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing), is(360));
    }

    @Test
    void shouldFallbackToEstimatedMinutesWhenNoHearingDaysOrNonDefaultDays() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withEstimatedMinutes(240)
                .build();

        assertThat(CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing), is(240));
    }

    @Test
    void shouldReturnZeroWhenNoDurationInfoAvailable() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .build();

        assertThat(CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing), is(0));
    }

    @Test
    void shouldCalculateAggregatedDurationForUpdateFromHearingDays() {
        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withHearingDays(Arrays.asList(
                        HearingDay.hearingDay().withDurationMinutes(360).build(),
                        HearingDay.hearingDay().withDurationMinutes(360).build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.calculateAggregatedDuration(update), is(720));
    }

    @Test
    void shouldCalculateAggregatedDurationForUpdateFromNonDefaultDays() {
        final UpdateHearingForListing update = UpdateHearingForListing.updateHearingForListing()
                .withNonDefaultDays(Collections.singletonList(
                        NonDefaultDay.nonDefaultDay().withDuration(360).build()))
                .build();

        assertThat(CourtScheduleEnrichmentService.calculateAggregatedDuration(update), is(360));
    }

    // ─── enrichCrownCourtScheduleFirst tests ────────────────────────────

    @Test
    void enrichCrownCourtScheduleFirst_shouldReturnUnchanged_whenNoCourtScheduleIdAnywhere() {
        final UUID hearingId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.of(2026, 4, 10))
                                .withDurationMinutes(120)
                                .build()))
                .build();

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        assertThat(result, is(hearing));
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void enrichCrownCourtScheduleFirst_shouldReturnUnchanged_whenNoCourtScheduleIdAndHasWeekCommencing() {
        final UUID hearingId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(LocalDate.now().toString())
                        .withDuration(1)
                        .build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.of(2026, 4, 10))
                                .withDurationMinutes(120)
                                .build()))
                .build();

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        assertThat(result, is(hearing));
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void enrichCrownCourtScheduleFirst_shouldReturnUnchanged_whenCourtScheduleIdOnlyOnBookedSlots() {
        final UUID hearingId = UUID.randomUUID();

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(240)
                                .build()))  // no courtScheduleId on hearingDays
                .withBookedSlots(Collections.singletonList(
                        RotaSlot.rotaSlot()
                                .withCourtScheduleId(UUID.randomUUID().toString())
                                .withCourtCentreId(UUID.randomUUID().toString())
                                .withRoomId(UUID.randomUUID().toString())
                                .build()))  // courtScheduleId on bookedSlots only
                .build();

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        assertThat(result, is(hearing));
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void enrichCrownCourtScheduleFirst_shouldCallSingleDay_whenCourtScheduleIdPresentAndDurationBelow360() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds
        final CourtSchedule cs = buildCourtSchedule(courtScheduleId, courtRoomId, courtHouseId, sessionDate, false);

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        // Mock listHearingInCourtSessions
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId, "2026-03-16T10:00:00Z", 240)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        verify(hearingSlotsService).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
        assertThat(result.getHearingDays().size(), is(1));
        assertThat(result.getHearingDays().get(0).getCourtScheduleId().toString(), is(courtScheduleId.toString()));
    }

    @Test
    void enrichCrownCourtScheduleFirst_shouldCallMultiDay_whenCourtScheduleIdPresentAndDurationAbove360() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtScheduleId3 = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(1080)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId1)
                                .withHearingDate(day1)
                                .withDurationMinutes(1080)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook
        final CourtSchedule cs1 = buildCourtSchedule(courtScheduleId1, courtRoomId, courtHouseId, day1, false);
        final CourtSchedule cs2 = buildCourtSchedule(courtScheduleId2, courtRoomId, courtHouseId, day1.plusDays(1), false);
        final CourtSchedule cs3 = buildCourtSchedule(courtScheduleId3, courtRoomId, courtHouseId, day1.plusDays(2), false);

        final JsonObject multiDayResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs1))
                        .add(buildCsJson(cs2))
                        .add(buildCsJson(cs3)))
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(multiDayResponseJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class)))
                .thenReturn(cs1, cs2, cs3);

        // Mock listHearingInCourtSessions
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId1, "2026-03-16T10:00:00Z", 360))
                        .add(buildListHearingJson(courtScheduleId2, "2026-03-17T10:00:00Z", 360))
                        .add(buildListHearingJson(courtScheduleId3, "2026-03-18T10:00:00Z", 360)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder()
                        .add(courtScheduleId1.toString())
                        .add(courtScheduleId2.toString())
                        .add(courtScheduleId3.toString())
                        .build());

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        verify(hearingSlotsService).multiDaySearchAndBook(anyMap());
        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        assertThat(result.getHearingDays().size(), is(3));
    }

    @Test
    void enrichCrownCourtScheduleFirst_shouldSkipListHearing_whenSingleDaySessionIsDraft() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds - isDraft=true
        final CourtSchedule cs = buildCourtSchedule(courtScheduleId, courtRoomId, courtHouseId, sessionDate, true);

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("isDraft", true)))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        // listHearingInCourtSessions should NOT be called for draft sessions
        verify(hearingSlotsService).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        // HearingDays should carry isDraft=true from sanity check
        assertThat(result.getHearingDays().size(), is(1));
        assertThat(result.getHearingDays().get(0).getIsDraft(), is(true));
    }

    @Test
    void enrichCrownCourtScheduleFirst_shouldSkipListHearing_whenMultiDaySessionsHaveDraft() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtScheduleId3 = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(720)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId1)
                                .withHearingDate(day1)
                                .withDurationMinutes(720)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook - one session is draft
        final CourtSchedule cs1 = buildCourtSchedule(courtScheduleId1, courtRoomId, courtHouseId, day1, false);
        final CourtSchedule cs2 = buildCourtSchedule(courtScheduleId2, courtRoomId, courtHouseId, day1.plusDays(1), true);  // draft!
        final CourtSchedule cs3 = buildCourtSchedule(courtScheduleId3, courtRoomId, courtHouseId, day1.plusDays(2), false);

        final JsonObject multiDayResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs1))
                        .add(buildCsJson(cs2))
                        .add(buildCsJson(cs3)))
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(multiDayResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class)))
                .thenReturn(cs1, cs2, cs3);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        // listHearingInCourtSessions should NOT be called when any session is draft
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        // Expanded hearingDays should be returned with isDraft from sessions
        assertThat(result.getHearingDays().size(), is(3));
    }

    // ─── enrichCrownCourtScheduleFirst (UpdateHearingForListing) tests ───

    @Test
    void enrichCrownCourtScheduleFirst_update_shouldReturnUnchanged_whenNoCourtScheduleId() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();

        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withStartDate(LocalDate.of(2026, 4, 10))
                .withEndDate(LocalDate.of(2026, 4, 10))
                .withCourtCentreId(courtCentreId)
                .withCourtRoomId(courtRoomId)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.of(2026, 4, 10))
                                .withDurationMinutes(120)
                                .build()
                ))
                .build();

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        assertThat(result, is(hearing));
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void enrichCrownCourtScheduleFirst_update_shouldCallSingleDay_whenCourtScheduleIdPresentAndDurationBelow360() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds - non-draft
        final CourtSchedule cs = buildCourtSchedule(courtScheduleId, courtRoomId, courtHouseId, sessionDate, false);

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        // Mock listHearingInCourtSessions
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId, "2026-03-16T10:00:00Z", 240)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        verify(hearingSlotsService).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
        assertThat(result.getHearingDays().size(), is(1));
    }

    @Test
    void enrichCrownCourtScheduleFirst_update_shouldCallMultiDay_whenCourtScheduleIdPresentAndDurationAbove360() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId1 = UUID.randomUUID();
        final UUID courtScheduleId2 = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now().plusDays(5);

        // Multi-day: totalDuration > 360
        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withHearingId(hearingId)
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Arrays.asList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId1)
                                .withHearingDate(day1)
                                .withDurationMinutes(360)
                                .build(),
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId2)
                                .withHearingDate(day1.plusDays(1))
                                .withDurationMinutes(360)
                                .build()))
                .build();

        // Mock multiDaySearchAndBook
        final CourtSchedule cs1 = buildCourtSchedule(courtScheduleId1, courtRoomId, courtHouseId, day1, false);
        final CourtSchedule cs2 = buildCourtSchedule(courtScheduleId2, courtRoomId, courtHouseId, day1.plusDays(1), false);

        final JsonObject multiDayResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(buildCsJson(cs1))
                        .add(buildCsJson(cs2)))
                .build();

        final Response multiDayResponse = mock(Response.class);
        when(multiDayResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.multiDaySearchAndBook(anyMap())).thenReturn(multiDayResponse);
        when(objectToJsonObjectConverter.convert(multiDayResponse.getEntity())).thenReturn(multiDayResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class)))
                .thenReturn(cs1, cs2);

        // Mock listHearingInCourtSessions
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId1, "2026-03-16T10:00:00Z", 360))
                        .add(buildListHearingJson(courtScheduleId2, "2026-03-17T10:00:00Z", 360)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder()
                        .add(courtScheduleId1.toString())
                        .add(courtScheduleId2.toString())
                        .build());

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        verify(hearingSlotsService).multiDaySearchAndBook(anyMap());
        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        verify(hearingSlotsService, never()).getCourtSchedulesById(anyMap());
        assertThat(result.getHearingDays().size(), is(2));
    }

    @Test
    void enrichCrownCourtScheduleFirst_update_shouldSkipListHearing_whenSingleDaySessionIsDraft() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingId(hearingId)
                .withStartDate(sessionDate)
                .withEndDate(sessionDate)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withCourtCentreId(courtHouseId)
                                .withCourtRoomId(courtRoomId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds - isDraft=true
        final CourtSchedule cs = buildCourtSchedule(courtScheduleId, courtRoomId, courtHouseId, sessionDate, true);

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("isDraft", true)))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        // listHearingInCourtSessions should NOT be called for draft sessions
        verify(hearingSlotsService).getCourtSchedulesById(anyMap());
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
        // HearingDays should carry isDraft=true
        assertThat(result.getHearingDays().size(), is(1));
        assertThat(result.getHearingDays().get(0).getIsDraft(), is(true));
    }

    // ─── Additional coverage tests ──────────────────────────────────────

    @Test
    void calculateAggregatedDuration_update_shouldReturnZeroWhenNoDurationInfo() {
        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing().build();
        assertThat(CourtScheduleEnrichmentService.calculateAggregatedDuration(hearing), is(0));
    }

    @Test
    void enrichCrownUpdateHearing_shouldReturnUnchanged_whenMultiDayAndNoCourtScheduleIdOnHearingDays() {
        final UUID hearingId = UUID.randomUUID();

        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingId(hearingId)
                .withStartDate(LocalDate.now().plusDays(5))
                .withEndDate(LocalDate.now().plusDays(7))
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(720)
                                .build()))  // no courtScheduleId
                .build();

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        assertThat(result, is(hearing));
        verify(hearingSlotsService, never()).multiDaySearchAndBook(anyMap());
    }

    @Test
    void enrichCrownUpdateHearing_shouldReturnUnchanged_whenSingleDayAndFetchReturnsEmpty() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();

        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingId(hearingId)
                .withStartDate(LocalDate.now().plusDays(5))
                .withEndDate(LocalDate.now().plusDays(5))
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withCourtCentreId(UUID.randomUUID())
                                .withCourtRoomId(UUID.randomUUID())
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds to return empty
        final JsonObject emptyResponse = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder())
                .build();
        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(emptyResponse);

        final UpdateHearingForListing result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, mock(JsonEnvelope.class));

        assertThat(result, is(hearing));
        verify(hearingSlotsService, never()).listHearingInCourtSessions(any());
    }

    @Test
    void enrichCrownCourtScheduleFirst_shouldEnrichJudiciary_whenListHearingReturnsJudiciaries() {
        final UUID hearingId = UUID.randomUUID();
        final UUID courtScheduleId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final UUID courtHouseId = UUID.randomUUID();
        final UUID judicialId = UUID.randomUUID();
        final LocalDate sessionDate = LocalDate.now().plusDays(5);

        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withId(hearingId)
                .withEstimatedMinutes(240)
                .withCourtCentre(CourtCentre.courtCentre().withId(courtHouseId).withRoomId(courtRoomId).build())
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(courtScheduleId)
                                .withHearingDate(sessionDate)
                                .withDurationMinutes(240)
                                .build()))
                .build();

        // Mock fetchCourtSchedulesByIds - non-draft
        final CourtSchedule cs = buildCourtSchedule(courtScheduleId, courtRoomId, courtHouseId, sessionDate, false);

        final JsonObject csResponseJson = JsonObjects.createObjectBuilder()
                .add("courtSchedules", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("courtScheduleId", courtScheduleId.toString())
                                .add("isDraft", false)))
                .build();

        final Response csResponse = mock(Response.class);
        when(csResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(hearingSlotsService.getCourtSchedulesById(anyMap())).thenReturn(csResponse);
        when(objectToJsonObjectConverter.convert(csResponse.getEntity())).thenReturn(csResponseJson);
        when(jsonObjectConverter.convert(any(JsonObject.class), eq(CourtSchedule.class))).thenReturn(cs);

        // Mock listHearingInCourtSessions with judiciaries
        final JsonObject listJson = JsonObjects.createObjectBuilder()
                .add("hearings", JsonObjects.createArrayBuilder()
                        .add(buildListHearingJson(courtScheduleId, "2026-03-16T10:00:00Z", 240)))
                .add("judiciaries", JsonObjects.createArrayBuilder()
                        .add(JsonObjects.createObjectBuilder()
                                .add("judicialId", judicialId.toString())
                                .add("judicialRoleTypeId", UUID.randomUUID().toString())
                                .add("isPrimary", true)))
                .build();

        final Response listResponse = mock(Response.class);
        when(listResponse.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        when(jsonObjectConverter.convert(any(JsonObject.class), eq(ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    JsonObject jo = inv.getArgument(0);
                    ListUpdateHearing luh = new ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(courtScheduleId.toString()).build());

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichCrownCourtScheduleFirst(hearing);

        verify(hearingSlotsService).listHearingInCourtSessions(any(JsonObject.class));
        assertThat(result.getHearingDays().size(), is(1));
    }

    @Test
    void needsCourtScheduleEnrichment_shouldReturnFalse_forUnknownJurisdiction() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(null)
                .build();
        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(false));
    }

    @Test
    void needsCourtScheduleEnrichment_shouldReturnTrue_forMagistratesWithBookingReference() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withBookingReference(UUID.randomUUID())
                .build();
        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(true));
    }

    @Test
    void needsCourtScheduleEnrichment_shouldReturnTrue_forCrownFixedDateWithCourtScheduleId() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withHearingDays(Collections.singletonList(
                        HearingDay.hearingDay()
                                .withCourtScheduleId(UUID.randomUUID())
                                .withHearingDate(LocalDate.now().plusDays(5))
                                .build()))
                .build();
        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(true));
    }

    @Test
    void needsCourtScheduleEnrichment_shouldReturnFalse_forCrownWithWeekCommencing() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withWeekCommencingDate(WeekCommencingDate.weekCommencingDate()
                        .withStartDate(LocalDate.now().plusDays(7).toString())
                        .withDuration(1)
                        .build())
                .build();
        assertThat(CourtScheduleEnrichmentService.needsCourtScheduleEnrichment(hearing), is(false));
    }

    @Test
    void isCandidateForAllocation_shouldReturnFalse_whenNoCourtCentre() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListedStartDateTime(java.time.ZonedDateTime.now())
                .build();
        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(hearing), is(false));
    }

    @Test
    void isCandidateForAllocation_shouldReturnTrue_whenAllCriteriaMet() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListedStartDateTime(java.time.ZonedDateTime.now())
                .withCourtCentre(CourtCentre.courtCentre().withRoomId(UUID.randomUUID()).build())
                .build();
        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(hearing), is(true));
    }

    @Test
    void isCandidateForAllocation_shouldReturnTrue_whenEarliestStartDateTimeUsed() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withEarliestStartDateTime(java.time.ZonedDateTime.now())
                .withCourtCentre(CourtCentre.courtCentre().withRoomId(UUID.randomUUID()).build())
                .build();
        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(hearing), is(true));
    }

    @Test
    void isCandidateForAllocation_update_shouldReturnFalse_whenNoStartDate() {
        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withCourtRoomId(UUID.randomUUID())
                .build();
        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(hearing), is(false));
    }

    @Test
    void isCandidateForAllocation_update_shouldReturnTrue_whenAllCriteriaMet() {
        final UpdateHearingForListing hearing = UpdateHearingForListing.updateHearingForListing()
                .withJurisdictionType(JurisdictionType.CROWN)
                .withStartDate(LocalDate.now())
                .withCourtRoomId(UUID.randomUUID())
                .build();
        assertThat(CourtScheduleEnrichmentService.isCandidateForAllocation(hearing), is(true));
    }

    @Test
    void hasCourtScheduleIdOnInput_shouldReturnFalse_whenNoHearingDaysOrBookedSlots() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds().build();
        assertThat(CourtScheduleEnrichmentService.hasCourtScheduleIdOnInput(hearing), is(false));
    }

    @Test
    void hasCourtScheduleIdOnInput_shouldReturnTrue_whenBookedSlotsHaveCourtScheduleId() {
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withBookedSlots(Collections.singletonList(
                        RotaSlot.rotaSlot()
                                .withCourtScheduleId(UUID.randomUUID().toString())
                                .build()))
                .build();
        assertThat(CourtScheduleEnrichmentService.hasCourtScheduleIdOnInput(hearing), is(true));
    }
}
