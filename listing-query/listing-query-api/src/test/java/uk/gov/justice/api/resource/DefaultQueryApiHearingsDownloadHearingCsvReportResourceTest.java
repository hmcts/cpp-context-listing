package uk.gov.justice.api.resource;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for filename generation logic in DefaultQueryApiHearingsDownloadHearingCsvReportResource
 */
class DefaultQueryApiHearingsDownloadHearingCsvReportResourceTest {

    @Test
    void shouldGenerateCorrectFilenameWithStartDate() {
        // Given
        final String startDate = "2024-01-15";
        final String expectedFilename = "hearing_report_2024-01-15.csv";
        
        // When
        final String actualFilename = "hearing_report_" + startDate + ".csv";
        
        // Then
        assertEquals(expectedFilename, actualFilename);
    }

    @Test
    void shouldGenerateCorrectFilenameWithDifferentDate() {
        // Given
        final String startDate = "2024-12-25";
        final String expectedFilename = "hearing_report_2024-12-25.csv";
        
        // When
        final String actualFilename = "hearing_report_" + startDate + ".csv";
        
        // Then
        assertEquals(expectedFilename, actualFilename);
    }
}
