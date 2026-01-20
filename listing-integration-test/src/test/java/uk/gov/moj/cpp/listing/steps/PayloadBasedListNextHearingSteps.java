package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;

import org.apache.http.HttpStatus;
import org.hamcrest.MatcherAssert;

import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.utils.PropertyUtil;
import uk.gov.moj.cpp.listing.utils.ReferenceDataStub;

import java.text.MessageFormat;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.core.IsEqual;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;

/**
 * Helper class for list next hearings operations using JSON payload files from test-data folder
 */
public class PayloadBasedListNextHearingSteps extends AbstractIT {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadBasedListNextHearingSteps.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    protected final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();
    
    private static final String LISTING_API_LIST_NEXT_HEARINGS = "listing.list-next-hearings-v2";
    private static final String LISTING_API_LIST_UNSCHEDULED_NEXT_HEARINGS = "listing.list-unscheduled-next-hearings"; 
    private static final String MEDIA_TYPE_LIST_NEXT_HEARINGS = "application/vnd.listing.next-hearings-v2+json";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";
    private static final String MEDIA_TYPE_UNSCHEDULED_NEXT_HEARINGS = "application/vnd.listing.list-unscheduled-next-hearings+json";
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    
    private PayloadGenerator.PayloadValues payloadValues;
    private String requestPayload;
    private String seedingHearingId; // The hearing ID that seeds the next hearings
    
    public PayloadBasedListNextHearingSteps(String seedingHearingId) {
        this.seedingHearingId = seedingHearingId;
        this.givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
    }

    public PayloadBasedListNextHearingSteps(String seedingHearingId, PayloadGenerator.PayloadValues firstHearingValues) {
        this.seedingHearingId = seedingHearingId;
        this.payloadValues = firstHearingValues;
        this.givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
    }

    public PayloadBasedListNextHearingSteps() {
        this.givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
    }


    /**
     * Submit next hearing request using adjournment crown fixed date scenario
     */
    public PayloadGenerator.PayloadValues whenListNextHearingSubmittedWithAdjournmentCrownFixedDate() {
        return this.whenListNextHearingSubmittedWithScenario("list-next-hearings-v2", "adjorunment_crown_fixed_date");
    }
    
    /**
     * Submit next hearing request using adjournment crown week commencing scenario
     */
    public PayloadGenerator.PayloadValues whenListNextHearingSubmittedWithAdjournmentCrownWeekCommencing() {
        return this.whenListNextHearingSubmittedWithScenario("list-next-hearings-v2", "adjournment_crown_week_commencing");
    }
    
    /**
     * Submit next hearing request using adjournment magistrates scenario
     */
    public PayloadGenerator.PayloadValues whenListNextHearingSubmittedWithAdjournmentMagistrates() {
        return this.whenListNextHearingSubmittedWithScenario("list-next-hearings-v2", "adjournment_mags");
    }
    
    /**
     * Submit unscheduled next hearing request using adjournment crown unscheduled scenario
     */
    public PayloadGenerator.PayloadValues whenListNextHearingSubmittedWithAdjournmentCrownUnscheduled() {
        return this.whenListUnscheduledNextHearingSubmittedWithScenario("list-unscheduled-next-hearings", "adjournment_crown-unscheduled");
    }
    
    /**
     * Submit unscheduled next hearing request using adjournment crown unscheduled 2 scenario
     */
    public PayloadGenerator.PayloadValues whenListNextHearingSubmittedWithAdjournmentCrownUnscheduled2() {
        return this.whenListUnscheduledNextHearingSubmittedWithScenario("list-unscheduled-next-hearings", "adjournment_crown-unscheduled_2");
    }
    
    /**
     * Generic method to submit list next hearing request with custom scenario and test case
     */
    public PayloadGenerator.PayloadValues whenListNextHearingSubmittedWithScenario(String scenario, String testCase) {
        return this.whenListNextHearingSubmittedWithScenario(scenario, testCase, new HashMap<>());
    }
    
    /**
     * Generic method to submit list next hearing request with custom scenario, test case, and custom values
     */
    public PayloadGenerator.PayloadValues whenListNextHearingSubmittedWithScenario(String scenario, String testCase, Map<String, String> customValues) {
        try {
            // Add seeding hearing ID to custom values
            customValues.put("%%SEEDING_HEARING_ID%%", this.seedingHearingId);
            
            // Load payload with dynamic values
            JsonNode payload = PayloadGenerator.loadPayloadWithCustomValues(scenario, testCase, customValues);
            
            // Extract values for verification
            this.payloadValues = PayloadGenerator.extractValues(payload);
            
            // Setup stubs with generated values
            this.setupStubsForNextHearing(this.payloadValues);
            
            // Convert JsonNode to string for the REST call
            String payloadString = PayloadBasedListNextHearingSteps.objectMapper.writeValueAsString(payload);
            this.requestPayload = payloadString;
            
            // Make the API call
            String listNextHearingUrl = String.format("%s/%s", PropertyUtil.getBaseUri(), MessageFormat.format(
                    PropertyUtil.readConfig().getProperty(PayloadBasedListNextHearingSteps.LISTING_API_LIST_NEXT_HEARINGS), this.seedingHearingId));
            
            Response response = AbstractIT.restClient.postCommand(listNextHearingUrl, PayloadBasedListNextHearingSteps.MEDIA_TYPE_LIST_NEXT_HEARINGS,
                    payloadString, this.getLoggedInHeader());

            MatcherAssert.assertThat("Expected HTTP 202 (Accepted) response", response.getStatus(), IsEqual.equalTo(HttpStatus.SC_ACCEPTED));
            
            PayloadBasedListNextHearingSteps.LOGGER.info("Successfully submitted list next hearing request for scenario: {}/{}", scenario, testCase);
            PayloadBasedListNextHearingSteps.LOGGER.info("Generated values: {}", this.payloadValues);
            
            return this.payloadValues;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit list next hearing request", e);
        }
    }
    
