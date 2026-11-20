package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.justice.services.common.converter.ZonedDateTimes.fromString;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedHearingData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

public class UpdateUnscheduledHearingSteps extends UpdateHearingSteps {
    public UpdateUnscheduledHearingSteps(final HearingsData hearingsData, final UpdatedHearingData updatedHearingData) {
        super(hearingsData, updatedHearingData);
    }

    public void verifyHearingIsNotUnscheduledListedFromAPI() {

        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.unscheduled-hearings"),
                        hearingData.getCourtCentreId()));

        pollForHearing(searchHearingUrl, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath("$.hearings", hasSize(0))
        });
    }
}
