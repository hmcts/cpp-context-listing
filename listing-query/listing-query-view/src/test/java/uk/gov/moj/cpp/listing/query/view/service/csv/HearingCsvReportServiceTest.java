package uk.gov.moj.cpp.listing.query.view.service.csv;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataCache;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;
import uk.gov.moj.cpp.listing.common.progression.ProgressionNotesCache;
import javax.json.Json;
import javax.json.JsonObject;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.listing.query.view.dto.csv.HearingCsvData;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for HearingCsvReportService
 */
@ExtendWith(MockitoExtension.class)
class HearingCsvReportServiceTest {

    @Mock
    private ReferenceDataCache referenceDataCache;

    @Mock
    private ProgressionNotesCache progressionNotesCache;

    @InjectMocks
    private HearingCsvReportService hearingCsvReportService;

    private UUID judiciaryId;
    private String judiciaryJson;
    private JsonEnvelope originalRequest;

    @BeforeEach
    void setUp() {
        judiciaryId = UUID.randomUUID();
        judiciaryJson = String.format("%s", judiciaryId);
        originalRequest = org.mockito.Mockito.mock(JsonEnvelope.class);
    }

    @Test
    void shouldGenerateCsvContentWithResolvedJudiciaryNames() {
        // Given
        final List<HearingCsvData> csvData = new ArrayList<>();
        csvData.add(new HearingCsvData(
                LocalDate.parse("2024-01-01"),
                "From 2024-01-01 to 2024-01-07",
                "Courtroom 1",
                judiciaryJson,
                "09:00",
                "Trial",
                "120",
                "Day 1 of 1",
                "URN123",
                "", // caseIds
                "John Doe",
                "Flag",
                "Offence",
                "Note",
                "English",
                "Video",
                "CTL 2025",
                "Multi-day details",
                "Pinned",
                "Unpinned",
                "Markers",
                "Restriction",
                CsvRecordType.CASE
        ));

        final Judiciary judiciary = new Judiciary.Builder()
                .withId(judiciaryId)
                .withTitleJudicialPrefix("His Honour Judge")
                .withSurname("Smith")
                .withTitleSuffix("")
                .build();

        when(referenceDataCache.getJudiciariesMapCache(judiciaryId))
                .thenReturn(Optional.of(judiciary));

        // When
        final String result = hearingCsvReportService.generateCsvContent(csvData, originalRequest);

        // Then
        assertThat(result, is(not("")));
        assertThat(result, containsString("Date of hearing"));
        assertThat(result, containsString("Courtroom"));
        assertThat(result, containsString("His Honour Judge Smith"));
        assertThat(result, containsString("2024-01-01"));
        assertThat(result, containsString("John Doe"));
    }

    @Test
    void shouldGenerateCsvContentWithEmptyJudiciaryWhenNoData() {
        // Given
        final List<HearingCsvData> csvData = new ArrayList<>();
        csvData.add(new HearingCsvData(
                LocalDate.parse("2024-01-01"),
                "From 2024-01-01 to 2024-01-07",
                "Courtroom 1",
                "", // Empty judiciary
                "09:00",
                "Trial",
                "120",
                "Day 1 of 1",
                "URN123",
                "", // caseIds
                "John Doe",
                "Flag",
                "Offence",
                "Note",
                "English",
                "Video",
                "CTL 2025",
                "Multi-day details",
                "Pinned",
                "Unpinned",
                "Markers",
                "Restriction",
                CsvRecordType.CASE
        ));

        // When
        final String result = hearingCsvReportService.generateCsvContent(csvData, originalRequest);

        // Then
        assertThat(result, is(not("")));
        assertThat(result, containsString("Date of hearing"));
        assertThat(result, containsString("Courtroom"));
        assertThat(result, containsString("2024-01-01"));
        assertThat(result, containsString("John Doe"));
    }

