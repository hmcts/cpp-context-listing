package uk.gov.moj.cpp.listing.command.api.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.listing.command.api.util.FileUtil.givenPayload;

import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.moj.cpp.listing.common.service.HearingSlotsService;
import uk.gov.moj.cpp.listing.domain.HearingSlotSearchResponse;

import java.time.LocalDate;
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

    @Test
    public void searchAndBookShouldReturnBookedHearingSlots() {
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
}