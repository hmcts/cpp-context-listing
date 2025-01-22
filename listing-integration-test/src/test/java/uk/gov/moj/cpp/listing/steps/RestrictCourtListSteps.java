package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.RestrictCourtListData;

import java.util.Arrays;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import org.hamcrest.Matcher;


public class RestrictCourtListSteps extends AbstractIT {
    private static final String LISTING_COMMAND_RESTRICT_COURT_LIST = "listing.command.restrict-court-list";
    private static final String MEDIA_TYPE_RESTRICT_COURT_LIST = "application/vnd.listing.command.restrict-court-list+json";

    private String request;

    private final HearingsData hearingsData;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public RestrictCourtListSteps(HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenRestrictingCaseOrStandaloneApplicationForCourtListing(RestrictCourtListData restrictListingFromCourtData) {
        final JsonObject restrictCourtListDataObject = (JsonObject) objectToJsonValueConverter.convert(restrictListingFromCourtData);
        final String hearingRestrictUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_RESTRICT_COURT_LIST, hearingsData.getHearingData().get(0).getId().toString())));

        request = restrictCourtListDataObject.toString();
        final Response response = restClient.postCommand(hearingRestrictUrl, MEDIA_TYPE_RESTRICT_COURT_LIST,
                request, getLoggedInHeader());
        assertThat(response.getStatus(), equalTo(SC_ACCEPTED));
    }


    public void verifyCaseOrDefendantOrOffenceListingRestrictedInHearing(Boolean restrictCourtListingOfCase, Boolean restrictCourtListingOfDefendant, Boolean restrictCourtListingOfOffence) {
        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].restrictFromCourtList",
                        equalTo(restrictCourtListingOfCase)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList",
                        equalTo(restrictCourtListingOfDefendant)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].restrictFromCourtList",
                        equalTo(restrictCourtListingOfOffence))
        });
    }

    public void verifyListingRestrictedInHearing(Boolean restrictCourtListingOfCase, Boolean restrictCourtListingOfDefendant, Boolean restrictCourtListingOfOffence) {
        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), true, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getCaseId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getDefendantId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].restrictFromCourtList",
                        equalTo(restrictCourtListingOfCase)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList",
                        equalTo(restrictCourtListingOfDefendant)),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].offences[0].restrictFromCourtList",
                        equalTo(restrictCourtListingOfOffence)),
                hasNoJsonPath("hearing.listedCases[0].defendants[0].offences[0].reportingRestrictions[0]")

        });
    }

    public void verifyCourtApplicationOrApplicantOrRespondentListingRestrictedInHearing(Boolean restrictCourtListingOfCourtApplication, Boolean restrictCourtListingOfApplicant, Boolean restrictCourtListingOfRespondent, Boolean restrictCourtListingOfCourtApplicationType) {
        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{

                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].applicant.id",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getApplicant().getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRespondent().getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].restrictFromCourtList",
                        equalTo(restrictCourtListingOfCourtApplication)),
                withJsonPath("$.hearings[0].courtApplications[0].applicant.restrictFromCourtList",
                        equalTo(restrictCourtListingOfApplicant)),
                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].restrictFromCourtList",
                        equalTo(restrictCourtListingOfRespondent)),
                withJsonPath("$.hearings[0].courtApplications[0].restrictCourtApplicationType",
                        equalTo(restrictCourtListingOfCourtApplicationType))
        });
    }

    public RestrictCourtListData getRestrictListingFromCourtData(HearingsData hearingsData) {
        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingsData.getHearingData().get(0).getListedCases().get(0);

        return RestrictCourtListData.restrictCourtList()
                .withCaseIds(Arrays.asList(listedCaseData.getCaseId()))
                .withDefendantIds(Arrays.asList(listedCaseData.getDefendants().get(0).getDefendantId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public RestrictCourtListData getDefendantsAndOffencesDataToBeUnrestricted(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingsData.getHearingData().get(0).getListedCases().get(0);

        return RestrictCourtListData.restrictCourtList()
                .withOffenceIds(Arrays.asList(listedCaseData.getDefendants().get(0).getOffences().get(0).getOffenceId()))
                .withDefendantIds(Arrays.asList(listedCaseData.getDefendants().get(0).getDefendantId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(false)
                .build();
    }


    public RestrictCourtListData getDefendantsAndOffencesDataToBeRestricted(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);
        ListedCaseData listedCaseData = hearingsData.getHearingData().get(0).getListedCases().get(0);

        return RestrictCourtListData.restrictCourtList()
                .withOffenceIds(Arrays.asList(listedCaseData.getDefendants().get(0).getOffences().get(0).getOffenceId()))
                .withDefendantIds(Arrays.asList(listedCaseData.getDefendants().get(0).getDefendantId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public RestrictCourtListData getCourtApplicationDataToBeRestricted(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        return RestrictCourtListData.restrictCourtList()
                .withCourtApplicatonIds(Arrays.asList(courtApplicationData.getId()))
                .withCourtApplicationApplicantIds(Arrays.asList(courtApplicationData.getApplicant().getId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public RestrictCourtListData getCourtApplicationTypeToBeRestricted(HearingsData hearingsData) {

        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        return RestrictCourtListData.restrictCourtList()
                .withCourtApplicatonIds(Arrays.asList(courtApplicationData.getId()))
                .withCourtApplicationType(java.util.Optional.of((courtApplicationData.getType())))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

}

