package uk.gov.moj.cpp.listing.command.api.service;

import uk.gov.justice.services.messaging.JsonObjects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.api.util.FileUtil.givenPayload;

import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.InitiationCode;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.commands.HearingDay;
import uk.gov.justice.listing.commands.HearingListingNeeds;
import uk.gov.justice.listing.courts.SelectedCourtCentre;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.command.api.util.SlotsToJsonStringConverter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.HearingSlotSearchResponse;
import uk.gov.moj.cpp.listing.domain.utils.DateAndTimeUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    @Mock
    private uk.gov.moj.cpp.listing.command.api.courtcentre.CourtCentreFactory courtCentreFactory;

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

    private HearingListingNeeds enforcementHearing(final UUID courtCentreId) {
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(20)
                .build();

        return HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                // OTHER-type case (initiationCode "O") - the Enforcement/civil discriminator
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withInitiationCode(InitiationCode.O)
                        .withProsecutionCaseIdentifier(uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withProsecutionAuthorityId(UUID.randomUUID())
                                .build())
                        .build()))
                // Enforcement: no court room pre-assigned - search/book by OUCODE (courtCentre) alone
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).build())
                .withEarliestStartDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withEndDate(LocalDate.now().plusDays(14).toString())
                .withEstimatedMinutes(20)
                .withHearingDays(List.of(hearingDay))
                .build();
    }

    @Test
    void enforcementHearingWithSlotAvailableOnFirstDayIsBookedWithoutFurtherSearching() {
        final UUID courtCentreId = UUID.randomUUID();
        final HearingListingNeeds hearing = enforcementHearing(courtCentreId);

        final JsonObject searchBookResponse = givenPayload("/courtscheduler.search.book.hearing.slots.json");
        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(searchBookResponse);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(searchBookResponse);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing,
                org.mockito.Mockito.mock(JsonEnvelope.class));

        // Auto-booked: courtScheduleId/courtRoomId come from the search & book response
        assertThat(result.getHearingDays().get(0).getCourtScheduleId().toString(), is("23681024-8eac-4890-8c44-4651ad48cb24"));
        assertThat(result.getHearingDays().get(0).getCourtRoomId().toString(), is("573bd1e6-92fa-49c2-8fa9-a355c1a4cded"));

        // Search was performed by OUCODE (courtCentre) alone, filtered by the "Enforcement (Auto)"
        // business type (this is the date-range/day-by-day path), with no pre-selected courtRoomId,
        // and stopped at the very first day (no cutoff sent - each day is searched one at a time,
        // not delegated to courtscheduler's own range search)
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hearingSlotsService, times(1)).searchBookSlots(paramsCaptor.capture());
        final Map<String, String> params = paramsCaptor.getValue();
        assertThat(params.get(CourtScheduleEnrichmentService.COURT_CENTRE_ID), is(courtCentreId.toString()));
        assertThat(params.get(CourtScheduleEnrichmentService.BUSINESS_TYPE), is(CourtScheduleEnrichmentService.ENFORCEMENT_AUTO_BUSINESS_TYPE));
        assertThat(params.containsKey(CourtScheduleEnrichmentService.COURT_ROOM_ID), is(false));
        assertThat(params.get(CourtScheduleEnrichmentService.HEARING_DATE), is(LocalDate.now().toString()));
        assertThat(params.containsKey(CourtScheduleEnrichmentService.HEARING_SESSION_DATE_CUT_OFF), is(false));
    }

    @Test
    void enforcementHearingWithSlotAvailableOnALaterDaySearchesDayByDayUntilFound() {
        final UUID courtCentreId = UUID.randomUUID();
        final HearingListingNeeds hearing = enforcementHearing(courtCentreId);

        final JsonObject searchBookResponse = givenPayload("/courtscheduler.search.book.hearing.slots.json");
        final Response notFoundResponse = org.mockito.Mockito.mock(Response.class);
        when(notFoundResponse.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(searchBookResponse);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(searchBookResponse);

        // No slot for the first 3 days, then found on day 4
        when(hearingSlotsService.searchBookSlots(anyMap()))
                .thenReturn(notFoundResponse, notFoundResponse, notFoundResponse, response);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing,
                org.mockito.Mockito.mock(JsonEnvelope.class));

        assertThat(result.getHearingDays().get(0).getCourtScheduleId().toString(), is("23681024-8eac-4890-8c44-4651ad48cb24"));

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hearingSlotsService, times(4)).searchBookSlots(paramsCaptor.capture());
        final List<Map<String, String>> allParams = paramsCaptor.getAllValues();
        assertThat(allParams.get(0).get(CourtScheduleEnrichmentService.HEARING_DATE), is(LocalDate.now().toString()));
        assertThat(allParams.get(1).get(CourtScheduleEnrichmentService.HEARING_DATE), is(LocalDate.now().plusDays(1).toString()));
        assertThat(allParams.get(2).get(CourtScheduleEnrichmentService.HEARING_DATE), is(LocalDate.now().plusDays(2).toString()));
        assertThat(allParams.get(3).get(CourtScheduleEnrichmentService.HEARING_DATE), is(LocalDate.now().plusDays(3).toString()));
    }

    @Test
    void enforcementHearingWithNoAvailableSlotAnywhereInRangeIsLeftUnallocated() {
        final UUID courtCentreId = UUID.randomUUID();
        final HearingListingNeeds hearing = enforcementHearing(courtCentreId);

        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        // No slot available on any day in range for the Enforcement business type
        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing,
                org.mockito.Mockito.mock(JsonEnvelope.class));

        // Hearing day is left unallocated: no courtScheduleId/courtRoomId populated
        assertThat(result.getHearingDays().get(0).getCourtScheduleId(), nullValue());
        assertThat(result.getHearingDays().get(0).getCourtRoomId(), nullValue());

        // Searched every day from start to end date (15 days: today .. today+14) before giving up
        verify(hearingSlotsService, times(15)).searchBookSlots(anyMap());
    }

    @Test
    void enforcementHearingWithoutEndDateSearchesTheSpecificRequestedDateAndTime() {
        final UUID courtCentreId = UUID.randomUUID();
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(20)
                .build();
        // A single-date OTHER-type submission (hearingDetails, not hearingDateRangeDetails): no
        // endDate, and dateOfHearing+timeOfHearing are mandatory/always populated all the way through
        // from staging, so listedStartDateTime carries a genuine, meaningful requested time here -
        // unlike the date-range case's placeholder "10:00:00" default.
        final ZonedDateTime requestedStartDateTime = ZonedDateTime.now(ZoneOffset.UTC).plusHours(3);
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withInitiationCode(InitiationCode.O)
                        .withProsecutionCaseIdentifier(uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withProsecutionAuthorityId(UUID.randomUUID())
                                .build())
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).build())
                .withListedStartDateTime(requestedStartDateTime)
                .withEstimatedMinutes(20)
                .withHearingDays(List.of(hearingDay))
                .build();

        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_NOT_FOUND);

        courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing, org.mockito.Mockito.mock(JsonEnvelope.class));

        // No endDate - not eligible for the day-by-day range loop, only the single day is searched,
        // the genuinely-requested start time IS sent (not nulled out as it would be for a range),
        // and the business type is plain "Enforcement" (ENF), not "Enforcement (Auto)"
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hearingSlotsService, times(1)).searchBookSlots(paramsCaptor.capture());
        final Map<String, String> params = paramsCaptor.getValue();
        assertThat(params.get(CourtScheduleEnrichmentService.HEARING_START_TIME), is(DateAndTimeUtils.toIsoString(requestedStartDateTime)));
        assertThat(params.get(CourtScheduleEnrichmentService.BUSINESS_TYPE), is(CourtScheduleEnrichmentService.ENFORCEMENT_BUSINESS_TYPE));
    }

    @Test
    void nonEnforcementMagistratesHearingAllocationIsUnaffected() {
        final UUID courtCentreId = UUID.randomUUID();
        final UUID courtRoomId = UUID.randomUUID();
        final HearingDay hearingDay = HearingDay.hearingDay()
                .withHearingDate(LocalDate.now())
                .withStartTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withDurationMinutes(20)
                .build();
        final HearingListingNeeds hearing = HearingListingNeeds.hearingListingNeeds()
                .withId(UUID.randomUUID())
                .withJurisdictionType(JurisdictionType.MAGISTRATES)
                // Non-OTHER-type case (e.g. SUMMONS) - regular MAGS hearing, pre-assigned court room required
                .withProsecutionCases(List.of(ProsecutionCase.prosecutionCase()
                        .withInitiationCode(InitiationCode.S)
                        .withProsecutionCaseIdentifier(uk.gov.justice.core.courts.ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withProsecutionAuthorityId(UUID.randomUUID())
                                .build())
                        .build()))
                .withCourtCentre(CourtCentre.courtCentre().withId(courtCentreId).withRoomId(courtRoomId).build())
                .withListedStartDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withEndDate(LocalDate.now().plusDays(1).toString())
                .withEstimatedMinutes(20)
                .withHearingDays(List.of(hearingDay))
                .build();

        final JsonObject searchBookResponse = givenPayload("/courtscheduler.search.book.hearing.slots.json");
        when(hearingSlotsService.searchBookSlots(anyMap())).thenReturn(response);
        when(response.getStatus()).thenReturn(HttpStatus.SC_OK);
        when(response.getEntity()).thenReturn(searchBookResponse);
        when(objectToJsonObjectConverter.convert(any())).thenReturn(searchBookResponse);

        final HearingListingNeeds result = courtScheduleEnrichmentService.enrichWithCourtSchedules(hearing,
                org.mockito.Mockito.mock(JsonEnvelope.class));

        assertThat(result.getHearingDays().get(0).getCourtScheduleId(), notNullValue());

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(hearingSlotsService).searchBookSlots(paramsCaptor.capture());
        final Map<String, String> params = paramsCaptor.getValue();
        // Existing MAGS behaviour: pre-assigned courtRoomId still passed, no businessType filter applied
        assertThat(params.get(CourtScheduleEnrichmentService.COURT_ROOM_ID), is(courtRoomId.toString()));
        assertThat(params.containsKey(CourtScheduleEnrichmentService.BUSINESS_TYPE), is(false));
    }

}