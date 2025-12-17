package uk.gov.moj.cpp.listing.steps;

import static java.lang.String.format;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParams;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetAvailableHearingSlotsWithQueryParamsForPayloadIT;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;

import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.utils.PropertyUtil;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
 * Helper class for update hearing operations using JSON payload files from test-data folder
 */
public class PayloadBasedUpdateHearingSteps extends AbstractIT {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PayloadBasedUpdateHearingSteps.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    protected final StringToJsonObjectConverter stringToJsonObjectConverter = new StringToJsonObjectConverter();

    private static final String LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING = "listing.command.update-hearing-for-listing";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";
    private static final String MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING = "application/vnd.listing.command.update-hearing-for-listing+json";
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 30);
    
    private PayloadGenerator.PayloadValues payloadValues;
    private String requestPayload;
    private String hearingIdToUpdate; // The hearing ID that will be updated
    
    public PayloadBasedUpdateHearingSteps(String hearingIdToUpdate) {
        this.hearingIdToUpdate = hearingIdToUpdate;
        this.givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
    }
    public PayloadBasedUpdateHearingSteps(String hearingIdToUpdate, PayloadGenerator.PayloadValues payloadValues) {
        this.hearingIdToUpdate = hearingIdToUpdate;
        this.payloadValues = payloadValues;
        this.givenAUserHasLoggedInAsAListingOfficer(AbstractIT.USER_ID_VALUE);
    }

    /**
     * Submit update hearing request for allocated room update scenario
     */
    public PayloadGenerator.PayloadValues whenUpdateHearingSubmittedWithAllocatedRoomUpdate() {
        return this.whenUpdateHearingSubmittedWithScenario("update-hearing-for-listing", "update-hearing-for-listing-allocated-room-update");
    }
    
    /**
     * Submit update hearing request for assign judiciary scenario
     */
    public PayloadGenerator.PayloadValues whenUpdateHearingSubmittedWithAssignJudiciary() {
        return this.whenUpdateHearingSubmittedWithScenario("update-hearing-for-listing", "update-hearing-for-listing-assign-judiciary");
    }
    
    /**
     * Submit update hearing request for change to multiday with non-default and non-sitting scenario
     */
    public PayloadGenerator.PayloadValues whenUpdateHearingSubmittedWithChangeToMultidayWithNonDefaultAndNonSitting() {
        return this.whenUpdateHearingSubmittedWithScenario("update-hearing-for-listing", "update-hearing-for-listing-change-to-multiday-with-nondefault-and-nonsitting");
    }
    
    /**
     * Submit update hearing request for week commencing to multiday scenario
     */
    public PayloadGenerator.PayloadValues whenUpdateHearingSubmittedWithFromWeekCommencingToMultiday() {
        return this.whenUpdateHearingSubmittedWithScenario("update-hearing-for-listing", "update-hearing-for-listing-from-weekcommencing-to-multiday");
    }
    
    /**
     * Submit update hearing request for unallocated to allocated scenario
     */
    public PayloadGenerator.PayloadValues whenUpdateHearingSubmittedWithUnallocatedToAllocated() {
        return this.whenUpdateHearingSubmittedWithScenario("update-hearing-for-listing", "update-hearing-for-listing-unallocated-to-allocated");
    }
    
    /**
     * Generic method to submit update hearing request with custom scenario and test case
     */
    public PayloadGenerator.PayloadValues whenUpdateHearingSubmittedWithScenario(String scenario, String testCase) {
        return this.whenUpdateHearingSubmittedWithScenario(scenario, testCase, new HashMap<>());
    }
    
    /**
     * Generic method to submit update hearing request with custom scenario, test case, and custom values
     */
    public PayloadGenerator.PayloadValues whenUpdateHearingSubmittedWithScenario(String scenario, String testCase, Map<String, String> customValues) {
        try {
            // Add hearing ID to update to custom values
            customValues.put("%%HEARING_ID_TO_UPDATE%%", this.hearingIdToUpdate);
            if (nonNull(this.payloadValues)) {//this is to prevent overriding by dynamic values
                customValues.put("%%COURT_CENTRE_ID%%", payloadValues.courtCentreId);
                customValues.put("%%COURT_SCHEDULE_ID%%", payloadValues.courtScheduleId);
                if (nonNull(payloadValues.courtRoomId)){
                    customValues.put("originalCourtRoomId", payloadValues.courtRoomId);
                }
            }

            // Load payload with dynamic values
            JsonNode payload = PayloadGenerator.loadPayloadWithCustomValues(scenario, testCase, customValues);
            
            // Extract values for verification
            //only applicable for hearing payloads. so wrap it with the check
            if (payload.get("hearings") != null) {
                this.payloadValues = PayloadGenerator.extractValues(payload);
            }
            // Setup stubs with generated values
            this.setupStubsForUpdateHearing(this.payloadValues);
            
            // Convert JsonNode to string for the REST call
            String payloadString = PayloadBasedUpdateHearingSteps.objectMapper.writeValueAsString(payload);
            this.requestPayload = payloadString;
            String path = MessageFormat.format(PropertyUtil.readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), this.hearingIdToUpdate);


            // Make the API call
            String updateHearingUrl = String.format("%s/%s", PropertyUtil.getBaseUri(), format(path));
            
            Response response = AbstractIT.restClient.postCommand(updateHearingUrl, PayloadBasedUpdateHearingSteps.MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,
                    payloadString, this.getLoggedInHeader());
            
            MatcherAssert.assertThat("Expected HTTP 202 (Accepted) response", response.getStatus(), IsEqual.equalTo(HttpStatus.SC_ACCEPTED));
            
            PayloadBasedUpdateHearingSteps.LOGGER.info("Successfully submitted update hearing request for scenario: {}/{}", scenario, testCase);
            PayloadBasedUpdateHearingSteps.LOGGER.info("Generated values: {}", this.payloadValues);
            
            return this.payloadValues;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit update hearing request", e);
        }
    }
    
    /**
     * Sets up required reference data stubs based on the payload values
     */
    private void setupStubsForUpdateHearing(PayloadGenerator.PayloadValues values) throws IOException {
        if (values.courtCentreId != null) {
            UUID courtCentreId = UUID.fromString(values.courtCentreId);
            UUID courtRoomId = values.courtRoomId != null ? UUID.fromString(values.courtRoomId) : UUID.randomUUID();

            CourtCentreData courtCentreData = new CourtCentreData(
                    courtCentreId,
                    PayloadBasedUpdateHearingSteps.DEFAULT_START_TIME,
                    PayloadBasedUpdateHearingSteps.DEFAULT_DURATION_HOURS_MINS,
                    courtRoomId,
                    "Test Court Centre"
            );

            stubGetReferenceDataCourtCentre(courtCentreData);
            stubGetReferenceDataCourtCentreById(courtCentreData);
            stubGetReferenceDataCourtMappings(courtCentreData);

        }

        if (values.hearingId != null) {
            // Stub hearing types - use a default hearing type ID
            UUID defaultHearingTypeId = UUID.randomUUID();
            stubGetReferenceDataHearingTypes(defaultHearingTypeId);
        }



        // Add judiciary stubs if needed for assign judiciary scenarios
        // stubGetReferenceDataJudiciaries(...);
        if (isNull(values.hearingStartTime)) {
            values.hearingStartTime = ZonedDateTime
                    .now(ZoneId.of("Europe/London"))    // use your desired time zone
                    .plusDays(30)                        // add 30 days
                    .withHour(10)                        // set default time if needed
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
        }
        if (values.courtRoomId != null) {
            stubGetAvailableHearingSlotsWithQueryParamsForPayloadIT(false,
                    values.courtRoomId,
                    values.courtScheduleId,
                    "B01LY00",
                    values.hearingDate,
                    values.hearingStartTime);
        }
    }
    
    /**
     * Verify that the hearing was updated successfully from the API
     */
    public void verifyHearingUpdatedFromAPI(boolean isAllocated) {
        if (this.payloadValues == null) {
            throw new IllegalStateException("No payload values available. Call a 'when' method first.");
        }
        
        this.verifyHearingUpdatedFromAPI(this.payloadValues, isAllocated);
    }
    
    /**
     * Verify hearing updated with specific payload values
     */
    private void verifyHearingUpdatedFromAPI(PayloadGenerator.PayloadValues values, boolean isAllocated) {
        PayloadBasedUpdateHearingSteps.LOGGER.info("Verifying hearing updated from API for hearing: {}, allocated: {}", this.hearingIdToUpdate, isAllocated);
        
        // This would need to poll the search API and verify the hearing exists with updated values
        // using the generated values from the payload
        // The actual implementation would be similar to the existing method
        // but would use the hearingIdToUpdate and the new values from the payload

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearings.by.allocated"), isAllocated));

        final String response =  poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser())).until(status().is(OK)).getPayload();
        JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
        final JsonObject hearingJsonObject = (JsonObject)jsonObject.getJsonArray("hearings").get(0);
        assertThat(hearingJsonObject.getString("id"), is(values.hearingId));

        // For now, we'll log the verification request
        PayloadBasedUpdateHearingSteps.LOGGER.info("Verification would check updated hearing ID: {} with new court centre: {}", this.hearingIdToUpdate, values.courtCentreId);
    }
    
    /**
     * Verify that judiciary was assigned to the hearing
     */
    public void verifyJudiciaryAssignedToHearing() {
        if (this.payloadValues == null) {
            throw new IllegalStateException("No payload values available. Call a 'when' method first.");
        }
        
        PayloadBasedUpdateHearingSteps.LOGGER.info("Verifying judiciary assigned to hearing: {}", this.hearingIdToUpdate);
        
        // This would verify that the judiciary information is present in the hearing
        // by polling the search API and checking for judiciary details
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearings.by.allocated"), true));

        final String response =  poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser())).until(status().is(OK)).getPayload();
        JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
        final JsonObject hearingJsonObject = (JsonObject)jsonObject.getJsonArray("hearings").get(0);
        assertNotNull(hearingJsonObject.getJsonArray("judiciary"));
    }
    /**
     * Verify that judiciary was assigned to the hearing
     */
    public void verifyCourtRoomUpdatedForHearing(String originalCourtRoomId) {
        if (this.payloadValues == null) {
            throw new IllegalStateException("No payload values available. Call a 'when' method first.");
        }

        PayloadBasedUpdateHearingSteps.LOGGER.info("Verifying CourtRoom updated for hearing: {}", this.hearingIdToUpdate);

        // This would verify that the courtroom information is present in the hearing
        // by polling the search API and checking for courtroom details
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearings.by.allocated"), true));

        final String response =  poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser())).until(status().is(OK)).getPayload();
        JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
        final JsonObject hearingJsonObject = (JsonObject)jsonObject.getJsonArray("hearings").get(0);
        String updatedCourtRoomId = hearingJsonObject.getString("courtRoomId");
        assertNotEquals(originalCourtRoomId, updatedCourtRoomId);

    }
    
    /**
     * Verify that the hearing allocation status changed
     */
    public void verifyHearingAllocationStatusChanged(boolean expectedAllocated) {
        if (this.payloadValues == null) {
            throw new IllegalStateException("No payload values available. Call a 'when' method first.");
        }
        
        PayloadBasedUpdateHearingSteps.LOGGER.info("Verifying hearing allocation status changed to: {} for hearing: {}", expectedAllocated, this.hearingIdToUpdate);
        
        // This would verify that the hearing allocation status is as expected
        // by polling the search API and checking the hearing details
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                MessageFormat.format(readConfig().getProperty("listing.search.hearings.by.allocated"), expectedAllocated));

        final String response =  poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser())).until(status().is(OK)).getPayload();
        JsonObject jsonObject = stringToJsonObjectConverter.convert(response);
        final JsonObject hearingJsonObject = (JsonObject)jsonObject.getJsonArray("hearings").get(0);
        assertThat(hearingJsonObject.getString("id"), is(this.payloadValues.hearingId));
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
     * Get the hearing ID that was updated
     */
    public String getHearingIdToUpdate() {
        return this.hearingIdToUpdate;
    }
} 