    /**
     * Generic method to submit list unscheduled next hearing request
     */
    public PayloadGenerator.PayloadValues whenListUnscheduledNextHearingSubmittedWithScenario(String scenario, String testCase) {
        return this.whenListUnscheduledNextHearingSubmittedWithScenario(scenario, testCase, new HashMap<>());
    }
    
    /**
     * Generic method to submit list unscheduled next hearing request with custom values
     */
    public PayloadGenerator.PayloadValues whenListUnscheduledNextHearingSubmittedWithScenario(String scenario, String testCase, Map<String, String> customValues) {
        try {
            // Add seeding hearing ID to custom values
            customValues.put("%%SEEDING_HEARING_ID%%", this.seedingHearingId);
            
            // Load payload with dynamic values
            JsonNode payload = PayloadGenerator.loadPayloadWithCustomValues(scenario, testCase, customValues);
            
            // Extract values for verification
            this.payloadValues = PayloadGenerator.extractValues(payload);
            
            // Setup stubs with generated values
            this.setupStubsForNextHearing(this.payloadValues);
            
            // Convert JsonNode to string for the REST call
            String payloadString = PayloadBasedListNextHearingSteps.objectMapper.writeValueAsString(payload);
            this.requestPayload = payloadString;
            
            // Make the API call
            String listUnscheduledNextHearingUrl = String.format("%s/%s", PropertyUtil.getBaseUri(), MessageFormat.format(
                    PropertyUtil.readConfig().getProperty(PayloadBasedListNextHearingSteps.LISTING_API_LIST_UNSCHEDULED_NEXT_HEARINGS), this.seedingHearingId));
            
            Response response = AbstractIT.restClient.postCommand(listUnscheduledNextHearingUrl, PayloadBasedListNextHearingSteps.MEDIA_TYPE_UNSCHEDULED_NEXT_HEARINGS,
                    payloadString, this.getLoggedInHeader());
            
            MatcherAssert.assertThat("Expected HTTP 202 (Accepted) response", response.getStatus(), IsEqual.equalTo(HttpStatus.SC_ACCEPTED));
            
            PayloadBasedListNextHearingSteps.LOGGER.info("Successfully submitted list unscheduled next hearing request for scenario: {}/{}", scenario, testCase);
            PayloadBasedListNextHearingSteps.LOGGER.info("Generated values: {}", this.payloadValues);
            
            return this.payloadValues;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit list unscheduled next hearing request", e);
        }
    }
    
    /**
     * Sets up required reference data stubs based on the payload values
     */
    private void setupStubsForNextHearing(PayloadGenerator.PayloadValues values) {
        if (values.courtCentreId != null) {
            UUID courtCentreId = UUID.fromString(values.courtCentreId);
            UUID courtRoomId = values.courtRoomId != null ? UUID.fromString(values.courtRoomId) : UUID.randomUUID();
            
            CourtCentreData courtCentreData = new CourtCentreData(
                    courtCentreId, 
                    PayloadBasedListNextHearingSteps.DEFAULT_START_TIME, 
                    PayloadBasedListNextHearingSteps.DEFAULT_DURATION_HOURS_MINS, 
                    courtRoomId, 
                    "Test Court Centre"
            );
            
            ReferenceDataStub.stubGetReferenceDataCourtCentre(courtCentreData);
            ReferenceDataStub.stubGetReferenceDataCourtCentreById(courtCentreData);
            ReferenceDataStub.stubGetReferenceDataCourtMappings(courtCentreData);
        }
        
        if (values.hearingId != null) {
            // Stub hearing types - use a default hearing type ID
            UUID defaultHearingTypeId = UUID.fromString("e78b1fe1-c3dd-40dc-accc-7aa452fc4d1d");
            ReferenceDataStub.stubGetReferenceDataHearingTypes(defaultHearingTypeId);
        }
        
        // Add judiciary stubs if needed
    }
    
    /**
     * Verify that the next hearing was listed successfully from the API
     */
    public void verifyNextHearingListedFromAPI(boolean isAllocated, int numberofHearings) {
        if (this.payloadValues == null) {
            throw new IllegalStateException("No payload values available. Call a 'when' method first.");
        }
        
        this.verifyNextHearingListedFromAPI(this.payloadValues, isAllocated,numberofHearings);
    }
    
    /**
     * Verify next hearing listed with specific payload values
     */
    private void verifyNextHearingListedFromAPI(PayloadGenerator.PayloadValues values, boolean isAllocated, int numberOfHearings) {
        PayloadBasedListNextHearingSteps.LOGGER.info("Verifying next hearing listed from API for hearing: {}, allocated: {}", values.hearingId, isAllocated);
        
        // This would need to poll the search API and verify the hearing exists
        // using the generated values from the payload
        // The actual implementation would be similar to the existing method
        // but would use values.hearingId, values.courtCentreId, etc.

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearings.by.allocated"), isAllocated));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(status().is(OK),payload().isJson(withJsonPath("$.hearings.length()", equalTo(numberOfHearings))));

        // For now, we'll log the verification request
        PayloadBasedListNextHearingSteps.LOGGER.info("Verification would check next hearing ID: {} in court centre: {}", values.hearingId, values.courtCentreId);
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
    
    /**
     * Get the seeding hearing ID
     */
    public String getSeedingHearingId() {
        return this.seedingHearingId;
    }
} 