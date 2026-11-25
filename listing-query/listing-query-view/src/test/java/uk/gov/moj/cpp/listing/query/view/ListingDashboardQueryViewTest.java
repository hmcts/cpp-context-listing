package uk.gov.moj.cpp.listing.query.view;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.persistence.entity.dashboard.ListingDashboardRow;
import uk.gov.moj.cpp.listing.persistence.repository.dashboard.ListingDashboardRepository;
import uk.gov.moj.cpp.listing.query.view.dto.PaginationParameterFactory;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListingDashboardQueryViewTest {

    private static final String COURT_CENTRE_ID = "6131bd34-33d9-3d1e-8152-8b5a2084f1bd";
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 29);
    private static final long MAX_PAGE_SIZE = 1000L;

    @Mock
    private ListingDashboardRepository listingDashboardRepository;

    @Mock
    private PaginationParameterFactory paginationParameterFactory;

    @Mock
    private UtcClock utcClock;

    @InjectMocks
    private ListingDashboardQueryView listingDashboardQueryView;

    @Test
    void shouldMoveAnyDateBackToMondayAndReturnMondayToSunday() {
        when(paginationParameterFactory.getMaxPageSize()).thenReturn(MAX_PAGE_SIZE);
        when(listingDashboardRepository.findDashboardHearings(COURT_CENTRE_ID, MONDAY, SUNDAY, MAX_PAGE_SIZE)).thenReturn(List.of());

        final JsonObject response = listingDashboardQueryView.getListingDashboard(query("2026-11-26")).payloadAsJsonObject();

        assertThat(response.getString("courtCentreId"), is(COURT_CENTRE_ID));
        assertThat(response.getString("startDate"), is("2026-11-23"));
        assertThat(response.getString("endDate"), is("2026-11-29"));
        assertThat(response.getJsonArray("hearings").size(), is(0));
    }

    @Test
    void shouldKeepMondayAndMoveSundayBackToItsMonday() {
        assertThat(listingDashboardQueryView.weekStart("2026-11-23"), is(MONDAY));
        assertThat(listingDashboardQueryView.weekStart("2026-11-29"), is(MONDAY));
        assertThat(listingDashboardQueryView.weekStart("2026-11-30"), is(MONDAY.plusWeeks(1)));
    }

    @Test
    void shouldUseTodayInUkWhenNoStartDate() {
        // 23:30 UTC on Sunday 29 Mar 2026 is 00:30 BST on Monday 30 Mar 2026 - a new week in the UK
        when(utcClock.now()).thenReturn(ZonedDateTime.parse("2026-03-29T23:30:00Z"));

        assertThat(listingDashboardQueryView.weekStart(null), is(LocalDate.of(2026, 3, 30)));
    }

    @Test
    void shouldRejectInvalidStartDate() {
        final JsonEnvelope query = query("26/11/2026");

        assertThrows(BadRequestException.class, () -> listingDashboardQueryView.getListingDashboard(query));
        verify(listingDashboardRepository, never()).findDashboardHearings(anyString(), any(), any(), anyLong());
    }

    @Test
    void shouldRejectInvalidCourtCentreId() {
        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID(ListingDashboardQueryView.LISTING_DASHBOARD_QUERY_NAME),
                createObjectBuilder().add("courtCentreId", "not-a-uuid").build());

        assertThrows(BadRequestException.class, () -> listingDashboardQueryView.getListingDashboard(query));
    }

    @Test
    void shouldReturnSlimHearingRowsAndOmitNullFields() {
        final ListingDashboardRow allocated = new ListingDashboardRow();
        allocated.setHearingId("hearing-1");
        allocated.setHearingDate(MONDAY);
        allocated.setCourtRoomId("room-1");
        allocated.setCourtRoomName("Courtroom 4");
        allocated.setStartTime("2026-11-23T10:00:00Z");
        allocated.setDurationMinutes(330);
        allocated.setHearingTypeId("type-1");
        allocated.setHearingType("Trial");
        allocated.setHearingDayNumber(13);
        allocated.setTotalHearingDays(16);
        allocated.setCaseUrns(List.of("URN1"));
        allocated.setApplicationReferences(List.of());
        allocated.setAllocated(true);

        final ListingDashboardRow unallocated = new ListingDashboardRow();
        unallocated.setHearingId("hearing-2");
        unallocated.setWeekCommencingStartDate(MONDAY);
        unallocated.setWeekCommencingEndDate(MONDAY.plusDays(4));
        unallocated.setAllocated(false);

        when(paginationParameterFactory.getMaxPageSize()).thenReturn(MAX_PAGE_SIZE);
        when(listingDashboardRepository.findDashboardHearings(COURT_CENTRE_ID, MONDAY, SUNDAY, MAX_PAGE_SIZE))
                .thenReturn(List.of(allocated, unallocated));

        final JsonEnvelope response = listingDashboardQueryView.getListingDashboard(query("2026-11-23"));

        assertThat(response.metadata().name(), is("listing.query.listing-dashboard"));
        final JsonObject first = response.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(0);
        assertThat(first.getString("hearingId"), is("hearing-1"));
        assertThat(first.getString("hearingDate"), is("2026-11-23"));
        assertThat(first.getString("courtRoomName"), is("Courtroom 4"));
        assertThat(first.getString("startTime"), is("2026-11-23T10:00:00Z"));
        assertThat(first.getInt("durationMinutes"), is(330));
        assertThat(first.getString("hearingType"), is("Trial"));
        assertThat(first.getInt("hearingDayNumber"), is(13));
        assertThat(first.getInt("totalHearingDays"), is(16));
        assertThat(first.getJsonArray("caseUrns").getString(0), is("URN1"));
        assertThat(first.getBoolean("allocated"), is(true));

        final JsonObject second = response.payloadAsJsonObject().getJsonArray("hearings").getJsonObject(1);
        assertThat(second.getString("weekCommencingStartDate"), is("2026-11-23"));
        assertThat(second.getString("weekCommencingEndDate"), is("2026-11-27"));
        assertFalse(second.containsKey("hearingDate"));
        assertFalse(second.containsKey("courtRoomId"));
        assertFalse(second.containsKey("durationMinutes"));
        assertThat(second.getJsonArray("caseUrns").size(), is(0));
        assertThat(second.getBoolean("allocated"), is(false));
    }

    private static JsonEnvelope query(final String startDate) {
        final JsonObjectBuilder payload = createObjectBuilder().add("courtCentreId", COURT_CENTRE_ID);
        if (startDate != null) {
            payload.add("startDate", startDate);
        }
        return envelopeFrom(metadataWithRandomUUID(ListingDashboardQueryView.LISTING_DASHBOARD_QUERY_NAME), payload.build());
    }
}
