package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDelayForJms;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetProsecutorPoliceFlag;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataHearingTypes;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalDate;
import java.time.LocalTime;

public class PddaListPayloadSteps extends AbstractIT {

    private static final String MEDIA_TYPE = "application/vnd.listing.search.pdda.list.payload+json";
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 0);

    private final UpdatedHearingData updatedHearingData;

    public PddaListPayloadSteps(final HearingsData hearingsData, final UpdatedHearingData updatedHearingData) {
        this.updatedHearingData = updatedHearingData;
        stubGetReferenceDataCourtMappings(new CourtCentreData(
                updatedHearingData.getCourtCentreId(),
                DEFAULT_START_TIME,
                DEFAULT_DURATION_HOURS_MINS,
                updatedHearingData.getCourtRoomId(),
                "Test Crown Court"));
        stubGetReferenceDataCourtCentreById(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataCpCourtRooms(updatedHearingData.getCourtRoomId(), 1970);
        stubGetReferenceDataXhibitCourtRoomMappings(updatedHearingData.getCourtRoomId());
        stubGetReferenceDataHearingTypes(updatedHearingData.getHearingTypData().getTypeId());
        stubOrganisationUnit(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataJudiciaries(updatedHearingData.getJudiciary().get(0).getJudicialId());
        stubGetProsecutorPoliceFlag(hearingsData.getHearingData().get(0).getListedCases().get(0).getAuthorityId());
    }

    /**
     * The list is the Xhibit document for the requested type, in the shape of the matching PDDA schema:
     * DRAFT and FINAL give a DailyList, WARN a WarnedList and FIRM a FirmList.
     */
    public void verifyPddaListPayloadIsReturned(final String publishCourtListType, final String rootElement, final LocalDate endDate) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.pdda.list.payload"),
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(),
                        endDate,
                        publishCourtListType));

        pollWithDelayForJms(requestParams(url, MEDIA_TYPE).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$." + rootElement + ".DocumentID.UniqueID", notNullValue()),
                                withJsonPath("$." + rootElement + ".DocumentID.DocumentType", notNullValue()),
                                withJsonPath("$." + rootElement + ".ListHeader.StartDate", is(updatedHearingData.getStartDate())),
                                withJsonPath("$." + rootElement + ".ListHeader.PublishedTime", notNullValue()),
                                withJsonPath("$." + rootElement + ".CrownCourt.CourtHouseName", notNullValue()),
                                withJsonPath("$." + rootElement + ".CrownCourt.CourtHouseCode", instanceOf(Integer.class)),
                                withJsonPath("$." + rootElement + ".CourtLists", notNullValue()),
                                withJsonPath("$." + rootElement + ".CourtLists[0].CourtHouse.CourtHouseCode", instanceOf(Integer.class))
                        )));
    }

    public void verifyPddaDailyListContainsTheListedHearing(final String publishCourtListType, final LocalDate endDate) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.pdda.list.payload"),
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(),
                        endDate,
                        publishCourtListType));

        pollWithDelayForJms(requestParams(url, MEDIA_TYPE).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.DailyList.CourtLists[*].Sittings[*].Hearings[*].CaseNumber", notNullValue()),
                                withJsonPath("$.DailyList.CourtLists[*].Sittings[*].Hearings[*].CaseNumberCaTH", notNullValue()),
                                withJsonPath("$.DailyList.CourtLists[*].Sittings[*].Hearings[*].HearingDetails.HearingDescription", notNullValue()),
                                withJsonPath("$.DailyList.CourtLists[*].Sittings[*].Hearings[*].Defendants[*].PersonalDetails.Name.CitizenNameSurname", notNullValue())
                        )));
    }
}
