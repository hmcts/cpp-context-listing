# Payload-Based Testing Approach

This document explains the new payload-based testing approach that uses JSON files from the `test-data` folder instead of creating complex nested class structures.

## Overview

The new approach provides several benefits:
- **Simplified test creation**: Use pre-defined JSON scenarios instead of complex object builders
- **Dynamic placeholder replacement**: Automatically replace placeholders with random/dynamic values for each test run
- **Scenario-based testing**: Each subfolder represents different scenarios for the same endpoint
- **Reusable payloads**: Same JSON files can be used across multiple tests with different dynamic values

## Architecture

### Core Components

1. **PayloadGenerator**: Utility class for loading JSON files and replacing placeholders
2. **PayloadBasedListCourtHearingSteps**: Helper for list court hearing operations
3. **PayloadBasedListNextHearingSteps**: Helper for list next hearings operations  
4. **PayloadBasedUpdateHearingSteps**: Helper for update hearing operations

### Test Data Structure

```
test-data/
├── list-court-hearing/
│   ├── adhoc_hearing_creation.json
│   ├── spi_allocated.json
│   ├── spi_unallocated.json
│   ├── mcc_without_courtschedule_allocated.json
│   ├── sjp_without_courthscheduleid.json
│   └── spi_two_defendants_unallocated.json
├── list-next-hearings-v2/
│   ├── adjorunment_crown_fixed_date.json
│   ├── adjournment_crown_week_commencing.json
│   └── adjournment_mags.json
├── list-unscheduled-next-hearings/
│   ├── adjournment_crown-unscheduled.json
│   └── adjournment_crown-unscheduled_2.json
└── update-hearing-for-listing/
    ├── update-hearing-for-listing-allocated-room-update.json
    ├── update-hearing-for-listing-assign-judiciary.json
    ├── update-hearing-for-listing-change-to-multiday-with-nondefault-and-nonsitting.json
    ├── update-hearing-for-listing-from-weekcommencing-to-multiday.json
    └── update-hearing-for-listing-unallocated-to-allocated.json
```

## Placeholder System

### Standard Placeholders

The following placeholders are automatically replaced with dynamic values:

- `%%HEARING_ID%%`: Random UUID for hearing ID
- `%%COURT_CENTRE_ID%%`: Random UUID for court centre ID
- `%%COURT_ROOM_ID%%`: Random UUID for court room ID
- `%%COURTSCHEDULE_ID%%`: Random UUID for court schedule ID
- `%%JURISDICTION_TYPE%%`: Default jurisdiction type (MAGISTRATES)
- `%%BOOKED_SLOT_START_TIME%%`: Future date/time (30 days from now)
- `%%HEARING_START_DATE%%`: Future date
- `%%HEARING_END_DATE%%`: Future date
- `%%CASE_URN%%`: Random case URN
- `%%PROSECUTION_AUTHORITY_CODE%%`: Random prosecution authority code
- `%%ORGANISATION_CODE%%`: Random organisation code

### Custom Placeholders

You can provide custom values to override defaults:

```java
Map<String, String> customValues = new HashMap<>();
customValues.put("%%JURISDICTION_TYPE%%", "CROWN");
customValues.put("%%COURT_CENTRE_ID%%", specificCourtCentreId);

PayloadGenerator.PayloadValues values = steps.whenListCourtHearingSubmittedWithScenario(
    "list-court-hearing", 
    "spi_allocated", 
    customValues
);
```

## Usage Examples

### Basic List Court Hearing Test

```java
@Test
public void shouldListCourtHearingWithAdhocHearingCreation() {
    // Create steps instance
    PayloadBasedListCourtHearingSteps steps = new PayloadBasedListCourtHearingSteps();
    
    // Submit request using adhoc hearing creation scenario
    PayloadGenerator.PayloadValues values = steps.whenListCourtHearingSubmittedWithAdhocHearingCreation();
    
    // Verify the hearing was listed
    steps.verifyHearingListedFromAPI(UNALLOCATED);
    
    // Access generated values for further assertions
    assertNotNull(values.hearingId);
    assertNotNull(values.courtCentreId);
}
```

### Next Hearings Test

```java
@Test 
public void shouldCreateNextHearings() {
    // First create an initial hearing
    PayloadBasedListCourtHearingSteps listSteps = new PayloadBasedListCourtHearingSteps();
    PayloadGenerator.PayloadValues firstHearing = listSteps.whenListCourtHearingSubmittedWithSpiAllocated();
    
    // Create next hearings using the first hearing as seed
    PayloadBasedListNextHearingSteps nextSteps = new PayloadBasedListNextHearingSteps(firstHearing.hearingId);
    PayloadGenerator.PayloadValues nextHearing = nextSteps.whenListNextHearingSubmittedWithAdjournmentCrownFixedDate();
    
    // Verify both hearings
    listSteps.verifyHearingListedFromAPI(ALLOCATED);
    nextSteps.verifyNextHearingListedFromAPI(ALLOCATED);
}
```

### Update Hearing Test

