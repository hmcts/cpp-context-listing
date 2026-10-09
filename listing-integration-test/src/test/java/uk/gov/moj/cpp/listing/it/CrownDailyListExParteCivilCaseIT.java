package uk.gov.moj.cpp.listing.it;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData.updatedHearingDataForCrownAllocation;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessionsWithMultipleSchedules;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubProvisionalBookingWithCustomParams;
import static uk.gov.moj.cpp.listing.utils.ProgressionCivilCaseStub.stubProgressionCivilCaseWithExParte;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.CrownDailyListExParteSteps;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.UpdateHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * CAD-1760: civil cases with an ex-parte offence, and applications linked to them, do not appear on
 * the public facing Crown DRAFT/FINAL lists (the Daily list download is a DRAFT request).
 */
public class CrownDailyListExParteCivilCaseIT extends AbstractIT {

    private static final UUID CROWN_COURT_CENTRE_ID = fromString("b52f805c-2821-4904-a0e0-26f7fda6dd08");
    private static final UUID CROWN_COURT_ROOM_ID = fromString("1d0199f8-8812-48a2-b13c-837e1c03ff19");

    private HearingsData civilCasesHearingsData;
    private UpdatedHearingData allocation;
    private CrownDailyListExParteSteps steps;

    @BeforeEach
    public void setUp() {
        super.setUp();
        // Each hearing lists a civil case with ex-parte offences on every defendant (AC1, AC3) and a
        // civil case with a single non ex-parte offence (AC2).
        civilCasesHearingsData = HearingsData.hearingsDataWithExParteOffence();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(civilCasesHearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);

        allocation = updatedHearingDataForCrownAllocation(civilCasesHearingsData.getHearingData().get(0).getId());
        stubListHearingInCourtSessionsWithMultipleSchedules(
                civilCasesHearingsData.getHearingData().get(0).getId().toString(),
                allocation.getNonDefaultDays().get(0).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                allocation.getNonDefaultDays().get(1).getCourtScheduleId().map(UUID::fromString).orElse(null).toString(),
                ZonedDateTime.parse(allocation.getNonDefaultDays().get(0).getStartTime()),
                allocation.getNonDefaultDays().get(0).getDuration().orElse(20));

        final UpdateHearingSteps updateHearingSteps = new UpdateHearingSteps(civilCasesHearingsData, allocation);
        updateHearingSteps.whenHearingIsUpdatedForListing();
        updateHearingSteps.verifyHearingAllocatedWhenQueryingFromAPIWithJmsDelay();

        steps = new CrownDailyListExParteSteps(civilCasesHearingsData, allocation);
    }

