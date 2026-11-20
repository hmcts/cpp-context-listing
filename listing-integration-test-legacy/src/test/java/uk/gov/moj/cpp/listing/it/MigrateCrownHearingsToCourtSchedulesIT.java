package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.justice.core.courts.JurisdictionType.CROWN;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubGetCourtSchedulesByIdWithDraftStatus;
import static uk.gov.moj.cpp.listing.utils.CourtSchedulerServiceStub.stubListHearingInCourtSessions;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtCenterId;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.getRandomCourtRoomId;

import uk.gov.moj.cpp.listing.helper.SearchHearingHelper;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.CaseAndDefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.Json;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

public class MigrateCrownHearingsToCourtSchedulesIT extends AbstractIT {

    private static final String MIGRATE_COMMAND_URL = "listing.migrate-crown-hearings-to-courtschedules";
    private static final String MIGRATE_MEDIA_TYPE = "application/vnd.listing.migrate-crown-hearings-to-courtschedules+json";

    private static final String SEEDED_COURT_SCHEDULE_ID = "8e837de0-743a-4a2c-9db3-b2e678c48729";

    private final UUID hearingId = randomUUID();
    private final UUID courtCentreId = getRandomCourtCenterId();
    private final UUID courtRoomId = getRandomCourtRoomId();
    private final LocalDate hearingDate = LocalDate.of(2027, 8, 17);
    private final ZonedDateTime hearingStartTime = ZonedDateTime.of(2027, 8, 17, 10, 30, 0, 0, ZoneOffset.UTC);

    @Test
    public void shouldMigrateOnlyCourtScheduleIdOnTheMatchingHearingDay() {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);

        final CaseAndDefendantData caseAndDefendantData = new CaseAndDefendantData(
                hearingId, null, "CASE_URN_MIGRATE", randomUUID(), null, CROWN.name(), CROWN.name(), null, null);

        final HearingsData hearingsData = HearingsData.hearingsDataWithAllocationDataAndJudiciary(
                caseAndDefendantData, courtCentreId, courtRoomId, hearingDate, hearingStartTime);

        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        stubGetCourtSchedulesByIdWithDraftStatus(singletonList(SEEDED_COURT_SCHEDULE_ID), false);
        stubListHearingInCourtSessions(hearingId.toString(), SEEDED_COURT_SCHEDULE_ID, hearingStartTime);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingIsCreated(hearingId, 1);
        listCourtHearingSteps.verifyHearingListedFromAPI(true);

        final UUID newCourtScheduleId = randomUUID();
        final Response response = restClient.postCommand(migrateCommandUrl(), MIGRATE_MEDIA_TYPE,
                migratePayload(newCourtScheduleId), getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));

        final String hearingFilter = SearchHearingHelper.getHearingFilter(hearingId.toString());
        SearchHearingHelper.pollForHearing(courtCentreId.toString(), ALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingFilter + ".hearingDays[?(@.hearingDate == '" + hearingDate + "')].courtScheduleId",
                        hasItem(newCourtScheduleId.toString())),
                withJsonPath(hearingFilter + ".hearingDays[?(@.hearingDate == '" + hearingDate + "')].courtRoomId",
                        hasItem(courtRoomId.toString())),
                withJsonPath(hearingFilter + ".hearingDays[?(@.hearingDate == '" + hearingDate + "')].courtCentreId",
                        hasItem(courtCentreId.toString()))
        });
    }

    private String migrateCommandUrl() {
        return String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(MIGRATE_COMMAND_URL)));
    }

    private String migratePayload(final UUID newCourtScheduleId) {
        return Json.createObjectBuilder()
                .add("hearings", Json.createArrayBuilder()
                        .add(Json.createObjectBuilder()
                                .add("hearingId", hearingId.toString())
                                .add("hearingDayCourtSchedules", Json.createArrayBuilder()
                                        .add(Json.createObjectBuilder()
                                                .add("hearingDate", hearingDate.toString())
                                                .add("courtScheduleId", newCourtScheduleId.toString())))))
                .build()
                .toString();
    }
}
