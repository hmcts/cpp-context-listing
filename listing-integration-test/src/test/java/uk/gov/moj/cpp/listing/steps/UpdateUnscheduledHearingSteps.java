package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;

public class UpdateUnscheduledHearingSteps extends UpdateHearingSteps {
    public UpdateUnscheduledHearingSteps(final HearingsData hearingsData, final UpdatedHearingData updatedHearingData) {
        super(hearingsData, updatedHearingData);
    }

    @Override
    protected void verifyHearingAllocatedEvent() {
        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerHearingAllocatedForListing);
        LOGGER.info("jsonResponse from privateMessageConsumerHearingAllocatedForListing: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("hearingId"), is(updatedHearingData.getHearingId().toString()));
        assertThat(jsonResponse.get("jurisdictionType"), is(updatedHearingData.getJurisdictionType()));
        assertThat(jsonResponse.get("courtRoomId"), is(updatedHearingData.getCourtRoomId().toString()));
        assertThat(jsonResponse.get("courtCentreId"), is(updatedHearingData.getCourtCentreId().toString()));
        assertThat(jsonResponse.get("type.description"), is(updatedHearingData.getHearingTypData().getTypeDescription()));

        assertThat(jsonResponse.get("judiciary[0].judicialId"), is(updatedHearingData.getJudiciary().get(0).getJudicialId().toString()));
        assertThat(jsonResponse.get("judiciary[0].judicialRoleType.judiciaryType"), is(updatedHearingData.getJudiciary().get(0).getJudicialRoleType().getJudiciaryType()));
        assertThat(jsonResponse.get("judiciary[0].isDeputy"), is(updatedHearingData.getJudiciary().get(0).getIsDeputy().get()));
        assertThat(jsonResponse.get("judiciary[0].isBenchChairman"), is(updatedHearingData.getJudiciary().get(0).getIsBenchChairman().get()));

        assertThat(fromString(jsonResponse.get("hearingDays[0].startTime")).toString(),
                is(fromString(updatedHearingData.getNonDefaultDays().get(0).getStartTime()).toString()));
        assertThat(jsonResponse.get("estimatedMinutes"), is(390));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].id"),
                is(hearingData.getListedCases().get(0).getCaseId().toString()));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].id"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getDefendantId().toString()));

        assertThat(jsonResponse.get("prosecutionCaseDefendantsOffenceIds[0].defendants[0].offenceIds[0]"),
                is(hearingData.getListedCases().get(0).getDefendants().get(0).getOffences().get(0).getOffenceId().toString()));

    }

    public void verifyHearingIsNotUnscheduledListedFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.unscheduled-hearings"),
                        hearingData.getCourtCentreId()));

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(Response.Status.OK),
                        payload().isJson(
                                withJsonPath("hearings", hasSize(0))
                        )

                );


    }
}
