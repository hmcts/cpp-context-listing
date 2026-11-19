package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.steps.data.HearingsData.hearingsData;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.services.test.utils.core.http.ResponseData;
import uk.gov.moj.cpp.listing.it.util.ItClock;
import uk.gov.moj.cpp.listing.steps.CivilOffenceData;
import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

/**
 * LPT-2698 BDF: the restore command fills orderIndex/count on stored offences that lost them
 * (LPT-2677), and never overwrites a value that is already there.
 */
class OffenceOrderingRestoredBdfIT extends AbstractIT {

    private static final String MEDIA_TYPE_SEARCH_HEARING_JSON = "application/vnd.listing.search.hearing+json";
    private static final String MEDIA_TYPE_RESTORE_OFFENCE_ORDERING_BDF = "application/vnd.listing.restore-offence-ordering-bdf+json";

    @Test
    void shouldRestoreAMissingOrderIndexAndLeaveAnExistingOneUnchanged() {
        final OffenceData lostOrdering = offence("Lost ordering", null, null);
        final OffenceData keptOrdering = offence("Kept ordering", 5, 0);
        final HearingsData hearings = hearingsData();
        final List<OffenceData> offences = firstDefendant(hearings).getOffences();
        offences.clear();
        offences.add(lostOrdering);
        offences.add(keptOrdering);

        final ListCourtHearingSteps steps = new ListCourtHearingSteps(hearings);
        steps.whenCaseIsSubmittedForListing();
        steps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        final UUID hearingId = hearings.getHearingData().get(0).getId();

        final Response response = restClient.postCommand(
                String.format("%s/%s", getBaseUri(), format(readConfig().getProperty("listing.update-related-hearing"), hearingId.toString())),
                MEDIA_TYPE_RESTORE_OFFENCE_ORDERING_BDF,
                createObjectBuilder().add("restoredOffences", createArrayBuilder()
                        .add(restoredOffence(hearings, lostOrdering, 2, 0))
                        .add(restoredOffence(hearings, keptOrdering, 9, 3))).build().toString(),
                getLoggedInHeader());
        assertThat(response.getStatus(), is(ACCEPTED.getStatusCode()));

        final String hearing = pollForHearingUntilOrderIndex(hearingId, hearings, lostOrdering, 2);

        assertThat(JsonPath.read(hearing, offencePath(hearings, keptOrdering) + ".orderIndex"), contains(5));
        assertThat(JsonPath.read(hearing, offencePath(hearings, keptOrdering) + ".count"), contains(0));
        assertThat(JsonPath.read(hearing, offencePath(hearings, lostOrdering) + ".count"), contains(0));
    }

    private String pollForHearingUntilOrderIndex(final UUID hearingId, final HearingsData hearings, final OffenceData offence, final int orderIndex) {
        final String url = String.format("%s/%s", getBaseUri(), format(readConfig().getProperty("listing.search.hearing"), hearingId.toString()));
        final ResponseData response = pollWithDefaults(requestParams(url, MEDIA_TYPE_SEARCH_HEARING_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(status().is(Response.Status.OK),
                        payload().isJson(withJsonPath(offencePath(hearings, offence) + ".orderIndex", hasItem(orderIndex))));
        return response.getPayload();
    }

    private static String offencePath(final HearingsData hearings, final OffenceData offence) {
        return format("$.listedCases[?(@.id==''{0}'')].defendants[?(@.id==''{1}'')].offences[?(@.id==''{2}'')]",
                firstCase(hearings).getCaseId(), firstDefendant(hearings).getDefendantId(), offence.getOffenceId());
    }

    private static JsonObject restoredOffence(final HearingsData hearings, final OffenceData offence, final int orderIndex, final int count) {
        return createObjectBuilder()
                .add("caseId", firstCase(hearings).getCaseId().toString())
                .add("defendantId", firstDefendant(hearings).getDefendantId().toString())
                .add("offenceId", offence.getOffenceId().toString())
                .add("orderIndex", orderIndex)
                .add("count", count)
                .build();
    }

    private static ListedCaseData firstCase(final HearingsData hearings) {
        return hearings.getHearingData().get(0).getListedCases().get(0);
    }

    private static DefendantData firstDefendant(final HearingsData hearings) {
        return firstCase(hearings).getDefendants().get(0);
    }

    private static OffenceData offence(final String title, final Integer orderIndex, final Integer count) {
        return new OffenceData(randomUUID(), "TW01046", ItClock.today(), ItClock.today(), title, title, "wording",
                count, orderIndex, "Contrary to section 1", randomUUID(), Optional.empty(), Optional.empty(), ItClock.today(),
                Optional.of(Boolean.FALSE), null, null, new CivilOffenceData(false));
    }
}
