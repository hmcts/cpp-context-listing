package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Boolean.FALSE;
import static java.text.MessageFormat.format;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.getHearingFilter;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_DURATION_HOURS_MINS;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.DEFAULT_START_TIME;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING;
import static uk.gov.moj.cpp.listing.steps.UpdateHearingSteps.MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING;
import static uk.gov.moj.cpp.listing.utils.JsonObjectBuilderHelper.prepareJsonForUpdatedHearingData;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.ReferenceDataStub.stubGetReferenceDataCourtCentre;

import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.CourtCentreData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;

import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class WeekCommencingHearingSteps extends AbstractIT {

    private final UpdatedHearingData updatedHearingData;

    private String request;

    public WeekCommencingHearingSteps(final UpdatedHearingData updatedHearingData) {
        this.updatedHearingData = updatedHearingData;
        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenHearingIsUpdatedForListingForWeekCommencingDate() {
        stubGetReferenceDataCourtCentre(new CourtCentreData(updatedHearingData.getCourtCentreId(), DEFAULT_START_TIME, DEFAULT_DURATION_HOURS_MINS, updatedHearingData.getCourtRoomId(), updatedHearingData.getName()));

        final String updateHearingUrl = String.format("%s/%s", getBaseUri(), format
                (readConfig().getProperty(LISTING_COMMAND_UPDATE_HEARING_FOR_LISTING), updatedHearingData.getHearingId()));

        request = prepareJsonForUpdatedHearingData(updatedHearingData);

        try (Response response = restClient.postCommand(updateHearingUrl, MEDIA_TYPE_UPDATE_HEARING_FOR_LISTING,  request, getLoggedInHeader())) {

            String responseBody = "";
            try {
                responseBody = response.readEntity(String.class);
            } catch (IllegalStateException e) {
                //no-op in case of no response
            }
            assertThat(format("Post returned not expected status code with body: %s", responseBody),
                    response.getStatus(), is(SC_ACCEPTED));
        }
    }

    public void verifyHearingUpdatedWithWeekCommencingDateAndUnallocatedWhenQueryingFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings.by.week.commencing"), updatedHearingData.getWeekCommencingStartDate(), updatedHearingData.getWeekCommencingEndDate(), updatedHearingData.getCourtCentreId(), FALSE));

        final String hearingIdFilter = getHearingFilter(updatedHearingData.getHearingId().toString());
        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter + ".jurisdictionType",
                        hasItem(updatedHearingData.getJurisdictionType())),
                withJsonPath(hearingIdFilter + ".courtCentreId",
                        hasItem(updatedHearingData.getCourtCentreId().toString())),
                withJsonPath(hearingIdFilter + ".type.id",
                        hasItem(updatedHearingData.getHearingTypData().getTypeId().toString())),
                withJsonPath(hearingIdFilter + ".startDate", hasSize(0)),
                withJsonPath(hearingIdFilter + ".endDate", hasSize(0)),
                withJsonPath(hearingIdFilter + ".weekCommencingStartDate",
                        hasItem(updatedHearingData.getWeekCommencingStartDate())),
                withJsonPath(hearingIdFilter + ".weekCommencingEndDate",
                        hasItem(updatedHearingData.getWeekCommencingEndDate())),
                withJsonPath(hearingIdFilter + ".weekCommencingDurationInWeeks",
                        hasItem(updatedHearingData.getWeekCommencingDurationInWeeks().toString()))
        });
    }
}