    @Test
    void shouldGenerateCsvContentWithMultipleJudiciaries() {
        // Given
        final UUID judiciaryId2 = UUID.randomUUID();
        final String multipleJudiciaryJson = String.format("%s,%s", judiciaryId, judiciaryId2);

        final List<HearingCsvData> csvData = new ArrayList<>();
        csvData.add(new HearingCsvData(
                LocalDate.parse("2024-01-01"),
                "From 2024-01-01 to 2024-01-07",
                "Courtroom 1",
                multipleJudiciaryJson,
                "09:00",
                "Trial",
                "120",
                "Day 1 of 1",
                "URN123",
                "", // caseIds
                "John Doe",
                "Flag",
                "Offence",
                "Note",
                "English",
                "Video",
                "CTL 2025",
                "Multi-day details",
                "Pinned",
                "Unpinned",
                "Markers",
                "Restriction",
                CsvRecordType.CASE
        ));

        final Judiciary judiciary1 = new Judiciary.Builder()
                .withId(judiciaryId)
                .withTitleJudicialPrefix("His Honour Judge")
                .withSurname("Smith")
                .withTitleSuffix("")
                .build();

        final Judiciary judiciary2 = new Judiciary.Builder()
                .withId(judiciaryId2)
                .withTitleJudicialPrefix("Recorder")
                .withSurname("Jones")
                .withTitleSuffix("")
                .build();

        when(referenceDataCache.getJudiciariesMapCache(judiciaryId))
                .thenReturn(Optional.of(judiciary1));
        when(referenceDataCache.getJudiciariesMapCache(judiciaryId2))
                .thenReturn(Optional.of(judiciary2));

        // When
        final String result = hearingCsvReportService.generateCsvContent(csvData, originalRequest);

        // Then
        assertThat(result, is(not("")));
        assertThat(result, containsString("His Honour Judge Smith, Recorder Jones"));
    }

    @Test
    void shouldGenerateCsvContentWithOnlyHeadersWhenEmptyData() {
        // Given
        final List<HearingCsvData> csvData = new ArrayList<>();

        // When
        final String result = hearingCsvReportService.generateCsvContent(csvData, originalRequest);

        // Then
        assertThat(result, containsString("Date of hearing"));
        assertThat(result, containsString("Courtroom"));
        assertThat(result, containsString("Judiciary"));
        // Should only have header line
        long lineCount = result.split("\n").length;
        assertThat(lineCount, is(1L));
    }

    @Test
    void shouldEnrichNotesDataFromProgressionService() {
        // Given
        final UUID caseId1 = UUID.randomUUID();
        final UUID caseId2 = UUID.randomUUID();
        final String caseIds = caseId1.toString() + ", " + caseId2.toString();
        
        final List<HearingCsvData> csvData = new ArrayList<>();
        final HearingCsvData hearingData = new HearingCsvData(
                LocalDate.parse("2024-01-01"),
                "From 2024-01-01 to 2024-01-07",
                "Courtroom 1",
                judiciaryJson,
                "09:00",
                "Trial",
                "120",
                "Day 1 of 1",
                "URN123",
                caseIds,
                "John Doe",
                "Flag",
                "Offence",
                "Note",
                "English",
                "Video",
                "CTL 2025",
                "Multi-day details",
                "Original Pinned",
                "Original Unpinned",
                "Markers",
                "Restriction",
                CsvRecordType.CASE
        );
        csvData.add(hearingData);

        // Mock judiciary resolution
        final Judiciary judiciary = new Judiciary.Builder()
                .withId(judiciaryId)
                .withTitleJudicialPrefix("His Honour Judge")
                .withSurname("Smith")
                .withTitleSuffix("")
                .build();
        when(referenceDataCache.getJudiciariesMapCache(judiciaryId))
                .thenReturn(Optional.of(judiciary));

        // Mock progression service responses
        final JsonObject caseNotes1 = Json.createObjectBuilder()
                .add("caseNotes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("note", "Important note for case 1").add("isPinned", true))
                        .add(Json.createObjectBuilder().add("note", "General note for case 1").add("isPinned", false))
                )
                .build();
        
        final JsonObject caseNotes2 = Json.createObjectBuilder()
                .add("caseNotes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("note", "Critical note for case 2").add("isPinned", true))
                        .add(Json.createObjectBuilder().add("note", "Additional note for case 2").add("isPinned", false))
                )
                .build();

        when(progressionNotesCache.getNotes(caseId1, CsvRecordType.CASE, originalRequest))
                .thenReturn(Optional.of(caseNotes1));
        when(progressionNotesCache.getNotes(caseId2, CsvRecordType.CASE, originalRequest))
                .thenReturn(Optional.of(caseNotes2));

        // When
        final String result = hearingCsvReportService.generateCsvContent(csvData, originalRequest);

        // Then
        assertThat(result, is(not("")));
        assertThat(result, containsString("Date of hearing"));
        assertThat(result, containsString("His Honour Judge Smith"));
        // Check that only pinned notes are in the pinned notes field
        assertThat(result, containsString("Important note for case 1; Critical note for case 2"));
        // Check that unpinned notes are in the unpinned notes field
        assertThat(result, containsString("General note for case 1; Additional note for case 2"));
        // Original notes should not be present
        assertThat(result, not(containsString("Original Pinned")));
        assertThat(result, not(containsString("Original Unpinned")));
    }

