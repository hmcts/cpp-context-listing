package uk.gov.moj.cpp.listing.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.nonNull;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;

/**
 * Utility class for loading JSON test data files and replacing placeholders with dynamic values
 */
public class PayloadGenerator {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadGenerator.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Loads a JSON file from test-data directory and replaces placeholders with dynamic values
     * 
     * @param scenario the scenario folder name (e.g., "list-court-hearing")
     * @param testCase the test case file name (e.g., "adhoc_hearing_creation")
     * @return JsonNode with placeholders replaced
     */
    public static JsonNode loadPayloadWithDynamicValues(String scenario, String testCase) {
        try {
            String resourcePath = String.format("/test-data/%s/%s.json", scenario, testCase);
            InputStream inputStream = PayloadGenerator.class.getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                throw new RuntimeException("Test data file not found: " + resourcePath);
            }
            
            JsonNode originalNode = objectMapper.readTree(inputStream);
            Map<String, String> placeholderValues = generateDynamicValues();
            
            JsonNode processedNode = replacePlaceholders(originalNode, placeholderValues);
            
            LOGGER.info("Loaded and processed test data from: {}", resourcePath);
            return processedNode;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test data file", e);
        }
    }
    
    /**
     * Generates dynamic values for common placeholders
     */
    private static Map<String, String> generateDynamicValues() {
        Map<String, String> values = new HashMap<>();
        
        // Generate UUIDs for common placeholders
        values.put("%%HEARING_ID%%", UUID.randomUUID().toString());
        values.put("%%COURT_CENTRE_ID%%", UUID.randomUUID().toString());
        values.put("%%COURT_ROOM_ID%%", UUID.randomUUID().toString());
        values.put("%%COURTSCHEDULE_ID%%", UUID.randomUUID().toString());
        
        // Generate jurisdiction types
        values.put("%%JURISDICTION_TYPE%%", "MAGISTRATES"); // Can be parameterized later
        
        // Generate dates and times
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDateTime = now.plusDays(30);
        LocalDate futureDate = futureDateTime.toLocalDate();
        ZonedDateTime zonedDateTime = now
                .plusDays(30)
                .withHour(10)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .atZone(ZoneId.of("Europe/London"));
        String utcFormatted = zonedDateTime
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        
//        values.put("%%BOOKED_SLOT_START_TIME%%", futureDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        values.put("%%BOOKED_SLOT_START_TIME%%", utcFormatted);
        values.put("%%HEARING_START_DATE%%", futureDate.toString());
        values.put("%%HEARING_END_DATE%%", futureDate.toString());
//        values.put("%%LISTED_START_DATE_TIME%%", futureDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z");
        values.put("%%LISTED_START_DATE_TIME%%", utcFormatted);

        // Generate random strings for various fields
        values.put("%%CASE_URN%%", "URN" + STRING.next());
        values.put("%%PROSECUTION_AUTHORITY_CODE%%", "PA" + STRING.next());
        values.put("%%ORGANISATION_CODE%%", "ORG" + STRING.next());
        values.put("%%OUCODE%%", "B01LY00");
        return values;
    }
    
    /**
     * Allows custom placeholder values to be provided
     */
    public static JsonNode loadPayloadWithCustomValues(String scenario, String testCase, Map<String, String> customValues) {
        try {
            String resourcePath = String.format("/test-data/%s/%s.json", scenario, testCase);
            InputStream inputStream = PayloadGenerator.class.getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                throw new RuntimeException("Test data file not found: " + resourcePath);
            }
            
            JsonNode originalNode = objectMapper.readTree(inputStream);
            
            // Start with default values and override with custom ones
            Map<String, String> placeholderValues = generateDynamicValues();
            placeholderValues.putAll(customValues);
            
            JsonNode processedNode = replacePlaceholders(originalNode, placeholderValues);
            
            LOGGER.info("Loaded and processed test data from: {} with custom values", resourcePath);
            return processedNode;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load test data file", e);
        }
    }
    
    /**
     * Recursively replaces placeholders in JSON nodes
     */
    private static JsonNode replacePlaceholders(JsonNode node, Map<String, String> placeholderValues) {
        if (node.isTextual()) {
            String text = node.asText();
            for (Map.Entry<String, String> entry : placeholderValues.entrySet()) {
                text = text.replace(entry.getKey(), entry.getValue());
            }
            return new TextNode(text);
        } else if (node.isObject()) {
            return replaceInObject(node, placeholderValues);
        } else if (node.isArray()) {
            return replaceInArray(node, placeholderValues);
        }
        return node;
    }
    
    private static JsonNode replaceInObject(JsonNode objectNode, Map<String, String> placeholderValues) {
        var objectBuilder = objectMapper.createObjectNode();
        objectNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            objectBuilder.set(key, replacePlaceholders(value, placeholderValues));
        });
        return objectBuilder;
    }
    
    private static JsonNode replaceInArray(JsonNode arrayNode, Map<String, String> placeholderValues) {
        var arrayBuilder = objectMapper.createArrayNode();
        arrayNode.forEach(element -> {
            arrayBuilder.add(replacePlaceholders(element, placeholderValues));
        });
        return arrayBuilder;
    }
    
    /**
     * Extracts values from a processed payload for verification purposes
     */
    public static PayloadValues extractValues(JsonNode payload) {
        PayloadValues values = new PayloadValues();
        
        // Extract hearing ID from the first hearing
        JsonNode hearings = payload.get("hearings");
        if (hearings != null && hearings.isArray() && hearings.size() > 0) {
            JsonNode firstHearing = hearings.get(0);
            
            values.hearingId = getTextValue(firstHearing, "id");
            values.courtCentreId = getTextValue(firstHearing.get("courtCentre"), "id");
            values.courtRoomId = getTextValue(firstHearing.get("courtCentre"), "roomId");
            values.jurisdictionType = getTextValue(firstHearing, "jurisdictionType");
            JsonNode bookedSlots = firstHearing.get("bookedSlots");
            if (nonNull(bookedSlots)) {
                values.courtScheduleId = getTextValue(bookedSlots.get(0), "courtScheduleId");
            }
            
            // Extract prosecution case information
            JsonNode prosecutionCases = firstHearing.get("prosecutionCases");
            if (prosecutionCases != null && prosecutionCases.isArray() && prosecutionCases.size() > 0) {
                JsonNode firstCase = prosecutionCases.get(0);
                values.caseId = getTextValue(firstCase, "id");
                JsonNode prosecutionCaseIdentifier = firstCase.get("prosecutionCaseIdentifier");
                values.prosecutorId = getTextValue(prosecutionCaseIdentifier,"prosecutionAuthorityId");
                // Extract defendant information
                JsonNode defendants = firstCase.get("defendants");
                if (defendants != null && defendants.isArray() && defendants.size() > 0) {
                    JsonNode firstDefendant = defendants.get(0);
                    values.defendantId = getTextValue(firstDefendant, "id");
                    
                    // Extract offence IDs
                    JsonNode offences = firstDefendant.get("offences");
                    if (offences != null && offences.isArray()) {
                        values.offenceIds = new String[offences.size()];
                        for (int i = 0; i < offences.size(); i++) {
                            values.offenceIds[i] = getTextValue(offences.get(i), "id");
                        }
                    }
                }
            }
        }

        return values;
    }
    
    private static String getTextValue(JsonNode node, String fieldName) {
        if (node != null && node.has(fieldName)) {
            return node.get(fieldName).asText();
        }
        return null;
    }
    
    /**
     * Container class for extracted values from payload for verification
     */
    public static class PayloadValues {
        public String hearingId;
        public String courtCentreId;
        public String courtScheduleId;
        public String courtRoomId;
        public String ouCode;
        public String hearingDate;
        public String jurisdictionType;
        public String caseId;
        public String defendantId;
        public String prosecutorId;
        public String[] offenceIds;
        public ZonedDateTime hearingStartTime;
        public ZonedDateTime hearingEndTime;

        @Override
        public String toString() {
            return String.format("PayloadValues{hearingId='%s', courtCentreId='%s', caseId='%s', defendantId='%s', hearingStartTime='%s', hearingEndTime='%s'}",
                               hearingId, courtCentreId, caseId, defendantId, hearingStartTime, hearingEndTime);
        }
    }
} 