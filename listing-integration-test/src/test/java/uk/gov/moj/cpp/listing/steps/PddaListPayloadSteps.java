package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDelayForJms;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetProsecutorPoliceFlag;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentreById;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCpCourtRooms;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataJudiciaries;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataXhibitCourtRoomMappings;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubOrganisationUnit;

import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import java.time.LocalTime;

/**
 * Steps for {@code application/vnd.listing.search.pdda.list.payload+json}, which is requested from the daily list
 * resource and returns the daily list plus linked cases, sitting notes, judge name parts and further court centre
 * address details.
 */
public class PddaListPayloadSteps extends AbstractIT {

    private static final String PDDA_MEDIA_TYPE = "application/vnd.listing.search.pdda.list.payload+json";
    private static final String DAILY_MEDIA_TYPE = "application/vnd.listing.search.daily.list.payload+json";
    private static final String DEFAULT_DURATION_HOURS_MINS = "6:30";
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(10, 0);
    private static final String EXPECTED_JUDICIARY_NAME = "Recorder Ainsworth judge";

    private static final String JUDICIARY = "$.courtLists[*].sittings[*].judiciary[*]";
    private static final String CREST_COURT_SITE = "$.courtLists[*].crestCourtSite";
    private static final String HEARINGS = "$.courtLists[*].sittings[*].hearings[*]";

    private final UpdatedHearingData updatedHearingData;
    private final NotesSteps notesSteps = new NotesSteps();

    public PddaListPayloadSteps(final HearingsData hearingsData, final UpdatedHearingData updatedHearingData) {
        this.updatedHearingData = updatedHearingData;
        final CourtCentreData courtCentreData = new CourtCentreData(
                updatedHearingData.getCourtCentreId(),
                DEFAULT_START_TIME,
                DEFAULT_DURATION_HOURS_MINS,
                updatedHearingData.getCourtRoomId(),
                "Test Crown Court");
        stubGetReferenceDataCourtMappings(courtCentreData);
        // serves the court centre record, with its postcode and address lines 3 to 5, to the query API
        stubGetReferenceDataCourtCentreById(courtCentreData);
        stubGetReferenceDataCpCourtRooms(updatedHearingData.getCourtRoomId(), 1970);
        stubGetReferenceDataXhibitCourtRoomMappings(updatedHearingData.getCourtRoomId());
        stubOrganisationUnit(updatedHearingData.getCourtCentreId());
        stubGetReferenceDataJudiciaries(updatedHearingData.getJudiciary().get(0).getJudicialId());
        stubGetProsecutorPoliceFlag(hearingsData.getHearingData().get(0).getListedCases().get(0).getAuthorityId());
    }

