package uk.gov.moj.cpp.listing.query.view.service.csv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.listing.persistence.entity.csv.HearingCsvRawData;
import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;
import uk.gov.moj.cpp.listing.query.view.dto.csv.HearingCsvData;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HearingJsonExtractionService.
 * Tests the JsonB extraction logic that was previously handled in SQL.
 */
@ExtendWith(MockitoExtension.class)
class HearingCsvJsonExtractionServiceTest {

    private HearingCsvJsonExtractionService extractionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        extractionService = new HearingCsvJsonExtractionService();
        objectMapper = new ObjectMapper();
    }

    @Test
    void shouldExtractBasicHearingData() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        JsonNode properties = createBasicProperties();
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 1, 15), result.getHearingDate());
        assertEquals("Fixed", result.getWeekCommencing());
        assertEquals("Courtroom 1", result.getCourtroom());
        assertEquals("09:00", result.getStartTime());
        assertEquals("120", result.getDuration()); // Should use durationMinutes (120) over estimatedMinutes (90)
        assertEquals("1 of 3", result.getDay());
        assertEquals("From 2024-01-15 to 2024-01-17", result.getMultiDayHearingDetails());
    }

    @Test
    void shouldExtractJudiciaryFromProperties() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        ObjectNode properties = objectMapper.createObjectNode();
        properties.put("judiciary", "Judge Smith");
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertEquals("Judge Smith", result.getJudiciary());
    }

    @Test
    void shouldExtractHearingTypeFromProperties() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        ObjectNode properties = objectMapper.createObjectNode();
        ObjectNode type = objectMapper.createObjectNode();
        type.put("description", "Trial");
        properties.set("type", type);
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertEquals("Trial", result.getHearingType());
    }

    @Test
    void shouldExtractCaseUrnsFromListedCases() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        JsonNode properties = createPropertiesWithListedCases();
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertEquals("CASE-001", result.getCaseUrns());
    }

    @Test
    void shouldExtractDefendantNamesFromListedCases() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        JsonNode properties = createPropertiesWithListedCases();
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertEquals("John Doe", result.getDefendantNames());
    }

    @Test
    void shouldDetectYouthDefendantFlag() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        JsonNode properties = createPropertiesWithYouthDefendant();
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertEquals("Youth", result.getDefendantFlag());
    }

    @Test
    void shouldExtractOffencesFromListedCases() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        JsonNode properties = createPropertiesWithListedCases();
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertEquals("Theft", result.getOffences());
    }

    @Test
    void shouldDetermineRecordTypeAsCase() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        JsonNode properties = createPropertiesWithListedCases();
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertEquals(CsvRecordType.CASE, result.getRecordType());
    }

    @Test
    void shouldDetermineRecordTypeAsUnknown() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        ObjectNode properties = objectMapper.createObjectNode();
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertEquals(CsvRecordType.UNKNOWN, result.getRecordType());
    }

    @Test
    void shouldHandleNullRawData() {
        // When
        HearingCsvData result = extractionService.extractCsvData(null);

        // Then
        assertNotNull(result);
        assertNull(result.getHearingDate());
    }

    @Test
    void shouldHandleNullProperties() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        rawData.setProperties(null);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertNotNull(result);
        assertEquals(LocalDate.of(2024, 1, 15), result.getHearingDate());
        assertEquals("", result.getJudiciary());
    }

    @Test
    void shouldHandleEmptyProperties() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        ObjectNode properties = objectMapper.createObjectNode();
        rawData.setProperties(properties);

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertNotNull(result);
        assertEquals("", result.getJudiciary());
        assertEquals("", result.getHearingType());
        assertEquals("", result.getCaseUrns());
    }

    @Test
    void shouldExtractDayField() {
        // Given
        HearingCsvRawData rawData = createBasicRawData();
        rawData.setDay("2 of 5");

        // When
        HearingCsvData result = extractionService.extractCsvData(rawData);

        // Then
        assertNotNull(result);
        assertEquals("2 of 5", result.getDay());
    }

    @Test
    void shouldExtractDurationWithCoalesceLogic() throws Exception {
        // Test case 1: durationMinutes takes precedence over estimatedMinutes
        HearingCsvRawData rawData1 = createBasicRawData();
        rawData1.setDurationMinutes(120);
        rawData1.setProperties(createPropertiesWithEstimatedMinutes("90"));

        HearingCsvData result1 = extractionService.extractCsvData(rawData1);
        assertEquals("120", result1.getDuration());

        // Test case 2: use estimatedMinutes when durationMinutes is null
        HearingCsvRawData rawData2 = createBasicRawData();
        rawData2.setDurationMinutes(null);
        rawData2.setProperties(createPropertiesWithEstimatedMinutes("90"));

        HearingCsvData result2 = extractionService.extractCsvData(rawData2);
        assertEquals("90", result2.getDuration());

        // Test case 3: return empty string when both are null/empty
        HearingCsvRawData rawData3 = createBasicRawData();
        rawData3.setDurationMinutes(null);
        rawData3.setProperties(createPropertiesWithEstimatedMinutes(""));

        HearingCsvData result3 = extractionService.extractCsvData(rawData3);
        assertEquals("", result3.getDuration());
    }

    @Test
    void shouldExtractWeekCommencingWithCaseLogic() {
        // Test case 1: return 'Fixed' when weekCommencingStartDate is null
        HearingCsvRawData rawData1 = createBasicRawData();
        rawData1.setWeekCommencingStartDate(null);
        rawData1.setWeekCommencingEndDate(null);

        HearingCsvData result1 = extractionService.extractCsvData(rawData1);
        assertEquals("Fixed", result1.getWeekCommencing());

        // Test case 2: format date range when both dates are present
        HearingCsvRawData rawData2 = createBasicRawData();
        rawData2.setWeekCommencingStartDate(LocalDate.of(2024, 1, 15));
        rawData2.setWeekCommencingEndDate(LocalDate.of(2024, 1, 19));

        HearingCsvData result2 = extractionService.extractCsvData(rawData2);
        assertEquals("From: 2024-01-15 To: 2024-01-19", result2.getWeekCommencing());

        // Test case 3: format with only start date when end date is null
        HearingCsvRawData rawData3 = createBasicRawData();
        rawData3.setWeekCommencingStartDate(LocalDate.of(2024, 1, 15));
        rawData3.setWeekCommencingEndDate(null);

        HearingCsvData result3 = extractionService.extractCsvData(rawData3);
        assertEquals("From: 2024-01-15", result3.getWeekCommencing());
    }

    @Test
    void shouldExtractJudiciaryWithArrayFormat() throws Exception {
        // Test case 1: judiciary as simple string (legacy format)
        HearingCsvRawData rawData1 = createBasicRawData();
        ObjectNode properties1 = objectMapper.createObjectNode();
        properties1.put("judiciary", "Judge Smith");
        rawData1.setProperties(properties1);

        HearingCsvData result1 = extractionService.extractCsvData(rawData1);
        assertEquals("Judge Smith", result1.getJudiciary());

        // Test case 2: judiciary as array of objects (new format)
        HearingCsvRawData rawData2 = createBasicRawData();
        ObjectNode properties2 = objectMapper.createObjectNode();
        ArrayNode judiciaryArray = objectMapper.createArrayNode();

        ObjectNode judge1 = objectMapper.createObjectNode();
        judge1.put("judicialId", "3566a6ee-64c0-4049-b366-e6237ebcc9bd");
        ObjectNode roleType1 = objectMapper.createObjectNode();
        roleType1.put("judiciaryType", "CIRCUIT_JUDGE");
        judge1.set("judicialRoleType", roleType1);

        ObjectNode judge2 = objectMapper.createObjectNode();
        judge2.put("judicialId", "4566a6ee-64c0-4049-b366-e6237ebcc9bd");
        ObjectNode roleType2 = objectMapper.createObjectNode();
        roleType2.put("judiciaryType", "DISTRICT_JUDGE");
        judge2.set("judicialRoleType", roleType2);

        judiciaryArray.add(judge1);
        judiciaryArray.add(judge2);
        properties2.set("judiciary", judiciaryArray);
        rawData2.setProperties(properties2);

        HearingCsvData result2 = extractionService.extractCsvData(rawData2);
        // Should contain both judicial IDs (order may vary due to HashSet)
        assertTrue(result2.getJudiciary().contains("3566a6ee-64c0-4049-b366-e6237ebcc9bd"));
        assertTrue(result2.getJudiciary().contains("4566a6ee-64c0-4049-b366-e6237ebcc9bd"));
        assertTrue(result2.getJudiciary().contains(",")); // Should be comma-separated

        // Test case 3: judiciary as array with single judge
        HearingCsvRawData rawData3 = createBasicRawData();
        ObjectNode properties3 = objectMapper.createObjectNode();
        ArrayNode judiciaryArray3 = objectMapper.createArrayNode();

        ObjectNode judge3 = objectMapper.createObjectNode();
        judge3.put("judicialId", "3566a6ee-64c0-4049-b366-e6237ebcc9bd");
        ObjectNode roleType3 = objectMapper.createObjectNode();
        roleType3.put("judiciaryType", "CIRCUIT_JUDGE");
        judge3.set("judicialRoleType", roleType3);

        judiciaryArray3.add(judge3);
        properties3.set("judiciary", judiciaryArray3);
        rawData3.setProperties(properties3);

        HearingCsvData result3 = extractionService.extractCsvData(rawData3);
        assertEquals("3566a6ee-64c0-4049-b366-e6237ebcc9bd", result3.getJudiciary());
    }

    // Helper methods to create test data

    private HearingCsvRawData createBasicRawData() {
        HearingCsvRawData rawData = new HearingCsvRawData();
        rawData.setId("hearing-123");
        rawData.setHearingDate(LocalDate.of(2024, 1, 15));
        rawData.setWeekCommencingStartDate(null); // This should result in "Fixed"
        rawData.setWeekCommencingEndDate(null);
        rawData.setCourtroom("Courtroom 1");
        rawData.setStartTime("09:00");
        rawData.setDurationMinutes(120);
        rawData.setStartDate(LocalDate.of(2024, 1, 15));
        rawData.setEndDate(LocalDate.of(2024, 1, 17));
        rawData.setDay("1 of 3");
        return rawData;
    }

    private JsonNode createBasicProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.put("judiciary", "Judge Smith");

        ObjectNode type = objectMapper.createObjectNode();
        type.put("description", "Trial");
        properties.set("type", type);

        return properties;
    }

    private JsonNode createPropertiesWithListedCases() {
        ObjectNode properties = objectMapper.createObjectNode();

        ArrayNode listedCases = objectMapper.createArrayNode();
        ObjectNode listedCase = objectMapper.createObjectNode();

        // Case identifier
        ObjectNode caseIdentifier = objectMapper.createObjectNode();
        caseIdentifier.put("caseReference", "CASE-001");
        listedCase.set("caseIdentifier", caseIdentifier);
        listedCase.put("id", "case-001");

        // Defendants
        ArrayNode defendants = objectMapper.createArrayNode();
        ObjectNode defendant = objectMapper.createObjectNode();
        defendant.put("firstName", "John");
        defendant.put("lastName", "Doe");
        defendant.put("isYouth", "false");

        // Offences
        ArrayNode offences = objectMapper.createArrayNode();
        ObjectNode offence = objectMapper.createObjectNode();
        ObjectNode statementOfOffence = objectMapper.createObjectNode();
        statementOfOffence.put("title", "Theft");
        offence.set("statementOfOffence", statementOfOffence);
        offences.add(offence);
        defendant.set("offences", offences);

        defendants.add(defendant);
        listedCase.set("defendants", defendants);
        listedCases.add(listedCase);
        properties.set("listedCases", listedCases);

        return properties;
    }

    private JsonNode createPropertiesWithYouthDefendant() {
        ObjectNode properties = objectMapper.createObjectNode();

        ArrayNode listedCases = objectMapper.createArrayNode();
        ObjectNode listedCase = objectMapper.createObjectNode();

        ArrayNode defendants = objectMapper.createArrayNode();
        ObjectNode defendant = objectMapper.createObjectNode();
        defendant.put("firstName", "Young");
        defendant.put("lastName", "Person");
        defendant.put("isYouth", "true");

        defendants.add(defendant);
        listedCase.set("defendants", defendants);
        listedCases.add(listedCase);
        properties.set("listedCases", listedCases);

        return properties;
    }

    private ObjectNode createPropertiesWithEstimatedMinutes(String estimatedMinutes) throws Exception {
        ObjectNode properties = objectMapper.createObjectNode();
        if (estimatedMinutes != null && !estimatedMinutes.isEmpty()) {
            properties.put("estimatedMinutes", estimatedMinutes);
        }
        return properties;
    }
}