package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
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
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;


public class RestrictCourtListSteps extends AbstractIT {
    private static final String LISTING_COMMAND_RESTRICT_COURT_LIST = "listing.command.restrict-court-list";
    private static final String MEDIA_TYPE_RESTRICT_COURT_LIST = "application/vnd.listing.command.restrict-court-list+json";
    private static final String PUBLIC_EVENT_COURT_LIST_RESTRICTED = "public.listing.court-list-restricted";

    private String request;

    private final HearingsData hearingsData;
    private final JmsMessageConsumerClient publicMessageConsumerCourtListRestricted;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public RestrictCourtListSteps(HearingsData hearingsData) {
        this.hearingsData = hearingsData;
        this.publicMessageConsumerCourtListRestricted = publicEvents.createPublicConsumer(PUBLIC_EVENT_COURT_LIST_RESTRICTED);

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

    public RestrictCourtListData getCourtApplicationRespondentDataToBeRestricted(HearingsData hearingsData) {
        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        return RestrictCourtListData.restrictCourtList()
                .withCourtApplicatonRespondentIds(Arrays.asList(courtApplicationData.getRespondent().getId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public RestrictCourtListData getCourtApplicationDataToBeUnrestricted(HearingsData hearingsData) {
        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        return RestrictCourtListData.restrictCourtList()
                .withCourtApplicatonIds(Arrays.asList(courtApplicationData.getId()))
                .withCourtApplicationApplicantIds(Arrays.asList(courtApplicationData.getApplicant().getId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(false)
                .build();
    }

    public RestrictCourtListData getCourtApplicationApplicantAndRespondentDataToBeRestricted(HearingsData hearingsData) {
        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        return RestrictCourtListData.restrictCourtList()
                .withCourtApplicatonIds(Arrays.asList(courtApplicationData.getId()))
                .withCourtApplicationApplicantIds(Arrays.asList(courtApplicationData.getApplicant().getId()))
                .withCourtApplicatonRespondentIds(Arrays.asList(courtApplicationData.getRespondent().getId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public RestrictCourtListData getCourtApplicationSubjectDataToBeRestricted(HearingsData hearingsData) {
        HearingData hearingData = hearingsData.getHearingData().get(0);
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        return RestrictCourtListData.restrictCourtList()
                .withCourtApplicationSubjectIds(Arrays.asList(courtApplicationData.getSubject().getId()))
                .withHearingId(hearingData.getId())
                .withRestrictCourtList(true)
                .build();
    }

    public void verifyPublicCourtListRestrictedEvent(final Boolean restrictCourtList) {
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerCourtListRestricted,
                isJson(allOf(withJsonPath("$.restrictCourtList", equalTo(restrictCourtList)),
                        withJsonPath("$.hearingId", equalTo(hearingsData.getHearingData().get(0).getId().toString())))));
        assertThat(jsonResponse, notNullValue());
        assertThat(jsonResponse.get("hearingId"), is(hearingsData.getHearingData().get(0).getId().toString()));
    }

    public void verifyPublicCourtListRestrictedEventWithApplicant(final Boolean restrictCourtList) {
        final CourtApplicationData courtApplication = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerCourtListRestricted,
                isJson(allOf(withJsonPath("$.restrictCourtList", equalTo(restrictCourtList)),
                        withJsonPath("$.hearingId", equalTo(hearingsData.getHearingData().get(0).getId().toString())))));
        assertThat(jsonResponse, notNullValue());
        assertThat(jsonResponse.get("hearingId"), is(hearingsData.getHearingData().get(0).getId().toString()));
        assertThat(jsonResponse.get("courtApplicationApplicantIds[0]"), is(courtApplication.getApplicant().getId().toString()));
    }

    public void verifyPublicCourtListRestrictedEventWithApplicantAndRespondent(final Boolean restrictCourtList) {
        final CourtApplicationData courtApplication = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerCourtListRestricted,
                isJson(allOf(withJsonPath("$.restrictCourtList", equalTo(restrictCourtList)),
                        withJsonPath("$.hearingId", equalTo(hearingsData.getHearingData().get(0).getId().toString())))));
        assertThat(jsonResponse, notNullValue());
        assertThat(jsonResponse.get("hearingId"), is(hearingsData.getHearingData().get(0).getId().toString()));
        assertThat(jsonResponse.get("courtApplicationApplicantIds[0]"), is(courtApplication.getApplicant().getId().toString()));
        assertThat(jsonResponse.get("courtApplicationRespondentIds[0]"), is(courtApplication.getRespondent().getId().toString()));
    }

    public void verifyPublicCourtListRestrictedEventWithSubject(final Boolean restrictCourtList) {
        final CourtApplicationData courtApplication = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        final JsonPath jsonResponse = retrieveMessage(publicMessageConsumerCourtListRestricted,
                isJson(allOf(withJsonPath("$.restrictCourtList", equalTo(restrictCourtList)),
                        withJsonPath("$.hearingId", equalTo(hearingsData.getHearingData().get(0).getId().toString())))));
        assertThat(jsonResponse, notNullValue());
        assertThat(jsonResponse.get("hearingId"), is(hearingsData.getHearingData().get(0).getId().toString()));
        assertThat(jsonResponse.get("courtApplicationSubjectIds[0]"), is(courtApplication.getSubject().getId().toString()));
    }

    public void verifyCourtApplicationSubjectListingRestrictedInHearing(final Boolean restrictCourtListingOfSubject) {
        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].subject.id",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getSubject().getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].subject.restrictFromCourtList",
                        equalTo(restrictCourtListingOfSubject))
        });
    }

}

