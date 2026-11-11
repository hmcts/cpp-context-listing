package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDelayForJms;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetProsecutorPoliceFlag;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * CAD-1760: Crown DRAFT/FINAL daily list payload assertions for civil ex-parte suppression. Each
 * check anchors on something that must be present before asserting that the ex-parte entry is
 * absent, so absence is never asserted against a list that has not been built yet.
 */
public class CrownDailyListExParteSteps extends AbstractIT {

    private static final String MEDIA_TYPE = "application/vnd.listing.search.daily.list.payload+json";
    private static final String DAILY_LIST_PAYLOAD = "listing.search.daily.list.payload";

    public CrownDailyListExParteSteps(final HearingsData hearingsData, final UpdatedHearingData updatedHearingData) {
        stubGetReferenceDataCourtMappings(new CourtCentreData(updatedHearingData.getCourtCentreId(), LocalTime.of(10, 0), "6:30",
                updatedHearingData.getCourtRoomId(), "Test Crown Court"));
        stubGetReferenceDataCpCourtRooms(updatedHearingData.getCourtRoomId(), 1970);
        stubGetReferenceDataXhibitCourtRoomMappings(updatedHearingData.getCourtRoomId());
        stubOrganisationUnit(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataJudiciaries(updatedHearingData.getJudiciary().get(0).getJudicialId());
        stubGetProsecutorPoliceFlag(hearingsData.getHearingData().get(0).getListedCases().get(0).getAuthorityId());
    }

    public void verifyDailyListShowsCaseButNotExParteCase(final UUID courtCentreId, final String startDate, final String publishCourtListType,
                                                          final UUID shownCaseId, final UUID exParteCaseId) {
        pollWithDelayForJms(requestParams(dailyListUrl(courtCentreId, startDate, publishCourtListType), MEDIA_TYPE)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtLists[*].sittings[*].hearings[*].defendants[*].prosecutionCaseId", hasItem(shownCaseId.toString())),
                                withJsonPath("$.courtLists[*].sittings[*].hearings[*].defendants[*].prosecutionCaseId", not(hasItem(exParteCaseId.toString())))
                        )));
    }

    public void verifyDailyListShowsApplicationButNotHiddenApplication(final UUID courtCentreId, final LocalDate startDate, final String publishCourtListType,
                                                                       final String shownSubjectFirstName, final String hiddenSubjectFirstName) {
        pollWithDelayForJms(requestParams(dailyListUrl(courtCentreId, startDate.toString(), publishCourtListType), MEDIA_TYPE)
                .withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(courtCentreId.toString())),
                                withJsonPath("$.courtLists[*].sittings[*].hearings[?(@.applicationReference)].subject.firstName", hasItem(shownSubjectFirstName)),
                                withJsonPath("$.courtLists[*].sittings[*].hearings[?(@.applicationReference)].subject.firstName", not(hasItem(hiddenSubjectFirstName)))
                        )));
    }

    private static String dailyListUrl(final UUID courtCentreId, final String startDate, final String publishCourtListType) {
        return String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(DAILY_LIST_PAYLOAD), courtCentreId, startDate, publishCourtListType));
    }
}
