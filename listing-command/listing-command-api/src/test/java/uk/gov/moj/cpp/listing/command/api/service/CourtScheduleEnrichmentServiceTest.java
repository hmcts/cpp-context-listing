package uk.gov.moj.cpp.listing.command.api.service;

import uk.gov.justice.services.messaging.JsonObjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.api.util.FileUtil.givenPayload;

import uk.gov.justice.listing.courts.SelectedCourtCentre;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.command.api.util.SlotsToJsonStringConverter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.HearingSlotSearchResponse;

import java.time.LocalDate;
import java.util.UUID;

import javax.inject.Inject;
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
        // Arrange: two hearing days -> isMultiDay = true
        final UUID hearingId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now();
        final LocalDate day2 = day1.plusDays(1);

        final uk.gov.justice.listing.commands.HearingDay hd1 =
                uk.gov.justice.listing.commands.HearingDay.hearingDay()
                        .withCourtRoomId(courtRoomId)
                        .withHearingDate(day1)
                        .withStartTime(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC))
                        .withDurationMinutes(30)
                        .build();

        final uk.gov.justice.listing.commands.HearingDay hd2 =
                uk.gov.justice.listing.commands.HearingDay.hearingDay()
                        .withCourtRoomId(courtRoomId)
                        .withHearingDate(day2)
                        .withStartTime(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusHours(1))
                        .withDurationMinutes(30)
                        .build();

        // Build a minimal UpdateHearingForListing with 2 days
        final uk.gov.justice.listing.commands.UpdateHearingForListing update =
                uk.gov.justice.listing.commands.UpdateHearingForListing.updateHearingForListing()
                        .withHearingId(hearingId)
                        // ensure ouCode resolution does not hit CourtCentreFactory
                        .withSelectedCourtCentre(
                                SelectedCourtCentre.selectedCourtCentre()
                                        .withOuCode("OU123")
                                        .build())
                        .withCourtRoomId(courtRoomId)
                        .withStartDate(day1) // non-null, not strictly used here but safe
                        .withHearingDays(java.util.Arrays.asList(hd1, hd2))
                        .build();

        // Mock search response (first available slot). Shape must match getFirstAvailableSlot()
        final String bookedCourtScheduleId = java.util.UUID.randomUUID().toString();
        final javax.json.JsonObject searchJson =
                JsonObjects.createObjectBuilder()
                        .add("hearingSlots", JsonObjects.createArrayBuilder()
                                .add(JsonObjects.createObjectBuilder()
                                        .add("courtScheduleId", bookedCourtScheduleId)
                                        .add("courtRoomId", courtRoomId.toString())
                                        .add("sessionStartTime", "2020-01-01T09:00:00Z")))
                        .build();

        final Response searchResponse = org.mockito.Mockito.mock(Response.class);
        when(searchResponse.getStatus()).thenReturn(org.apache.http.HttpStatus.SC_OK);
        when(searchResponse.getEntity()).thenReturn(searchJson);
        when(hearingSlotsService.search(anyMap())).thenReturn(searchResponse);
        when(objectToJsonObjectConverter.convert(searchJson)).thenReturn(searchJson);

        // Stub SlotsToJsonStringConverter so getUpdateSlotsPayload() never sees nulls
        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(JsonObjects.createArrayBuilder()
                        .add(bookedCourtScheduleId)
                        .build());

        // Mock list response used by combineSearchAndBookResponseAndListResponse()
        final javax.json.JsonObject listJson =
                JsonObjects.createObjectBuilder()
                        .add("hearings", JsonObjects.createArrayBuilder()
                                .add(JsonObjects.createObjectBuilder()
                                        .add("courtScheduleId", bookedCourtScheduleId)
                                        .add("hearingStartTime", "2020-01-01T09:00:00Z")
                                        .add("duration", 30)))
                        .build();

        final Response listResponse = org.mockito.Mockito.mock(Response.class);
        when(listResponse.getStatus()).thenReturn(org.apache.http.HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(javax.json.JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        // jsonObjectConverter must translate each "hearings" item to ListUpdateHearing (POJO with setters)
        when(jsonObjectConverter.convert(
                org.mockito.ArgumentMatchers.any(javax.json.JsonObject.class),
                org.mockito.ArgumentMatchers.eq(uk.gov.moj.cpp.listing.domain.ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    javax.json.JsonObject jo = inv.getArgument(0);
                    uk.gov.moj.cpp.listing.domain.ListUpdateHearing luh = new uk.gov.moj.cpp.listing.domain.ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        // Capture the search query maps for both days
        @SuppressWarnings("unchecked")
        final org.mockito.ArgumentCaptor<java.util.Map<String, String>> mapCaptor =
                org.mockito.ArgumentCaptor.forClass(java.util.Map.class);

        // Act
        courtScheduleEnrichmentService.enrichWithCourtSchedules(
                update,
                org.mockito.Mockito.mock(uk.gov.justice.services.messaging.JsonEnvelope.class));

        // Assert: search() called twice (two days) and includes multi-day params
        verify(hearingSlotsService, times(2)).search(mapCaptor.capture());

        // Each captured map must contain the multi-day flags
        for (java.util.Map<String, String> qp : mapCaptor.getAllValues()) {
            org.hamcrest.MatcherAssert.assertThat(qp.get("courtSession"), is("AD"));
            org.hamcrest.MatcherAssert.assertThat(qp.get("showOverbookedSlots"), is(Boolean.TRUE.toString()));
            org.hamcrest.MatcherAssert.assertThat(qp.get("isSlotBased"), is(Boolean.FALSE.toString()));
        }
    }

    @Test
    void enrichShouldNotIncludeStartTimeForMultiDaySearch() {
        // Arrange: two hearing days -> isMultiDay = true
        final UUID hearingId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final LocalDate day1 = LocalDate.now();
        final LocalDate day2 = day1.plusDays(1);

        final uk.gov.justice.listing.commands.HearingDay hd1 =
                uk.gov.justice.listing.commands.HearingDay.hearingDay()
                        .withCourtRoomId(courtRoomId)
                        .withHearingDate(day1)
                        .withStartTime(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC))
                        .withDurationMinutes(30)
                        .build();

        final uk.gov.justice.listing.commands.HearingDay hd2 =
                uk.gov.justice.listing.commands.HearingDay.hearingDay()
                        .withCourtRoomId(courtRoomId)
                        .withHearingDate(day2)
                        .withStartTime(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusHours(1))
                        .withDurationMinutes(30)
                        .build();

        final uk.gov.justice.listing.commands.UpdateHearingForListing update =
                uk.gov.justice.listing.commands.UpdateHearingForListing.updateHearingForListing()
                        .withHearingId(hearingId)
                        .withSelectedCourtCentre(
                                uk.gov.justice.listing.courts.SelectedCourtCentre.selectedCourtCentre()
                                        .withOuCode("OU123")
                                        .build())
                        .withCourtRoomId(courtRoomId)
                        .withStartDate(day1)
                        .withHearingDays(java.util.Arrays.asList(hd1, hd2))
                        .build();

        // Mock search response (first available slot)
        final String bookedCourtScheduleId = java.util.UUID.randomUUID().toString();
        final javax.json.JsonObject searchJson =
                JsonObjects.createObjectBuilder()
                        .add("hearingSlots", JsonObjects.createArrayBuilder()
                                .add(JsonObjects.createObjectBuilder()
                                        .add("courtScheduleId", bookedCourtScheduleId)
                                        .add("courtRoomId", courtRoomId.toString())
                                        .add("sessionStartTime", "2020-01-01T09:00:00Z")))
                        .build();

        final Response searchResponse = org.mockito.Mockito.mock(Response.class);
        when(searchResponse.getStatus()).thenReturn(org.apache.http.HttpStatus.SC_OK);
        when(searchResponse.getEntity()).thenReturn(searchJson);
        when(hearingSlotsService.search(anyMap())).thenReturn(searchResponse);
        when(objectToJsonObjectConverter.convert(searchJson)).thenReturn(searchJson);

        // Ensure payload building doesn't see nulls
        when(slotsToJsonStringConverter.convertHearingDaysToCourtScheduleIdsJson(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(JsonObjects.createArrayBuilder().add(bookedCourtScheduleId).build());

        // Mock list response used by combineSearchAndBookResponseAndListResponse()
        final javax.json.JsonObject listJson =
                JsonObjects.createObjectBuilder()
                        .add("hearings", JsonObjects.createArrayBuilder()
                                .add(JsonObjects.createObjectBuilder()
                                        .add("courtScheduleId", bookedCourtScheduleId)
                                        .add("hearingStartTime", "2020-01-01T09:00:00Z")
                                        .add("duration", 30)))
                        .build();

        final Response listResponse = org.mockito.Mockito.mock(Response.class);
        when(listResponse.getStatus()).thenReturn(org.apache.http.HttpStatus.SC_OK);
        when(listResponse.getEntity()).thenReturn(listJson);
        when(hearingSlotsService.listHearingInCourtSessions(any(javax.json.JsonObject.class))).thenReturn(listResponse);
        when(objectToJsonObjectConverter.convert(listJson)).thenReturn(listJson);

        // Map each "hearings" item to ListUpdateHearing
        when(jsonObjectConverter.convert(
                org.mockito.ArgumentMatchers.any(javax.json.JsonObject.class),
                org.mockito.ArgumentMatchers.eq(uk.gov.moj.cpp.listing.domain.ListUpdateHearing.class)))
                .thenAnswer(inv -> {
                    javax.json.JsonObject jo = inv.getArgument(0);
                    uk.gov.moj.cpp.listing.domain.ListUpdateHearing luh = new uk.gov.moj.cpp.listing.domain.ListUpdateHearing();
                    luh.setCourtScheduleId(jo.getString("courtScheduleId"));
                    luh.setHearingStartTime(jo.getString("hearingStartTime"));
                    luh.setDuration(jo.getInt("duration"));
                    return luh;
                });

        // Capture the search query maps for both days
        @SuppressWarnings("unchecked")
        final org.mockito.ArgumentCaptor<java.util.Map<String, String>> mapCaptor =
                org.mockito.ArgumentCaptor.forClass(java.util.Map.class);

        // Act
        courtScheduleEnrichmentService.enrichWithCourtSchedules(
                update,
                org.mockito.Mockito.mock(uk.gov.justice.services.messaging.JsonEnvelope.class));

        // Assert: search() called twice and multi-day flags present…
        verify(hearingSlotsService, times(2)).search(mapCaptor.capture());
        for (java.util.Map<String, String> qp : mapCaptor.getAllValues()) {
            // multi-day flags
            org.hamcrest.MatcherAssert.assertThat(qp.get("courtSession"), is("AD"));
            org.hamcrest.MatcherAssert.assertThat(qp.get("isSlotBased"), is(Boolean.FALSE.toString()));
            // …and hearingStartTime MUST NOT be present
            org.hamcrest.MatcherAssert.assertThat("hearingStartTime should not be sent for multi-day search",
                    qp.containsKey(CourtScheduleEnrichmentService.HEARING_START_TIME), is(false));
        }
    }

}