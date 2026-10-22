package uk.gov.moj.cpp.listing.it;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.UUID.randomUUID;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.getDefendantFilter;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingWithJmsDelay;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.WireMockStubUtils.setupAsUnauthorisedUser;

import uk.gov.moj.cpp.listing.steps.ListCourtHearingSteps;
import uk.gov.moj.cpp.listing.steps.data.DefendantData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.steps.data.OffenceData;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

/**
 * REST access to the four hearing offence/defendant actions on {@code POST /hearings/{hearingId}}
 * (LPT-2526). Users in the groups allowed by {@code listing-command-api.drl} get through and the
 * hearing view changes; users in any other group get 403.
 *
 * <p>Every request body passes the command API schema, because the REST adapter validates the body
 * before the access check runs: an invalid body would return 400 and never reach the Drools rules.</p>
 *
 * <p>Add and delete offences have no allowed-user scenario here. Their command API schemas reject
 * {@code caseId} and {@code defendantId}, which the command handler schemas require, so any body the
 * API accepts is rejected by the handler.</p>
 */
class HearingOffencesAndDefendantsAccessIT extends AbstractIT {

    private static final String ADD_OFFENCES_FOR_HEARING = "listing.command.add-offences-for-hearing";
    private static final String DELETE_OFFENCES_FOR_HEARING = "listing.command.delete-offences-for-hearing";
    private static final String UPDATE_OFFENCES_FOR_HEARING = "listing.command.update-offences-for-hearing";
    private static final String UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing";

    @Test
    void shouldUpdateDefendantsForHearingWhenUserIsInAnAllowedGroup() {
        final HearingsData hearingsData = listHearing();
        final HearingData hearing = hearingsData.getHearingData().get(0);
        final UUID caseId = hearing.getListedCases().get(0).getCaseId();
        final DefendantData defendant = hearing.getListedCases().get(0).getDefendants().get(0);
        final String updatedLastName = "Updated" + randomUUID().toString().substring(0, 8);

        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearing.getId().toString())
                .add("caseId", caseId.toString())
                .add("defendants", createArrayBuilder().add(createObjectBuilder()
                        .add("id", defendant.getDefendantId().toString())
                        .add("firstName", defendant.getFirstName())
                        .add("lastName", updatedLastName)
                        .add("dateOfBirth", defendant.getDateOfBirth().toString())
                        .add("offences", createArrayBuilder())))
                .build();

        assertStatus(post(UPDATE_DEFENDANTS_FOR_HEARING, hearing.getId(), payload), SC_ACCEPTED);

