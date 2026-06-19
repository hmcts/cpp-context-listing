package uk.gov.moj.cpp.listing.query.view.service.csv;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.moj.cpp.listing.query.view.dto.csv.HearingCsvData;
import uk.gov.moj.cpp.listing.persistence.entity.csv.HearingCsvRawData;
import uk.gov.moj.cpp.listing.persistence.enums.CsvRecordType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Service responsible for extracting CSV data from hearing JSON properties.
 * This service replaces the complex SQL JsonB extraction with clean, testable Java code.
 */
@ApplicationScoped
public class HearingCsvJsonExtractionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingCsvJsonExtractionService.class);
    public static final String OFFENCES = "offences";
    public static final String APPLICANT = "applicant";
    public static final String COURT_APPLICATIONS = "courtApplications";
    public static final String RESPONDENTS = "respondents";
    public static final String LISTED_CASES = "listedCases";
    public static final String DEFENDANTS = "defendants";

    /**
     * Extracts CSV data from raw hearing data by processing the JSON properties.
     *
     * @param rawData The raw hearing data from the database
     * @return Processed CSV data ready for export
     */
    public HearingCsvData extractCsvData(HearingCsvRawData rawData) {
        if (rawData == null) {
            LOGGER.warn("Raw data is null, returning empty CSV data");
            return new HearingCsvData();
        }

        JsonNode properties = rawData.getProperties();
        if (properties == null) {
            LOGGER.warn("Properties JSON is null for hearing ID: {}", rawData.getId());
            return createBasicCsvData(rawData);
        }

        try {
            HearingCsvData csvData = createBasicCsvData(rawData);
            
            // Extract data from JSON properties
            csvData.setJudiciary(extractJudiciary(properties));
            csvData.setHearingType(extractHearingType(properties));
            csvData.setCaseUrns(extractCaseUrns(properties));
            csvData.setCaseIds(extractCaseIds(properties));
            csvData.setDefendantNames(extractDefendantNames(properties));
            csvData.setDefendantFlag(extractDefendantFlag(properties));
            csvData.setOffences(extractOffences(properties));
            csvData.setPublicListNote(extractPublicListNote(properties));
            csvData.setLanguage(extractLanguage(properties));
            csvData.setVideoHearing(extractVideoHearing(properties));
            csvData.setCustodyStatus(extractCustodyStatus(properties));
            csvData.setMultiDayHearingDetails(extractMultiDayHearingDetails(rawData));
            csvData.setMarkers(extractMarkers(properties));
            csvData.setReportingRestriction(extractReportingRestrictions(properties));
            csvData.setRecordType(determineRecordType(properties));
            
            return csvData;
        } catch (Exception e) {
            LOGGER.error("Error extracting CSV data for hearing ID: {}", rawData.getId(), e);
            return createBasicCsvData(rawData);
        }
    }

    /**
     * Creates basic CSV data from raw data without JSON processing.
     */
    private HearingCsvData createBasicCsvData(HearingCsvRawData rawData) {
        HearingCsvData csvData = new HearingCsvData();
        csvData.setHearingDate(rawData.getHearingDate());
        csvData.setWeekCommencing(extractWeekCommencing(rawData));
        csvData.setCourtroom(rawData.getCourtroom());
        csvData.setStartTime(rawData.getStartTime());
        csvData.setDuration(extractDuration(rawData));
        csvData.setDay(rawData.getDay());
        csvData.setMultiDayHearingDetails(extractMultiDayHearingDetails(rawData));
        csvData.setPinnedNotes("");
        csvData.setUnpinnedNotes("");
        
        // Set all JSON-extracted fields to empty strings
        csvData.setJudiciary("");
        csvData.setHearingType("");
        csvData.setCaseUrns("");
        csvData.setCaseIds("");
        csvData.setDefendantNames("");
        csvData.setDefendantFlag("");
        csvData.setOffences("");
        csvData.setPublicListNote("");
        csvData.setLanguage("");
        csvData.setVideoHearing("false");
        csvData.setCustodyStatus("");
        csvData.setMarkers("");
        csvData.setReportingRestriction("");
        csvData.setRecordType(CsvRecordType.UNKNOWN);
        
        return csvData;
    }

    /**
     * Extracts judiciary information from JSON properties.
     * Judiciary is stored as an array of objects with judicialId and judicialRoleType.
     * We extract the judicialIds as a comma-separated string for later resolution.
     */
    private String extractJudiciary(JsonNode properties) {
        if (properties == null) return "";
        
        JsonNode judiciary = properties.get("judiciary");
        if (judiciary == null || judiciary.isNull()) {
            return "";
        }
        
        // Handle case where judiciary is a simple string (legacy format)
        if (judiciary.isTextual()) {
            return judiciary.asText();
        }
        
        // Handle case where judiciary is an array of objects
        if (judiciary.isArray()) {
            Set<String> judicialIds = new HashSet<>();
            for (JsonNode judge : judiciary) {
                extractJudiciaryIdsSeparatedByComma(judge, judicialIds);
            }
            
            if (judicialIds.isEmpty()) {
                return "";
            }
            
            // Return comma-separated judicial IDs for later resolution
            return String.join(",", judicialIds);
        }
        
        return "";
    }

    private void extractJudiciaryIdsSeparatedByComma(final JsonNode judge, final Set<String> judicialIds) {
        if (judge != null && !judge.isNull()) {
            String judicialId = getTextValue(judge, "judicialId");
            if (judicialId != null && !judicialId.trim().isEmpty()) {
                judicialIds.add(judicialId);
            }
        }
    }

    /**
     * Extracts hearing type description from JSON properties.
     */
    private String extractHearingType(JsonNode properties) {
        if (properties == null) return "";
        JsonNode type = properties.get("type");
        if (type == null || type.isNull()) {
            return "";
        }
        JsonNode description = type.get("description");
        return (description != null && !description.isNull()) ? description.asText() : "";
    }

    /**
     * Extracts case URNs from both listedCases and courtApplications.
     */
    private String extractCaseUrns(JsonNode properties) {
        if (properties == null) return "";
        
        Set<String> caseUrns = new HashSet<>();
        
        // Extract from listedCases
        caseUrns.addAll(extractCaseUrnsFromListedCases(properties));
        
        // Extract from courtApplications
        caseUrns.addAll(extractCaseUrnsFromCourtApplications(properties));
        
        return String.join(", ", caseUrns);
    }

    private Set<String> extractCaseUrnsFromListedCases(JsonNode properties) {
        Set<String> caseUrns = new HashSet<>();
        JsonNode listedCases = properties.get(LISTED_CASES);
        
        if (listedCases != null && listedCases.isArray()) {
            for (JsonNode listedCase : listedCases) {
                JsonNode caseIdentifier = listedCase.get("caseIdentifier");
                if (caseIdentifier != null) {
                    JsonNode caseReference = caseIdentifier.get("caseReference");
                    if (caseReference != null && !caseReference.isNull()) {
                        caseUrns.add(caseReference.asText());
                    }
                }
            }
        }
        return caseUrns;
    }

    private Set<String> extractCaseUrnsFromCourtApplications(JsonNode properties) {
        Set<String> caseUrns = new HashSet<>();
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);
        
        if (courtApplications != null && courtApplications.isArray()) {
            for (JsonNode application : courtApplications) {
                JsonNode applicationReference = application.get("applicationReference");
                if (applicationReference != null && !applicationReference.isNull()) {
                    caseUrns.add(applicationReference.asText());
                }
            }
        }
        return caseUrns;
    }

    /**
     * Extracts case IDs from both listedCases and courtApplications.
     */
    private String extractCaseIds(JsonNode properties) {
        if (properties == null) return "";
        
        Set<String> caseIds = new HashSet<>();
        
        // Extract from listedCases
        caseIds.addAll(extractCaseIdsFromListedCases(properties));
        
        // Extract from courtApplications
        caseIds.addAll(extractCaseIdsFromCourtApplications(properties));
        
        return String.join(", ", caseIds);
    }

    private Set<String> extractCaseIdsFromListedCases(JsonNode properties) {
        Set<String> caseIds = new HashSet<>();
        JsonNode listedCases = properties.get(LISTED_CASES);
        
        if (listedCases != null && listedCases.isArray()) {
            for (JsonNode listedCase : listedCases) {
                JsonNode id = listedCase.get("id");
                if (id != null && !id.isNull()) {
                    caseIds.add(id.asText());
                }
            }
        }
        return caseIds;
    }

    private Set<String> extractCaseIdsFromCourtApplications(JsonNode properties) {
        Set<String> caseIds = new HashSet<>();
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);
        
        if (courtApplications != null && courtApplications.isArray()) {
            for (JsonNode application : courtApplications) {
                JsonNode id = application.get("id");
                if (id != null && !id.isNull()) {
                    caseIds.add(id.asText());
                }
            }
        }
        return caseIds;
    }

    /**
     * Extracts defendant names from both listedCases and courtApplications.
     */
    private String extractDefendantNames(JsonNode properties) {
        if (properties == null) return "";
        
        Set<String> names = new HashSet<>();
        
        // Extract from listedCases defendants
        names.addAll(extractDefendantNamesFromListedCases(properties));
        
        // Extract from courtApplications
        names.addAll(extractDefendantNamesFromCourtApplications(properties));
        
        return String.join(", ", names);
    }

    private Set<String> extractDefendantNamesFromListedCases(JsonNode properties) {
        Set<String> names = new HashSet<>();
        JsonNode listedCases = properties.get(LISTED_CASES);
        
        if (listedCases != null && listedCases.isArray()) {
            for (JsonNode listedCase : listedCases) {
                JsonNode defendants = listedCase.get(DEFENDANTS);
                extractDefendantNames(defendants, names);
            }
        }
        return names;
    }

    private void extractDefendantNames(final JsonNode defendants, final Set<String> names) {
        if (defendants != null && defendants.isArray()) {
            for (JsonNode defendant : defendants) {
                String name = buildDefendantName(defendant);
                if (name != null && !name.trim().isEmpty()) {
                    names.add(name);
                }
            }
        }
    }

    private Set<String> extractDefendantNamesFromCourtApplications(JsonNode properties) {
        Set<String> names = new HashSet<>();
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);
        
        if (courtApplications != null && courtApplications.isArray()) {
            for (JsonNode application : courtApplications) {
                // Extract from applicant
                extractDefendantFromApplicant(application, names);

                // Extract from respondents
                extractDefendantFromRespondent(application, names);
            }
        }
        return names;
    }

    private void extractDefendantFromRespondent(final JsonNode application, final Set<String> names) {
        JsonNode respondents = application.get(RESPONDENTS);
        if (respondents != null && respondents.isArray()) {
            for (JsonNode respondent : respondents) {
                if (isPersonDefendant(respondent)) {
                    String name = buildDefendantName(respondent);
                    if (name != null && !name.trim().isEmpty()) {
                        names.add(name);
                    }
                }
            }
        }
    }

    private void extractDefendantFromApplicant(final JsonNode application, final Set<String> names) {
        JsonNode applicant = application.get(APPLICANT);
        if (isPersonDefendant(applicant)) {
            String name = buildDefendantName(applicant);
            if (name != null && !name.trim().isEmpty()) {
                names.add(name);
            }
        }
    }

    private String buildDefendantName(JsonNode person) {
        if (person == null) return null;
        
        String firstName = getTextValue(person, "firstName");
        String lastName = getTextValue(person, "lastName");
        
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return null;
    }

    private boolean isPersonDefendant(JsonNode person) {
        if (person == null) return false;
        return "PERSON_DEFENDANT".equals(getTextValue(person, "courtApplicationPartyType"));
    }

    /**
     * Extracts defendant flag (Youth indicator) from both listedCases and courtApplications.
     */
    private String extractDefendantFlag(JsonNode properties) {
        if (properties == null) return "";
        
        // Check listedCases defendants
        if (hasYouthDefendantInListedCases(properties)) {
            return "Youth";
        }
        
        // Check courtApplications
        if (hasYouthDefendantInCourtApplications(properties)) {
            return "Youth";
        }
        
        return "";
    }

    private boolean hasYouthDefendantInListedCases(JsonNode properties) {
        JsonNode listedCases = properties.get(LISTED_CASES);
        if (listedCases != null && listedCases.isArray()) {
            for (JsonNode listedCase : listedCases) {
                JsonNode defendants = listedCase.get(DEFENDANTS);
                if (hasYouthDefendantInDefendantArray(defendants)) return true;
            }
        }
        return false;
    }

    private boolean hasYouthDefendantInDefendantArray(final JsonNode defendants) {
        if (defendants != null && defendants.isArray()) {
            for (JsonNode defendant : defendants) {
                if (isYouthDefendant(defendant)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasYouthDefendantInCourtApplications(JsonNode properties) {
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);
        return (courtApplications != null && courtApplications.isArray() && hasAnyYouthApplicantOrRespondent(courtApplications));
    }

    private boolean hasAnyYouthApplicantOrRespondent(final JsonNode courtApplications) {
        for (JsonNode application : courtApplications) {
            // Check applicant
            JsonNode applicant = application.get(APPLICANT);
            if (isYouthDefendant(applicant)) {
                return true;
            }

            // Check respondents
            JsonNode respondents = application.get(RESPONDENTS);
            if (hasYouthDefendantInDefendantArray(respondents)) return true;
        }
        return false;
    }

    private boolean isYouthDefendant(JsonNode person) {
        if (person == null) return false;
        return "true".equals(getTextValue(person, "isYouth"));
    }

    /**
     * Extracts offences from both listedCases and courtApplications.
     */
    private String extractOffences(JsonNode properties) {
        if (properties == null) return "";
        
        Set<String> offences = new HashSet<>();
        
        // Extract from listedCases defendants offences
        offences.addAll(extractOffencesFromListedCases(properties));
        
        // Extract from courtApplications offences
        offences.addAll(extractOffencesFromCourtApplications(properties));
        
        return String.join(", ", offences);
    }

    private Set<String> extractOffencesFromListedCases(JsonNode properties) {
        Set<String> offences = new HashSet<>();
        JsonNode listedCases = properties.get(LISTED_CASES);
        
        if (listedCases != null && listedCases.isArray()) {
            for (JsonNode listedCase : listedCases) {
                JsonNode defendants = listedCase.get(DEFENDANTS);
                if (defendants != null && defendants.isArray()) {
                    for (JsonNode defendant : defendants) {
                        JsonNode defendantOffences = defendant.get(OFFENCES);
                        extractOffenceTitle(defendantOffences, offences);
                    }
                }
            }
        }
        return offences;
    }

    private void extractOffenceTitle(final JsonNode defendantOffences, final Set<String> offences) {
        if (defendantOffences != null && defendantOffences.isArray()) {
            for (JsonNode offence : defendantOffences) {
                String offenceTitle = extractOffenceTitle(offence);
                if (offenceTitle != null && !offenceTitle.trim().isEmpty()) {
                    offences.add(offenceTitle);
                }
            }
        }
    }

    private Set<String> extractOffencesFromCourtApplications(JsonNode properties) {
        Set<String> offences = new HashSet<>();
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);
        
        if (courtApplications != null && courtApplications.isArray()) {
            for (JsonNode application : courtApplications) {
                JsonNode applicationOffences = application.get(OFFENCES);
                extractOffenceTitle(applicationOffences, offences);
            }
        }
        return offences;
    }

    private String extractOffenceTitle(JsonNode offence) {
        if (offence == null) return null;
        
        JsonNode statementOfOffence = offence.get("statementOfOffence");
        if (statementOfOffence == null) return null;
        
        JsonNode title = statementOfOffence.get("title");
        return (title != null && !title.isNull()) ? title.asText() : null;
    }

    /**
     * Extracts public list note from JSON properties.
     */
    private String extractPublicListNote(JsonNode properties) {
        return getTextValueSafe(properties, "publicListNote");
    }

    /**
     * Extracts hearing language from JSON properties.
     */
    private String extractLanguage(JsonNode properties) {
        return getTextValueSafe(properties, "hearingLanguage");
    }

    /**
     * Extracts video hearing flag from JSON properties.
     */
    private String extractVideoHearing(JsonNode properties) {
        String hasVideoLink = getTextValueSafe(properties, "hasVideoLink");
        return isNotBlank(hasVideoLink) ? hasVideoLink : "false";
    }

    /**
     * Extracts custody status from both listedCases and courtApplications.
     */
    private String extractCustodyStatus(JsonNode properties) {
        if (properties == null) return "";
        
        Set<String> custodyStatuses = new HashSet<>();
        
        // Extract from listedCases defendants
        custodyStatuses.addAll(extractCustodyStatusFromListedCases(properties));
        
        // Extract from courtApplications
        custodyStatuses.addAll(extractCustodyStatusFromCourtApplications(properties));
        
        return String.join(", ", custodyStatuses);
    }

    private Set<String> extractCustodyStatusFromListedCases(JsonNode properties) {
        Set<String> custodyStatuses = new HashSet<>();
        JsonNode listedCases = properties.get(LISTED_CASES);
        
        if (listedCases != null && listedCases.isArray()) {
            for (JsonNode listedCase : listedCases) {
                JsonNode defendants = listedCase.get(DEFENDANTS);
                extractCustodyStatusFromDefendants(defendants, custodyStatuses);
            }
        }
        return custodyStatuses;
    }

    private void extractCustodyStatusFromDefendants(final JsonNode defendants, final Set<String> custodyStatuses) {
        if (defendants != null && defendants.isArray()) {
            for (JsonNode defendant : defendants) {
                String custodyStatus = buildCustodyStatus(defendant);
                if (custodyStatus != null && !custodyStatus.trim().isEmpty()) {
                    custodyStatuses.add(custodyStatus);
                }
            }
        }
    }

    private Set<String> extractCustodyStatusFromCourtApplications(JsonNode properties) {
        Set<String> custodyStatuses = new HashSet<>();
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);
        
        if (courtApplications != null && courtApplications.isArray()) {
            for (JsonNode application : courtApplications) {
                // Extract from applicant
                JsonNode applicant = application.get(APPLICANT);
                if (applicant != null) {
                    String custodyStatus = buildCustodyStatus(applicant);
                    if (custodyStatus != null && !custodyStatus.trim().isEmpty()) {
                        custodyStatuses.add(custodyStatus);
                    }
                }
                
                // Extract from respondents
                JsonNode respondents = application.get(RESPONDENTS);
                extractCustodyStatusFromDefendants(respondents, custodyStatuses);
            }
        }
        return custodyStatuses;
    }

    private String buildCustodyStatus(JsonNode person) {
        if (person == null) return null;
        
        String bailStatus = getTextValue(person, "bailStatus", "description");
        String custodyTimeLimit = getTextValue(person, "custodyTimeLimit");
        
        if (bailStatus == null && custodyTimeLimit == null) {
            return null;
        }
        
        StringBuilder status = new StringBuilder();
        if (bailStatus != null) {
            status.append(bailStatus);
        }
        if (bailStatus != null && custodyTimeLimit != null) {
            status.append(" - ");
        }
        if (custodyTimeLimit != null) {
            status.append(custodyTimeLimit);
        }
        
        return status.toString();
    }

    /**
     * Extracts week commencing using CASE logic: return 'Fixed' if start date is null, otherwise format date range.
     * This replicates the SQL CASE logic for week commencing.
     */
    private String extractWeekCommencing(HearingCsvRawData rawData) {
        if (rawData == null) {
            return "";
        }
        
        // If week_commencing_start_date is NULL, return 'Fixed'
        if (rawData.getWeekCommencingStartDate() == null) {
            return "Fixed";
        }
        
        // Otherwise, format as 'From: [start_date] To: [end_date]'
        LocalDate startDate = rawData.getWeekCommencingStartDate();
        LocalDate endDate = rawData.getWeekCommencingEndDate();
        
        if (endDate == null) {
            return "From: " + startDate;
        }
        
        return "From: " + startDate + " To: " + endDate;
    }

    /**
     * Extracts duration using COALESCE logic: use durationMinutes if available, otherwise use estimatedMinutes.
     * This replicates the SQL COALESCE(hd.duration_minutes::text, h.properties::json->>'estimatedMinutes') logic.
     */
    private String extractDuration(HearingCsvRawData rawData) {
        if (rawData == null) {
            return "";
        }
        
        // Use durationMinutes if available (equivalent to hd.duration_minutes::text)
        if (rawData.getDurationMinutes() != null) {
            return rawData.getDurationMinutes().toString();
        }
        
        // Fall back to estimatedMinutes from JSON (equivalent to h.properties::json->>'estimatedMinutes')
        String estimatedMinutes = getTextValueSafe(rawData.getProperties(), "estimatedMinutes");
        if (isNotBlank(estimatedMinutes)) {
            return estimatedMinutes;
        }
        
        // Return empty string if neither is available
        return "";
    }

    /**
     * Extracts multi-day hearing details from raw data.
     */
    private String extractMultiDayHearingDetails(HearingCsvRawData rawData) {
        LocalDate startDate = rawData.getStartDate();
        LocalDate endDate = rawData.getEndDate();
        
        if (startDate == null) {
            return "";
        }
        
        return "From " + startDate + " to " + endDate;
    }

    /**
     * Extracts markers from both listedCases and courtApplications.
     */
    private String extractMarkers(JsonNode properties) {
        if (properties == null) return "";
        
        Set<String> markers = new HashSet<>();
        
        // Extract from listedCases markers
        markers.addAll(extractMarkersFromListedCases(properties));
        
        // Extract from courtApplications markers
        markers.addAll(extractMarkersFromCourtApplications(properties));
        
        return String.join(", ", markers);
    }

    private Set<String> extractMarkersFromListedCases(JsonNode properties) {
        Set<String> markers = new HashSet<>();
        JsonNode listedCases = properties.get(LISTED_CASES);
        
        if (listedCases != null && listedCases.isArray()) {
            for (JsonNode listedCase : listedCases) {
                JsonNode caseMarkers = listedCase.get("markers");
                arkersToText(caseMarkers, markers);
            }
        }
        return markers;
    }

    private void arkersToText(final JsonNode caseMarkers, final Set<String> markers) {
        if (caseMarkers != null && caseMarkers.isArray()) {
            for (JsonNode marker : caseMarkers) {
                String markerText = buildMarkerText(marker);
                if (markerText != null && !markerText.trim().isEmpty()) {
                    markers.add(markerText);
                }
            }
        }
    }

    private Set<String> extractMarkersFromCourtApplications(JsonNode properties) {
        Set<String> markers = new HashSet<>();
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);
        
        if (courtApplications != null && courtApplications.isArray()) {
            for (JsonNode application : courtApplications) {
                JsonNode applicationMarkers = application.get("markers");
                arkersToText(applicationMarkers, markers);
            }
        }
        return markers;
    }

    private String buildMarkerText(JsonNode marker) {
        if (marker == null) return null;
        
        String markerTypeCode = getTextValue(marker, "markerTypeCode");
        String markerTypeDescription = getTextValue(marker, "markerTypeDescription");
        
        if (markerTypeCode != null && markerTypeDescription != null) {
            return markerTypeCode + " - " + markerTypeDescription;
        }
        return null;
    }

    /**
     * Extracts reporting restrictions from both listedCases and courtApplications.
     */
    private String extractReportingRestrictions(JsonNode properties) {
        if (properties == null) return "";
        
        Set<String> restrictions = new HashSet<>();
        
        // Extract from listedCases defendants offences reporting restrictions
        restrictions.addAll(extractReportingRestrictionsFromListedCases(properties));
        
        // Extract from courtApplications offences reporting restrictions
        restrictions.addAll(extractReportingRestrictionsFromCourtApplications(properties));
        
        return String.join(", ", restrictions);
    }

    private Set<String> extractReportingRestrictionsFromListedCases(JsonNode properties) {
        Set<String> restrictions = new HashSet<>();
        JsonNode listedCases = properties.get(LISTED_CASES);
        
        if (listedCases != null && listedCases.isArray()) {
            for (JsonNode listedCase : listedCases) {
                JsonNode defendants = listedCase.get(DEFENDANTS);
                defendantsToReportingRestriction(defendants, restrictions);
            }
        }
        return restrictions;
    }

    private void defendantsToReportingRestriction(final JsonNode defendants, final Set<String> restrictions) {
        if (defendants != null && defendants.isArray()) {
            for (JsonNode defendant : defendants) {
                JsonNode defendantOffences = defendant.get(OFFENCES);
                if (defendantOffences != null && defendantOffences.isArray()) {
                    for (JsonNode offence : defendantOffences) {
                        JsonNode reportingRestrictions = offence.get("reportingRestrictions");
                        reportingRestrictionsAsText(reportingRestrictions, restrictions);
                    }
                }
            }
        }
    }

    private Set<String> extractReportingRestrictionsFromCourtApplications(JsonNode properties) {
        Set<String> restrictions = new HashSet<>();
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);

        defendantsToReportingRestriction(courtApplications, restrictions);
        return restrictions;
    }

    private void reportingRestrictionsAsText(final JsonNode reportingRestrictions, final Set<String> restrictions) {
        if (reportingRestrictions != null && reportingRestrictions.isArray()) {
            for (JsonNode restriction : reportingRestrictions) {
                String restrictionText = buildReportingRestrictionText(restriction);
                if (restrictionText != null && !restrictionText.trim().isEmpty()) {
                    restrictions.add(restrictionText);
                }
            }
        }
    }

    private String buildReportingRestrictionText(JsonNode restriction) {
        if (restriction == null) return null;
        
        String orderedDate = getTextValue(restriction, "orderedDate");
        String label = getTextValue(restriction, "label");
        
        if (orderedDate == null && label == null) {
            return null;
        }
        
        StringBuilder restrictionText = new StringBuilder();
        if (orderedDate != null) {
            restrictionText.append(orderedDate);
        }
        if (orderedDate != null && label != null) {
            restrictionText.append(" - ");
        }
        if (label != null) {
            restrictionText.append(label);
        }
        
        return restrictionText.toString();
    }

    /**
     * Determines the record type based on the presence of listedCases or courtApplications.
     */
    private CsvRecordType determineRecordType(JsonNode properties) {
        if (properties == null) return CsvRecordType.UNKNOWN;
        
        // Check if listedCases exist and are not empty
        JsonNode listedCases = properties.get(LISTED_CASES);
        if (listedCases != null && listedCases.isArray() && listedCases.size() > 0) {
            return CsvRecordType.CASE;
        }
        
        // Check if courtApplications exist and are not empty
        JsonNode courtApplications = properties.get(COURT_APPLICATIONS);
        if (courtApplications != null && courtApplications.isArray() && courtApplications.size() > 0) {
            return CsvRecordType.APPLICATION;
        }
        
        return CsvRecordType.UNKNOWN;
    }

    /**
     * Helper method to safely extract text values from JsonNode.
     */
    private String getTextValue(JsonNode node, String fieldName) {
        if (node == null) return null;
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    /**
     * Helper method to safely extract text values from JsonNode with null safety for properties.
     */
    private String getTextValueSafe(JsonNode properties, String fieldName) {
        if (properties == null) return "";
        String value = getTextValue(properties, fieldName);
        return value != null ? value : "";
    }

    /**
     * Helper method to safely extract nested text values from JsonNode.
     */
    private String getTextValue(JsonNode node, String parentField, String childField) {
        if (node == null) return null;
        JsonNode parent = node.get(parentField);
        if (parent == null) return null;
        return getTextValue(parent, childField);
    }
}