    @Test
    void shouldEnrichApplicationNotesDataFromProgressionService() {
        // Given
        final UUID applicationId1 = UUID.randomUUID();
        final UUID applicationId2 = UUID.randomUUID();
        final String caseIds = applicationId1.toString() + ", " + applicationId2.toString();
        
        final List<HearingCsvData> csvData = new ArrayList<>();
        final HearingCsvData hearingData = new HearingCsvData(
                LocalDate.parse("2024-01-01"),
                "From 2024-01-01 to 2024-01-07",
                "Courtroom 1",
                judiciaryJson,
                "09:00",
                "Trial",
                "120",
                "Day 1 of 1",
                "URN123",
                caseIds,
                "John Doe",
                "Flag",
                "Offence",
                "Note",
                "English",
                "Video",
                "CTL 2025",
                "Multi-day details",
                "Original Pinned",
                "Original Unpinned",
                "Markers",
                "Restriction",
                CsvRecordType.APPLICATION
        );
        csvData.add(hearingData);

        // Mock judiciary resolution
        final Judiciary judiciary = new Judiciary.Builder()
                .withId(judiciaryId)
                .withTitleJudicialPrefix("His Honour Judge")
                .withSurname("Smith")
                .withTitleSuffix("")
                .build();
        when(referenceDataCache.getJudiciariesMapCache(judiciaryId))
                .thenReturn(Optional.of(judiciary));

        // Mock progression service responses for applications
        final JsonObject applicationNotes1 = Json.createObjectBuilder()
                .add("applicationNotes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("note", "Important application note 1").add("isPinned", true))
                        .add(Json.createObjectBuilder().add("note", "General application note 1").add("isPinned", false))
                )
                .build();
        
        final JsonObject applicationNotes2 = Json.createObjectBuilder()
                .add("applicationNotes", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder().add("note", "Critical application note 2").add("isPinned", true))
                        .add(Json.createObjectBuilder().add("note", "Additional application note 2").add("isPinned", false))
                )
                .build();

        when(progressionNotesCache.getNotes(applicationId1, CsvRecordType.APPLICATION, originalRequest))
                .thenReturn(Optional.of(applicationNotes1));
        when(progressionNotesCache.getNotes(applicationId2, CsvRecordType.APPLICATION, originalRequest))
                .thenReturn(Optional.of(applicationNotes2));

        // When
        final String result = hearingCsvReportService.generateCsvContent(csvData, originalRequest);

        // Then
        assertThat(result, is(not("")));
        assertThat(result, containsString("Date of hearing"));
        assertThat(result, containsString("His Honour Judge Smith"));
        // Check that only pinned application notes are in the pinned notes field
        assertThat(result, containsString("Important application note 1; Critical application note 2"));
        // Check that unpinned application notes are in the unpinned notes field
        assertThat(result, containsString("General application note 1; Additional application note 2"));
        // Original notes should not be present
        assertThat(result, not(containsString("Original Pinned")));
        assertThat(result, not(containsString("Original Unpinned")));
    }

    @Test
    void shouldHandleEmptyCaseIdsGracefully() {
        // Given
        final List<HearingCsvData> csvData = new ArrayList<>();
        final HearingCsvData hearingData = new HearingCsvData(
                LocalDate.parse("2024-01-01"),
                "From 2024-01-01 to 2024-01-07",
                "Courtroom 1",
                judiciaryJson,
                "09:00",
                "Trial",
                "120",
                "Day 1 of 1",
                "URN123",
                "John Doe",
                "Flag",
                "Offence",
                "Note",
                "English",
                "Video",
                "CTL 2025",
                "Multi-day details",
                "Original Pinned",
                "Original Unpinned",
                "Markers",
                "Restriction",
                "", // Empty case IDs
                CsvRecordType.CASE
        );
        csvData.add(hearingData);

        // Mock judiciary resolution
        final Judiciary judiciary = new Judiciary.Builder()
                .withId(judiciaryId)
                .withTitleJudicialPrefix("His Honour Judge")
                .withSurname("Smith")
                .withTitleSuffix("")
                .build();
        when(referenceDataCache.getJudiciariesMapCache(judiciaryId))
                .thenReturn(Optional.of(judiciary));

        // When
        final String result = hearingCsvReportService.generateCsvContent(csvData, originalRequest);

        // Then
        assertThat(result, is(not("")));
        assertThat(result, containsString("Date of hearing"));
        assertThat(result, containsString("His Honour Judge Smith"));
        // Original notes should remain unchanged
        assertThat(result, containsString("Original Pinned"));
        assertThat(result, containsString("Original Unpinned"));
    }
}