    // AC1, AC2, AC3
    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "FINAL"})
    void shouldNotShowCivilCaseWithExParteOffence(final String publishCourtListType) {
        steps.verifyDailyListShowsCaseButNotExParteCase(allocation.getCourtCentreId(), allocation.getStartDate(), publishCourtListType,
                nonExParteCase().getCaseId(), exParteCase().getCaseId());
    }

    // AC5, AC6 - linked civil case listed in Listing
    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "FINAL"})
    void shouldNotShowApplicationLinkedToExParteCivilCase(final String publishCourtListType) {
        verifyApplications(publishCourtListType, exParteCase().getCaseId(), nonExParteCase().getCaseId());
    }

    // AC5, AC6 - linked civil case Listing has never listed, resolved from Progression
    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "FINAL"})
    void shouldNotShowApplicationLinkedToExParteCivilCaseKnownOnlyToProgression(final String publishCourtListType) {
        final UUID exParteCaseId = randomUUID();
        final UUID nonExParteCaseId = randomUUID();
        stubProgressionCivilCaseWithExParte(exParteCaseId, true);
        stubProgressionCivilCaseWithExParte(nonExParteCaseId, false);

        verifyApplications(publishCourtListType, exParteCaseId, nonExParteCaseId);
    }

    private void verifyApplications(final String publishCourtListType, final UUID exParteLinkedCaseId, final UUID nonExParteLinkedCaseId) {
        final LocalDate hearingDate = ItClock.today();
        final ZonedDateTime hearingStartTime = ItClock.nowUtc().withHour(10).withMinute(0).withSecond(0).withNano(0);
        stubOrganisationUnit(CROWN_COURT_CENTRE_ID);
        stubGetReferenceDataCourtMappings(new CourtCentreData(CROWN_COURT_CENTRE_ID, LocalTime.of(10, 0), "6:30", CROWN_COURT_ROOM_ID, "Test Crown Court"));

        // listed and confirmed first, so its absence from the list is not a race with its listing
        final HearingsData hiddenApplication = applicationWithSubjectLinkedTo(exParteLinkedCaseId);
        listStandaloneApplication(hiddenApplication, hearingDate, hearingStartTime);
        new ListCourtHearingSteps(hiddenApplication).verifyHearingListedFromAPIForStandaloneApplication(ALLOCATED);

        final HearingsData shownApplication = applicationWithSubjectLinkedTo(nonExParteLinkedCaseId);
        listStandaloneApplication(shownApplication, hearingDate, hearingStartTime);

        steps.verifyDailyListShowsApplicationButNotHiddenApplication(CROWN_COURT_CENTRE_ID, hearingDate, publishCourtListType,
                subjectFirstName(shownApplication), subjectFirstName(hiddenApplication));
    }

    private ListedCaseData exParteCase() {
        return civilCasesHearingsData.getHearingData().get(0).getListedCases().get(0);
    }

    private ListedCaseData nonExParteCase() {
        return civilCasesHearingsData.getHearingData().get(0).getListedCases().get(1);
    }

    // A Crown daily list renders an application through its PERSON_DEFENDANT subject - an application
    // without one has no defendants and is dropped from the list whatever its ex-parte status.
    private static HearingsData applicationWithSubjectLinkedTo(final UUID linkedCaseId) {
        final HearingsData applicationHearingsData = HearingsData.hearingsDataStandaloneApplicationWithSubject();
        setField(applicationHearingsData.getHearingData().get(0).getCourtApplications().get(0), "linkedCaseId", linkedCaseId);
        return applicationHearingsData;
    }

    private static String subjectFirstName(final HearingsData applicationHearingsData) {
        return applicationHearingsData.getHearingData().get(0).getCourtApplications().get(0).getSubject().getFirstName();
    }

    private void listStandaloneApplication(final HearingsData applicationHearingsData, final LocalDate hearingDate, final ZonedDateTime hearingStartTime) {
        final HearingData hearing = applicationHearingsData.getHearingData().get(0);
        final String courtScheduleId = randomUUID().toString();

        setField(hearing, "courtCentreId", CROWN_COURT_CENTRE_ID);
        setField(hearing, "courtRoomId", CROWN_COURT_ROOM_ID);
        setField(hearing, "hearingStartDate", hearingDate);
        setField(hearing, "hearingEndDate", hearingDate);
        setField(hearing, "hearingStartTime", hearingStartTime);
        hearing.setName("Test Crown Court");

        stubGetReferenceDataHearingTypes(hearing.getHearingTypeData().getTypeId());
        final Map<String, String> bookingParams = new HashMap<>();
        bookingParams.put("SESSION_DATE", hearingDate.toString());
        bookingParams.put("COURT_CENTRE_ID", CROWN_COURT_CENTRE_ID.toString());
        bookingParams.put("COURT_SCHEDULE_ID", courtScheduleId);
        bookingParams.put("COURT_ROOM_ID", CROWN_COURT_ROOM_ID.toString());
        bookingParams.put("BOOKING_ID", randomUUID().toString());
        bookingParams.put("HEARING_START_TIME", hearingStartTime.toString());
        stubProvisionalBookingWithCustomParams(bookingParams);
        stubListHearingInCourtSessions(hearing.getId().toString(), courtScheduleId, hearingStartTime);

        new ListCourtHearingSteps(applicationHearingsData).whenCaseIsSubmittedForListingStandaloneApplication();
    }

    private static void setField(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set " + fieldName, e);
        }
    }
}