    public void verifyPddaListContainsTheDailyListDataAndJudiciaryNameParts(final String publishCourtListType) {
        pollWithDelayForJms(request(PDDA_MEDIA_TYPE, publishCourtListType))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtCentreId", is(updatedHearingData.getCourtCentreId().toString())),
                                withJsonPath("$.documentType", is(publishCourtListType)),
                                withJsonPath("$.courtLists[0].crestCourtSite", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].sittingDate", notNullValue()),
                                withJsonPath("$.courtLists[0].sittings[0].hearings[0].hearingType.id",
                                        is(updatedHearingData.getHearingTypData().getTypeId().toString())),
                                withJsonPath("$.courtLists[0].sittings[0].judiciary[0].judiciaryName", is(EXPECTED_JUDICIARY_NAME)),
                                withJsonPath("$.courtLists[0].sittings[0].judiciary[0].judiciaryTitle", is("Recorder")),
                                withJsonPath("$.courtLists[0].sittings[0].judiciary[0].judiciaryForenames", is("Mark J")),
                                withJsonPath("$.courtLists[0].sittings[0].judiciary[0].judiciarySurname", is("Ainsworth")),
                                withJsonPath("$.courtLists[0].sittings[0].judiciary[0].judiciaryTitleSuffix", is("judge")),
                                withJsonPath("$.courtLists[0].sittings[0].judiciary[0].welshJudiciaryTitle", is("Recorder"))
                        )));
    }

    public void verifyPddaListContainsCourtCentreAddressDetails(final String publishCourtListType) {
        pollWithDelayForJms(request(PDDA_MEDIA_TYPE, publishCourtListType))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtLists[0].crestCourtSite.courtCentreAddress3", is("Port Talbot")),
                                withJsonPath("$.courtLists[0].crestCourtSite.courtCentreAddress4", is("West Glam.")),
                                withJsonPath("$.courtLists[0].crestCourtSite.courtCentreAddress5", is("address line 5")),
                                withJsonPath("$.courtLists[0].crestCourtSite.courtCentrePostcode", is("SA13 1RU")),
                                withJsonPath("$.courtLists[0].crestCourtSite.welshCourtCentreAddress3", is("Adeilad y Llys")),
                                withJsonPath("$.courtLists[0].crestCourtSite.welshCourtCentreAddress4", is("Gorllewin Morgannwgc")),
                                withJsonPath("$.courtLists[0].crestCourtSite.welshCourtCentreAddress5", is("Caerdydd"))
                        )));
    }

    public void createSittingNote(final String noteDescription) {
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
        notesSteps.createNoteForListing(updatedHearingData.getCourtRoomId(), updatedHearingData.getStartDate(), noteDescription);
    }

    public void verifyPddaListContainsSittingNote(final String publishCourtListType, final String noteDescription) {
        pollWithDelayForJms(request(PDDA_MEDIA_TYPE, publishCourtListType))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath("$.courtLists[0].sittings[0].sittingNote", is(noteDescription))));
    }

    /**
     * Polls briefly, so that a caller can re-publish the link event and call this again until the read model has it.
     */
    public void verifyPddaListContainsLinkedCases(final String publishCourtListType, final String... linkedCaseUrns) {
        pollWithDefaults(request(PDDA_MEDIA_TYPE, publishCourtListType))
                .until(
                        status().is(OK),
                        payload().isJson(withJsonPath(HEARINGS + ".linkedCases[*].caseUrn", hasItems(linkedCaseUrns))));
    }

    /**
     * The daily list media type must stay as it was: none of the additional data may appear in it.
     */
    public void verifyDailyListHasNoAdditionalData(final String publishCourtListType) {
        pollWithDelayForJms(request(DAILY_MEDIA_TYPE, publishCourtListType))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.courtLists[0].sittings[0].judiciary[0].judiciaryName", is(EXPECTED_JUDICIARY_NAME)),
                                withJsonPath(JUDICIARY + ".judiciaryTitle", empty()),
                                withJsonPath(JUDICIARY + ".judiciaryForenames", empty()),
                                withJsonPath(JUDICIARY + ".judiciarySurname", empty()),
                                withJsonPath(JUDICIARY + ".judiciaryTitleSuffix", empty()),
                                withJsonPath(JUDICIARY + ".welshJudiciaryTitle", empty()),
                                withJsonPath(CREST_COURT_SITE + ".courtCentrePostcode", empty()),
                                withJsonPath(CREST_COURT_SITE + ".courtCentreAddress3", empty()),
                                withJsonPath(CREST_COURT_SITE + ".welshCourtCentreAddress3", empty()),
                                withJsonPath("$.courtLists[*].sittings[*].sittingNote", empty()),
                                withJsonPath(HEARINGS + ".linkedCases", empty())
                        )));
    }

    private RequestParams request(final String mediaType, final String publishCourtListType) {
        final String url = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.search.daily.list.payload"),
                        updatedHearingData.getCourtCentreId(),
                        updatedHearingData.getStartDate(),
                        publishCourtListType));
        return requestParams(url, mediaType).withHeader(USER_ID, getLoggedInUser()).build();
    }
}
