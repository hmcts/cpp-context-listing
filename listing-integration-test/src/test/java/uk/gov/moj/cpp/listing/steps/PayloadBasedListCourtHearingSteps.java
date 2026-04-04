package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static java.util.Objects.isNull;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.*;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.*;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.utils.PropertyUtil;

import java.text.MessageFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for list court hearing operations using JSON payload files from test-data folder
 */
public class PayloadBasedListCourtHearingSteps extends AbstractIT {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadBasedListCourtHearingSteps.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    protected final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();


    private static final String LISTING_COMMAND_LIST_COURT_HEARING = "listing.command.list-court-hearing";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";
    private static final String MEDIA_TYPE_LIST_COURT_HEARING = "application/vnd.listing.command.list-court-hearing+json";
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    
    private PayloadGenerator.PayloadValues payloadValues;
    private String requestPayload;

    public PayloadBasedListCourtHearingSteps() {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    /**
     * Submit list court hearing request using adhoc hearing creation scenario
     */
    public PayloadGenerator.PayloadValues whenListCourtHearingSubmittedWithAdhocHearingCreation() {
        return this.whenListCourtHearingSubmittedWithScenario("list-court-hearing", "adhoc_hearing_creation");
    }
    
    /**
     * Submit list court hearing request using SPI allocated scenario
     */
    public PayloadGenerator.PayloadValues whenListCourtHearingSubmittedWithSpiAllocated() {
        return this.whenListCourtHearingSubmittedWithScenario("list-court-hearing", "spi_allocated");
    }
    
    /**
     * Submit list court hearing request using SPI unallocated scenario
     */
    public PayloadGenerator.PayloadValues whenListCourtHearingSubmittedWithSpiUnallocated() {
        return this.whenListCourtHearingSubmittedWithScenario("list-court-hearing", "spi_unallocated");
    }
    
    /**
     * Submit list court hearing request using MCC without court schedule allocated scenario
     */
    public PayloadGenerator.PayloadValues whenListCourtHearingSubmittedWithMccWithoutCourtScheduleAllocated() {
        return this.whenListCourtHearingSubmittedWithScenario("list-court-hearing", "mcc_without_courtschedule_allocated");
    }
    
    /**
     * Submit list court hearing request using SJP without court schedule ID scenario
     */
    public PayloadGenerator.PayloadValues whenListCourtHearingSubmittedWithSjpWithoutCourtScheduleId() {
        return this.whenListCourtHearingSubmittedWithScenario("list-court-hearing", "sjp_without_courthscheduleid");
    }
    
    /**
     * Submit list court hearing request using SPI two defendants unallocated scenario
     */
    public PayloadGenerator.PayloadValues whenListCourtHearingSubmittedWithSpiTwoDefendantsUnallocated() {
        return this.whenListCourtHearingSubmittedWithScenario("list-court-hearing", "spi_two_defendants_unallocated");
    }
    
    /**
     * Generic method to submit list court hearing request with custom scenario and test case
     */
    public PayloadGenerator.PayloadValues whenListCourtHearingSubmittedWithScenario(String scenario, String testCase) {
        return this.whenListCourtHearingSubmittedWithScenario(scenario, testCase, new HashMap<>());
    }
    
    /**
     * Generic method to submit list court hearing request with custom scenario, test case, and custom values
     */
    public PayloadGenerator.PayloadValues whenListCourtHearingSubmittedWithScenario(String scenario, String testCase, Map<String, String> customValues) {
        try {
            // Load payload with dynamic values
            JsonNode payload = PayloadGenerator.loadPayloadWithCustomValues(scenario, testCase, customValues);
            
            // Extract values for verification
            this.payloadValues = PayloadGenerator.extractValues(payload);
            
            // Setup stubs with generated values
            this.setupStubsForHearing(this.payloadValues);
            
            // Convert JsonNode to JsonObject for the REST call
            String payloadString = PayloadBasedListCourtHearingSteps.objectMapper.writeValueAsString(payload);
            this.requestPayload = payloadString;
            
            // Make the API call
            String listCaseForHearingUrl = String.format("%s/%s", PropertyUtil.getBaseUri(), format(
                    PropertyUtil.readConfig().getProperty(PayloadBasedListCourtHearingSteps.LISTING_COMMAND_LIST_COURT_HEARING)));
            
            Response response = AbstractIT.restClient.postCommand(listCaseForHearingUrl, PayloadBasedListCourtHearingSteps.MEDIA_TYPE_LIST_COURT_HEARING,
                    payloadString, getLoggedInHeader());
            
            MatcherAssert.assertThat("Expected HTTP 202 (Accepted) response", response.getStatus(), IsEqual.equalTo(HttpStatus.SC_ACCEPTED));
            
            PayloadBasedListCourtHearingSteps.LOGGER.info("Successfully submitted list court hearing request for scenario: {}/{}", scenario, testCase);
            PayloadBasedListCourtHearingSteps.LOGGER.info("Generated values: {}", this.payloadValues);
            
            return this.payloadValues;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit list court hearing request", e);
        }
    }
    
    /**
     * Sets up required reference data stubs based on the payload values
     */
    private void setupStubsForHearing(PayloadGenerator.PayloadValues values) {
        if (values.courtCentreId != null) {
            UUID courtCentreId = UUID.fromString(values.courtCentreId);
            UUID courtRoomId = values.courtRoomId != null ? UUID.fromString(values.courtRoomId) : getRandomCourtRoomId();
            
            CourtCentreData courtCentreData = new CourtCentreData(
                    courtCentreId, 
                    PayloadBasedListCourtHearingSteps.DEFAULT_START_TIME, 
                    PayloadBasedListCourtHearingSteps.DEFAULT_DURATION_HOURS_MINS, 
                    courtRoomId, 
                    "Test Court Centre"
            );
            
            stubGetReferenceDataCourtCentre(courtCentreData);
            stubGetReferenceDataCourtCentreById(courtCentreData);
            stubGetReferenceDataCourtMappings(courtCentreData);
            stubGetReferenceDataCourtCentreById(courtCentreId);
        }
        
        if (values.hearingId != null) {
            // Stub hearing types - use a default hearing type ID
            UUID defaultHearingTypeId = UUID.randomUUID();
            stubGetReferenceDataHearingTypes(defaultHearingTypeId);
            
            // Stub court scheduler service for listing hearings in court sessions
            if (isNull(values.courtScheduleId)) {
                values.courtScheduleId = UUID.randomUUID().toString();
            }
            if (isNull(values.hearingStartTime)) {
                values.hearingStartTime = ZonedDateTime
                        .now(ZoneId.of("Europe/London"))    // use your desired time zone
                        .plusDays(30)                        // add 30 days
                        .withHour(10)                        // set default time if needed
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);
            }
            if (isNull(values.hearingDate)) {
                values.hearingDate = LocalDate.now().plusDays(30).toString();
            }

            stubListHearingInCourtSessions(values.hearingId, values.courtScheduleId,values.hearingStartTime);
            stubGetProsecutorPoliceFlag(UUID.fromString(values.prosecutorId));
            stubSearchBookHearingSlots(values.hearingId,values.courtCentreId,values.hearingDate,values.hearingStartTime);
            if ("CROWN".equals(values.jurisdictionType) && values.courtRoomId != null) {
                stubSearchBookHearingSlotsForCrown(values.hearingId, values.courtCentreId, values.courtRoomId);
            }
        }
        
        // Add more stub setups as needed based on the payload content
    }
    
