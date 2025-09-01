package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.moj.cpp.listing.helper.SearchHearingHelper;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HearingDayCourtRoomChangeForCrownIT extends AbstractIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(HearingDayCourtRoomChangeForCrownIT.class);

    private final UUID HEARING_ID = randomUUID();
    private final UUID COURT_CENTRE_ID = randomUUID(); // Croydon Crown Court
    private final UUID COURT_ROOM_ID = randomUUID(); // Court Room 1
    private final UUID COURT_ROOM_ID2 = UUID.fromString("33b7d399-8379-437c-980d-af9487b1198c");
    private final UUID COURT_ROOM_ID3 = UUID.fromString("2a128f95-5892-4ca9-b6ba-45d027d389e7");
    private final String CASE_URN = "CASE_URN_123";
    private final String JURISDICTION_TYPE = CROWN.name();

    // Test dates
    private final LocalDate START_DATE = LocalDate.of(2025, 8, 15);
    private final LocalDate END_DATE = LocalDate.of(2025, 8, 19);
    private final ZonedDateTime HEARING_START_TIME = ZonedDateTime.of(2025, 8, 15, 10, 30, 0, 0, java.time.ZoneOffset.UTC);

    @Test
    public void shouldChangeCourtRoomForAllocatedMultiDayCrownCourtHearing() {

        // Clear database before test

        // Given: A Crown Court hearing is listed in Crown Court
        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(
                HEARING_ID,
                null,
                CASE_URN,
                randomUUID(),
                null,
                JURISDICTION_TYPE,
                JURISDICTION_TYPE,
                null,
                null
        );

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary(
                caseAndDefendantData,
                COURT_CENTRE_ID,
                COURT_ROOM_ID,
                END_DATE,
                HEARING_START_TIME
        );

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();

        // Verify the hearing is created and allocated
        listCourtHearingSteps.verifyHearingIsCreated(HEARING_ID, 1);
        listCourtHearingSteps.verifyHearingListedFromAPI(true);

        LOGGER.info("Crown Court hearing created with ID: {} in Crown Court", HEARING_ID);

        // When: We change rooms of 2025-08-18 and 2025-08-19 to room 2
        var virtualNonDefaultDaysNotPersistedRoom2 = java.util.List.of(
                new uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData(
                        java.time.ZonedDateTime.of(2025, 8, 18, 10, 30, 0, 0, java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
                        Optional.of(360), // 1 d in minutes
                        Optional.of(COURT_CENTRE_ID.toString()),
                        null,
                        Optional.of(COURT_ROOM_ID2.toString()),
                        Optional.of(Boolean.TRUE)
                ),
                new uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData(
                        java.time.ZonedDateTime.of(2025, 8, 19, 10, 30, 0, 0, java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
                        Optional.of(360),
                        Optional.of(COURT_CENTRE_ID.toString()),
                        null,
                        Optional.of(COURT_ROOM_ID2.toString()),
                        Optional.of(Boolean.TRUE)
                )
        );

        var hearingData = hearingsData.getHearingData().get(0);
        var virtualNonDefaultDaysNotPersistedUpdatedHearingDataRoom2 = new uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData(
                HEARING_ID,
                COURT_CENTRE_ID,
                hearingData.getName(),
                COURT_ROOM_ID,
                hearingData.getHearingTypeData(),
                START_DATE.toString(),
                END_DATE.toString(),
                virtualNonDefaultDaysNotPersistedRoom2,
                emptyList(),
                "ENGLISH",
                hearingData.getJudiciary(),
                JURISDICTION_TYPE,
                null,
                null,
                null,
                hearingData.getHasVideoLink(),
                hearingData.getPublicListNote(),
                "High",
                null,
                null,
                false,
                null
        );

        final UpdateHearingSteps virtualNonDefaultDaysNotPersistedUpdateHearingStepsRoom2 = new UpdateHearingSteps(hearingsData, virtualNonDefaultDaysNotPersistedUpdatedHearingDataRoom2);
        virtualNonDefaultDaysNotPersistedUpdateHearingStepsRoom2.whenHearingIsUpdatedForListing();

        // Then: Verify the hearing is still allocated in Crown Court
        virtualNonDefaultDaysNotPersistedUpdateHearingStepsRoom2.verifyHearingUpdatedWhenQueryingFromAPICourtCalendar();

        // Verify the hearing remains allocated
        virtualNonDefaultDaysNotPersistedUpdateHearingStepsRoom2.verifyHearingAllocatedWhenQueryingFromAPICourtCalendar();

        // Verify that hearing days on 2025-08-18 and 2025-08-19 have courtRoom COURT_ROOM_ID2
        // and that the hearing has empty virtualNonDefaultDaysNotPersistedRoom2
        verifyHearingDaysWithSpecificCourtRoomAndEmptyNonDefaultDays(virtualNonDefaultDaysNotPersistedUpdateHearingStepsRoom2, HEARING_ID, 0, COURT_ROOM_ID2, "2025-08-18", "2025-08-19");


        var updatedHearingDataWithoutNonDefaultDaysShouldPreservePrevRoomChange = new uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData(
                HEARING_ID,
                COURT_CENTRE_ID,
                hearingData.getName(),
                COURT_ROOM_ID,
                hearingData.getHearingTypeData(),
                START_DATE.toString(),
                END_DATE.plusDays(3).toString(),
                emptyList(),
                emptyList(),
                "ENGLISH",
                hearingData.getJudiciary(),
                JURISDICTION_TYPE,
                null,
                null,
                null,
                hearingData.getHasVideoLink(),
                hearingData.getPublicListNote(),
                "High",
                null,
                null,
                false,
                null
        );


        final UpdateHearingSteps updateHearingStepsWithoutNonDefaultDaysShouldPreservePrevRoomChange = new UpdateHearingSteps(hearingsData, updatedHearingDataWithoutNonDefaultDaysShouldPreservePrevRoomChange);
        updateHearingStepsWithoutNonDefaultDaysShouldPreservePrevRoomChange.whenHearingIsUpdatedForListing();

        // Then: Verify the hearing is still allocated in Crown Court
        updateHearingStepsWithoutNonDefaultDaysShouldPreservePrevRoomChange.verifyHearingUpdatedWhenQueryingFromAPICourtCalendar();
        LOGGER.info("Successfully verified Crown Court hearing remains allocated after duration change");

        verifyHearingDaysWithSpecificCourtRoomAndEmptyNonDefaultDays(updateHearingStepsWithoutNonDefaultDaysShouldPreservePrevRoomChange, HEARING_ID, 0, COURT_ROOM_ID2, "2025-08-18", "2025-08-19");
        verifyHearingDaysWithSpecificCourtRoomAndEmptyNonDefaultDays(updateHearingStepsWithoutNonDefaultDaysShouldPreservePrevRoomChange, HEARING_ID, 0, COURT_ROOM_ID, "2025-08-15", "2025-08-16",
                "2025-08-17", "2025-08-20", "2025-08-21", "2025-08-22");

        var userCreatedNonDefaultDaysPersisted = java.util.List.of(
                new uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData(
                        java.time.ZonedDateTime.of(2025, 8, 22, 10, 30, 0, 0, java.time.ZoneOffset.UTC).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")),
                        Optional.of(360), // 1 d in minutes
                        Optional.of(COURT_CENTRE_ID.toString()),
                        Optional.of(COURT_ROOM_ID2.toString())
                )
        );


        var updatedHearingDataWithUserCreatedPersistedNonDefaultDays = new uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData(
                HEARING_ID,
                COURT_CENTRE_ID,
                hearingData.getName(),
                COURT_ROOM_ID,
                hearingData.getHearingTypeData(),
                START_DATE.toString(),
                END_DATE.plusDays(3).toString(),
                userCreatedNonDefaultDaysPersisted,
                emptyList(),
                "ENGLISH",
                hearingData.getJudiciary(),
                JURISDICTION_TYPE,
                null,
                null,
                null,
                hearingData.getHasVideoLink(),
                hearingData.getPublicListNote(),
                "High",
                null,
                null,
                false,
                null
        );


        final UpdateHearingSteps updateHearingStepWithUserCreatedPersistedNonDefaultDays = new UpdateHearingSteps(hearingsData, updatedHearingDataWithUserCreatedPersistedNonDefaultDays);
        updateHearingStepWithUserCreatedPersistedNonDefaultDays.whenHearingIsUpdatedForListing();

        // Then: Verify the hearing is still allocated in Crown Court
        updateHearingStepWithUserCreatedPersistedNonDefaultDays.verifyHearingUpdatedWhenQueryingFromAPICourtCalendar();
        LOGGER.info("Successfully verified Crown Court hearing remains allocated after duration change");
        verifyHearingDaysWithSpecificCourtRoomAndEmptyNonDefaultDays(updateHearingStepWithUserCreatedPersistedNonDefaultDays, HEARING_ID, 1, COURT_ROOM_ID2, "2025-08-18", "2025-08-19", "2025-08-22");
        verifyHearingDaysWithSpecificCourtRoomAndEmptyNonDefaultDays(updateHearingStepWithUserCreatedPersistedNonDefaultDays, HEARING_ID, 1, COURT_ROOM_ID, "2025-08-15", "2025-08-16", "2025-08-17",
                "2025-08-20", "2025-08-21");
        // delete the user created hon default days above
        final List<uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData> anotherEmptyNonDefaultDays = emptyList();
        var updatedHearingDataWithAnotherEmptyNonDefaultDays = new uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData(
                HEARING_ID,
                COURT_CENTRE_ID,
                hearingData.getName(),
                COURT_ROOM_ID,
                hearingData.getHearingTypeData(),
                START_DATE.toString(),
                END_DATE.plusDays(3).toString(),
                anotherEmptyNonDefaultDays,
                emptyList(),
                "ENGLISH",
                hearingData.getJudiciary(),
                JURISDICTION_TYPE,
                null,
                null,
                null,
                hearingData.getHasVideoLink(),
                hearingData.getPublicListNote(),
                "High",
                null,
                null,
                false,
                null
        );


        final UpdateHearingSteps updateHearingStepsForAnotherEmptyNonDefaulsDays = new UpdateHearingSteps(hearingsData, updatedHearingDataWithAnotherEmptyNonDefaultDays);
        updateHearingStepsForAnotherEmptyNonDefaulsDays.whenHearingIsUpdatedForListing();

        // Then: Verify the hearing is still allocated in Crown Court
        updateHearingStepsForAnotherEmptyNonDefaulsDays.verifyHearingUpdatedWhenQueryingFromAPICourtCalendar();
        LOGGER.info("Successfully verified Crown Court hearing remains allocated after duration change");

        verifyHearingDaysWithSpecificCourtRoomAndEmptyNonDefaultDays(updateHearingStepsForAnotherEmptyNonDefaulsDays, HEARING_ID, 0, COURT_ROOM_ID2, "2025-08-18", "2025-08-19", "2025-08-22");
        verifyHearingDaysWithSpecificCourtRoomAndEmptyNonDefaultDays(updateHearingStepsForAnotherEmptyNonDefaulsDays, HEARING_ID, 0, COURT_ROOM_ID, "2025-08-15", "2025-08-16",
                "2025-08-17", "2025-08-20", "2025-08-21");

        // Search for hearings using court calendar endpoint with COURT_ROOM_ID2
        verifyCourtCalendarSearch(COURT_CENTRE_ID, COURT_ROOM_ID2, "2025-08-15", "2025-08-22", null, 3);

        // Search for hearings using court calendar endpoint with COURT_ROOM_ID
        verifyCourtCalendarSearch(COURT_CENTRE_ID, COURT_ROOM_ID, "2025-08-15", "2025-08-22", null, 5);

        verifyCourtCalendarSearch(COURT_CENTRE_ID, null, "2025-08-15", "2025-08-22", null, 8);

        // Search for hearings using court calendar endpoint with COURT_ROOM_ID and exactHearingStartDateTime
        verifyCourtCalendarSearch(COURT_CENTRE_ID, COURT_ROOM_ID, "2025-08-15", "2025-08-22", "2025-08-15T09:00:00.000Z", 1);

        // Search for hearings using court calendar endpoint with COURT_ROOM_ID and WRONG exactHearingStartDateTime
        verifyCourtCalendarSearch(COURT_CENTRE_ID, COURT_ROOM_ID, "2025-08-15", "2025-08-22", "2025-08-15T10:30:00.000Z", 0);


        // perform re-allocation by moving all days including the parent room to room03
        final List<uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData> reallocationToRoom3NonDefaultDays = new ArrayList<>();
        updateHearingStepsForAnotherEmptyNonDefaulsDays.
                getUpdatedHearingData().
                getNonDefaultDays().
                forEach(ndd -> reallocationToRoom3NonDefaultDays.add(new uk.gov.moj.cpp.listing.steps.data.NonDefaultDayData(
                        ndd.getStartTime(),
                        ndd.getDuration(),
                        ndd.getCourtScheduleId(),
                        ndd.getCourtRoomId(),
                        ndd.getOucode(),
                        ndd.getSession(),
                        ndd.getCourtCentreId(),
                        Optional.of(COURT_ROOM_ID3.toString()),
                        ndd.getVirtual()
                )));

        var updatedHearingDataForReallocationToRoom3NonDefaultDays = new uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData(
                HEARING_ID,
                COURT_CENTRE_ID,
                hearingData.getName(),
                COURT_ROOM_ID3,
                hearingData.getHearingTypeData(),
                START_DATE.toString(),
                END_DATE.plusDays(3).toString(),
                reallocationToRoom3NonDefaultDays,
                emptyList(),
                "ENGLISH",
                hearingData.getJudiciary(),
                JURISDICTION_TYPE,
                null,
                null,
                null,
                hearingData.getHasVideoLink(),
                hearingData.getPublicListNote(),
                "High",
                null,
                null,
                false,
                null
        );

        final UpdateHearingSteps updateHearingStepsForReallocationToRoom3NonDefaultDays = new UpdateHearingSteps(hearingsData, updatedHearingDataForReallocationToRoom3NonDefaultDays);
        updateHearingStepsForReallocationToRoom3NonDefaultDays.whenHearingIsUpdatedForListing();

        // Then: Verify the hearing is still allocated in Crown Court
        updateHearingStepsForReallocationToRoom3NonDefaultDays.verifyHearingUpdatedWhenQueryingFromAPICourtCalendar();
        // Search for hearings using court calendar endpoint with COURT_ROOM_ID3
        verifyCourtCalendarSearch(COURT_CENTRE_ID, COURT_ROOM_ID3, "2025-08-15", "2025-08-22", null, 8);

        LOGGER.info("Successfully verified Crown Court hearing remains allocated after duration change");
    }

    /**
     * Verifies that specific hearing days have the expected court room and that non-default days are empty
     */
    private void verifyHearingDaysWithSpecificCourtRoomAndEmptyNonDefaultDays(
            UpdateHearingSteps updateHearingSteps,
            UUID hearingId,
            int nonDefaultDaysSize,
            UUID expectedCourtRoomId,
            String... expectedDates) {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id.search-date"),
                        ALLOCATED,
                        COURT_CENTRE_ID,
                        expectedCourtRoomId,
                        expectedDates[0]));

        final String hearingIdFilter = SearchHearingHelper.getHearingFilter(hearingId.toString());

        // Verify that the hearing is found and has the expected court room for the specified dates
        SearchHearingHelper.pollForHearing(searchHearingUrl, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].id", equalTo(hearingId.toString())),
                withJsonPath("$.hearings[0].nonDefaultDays", hasSize(nonDefaultDaysSize)) // Verify empty nonDefaultDays
        });

        // Verify each expected date has the correct court room
        for (String expectedDate : expectedDates) {
            final String dateSpecificUrl = String.format("%s/%s", getBaseUri(),
                    format(readConfig().getProperty("listing.search.hearings.by.allocated.court-centre-id.court-room-id.search-date"),
                            ALLOCATED,
                            COURT_CENTRE_ID,
                            expectedCourtRoomId,
                            expectedDate));

            SearchHearingHelper.pollForHearing(dateSpecificUrl, getLoggedInUser().toString(), new Matcher[]{
                    withJsonPath(hearingIdFilter),
                    withJsonPath("$.hearings[0].hearingDays[?(@.hearingDate == '" + expectedDate + "')].courtRoomId",
                            hasItem(expectedCourtRoomId.toString())),
                    withJsonPath("$.hearings[0].hearingDays[?(@.hearingDate == '" + expectedDate + "')].courtCentreId",
                            hasItem(COURT_CENTRE_ID.toString()))
            });
        }

        LOGGER.info("Successfully verified hearing days on {} have courtRoom {} and empty nonDefaultDays",
                String.join(", ", expectedDates), expectedCourtRoomId);
    }

    /**
     * Verifies court calendar search returns expected number of hearings
     */
    private String verifyCourtCalendarSearch(UUID courtCentreId, UUID courtRoomId, String startDate, String endDate, final String exactHearingStartDateTime, int expectedHearingCount) {
        String searchUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.hearingscourt.calendar.by.allocated.court-centre-id.start-date.end-date"),
                        ALLOCATED,
                        courtCentreId,
                        startDate,
                        endDate)) + "&jurisdictionType=CROWN";

        if (courtRoomId != null) {
            searchUrl = searchUrl + "&courtRoomId=" + courtRoomId;
        }

        if (exactHearingStartDateTime != null) {
            searchUrl = searchUrl + "&exactHearingStartDateTime=" + exactHearingStartDateTime;
        }

        final String response = SearchHearingHelper.pollForHearing(searchUrl, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings", hasSize(expectedHearingCount))
        }, "application/vnd.listing.search.hearings.court.calendar+json");

        LOGGER.info(response);

        LOGGER.info("Successfully verified court calendar search returned {} hearings for courtCentreId={}, courtRoomId={}, startDate={}, endDate={}, jurisdictionType=CROWN",
                expectedHearingCount, courtCentreId, courtRoomId, startDate, endDate);

        return response;
    }
} 