```java
@Test
public void shouldUpdateHearingFromUnallocatedToAllocated() {
    // Create unallocated hearing
    PayloadBasedListCourtHearingSteps listSteps = new PayloadBasedListCourtHearingSteps();
    PayloadGenerator.PayloadValues hearing = listSteps.whenListCourtHearingSubmittedWithSpiUnallocated();
    
    // Update to allocated
    PayloadBasedUpdateHearingSteps updateSteps = new PayloadBasedUpdateHearingSteps(hearing.hearingId);
    updateSteps.whenUpdateHearingSubmittedWithUnallocatedToAllocated();
    
    // Verify update
    updateSteps.verifyHearingUpdatedFromAPI(ALLOCATED);
    updateSteps.verifyHearingAllocationStatusChanged(true);
}
```

### Custom Values Test

```java
@Test
public void shouldListCourtHearingWithCustomJurisdiction() {
    PayloadBasedListCourtHearingSteps steps = new PayloadBasedListCourtHearingSteps();
    
    // Specify custom jurisdiction type
    Map<String, String> customValues = new HashMap<>();
    customValues.put("%%JURISDICTION_TYPE%%", "CROWN");
    
    PayloadGenerator.PayloadValues values = steps.whenListCourtHearingSubmittedWithScenario(
        "list-court-hearing", 
        "spi_allocated", 
        customValues
    );
    
    assertEquals("CROWN", values.jurisdictionType);
    steps.verifyHearingListedFromAPI(ALLOCATED);
}
```

## Available Helper Methods

### PayloadBasedListCourtHearingSteps

- `whenListCourtHearingSubmittedWithAdhocHearingCreation()`
- `whenListCourtHearingSubmittedWithSpiAllocated()`
- `whenListCourtHearingSubmittedWithSpiUnallocated()`
- `whenListCourtHearingSubmittedWithMccWithoutCourtScheduleAllocated()`
- `whenListCourtHearingSubmittedWithSjpWithoutCourtScheduleId()`
- `whenListCourtHearingSubmittedWithSpiTwoDefendantsUnallocated()`
- `whenListCourtHearingSubmittedWithScenario(scenario, testCase)`
- `whenListCourtHearingSubmittedWithScenario(scenario, testCase, customValues)`

### PayloadBasedListNextHearingSteps

- `whenListNextHearingSubmittedWithAdjournmentCrownFixedDate()`
- `whenListNextHearingSubmittedWithAdjournmentCrownWeekCommencing()`
- `whenListNextHearingSubmittedWithAdjournmentMagistrates()`
- `whenListNextHearingSubmittedWithAdjournmentCrownUnscheduled()`
- `whenListNextHearingSubmittedWithAdjournmentCrownUnscheduled2()`
- `whenListNextHearingSubmittedWithScenario(scenario, testCase)`
- `whenListUnscheduledNextHearingSubmittedWithScenario(scenario, testCase)`

### PayloadBasedUpdateHearingSteps

- `whenUpdateHearingSubmittedWithAllocatedRoomUpdate()`
- `whenUpdateHearingSubmittedWithAssignJudiciary()`
- `whenUpdateHearingSubmittedWithChangeToMultidayWithNonDefaultAndNonSitting()`
- `whenUpdateHearingSubmittedWithFromWeekCommencingToMultiday()`
- `whenUpdateHearingSubmittedWithUnallocatedToAllocated()`
- `whenUpdateHearingSubmittedWithScenario(scenario, testCase)`

## PayloadValues Object

The `PayloadGenerator.PayloadValues` object contains extracted values from the processed payload:

```java
public static class PayloadValues {
    public String hearingId;
    public String courtCentreId;
    public String courtRoomId;
    public String jurisdictionType;
    public String caseId;
    public String defendantId;
    public String[] offenceIds;
}
```

These values can be used for:
- Verification assertions
- Linking between multiple API calls
- Creating dependent test scenarios

## Migration from Old Approach

### Before (Old Approach)
```java
@Test
public void shouldListNextHearings() {
    final HearingsData firstHearings = hearingsData();
    final HearingsData nextHearings = hearingsData();
    final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(firstHearings);
    listCourtHearingSteps.whenCaseIsSubmittedForListing();
    listCourtHearingSteps.verifyHearingListedFromAPI(UNALLOCATED);

    final ListNextHearingSteps listNextHearingSteps = new ListNextHearingSteps(firstHearings.getHearingData().get(0));
    listNextHearingSteps.whenNextHearingSubmittedForListing(nextHearings);
    listNextHearingSteps.verifyHearingListedFromAPI(nextHearings);
}
```

### After (New Approach)
```java
@Test
public void shouldListNextHearings() {
    PayloadBasedListCourtHearingSteps listSteps = new PayloadBasedListCourtHearingSteps();
    PayloadGenerator.PayloadValues firstHearing = listSteps.whenListCourtHearingSubmittedWithSpiUnallocated();
    listSteps.verifyHearingListedFromAPI(UNALLOCATED);

    PayloadBasedListNextHearingSteps nextSteps = new PayloadBasedListNextHearingSteps(firstHearing.hearingId);
    PayloadGenerator.PayloadValues nextHearing = nextSteps.whenListNextHearingSubmittedWithAdjournmentCrownFixedDate();
    nextSteps.verifyNextHearingListedFromAPI(ALLOCATED);
}
```

## Benefits

1. **Simplicity**: No need to understand complex object builders
2. **Maintainability**: JSON files are easier to read and modify than nested Java objects
3. **Reusability**: Same scenarios can be used across multiple tests
4. **Flexibility**: Easy to create variations using custom placeholder values
5. **Transparency**: Clear visibility of what data is being sent to the API
6. **Dynamic**: Each test run generates fresh random data 