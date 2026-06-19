package uk.gov.moj.cpp.listing.query.view.service.csv;

import static com.google.common.base.Strings.isNullOrEmpty;

import uk.gov.moj.cpp.listing.common.xhibit.ReferenceDataCache;
import uk.gov.moj.cpp.listing.domain.referencedata.Judiciary;
import uk.gov.moj.cpp.listing.query.view.dto.csv.HearingCsvData;
import uk.gov.moj.cpp.listing.persistence.entity.csv.HearingCsvRawData;
import uk.gov.moj.cpp.listing.persistence.constants.CsvColumnConstants;
import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;
import uk.gov.moj.cpp.listing.query.view.service.csv.HearingCsvJsonExtractionService;
import uk.gov.moj.cpp.listing.persistence.repository.csv.HearingCsvRawDataRepository;
import uk.gov.moj.cpp.listing.common.progression.ProgressionNotesCache;
import javax.json.JsonArray;
import javax.json.JsonObject;

import uk.gov.justice.services.messaging.JsonEnvelope;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for generating CSV reports for hearings.
 * Handles judiciary name resolution and CSV content generation.
 */
@ApplicationScoped
public class HearingCsvReportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingCsvReportService.class);

    @Inject
    private ReferenceDataCache referenceDataCache;

    @Inject
    private ProgressionNotesCache progressionNotesCache;

    @Inject
    private HearingCsvRawDataRepository hearingRawDataRepository;

    @Inject
    private HearingCsvJsonExtractionService hearingJsonExtractionService;

    /**
     * Fetches and processes hearing data for CSV report generation.
     * This method orchestrates the data retrieval and processing pipeline.
     *
     * @param courtCentreId the court centre ID
     * @param startDate     the start date
     * @param numberOfWeeks the number of weeks to include
     * @param pageSize      the page size limit
     * @return List of HearingCsvData containing processed CSV data
     */
    public List<HearingCsvData> findHearingsForCsvReport(final String courtCentreId, final LocalDate startDate, final Integer numberOfWeeks, final Long pageSize) {
        // Fetch raw data using dedicated repository
        final List<HearingCsvRawData> rawData = hearingRawDataRepository.findHearingsRawDataForCsvReport(courtCentreId, startDate, numberOfWeeks, pageSize);
        
        // Process raw data using Java extraction service
        return rawData.stream()
                      .map(hearingJsonExtractionService::extractCsvData).toList();
    }

    /**
     * Generates CSV content from hearing data, resolving judiciary names.
     * 
     * @param csvData List of hearing CSV data
     * @param originalRequest The original client request envelope to preserve metadata
     * @return Generated CSV content as string
     */
    public String generateCsvContent(final List<HearingCsvData> csvData, final JsonEnvelope originalRequest) {
        // Enrich notes data for all hearings
        enrichNotesData(csvData, originalRequest);
        
        try (StringWriter writer = new StringWriter();
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     CsvColumnConstants.DATE_OF_HEARING,
                     CsvColumnConstants.FIXED_WEEK_COMMENCING,
                     CsvColumnConstants.COURTROOM,
                     CsvColumnConstants.JUDICIARY,
                     CsvColumnConstants.TIME,
                     CsvColumnConstants.HEARING_TYPE,
                     CsvColumnConstants.DURATION,
                     CsvColumnConstants.DAY,
                     CsvColumnConstants.URN_S,
                     CsvColumnConstants.DEFENDANT_NAMES,
                     CsvColumnConstants.DEFT_FLAG,
                     CsvColumnConstants.OFFENCES,
                     CsvColumnConstants.PUBLIC_LIST_NOTE,
                     CsvColumnConstants.LANGUAGE,
                     CsvColumnConstants.VIDEO_HEARING,
                     CsvColumnConstants.CUSTODY_STATUS,
                     CsvColumnConstants.MULTI_DAY_HEARING_DETAILS,
                     CsvColumnConstants.PINNED_NOTES,
                     CsvColumnConstants.UNPINNED_NOTES,
                     CsvColumnConstants.MARKERS,
                     CsvColumnConstants.REPORTING_RESTRICTION
             ))) {

            for (HearingCsvData row : csvData) {
                // Process judiciary data to resolve names
                final String judiciaryJson = row.getJudiciary();
                final List<UUID> judicialIds = extractJudicialIds(judiciaryJson);
                final String judiciaryNames = resolveJudiciaryNames(judicialIds);
                
                // Create a copy of the row with resolved judiciary names
                final HearingCsvData processedRow = createProcessedRow(row, judiciaryNames);
                
                csvPrinter.printRecord(processedRow.toObjectArray());
            }

            csvPrinter.flush();
            return writer.toString();

        } catch (IOException e) {
            LOGGER.error("Error generating CSV content", e);
            throw new RuntimeException("Failed to generate CSV content", e);
        }
    }

    /**
     * Parses judiciary string and extracts judicial IDs
     * @param judiciaryIdsCommaSeparated The judiciary string - either JSON array format or comma-separated judicial IDs
     * @return List of judicial IDs
     */
    private List<UUID> extractJudicialIds(final String judiciaryIdsCommaSeparated) {
        final List<UUID> judicialIds = new ArrayList<>();

        if (isNullOrEmpty(judiciaryIdsCommaSeparated)) {
            return judicialIds;
        }

        // Check if it's a comma-separated list of judicial IDs (new format from extraction service)
        // Handle comma-separated judicial IDs
        final String[] idStrings = judiciaryIdsCommaSeparated.split(",");
        for (final String idStr : idStrings) {
            final String trimmedId = idStr.trim();
            if (!isNullOrEmpty(trimmedId)) {
                try {
                    judicialIds.add(UUID.fromString(trimmedId));
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid judicial ID format: {}", trimmedId, e);
                }
            }
        }
        return judicialIds;
    }

    /**
     * Resolves judiciary names using reference data cache
     * @param judicialIds List of judicial IDs
     * @return Comma-separated string of judiciary names
     */
    private String resolveJudiciaryNames(final List<UUID> judicialIds) {
        if (judicialIds.isEmpty()) {
            return "";
        }
        
        final List<String> judiciaryNames = new ArrayList<>();
        
        for (final UUID judicialId : judicialIds) {
            try {
                final Optional<Judiciary> judiciary = referenceDataCache.getJudiciariesMapCache(judicialId);
                if (judiciary.isPresent()) {
                    final Judiciary judge = judiciary.get();
                    final String name = buildJudiciaryName(judge);
                    if (!isNullOrEmpty(name)) {
                        judiciaryNames.add(name);
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to resolve judiciary name for ID: {}", judicialId, e);
            }
        }
        
        return String.join(", ", judiciaryNames);
    }

    /**
     * Builds judiciary name from Judiciary object
     * @param judiciary The Judiciary object
     * @return Formatted judiciary name
     */
    private String buildJudiciaryName(final Judiciary judiciary) {
        final StringBuilder name = new StringBuilder();
        
        // Use titleJudicialPrefix if available, otherwise use titlePrefix
        final String prefix = !isNullOrEmpty(judiciary.getTitleJudicialPrefix()) 
            ? judiciary.getTitleJudicialPrefix() 
            : judiciary.getTitlePrefix();
            
        if (!isNullOrEmpty(prefix)) {
            name.append(prefix);
        }
        
        if (!isNullOrEmpty(judiciary.getSurname())) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(judiciary.getSurname());
        }
        
        if (!isNullOrEmpty(judiciary.getTitleSuffix())) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(judiciary.getTitleSuffix());
        }
        
        return name.toString();
    }

    /**
     * Creates a processed row with resolved judiciary names
     * @param originalRow The original hearing CSV data row
     * @param judiciaryNames The resolved judiciary names
     * @return New HearingCsvData with resolved judiciary names
     */
    private HearingCsvData createProcessedRow(final HearingCsvData originalRow, final String judiciaryNames) {
        final HearingCsvData processedRow = new HearingCsvData();
        processedRow.setHearingDate(originalRow.getHearingDate());
        processedRow.setWeekCommencing(originalRow.getWeekCommencing());
        processedRow.setCourtroom(originalRow.getCourtroom());
        processedRow.setJudiciary(judiciaryNames); // Use resolved names instead of JSON
        processedRow.setStartTime(originalRow.getStartTime());
        processedRow.setHearingType(originalRow.getHearingType());
        processedRow.setDuration(originalRow.getDuration());
        processedRow.setDay(originalRow.getDay());
        processedRow.setCaseUrns(originalRow.getCaseUrns());
        processedRow.setDefendantNames(originalRow.getDefendantNames());
        processedRow.setDefendantFlag(originalRow.getDefendantFlag());
        processedRow.setOffences(originalRow.getOffences());
        processedRow.setPublicListNote(originalRow.getPublicListNote());
        processedRow.setLanguage(originalRow.getLanguage());
        processedRow.setVideoHearing(Boolean.TRUE.equals(BooleanUtils.toBoolean(originalRow.getVideoHearing()))?"Yes":"No");
        processedRow.setCustodyStatus(originalRow.getCustodyStatus());
        processedRow.setMultiDayHearingDetails(originalRow.getMultiDayHearingDetails());
        processedRow.setPinnedNotes(originalRow.getPinnedNotes()); // Already enriched by enrichNotesData
        processedRow.setUnpinnedNotes(originalRow.getUnpinnedNotes()); // Already enriched by enrichNotesData
        processedRow.setMarkers(originalRow.getMarkers());
        processedRow.setReportingRestriction(originalRow.getReportingRestriction());

        return processedRow;
    }

    /**
     * Enriches hearing data with case or application notes from the progression service.
     * 
     * @param csvData List of hearing CSV data to enrich
     */
    private void enrichNotesData(final List<HearingCsvData> csvData, final JsonEnvelope originalRequest) {
        for (final HearingCsvData data : csvData) {
            final String caseIds = data.getCaseIds();
            final CsvRecordType recordType = data.getRecordType();

            LOGGER.info("====caseIds {}",caseIds);
            if (!isNullOrEmpty(caseIds)) {
                // Parse case IDs (comma-separated)
                        final String[] caseIdArray = caseIds.split(",");
                        final StringBuilder pinnedNotes = new StringBuilder();
                        final StringBuilder unpinnedNotes = new StringBuilder();
                
                for (final String caseIdStr : caseIdArray) {
                    final String trimmedCaseId = caseIdStr.trim();
                    if (!trimmedCaseId.isEmpty()) {
                        try {
                            LOGGER.info("====trimmedCaseId {}",trimmedCaseId);
                            final UUID caseId = UUID.fromString(trimmedCaseId);
                            Optional<JsonObject> notes = Optional.empty();
                            
                            // Get notes from cache based on record type
                            notes = progressionNotesCache.getNotes(caseId, recordType, originalRequest);

                            if (notes.isPresent()) {
                                final JsonObject notesData = notes.get();
                                
                                // Extract notes from the correct structure
                                // For cases: notesData contains "caseNotes" array
                                // For applications: notesData contains "applicationNotes" array
                                final String notesArrayKey = CsvRecordType.APPLICATION.equals(recordType)
                                    ? "applicationNotes" : "caseNotes";
                                
                                if (notesData.containsKey(notesArrayKey) && !notesData.isNull(notesArrayKey)) {
                                    final JsonArray notesArray = notesData.getJsonArray(notesArrayKey);
                                    
                                    for (int i = 0; i < notesArray.size(); i++) {
                                        final JsonObject note = notesArray.getJsonObject(i);
                                        LOGGER.info("====note {}",note);
                                        if (note.containsKey("note") && !note.isNull("note")) {
                                            final String noteText = note.getString("note");
                                            LOGGER.info("====noteText {}",noteText);
                                            if (!isNullOrEmpty(noteText)) {
                                                // Check if the note is pinned based on isPinned property
                                                boolean isPinned = false;
                                                if (note.containsKey("isPinned") && !note.isNull("isPinned")) {
                                                    isPinned = note.getBoolean("isPinned");
                                                }
                                                
                                                if (isPinned) {
                                                    if (pinnedNotes.length() > 0) {
                                                        pinnedNotes.append("; ");
                                                    }
                                                    pinnedNotes.append(noteText);
                                                } else {
                                                    if (unpinnedNotes.length() > 0) {
                                                        unpinnedNotes.append("; ");
                                                    }
                                                    unpinnedNotes.append(noteText);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (final IllegalArgumentException e) {
                            LOGGER.warn("Invalid case ID format: {}", trimmedCaseId, e);
                        } catch (final Exception e) {
                            LOGGER.warn("Failed to fetch notes for case ID: {} (record type: {})", trimmedCaseId, recordType, e);
                        }
                    }
                }

                LOGGER.info("====pinnedNotes {}",pinnedNotes);
                LOGGER.info("====unpinnedNotes {}",unpinnedNotes);
                // Update the data with enriched notes
                if (pinnedNotes.length() > 0) {
                    data.setPinnedNotes(pinnedNotes.toString());
                }
                if (unpinnedNotes.length() > 0) {
                    data.setUnpinnedNotes(unpinnedNotes.toString());
                }
            }
        }
    }
}
