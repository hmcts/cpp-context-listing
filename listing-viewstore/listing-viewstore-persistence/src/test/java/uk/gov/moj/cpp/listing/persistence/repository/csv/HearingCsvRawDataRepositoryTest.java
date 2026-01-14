package uk.gov.moj.cpp.listing.persistence.repository.csv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.jdbc.persistence.ViewStoreJdbcDataSourceProvider;
import uk.gov.moj.cpp.listing.persistence.entity.csv.HearingCsvRawData;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HearingRawDataRepository.
 * Tests the raw data retrieval functionality.
 */
@ExtendWith(MockitoExtension.class)
class HearingCsvRawDataRepositoryTest {

    @Mock
    private ViewStoreJdbcDataSourceProvider dataSourceProvider;

    @Mock
    private ObjectMapperProducer objectMapperProducer;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ObjectMapper objectMapper;

    private HearingCsvRawDataRepository repository;

    @BeforeEach
    void setUp() {
        repository = new HearingCsvRawDataRepository();
        repository.viewStoreJdbcDataSourceProvider = dataSourceProvider;
        repository.objectMapperProducer = objectMapperProducer;
        when(dataSourceProvider.getDataSource()).thenReturn(dataSource);
        repository.initialiseDataSource();
    }

    @Test
    void shouldFindHearingsRawDataForCsvReport() throws SQLException, JsonProcessingException {
        // Given
        String courtCentreId = "court-123";
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        Integer numberOfWeeks = 2;
        Long pageSize = 100L;

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(objectMapperProducer.objectMapper()).thenReturn(objectMapper);

        // Mock result set
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("id")).thenReturn("hearing-123");
        when(resultSet.getObject("hearingDate", LocalDate.class)).thenReturn(LocalDate.of(2024, 1, 15));
        when(resultSet.getObject("weekCommencingStartDate", LocalDate.class)).thenReturn(null);
        when(resultSet.getObject("weekCommencingEndDate", LocalDate.class)).thenReturn(null);
        when(resultSet.getString("courtroom")).thenReturn("Courtroom 1");
        when(resultSet.getTimestamp("startTime")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2024, 1, 15, 9, 0)));
        when(resultSet.getObject("durationMinutes", Integer.class)).thenReturn(120);
        when(resultSet.getObject("start_date", LocalDate.class)).thenReturn(LocalDate.of(2024, 1, 15));
        when(resultSet.getObject("end_date", LocalDate.class)).thenReturn(LocalDate.of(2024, 1, 17));
        when(resultSet.getString("day")).thenReturn("1 of 1");
        when(resultSet.getString("properties")).thenReturn("{\"judiciary\":\"Judge Smith\"}");

        JsonNode mockProperties = mock(JsonNode.class);
        when(objectMapper.readTree("{\"judiciary\":\"Judge Smith\"}")).thenReturn(mockProperties);

        // When
        List<HearingCsvRawData> result = repository.findHearingsRawDataForCsvReport(courtCentreId, startDate, numberOfWeeks, pageSize);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        HearingCsvRawData rawData = result.get(0);
        assertEquals("hearing-123", rawData.getId());
        assertEquals(LocalDate.of(2024, 1, 15), rawData.getHearingDate());
        assertEquals(null, rawData.getWeekCommencingStartDate());
        assertEquals(null, rawData.getWeekCommencingEndDate());
        assertEquals("Courtroom 1", rawData.getCourtroom());
        assertNotNull(rawData.getStartTime());
        assertEquals(Integer.valueOf(120), rawData.getDurationMinutes());
        assertEquals(LocalDate.of(2024, 1, 15), rawData.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 17), rawData.getEndDate());
        assertEquals(mockProperties, rawData.getProperties());

        // Verify database interactions
        verify(preparedStatement, times(1)).setString(anyInt(), anyString());
        verify(preparedStatement, times(10)).setTimestamp(anyInt(), any());
        verify(preparedStatement, times(1)).setLong(anyInt(), eq(pageSize));
        verify(preparedStatement).executeQuery();
    }

    @Test
    void shouldHandleEmptyResultSet() throws SQLException {
        // Given
        String courtCentreId = "court-123";
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        Integer numberOfWeeks = 2;
        Long pageSize = 100L;

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        List<HearingCsvRawData> result = repository.findHearingsRawDataForCsvReport(courtCentreId, startDate, numberOfWeeks, pageSize);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleNullProperties() throws SQLException {
        // Given
        String courtCentreId = "court-123";
        LocalDate startDate = LocalDate.of(2024, 1, 15);
        Integer numberOfWeeks = 2;
        Long pageSize = 100L;

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Mock result set with null properties
        when(resultSet.next()).thenReturn(true).thenReturn(false);
        when(resultSet.getString("id")).thenReturn("hearing-123");
        when(resultSet.getObject("hearingDate", LocalDate.class)).thenReturn(LocalDate.of(2024, 1, 15));
        when(resultSet.getObject("weekCommencingStartDate", LocalDate.class)).thenReturn(null);
        when(resultSet.getObject("weekCommencingEndDate", LocalDate.class)).thenReturn(null);
        when(resultSet.getString("courtroom")).thenReturn("Courtroom 1");
        when(resultSet.getTimestamp("startTime")).thenReturn(Timestamp.valueOf(LocalDateTime.of(2024, 1, 15, 9, 0)));
        when(resultSet.getObject("durationMinutes", Integer.class)).thenReturn(120);
        when(resultSet.getObject("start_date", LocalDate.class)).thenReturn(LocalDate.of(2024, 1, 15));
        when(resultSet.getObject("end_date", LocalDate.class)).thenReturn(LocalDate.of(2024, 1, 17));
        when(resultSet.getString("day")).thenReturn("1 of 1");
        when(resultSet.getString("properties")).thenReturn(null);

        // When
        List<HearingCsvRawData> result = repository.findHearingsRawDataForCsvReport(courtCentreId, startDate, numberOfWeeks, pageSize);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        
        HearingCsvRawData rawData = result.get(0);
        assertEquals("hearing-123", rawData.getId());
        assertNull(rawData.getProperties());
    }

}
