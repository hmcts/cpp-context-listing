package uk.gov.moj.cpp.listing.persistence.repository.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;
import uk.gov.moj.cpp.listing.persistence.entity.dashboard.ListingDashboardRow;
import uk.gov.moj.cpp.listing.persistence.repository.courtlist.HearingJdbcException;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListingDashboardRepositoryTest {

    private static final String COURT_CENTRE_ID = "6131bd34-33d9-3d1e-8152-8b5a2084f1bd";
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 23);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 11, 29);
    private static final long PAGE_SIZE = 500L;

    @Mock
    private ViewStoreJdbcDataSourceProvider dataSourceProvider;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    // Lenient: each test stubs only the columns it cares about; the rest return null.
    @Mock(strictness = Mock.Strictness.LENIENT)
    private ResultSet resultSet;

    @Mock
    private Array caseUrns;

    @Mock
    private Array applicationReferences;

    private ListingDashboardRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ListingDashboardRepository();
        repository.viewStoreJdbcDataSourceProvider = dataSourceProvider;
        when(dataSourceProvider.getDataSource()).thenReturn(dataSource);
        repository.initialiseDataSource();
    }

    @Test
    void shouldMapHearingDayRowAndBindWeekParameters() throws SQLException {
        givenQueryReturnsRows(true, false);
        when(resultSet.getString("hearingId")).thenReturn("hearing-1");
        when(resultSet.getObject("hearingDate", LocalDate.class)).thenReturn(MONDAY);
        when(resultSet.getString("courtRoomId")).thenReturn("room-1");
        when(resultSet.getString("courtRoomName")).thenReturn("Courtroom 4");
        when(resultSet.getTimestamp("startTime")).thenReturn(Timestamp.from(Instant.parse("2026-11-23T10:00:00Z")));
        when(resultSet.getObject("durationMinutes", Integer.class)).thenReturn(330);
        when(resultSet.getString("hearingTypeId")).thenReturn("type-1");
        when(resultSet.getString("hearingType")).thenReturn("Trial");
        when(resultSet.getObject("hearingDayNumber", Integer.class)).thenReturn(13);
        when(resultSet.getObject("totalHearingDays", Integer.class)).thenReturn(16);
        when(resultSet.getArray("caseUrns")).thenReturn(caseUrns);
        when(caseUrns.getArray()).thenReturn(new String[]{"URN1", "URN2"});
        when(resultSet.getArray("applicationReferences")).thenReturn(applicationReferences);
        when(applicationReferences.getArray()).thenReturn(new String[]{});
        when(resultSet.getObject("allocated", Boolean.class)).thenReturn(true);

        final List<ListingDashboardRow> rows = repository.findDashboardHearings(COURT_CENTRE_ID, MONDAY, SUNDAY, PAGE_SIZE);

        assertEquals(1, rows.size());
        final ListingDashboardRow row = rows.get(0);
        assertEquals("hearing-1", row.getHearingId());
        assertEquals(MONDAY, row.getHearingDate());
        assertEquals("room-1", row.getCourtRoomId());
        assertEquals("Courtroom 4", row.getCourtRoomName());
        assertEquals("2026-11-23T10:00:00Z", row.getStartTime());
        assertEquals(Integer.valueOf(330), row.getDurationMinutes());
        assertEquals("type-1", row.getHearingTypeId());
        assertEquals("Trial", row.getHearingType());
        assertEquals(Integer.valueOf(13), row.getHearingDayNumber());
        assertEquals(Integer.valueOf(16), row.getTotalHearingDays());
        assertEquals(List.of("URN1", "URN2"), row.getCaseUrns());
        assertEquals(List.of(), row.getApplicationReferences());
        assertEquals(Boolean.TRUE, row.getAllocated());

        verify(preparedStatement).setString(1, COURT_CENTRE_ID);
        verify(preparedStatement).setObject(2, MONDAY);
        verify(preparedStatement).setObject(3, SUNDAY);
        verify(preparedStatement).setObject(4, SUNDAY);
        verify(preparedStatement).setObject(5, MONDAY);
        verify(preparedStatement).setObject(6, SUNDAY);
        verify(preparedStatement).setObject(7, MONDAY);
        verify(preparedStatement).setLong(8, PAGE_SIZE);
    }

    @Test
    void shouldMapUndatedUnallocatedHearingWithNullsAndMissingArrays() throws SQLException {
        givenQueryReturnsRows(true, false);
        when(resultSet.getString("hearingId")).thenReturn("hearing-2");
        when(resultSet.getObject("weekCommencingStartDate", LocalDate.class)).thenReturn(MONDAY);
        when(resultSet.getObject("weekCommencingEndDate", LocalDate.class)).thenReturn(MONDAY.plusDays(4));
        when(resultSet.getObject("totalHearingDays", Integer.class)).thenReturn(0);
        when(resultSet.getObject("allocated", Boolean.class)).thenReturn(false);

        final List<ListingDashboardRow> rows = repository.findDashboardHearings(COURT_CENTRE_ID, MONDAY, SUNDAY, PAGE_SIZE);

        final ListingDashboardRow row = rows.get(0);
        assertNull(row.getHearingDate());
        assertNull(row.getCourtRoomId());
        assertNull(row.getStartTime());
        assertNull(row.getHearingDayNumber());
        assertEquals(MONDAY, row.getWeekCommencingStartDate());
        assertEquals(MONDAY.plusDays(4), row.getWeekCommencingEndDate());
        assertEquals(List.of(), row.getCaseUrns());
        assertEquals(List.of(), row.getApplicationReferences());
        assertEquals(Boolean.FALSE, row.getAllocated());
    }

    @Test
    void shouldReturnEmptyListWhenNoHearings() throws SQLException {
        givenQueryReturnsRows(false);

        assertTrue(repository.findDashboardHearings(COURT_CENTRE_ID, MONDAY, SUNDAY, PAGE_SIZE).isEmpty());
    }

    @Test
    void shouldWrapSqlException() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("boom"));

        assertThrows(HearingJdbcException.class,
                () -> repository.findDashboardHearings(COURT_CENTRE_ID, MONDAY, SUNDAY, PAGE_SIZE));
    }

    private void givenQueryReturnsRows(final Boolean first, final Boolean... rest) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(ListingDashboardRepository.LISTING_DASHBOARD_QUERY)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(first, rest);
    }
}