        pollForHearingWithJmsDelay(hearing.getCourtCentreId().toString(), UNALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(getDefendantFilter(hearing.getId().toString(), caseId.toString(), defendant.getDefendantId().toString()) + ".lastName",
                        hasItem(equalTo(updatedLastName)))
        });
    }

    @Test
    void shouldUpdateOffencesForHearingWhenUserIsInAnAllowedGroup() {
        final HearingsData hearingsData = listHearing();
        final HearingData hearing = hearingsData.getHearingData().get(0);
        final UUID caseId = hearing.getListedCases().get(0).getCaseId();
        final DefendantData defendant = hearing.getListedCases().get(0).getDefendants().get(0);
        final OffenceData offence = defendant.getOffences().get(0);
        final String updatedWording = "Updated wording " + randomUUID();

        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearing.getId().toString())
                .add("caseId", caseId.toString())
                .add("defendantId", defendant.getDefendantId().toString())
                .add("offences", createArrayBuilder().add(createObjectBuilder()
                        .add("id", offence.getOffenceId().toString())
                        .add("offenceCode", offence.getOffenceCode())
                        .add("offenceWording", updatedWording)
                        .add("startDate", offence.getStartDate().toString())
                        .add("statementOfOffence", createObjectBuilder()
                                .add("title", offence.getStatementOfOffenceTitle())
                                .add("welshTitle", Optional.ofNullable(offence.getStatementOfOffenceTitleWelsh()).orElse(offence.getStatementOfOffenceTitle())))))
                .build();

        assertStatus(post(UPDATE_OFFENCES_FOR_HEARING, hearing.getId(), payload), SC_ACCEPTED);

        pollForHearingWithJmsDelay(hearing.getCourtCentreId().toString(), UNALLOCATED, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(getDefendantFilter(hearing.getId().toString(), caseId.toString(), defendant.getDefendantId().toString())
                                + ".offences[?(@.id == '" + offence.getOffenceId() + "')].offenceWording",
                        hasItem(equalTo(updatedWording)))
        });
    }

    @Test
    void shouldRefuseAddOffencesForHearingWhenUserIsNotInAnAllowedGroup() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("offences", createArrayBuilder().add(offence(randomUUID())))
                .build();

        assertStatus(postAsUnauthorisedUser(ADD_OFFENCES_FOR_HEARING, hearingId, payload), SC_FORBIDDEN);
    }

    @Test
    void shouldRefuseDeleteOffencesForHearingWhenUserIsNotInAnAllowedGroup() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("offences", createArrayBuilder().add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("defendantId", randomUUID().toString())))
                .build();

        assertStatus(postAsUnauthorisedUser(DELETE_OFFENCES_FOR_HEARING, hearingId, payload), SC_FORBIDDEN);
    }

    @Test
    void shouldRefuseUpdateOffencesForHearingWhenUserIsNotInAnAllowedGroup() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("caseId", randomUUID().toString())
                .add("defendantId", randomUUID().toString())
                .add("offences", createArrayBuilder().add(offence(randomUUID())))
                .build();

        assertStatus(postAsUnauthorisedUser(UPDATE_OFFENCES_FOR_HEARING, hearingId, payload), SC_FORBIDDEN);
    }

    @Test
    void shouldRefuseUpdateDefendantsForHearingWhenUserIsNotInAnAllowedGroup() {
        final UUID hearingId = randomUUID();
        final JsonObject payload = createObjectBuilder()
                .add("hearingId", hearingId.toString())
                .add("caseId", randomUUID().toString())
                .add("defendants", createArrayBuilder().add(createObjectBuilder()
                        .add("id", randomUUID().toString())
                        .add("offences", createArrayBuilder())))
                .build();

        assertStatus(postAsUnauthorisedUser(UPDATE_DEFENDANTS_FOR_HEARING, hearingId, payload), SC_FORBIDDEN);
    }

    private HearingsData listHearing() {
        final HearingsData hearingsData = HearingsData.hearingsData();
        final ListCourtHearingSteps listCourtHearingSteps = new ListCourtHearingSteps(hearingsData);
        listCourtHearingSteps.whenCaseIsSubmittedForListing();
        listCourtHearingSteps.verifyHearingListedFromAPIWithJmsDelay(UNALLOCATED);
        return hearingsData;
    }

    private static JsonObject offence(final UUID offenceId) {
        return createObjectBuilder()
                .add("id", offenceId.toString())
                .add("offenceCode", "CA03012")
                .add("offenceWording", "Offence wording")
                .add("startDate", "2024-01-01")
                .add("statementOfOffence", createObjectBuilder()
                        .add("title", "Statement of offence")
                        .add("welshTitle", "Statement of offence"))
                .build();
    }

    private Response postAsUnauthorisedUser(final String action, final UUID hearingId, final JsonObject payload) {
        setupAsUnauthorisedUser(USER_ID_VALUE);
        setLoggedInUser(USER_ID_VALUE);
        return post(action, hearingId, payload);
    }

    private Response post(final String action, final UUID hearingId, final JsonObject payload) {
        final String url = String.format("%s/%s", getBaseUri(), format(readConfig().getProperty(action), hearingId));
        return restClient.postCommand(url, "application/vnd." + action + "+json", payload.toString(), getLoggedInHeader());
    }

    private static void assertStatus(final Response response, final int expectedStatus) {
        assertThat(response.getStatus(), equalTo(expectedStatus));
    }
}
