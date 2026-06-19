package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDelayForJms;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalTime;

public class DailyListPayloadSteps extends AbstractIT {

    private static final String MEDIA_TYPE = "application/vnd.listing.search.daily.list.payload+json";
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 0);

    private final UpdatedHearingData updatedHearingData;

    public DailyListPayloadSteps(final UpdatedHearingData updatedHearingData) {
        this.updatedHearingData = updatedHearingData;
        stubGetReferenceDataCourtMappings(new CourtCentreData(
                updatedHearingData.getCourtCentreId(),
                DEFAULT_START_TIME,
                DEFAULT_DURATION_HOURS_MINS,
                updatedHearingData.getCourtRoomId(),
                "Test Crown Court"));
        stubGetReferenceDataCpCourtRooms(updatedHearingData.getCourtRoomId(), 1970);
        stubGetReferenceDataXhibitCourtRoomMappings(updatedHearingData.getCourtRoomId());
        stubOrganisationUnit(updatedHearingData.getCourtCentreId());
    }

    public void verifyDailyListPayloadContainsHearing(final String publishCourtListType) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.daily.list.payload"),
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(),
                        publishCourtListType));

        pollWithDelayForJms(requestParams(url, MEDIA_TYPE).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(updatedHearingData.getCourtCentreId().toString())),
                                withJsonPath("$.courtLists", notNullValue()),
                                withJsonPath("$.courtLists[0].crestCourtSite", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].sittingDate", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].hearings", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].hearings[0].startTime", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].hearings[0].hearingType.id",
                                        is(updatedHearingData.getHearingTypData().getTypeId().toString()))
                        )));
    }

    public void verifyWeekCommencingListPayloadContainsHearing(final String publishCourtListType, final String weekCommencingEndDate) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.daily.list.week.commencing.payload"),
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(),
                        publishCourtListType,
                        weekCommencingEndDate));

        pollWithDelayForJms(requestParams(url, MEDIA_TYPE).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(updatedHearingData.getCourtCentreId().toString())),
                                withJsonPath("$.weekCommencingStartDate", is(updatedHearingData.getStartDate())),
                                withJsonPath("$.weekCommencingEndDate", is(weekCommencingEndDate)),
                                withJsonPath("$.courtLists", notNullValue()),
                                withJsonPath("$.courtLists[0].crestCourtSite", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].sittingDate", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].hearings", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].hearings[0].startTime", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].hearings[0].hearingType.id",
                                        is(updatedHearingData.getHearingTypData().getTypeId().toString()))
                        )));
    }
}