    /**
     * Verify that the hearing was listed successfully from the API
     */
    public void verifyHearingListedFromAPI(boolean isAllocated) {
        if (this.payloadValues == null) {
            throw new IllegalStateException("No payload values available. Call a 'when' method first.");
        }
        
        // Use existing verification logic from ListCourtHearingSteps
        // This can be extracted to a common verification utility
        this.verifyHearingListedFromAPI(this.payloadValues, isAllocated);
    }
    
    /**
     * Verify hearing listed with specific payload values
     */
    private void verifyHearingListedFromAPI(PayloadGenerator.PayloadValues values, boolean isAllocated) {
        // Implementation would mirror the existing verifyHearingListedFromAPI method
        // but use the values from the payload instead of hearingsData
        
        PayloadBasedListCourtHearingSteps.LOGGER.info("Verifying hearing listed from API for hearing: {}, allocated: {}", values.hearingId, isAllocated);
        
        // This would need to poll the search API and verify the hearing exists
        // using the generated values from the payload
        // The actual implementation would be similar to the existing method
        // but would use values.hearingId, values.courtCentreId, etc.

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearings.by.allocated"), isAllocated));

        final String response =  pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser())).until(
                allOf(status().is(OK),
                        payload().isJson(withJsonPath("$.hearings.size()", greaterThan(0))))).getPayload();

        JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
        final JsonObject hearingJsonObject = (JsonObject)jsonObject.getJsonArray("hearings").get(0);
        assertThat(hearingJsonObject.getString("id"), is(values.hearingId));
        assertThat(hearingJsonObject.getString("courtCentreId"), is(values.courtCentreId));


        // For now, we'll log the verification request
        PayloadBasedListCourtHearingSteps.LOGGER.info("Verification would check hearing ID: {} in court centre: {}", values.hearingId, values.courtCentreId);
    }
    
    /**
     * Get the payload values for verification in tests
     */
    public PayloadGenerator.PayloadValues getPayloadValues() {
        return this.payloadValues;
    }
    
    /**
     * Get the request payload that was sent
     */
    public String getRequestPayload() {
        return this.requestPayload;
    }
} 