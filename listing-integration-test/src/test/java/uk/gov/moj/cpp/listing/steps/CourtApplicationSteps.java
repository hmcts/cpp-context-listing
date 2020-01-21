package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;

import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationRespondent;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.listing.courts.ApplicationJurisdictionType;
import uk.gov.justice.listing.courts.ApplicationStatus;
import uk.gov.justice.listing.courts.ApplicationSummonsTemplateType;
import uk.gov.justice.listing.courts.Gender;
import uk.gov.justice.listing.courts.LinkType;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.AddCourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationUpdateData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtApplicationSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationSteps.class);

    private static final String APPLICANT_FIRST_NAME = STRING.next();
    private static final String APPLICANT_LAST_NAME = STRING.next();
    private static final String RESPONDENT_FIRST_NAME = STRING.next();
    private static final String RESPONDENT_LAST_NAME = STRING.next();
    private static final String APPLICATION_TYPE = STRING.next();
    public static final UUID APPLICANT_ID = UUID.randomUUID();
    public static final UUID RESPONDENT_ID = UUID.randomUUID();
    private static final UUID LINKED_CASE_ID = UUID.randomUUID();
    private static final UUID LINKED_APPLICATION_ID = UUID.randomUUID();
    private static final String PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED = "public.progression.events.hearing-extended";
    private static final String PRIVATE_EVENT_APPLICATION_ADD_COURT_APPLICATION_FOR_HEARING = "listing.events.court-application-added-for-hearing";
    private static final String PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED = "public.progression.court-application-changed";
    private static final String PRIVATE_EVENT_APPLICATION_UPDATED_FOR_HEARING = "listing.events.court-application-updated-for-hearing";

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";

    private MessageProducer publicEventCourtApplicationUpdated;
    private MessageConsumer publicEventMessageConsumerCourtApplicationUpdated;
    private MessageConsumer privateMessageConsumerCourtApplicationUpdatedForHearing;
    private MessageProducer publicEventCourtApplicationAdded;
    private MessageConsumer publicEventMessageConsumerCourtApplicationAdded;
    private MessageConsumer privateMessageConsumerCourtApplicationAddedForHearing;

    private String request;

    private final HearingsData hearingsData;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public CourtApplicationSteps(HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        publicEventCourtApplicationUpdated = QueueUtil.publicEvents.createProducer();
        publicEventMessageConsumerCourtApplicationUpdated = QueueUtil.publicEvents.createConsumer(PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED);
        privateMessageConsumerCourtApplicationUpdatedForHearing = QueueUtil.privateEvents.createConsumer(PRIVATE_EVENT_APPLICATION_UPDATED_FOR_HEARING);
        publicEventCourtApplicationAdded = QueueUtil.publicEvents.createProducer();
        publicEventMessageConsumerCourtApplicationAdded = QueueUtil.publicEvents.createConsumer(PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED);
        privateMessageConsumerCourtApplicationAddedForHearing = QueueUtil.privateEvents.createConsumer(PRIVATE_EVENT_APPLICATION_ADD_COURT_APPLICATION_FOR_HEARING);

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
    }

    public void whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended() {
        AddCourtApplicationData addCourtApplicationData = getCourtApplicationForHearingData(hearingsData);
        final JsonObject courtApplicationUpdateDataObject = (JsonObject) objectToJsonValueConverter.convert(addCourtApplicationData);
        QueueUtil.sendMessage(
                publicEventCourtApplicationAdded,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED,
                courtApplicationUpdateDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
        request = courtApplicationUpdateDataObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED, request, getLoggedInHeader());
    }

    public void whenCaseCourtApplicationUpdatedPublicEventIsPublished() {
        CourtApplicationUpdateData courtApplicationUpdateData = getUpdateCourtApplicationForHearingsData(hearingsData);
        final JsonObject courtApplicationUpdateDataObject = (JsonObject) objectToJsonValueConverter.convert(courtApplicationUpdateData);

        QueueUtil.sendMessage(
                publicEventCourtApplicationUpdated,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED,
                courtApplicationUpdateDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED).withUserId(randomUUID().toString()).build());

        request = courtApplicationUpdateDataObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED, request, getLoggedInHeader());
    }

    public void verifyCourtApplicationUpdatedInPrivateMessage() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtApplicationUpdatedForHearing);
        LOGGER.debug("jsonResponse from privateMessageConsumerCourtApplicationUpdatedForHearing: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("courtApplication.id"), is(jsRequest.getString("courtApplication.id")));
    }

    public void verifyCourtApplicationAddedInPrivateMessage() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(privateMessageConsumerCourtApplicationAddedForHearing);
        LOGGER.debug("jsonResponse from privateMessageConsumerCourtApplicationAddedForHearing: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(jsRequest.getString("hearingId")));
        assertThat(jsonResponse.get("courtApplication.id"), is(jsRequest.getString("courtApplication.id")));
    }

    public void verifyCourtApplicationUpdatedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(publicEventMessageConsumerCourtApplicationUpdated);
        LOGGER.debug("jsonResponse from publicEventMessageConsumerCourtApplicationAdded: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("courtApplication.applicant.personDetails.firstName"), is(jsRequest.getString("courtApplication.applicant.personDetails.firstName")));
        assertThat(jsonResponse.get("courtApplication.applicant.personDetails.lastName"), is(jsRequest.getString("courtApplication.applicant.personDetails.lastName")));
        assertThat(jsonResponse.get("courtApplication.respondents[0].partyDetails.personDetails.firstName"), is(jsRequest.getString("courtApplication.respondents[0].partyDetails.personDetails.firstName")));
        assertThat(jsonResponse.get("courtApplication.respondents[0].partyDetails.personDetails.lastName"), is(jsRequest.getString("courtApplication.respondents[0].partyDetails.personDetails.lastName")));
        assertThat(jsonResponse.get("courtApplication.type.applicationType"), is(jsRequest.getString("courtApplication.type.applicationType")));
        assertThat(jsonResponse.get("courtApplication.id"), is(jsRequest.getString("courtApplication.id")));
        assertThat(jsonResponse.get("courtApplication.applicationStatus"), is(jsRequest.getString("courtApplication.applicationStatus")));
        assertThat(jsonResponse.get("courtApplication.linkedCaseId"), is(jsRequest.getString("courtApplication.linkedCaseId")));
    }

    public void verifyCourtApplicationAddedInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = QueueUtil.retrieveMessage(publicEventMessageConsumerCourtApplicationAdded);
        LOGGER.debug("jsonResponse from publicEventMessageConsumerCourtApplicationAdded: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(jsRequest.getString("hearingId")));
        assertThat(jsonResponse.get("courtApplication.applicant.personDetails.firstName"), is(jsRequest.getString("courtApplication.applicant.personDetails.firstName")));
        assertThat(jsonResponse.get("courtApplication.applicant.personDetails.lastName"), is(jsRequest.getString("courtApplication.applicant.personDetails.lastName")));
        assertThat(jsonResponse.get("courtApplication.respondents[0].partyDetails.personDetails.firstName"), is(jsRequest.getString("courtApplication.respondents[0].partyDetails.personDetails.firstName")));
        assertThat(jsonResponse.get("courtApplication.respondents[0].partyDetails.personDetails.lastName"), is(jsRequest.getString("courtApplication.respondents[0].partyDetails.personDetails.lastName")));
        assertThat(jsonResponse.get("courtApplication.type.applicationType"), is(jsRequest.getString("courtApplication.type.applicationType")));
        assertThat(jsonResponse.get("courtApplication.id"), is(jsRequest.getString("courtApplication.id")));
        assertThat(jsonResponse.get("courtApplication.applicant.id"), is(jsRequest.getString("courtApplication.applicant.id")));
        assertThat(jsonResponse.get("courtApplication.applicationStatus"), is(jsRequest.getString("courtApplication.applicationStatus")));
        assertThat(jsonResponse.get("courtApplication.linkedCaseId"), is(jsRequest.getString("courtApplication.linkedCaseId")));
    }

    public void verifyCourtApplicationUpdatedFromAPI() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), false));

        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.lastName",
                                        equalTo(APPLICANT_LAST_NAME)),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.firstName",
                                        equalTo(APPLICANT_FIRST_NAME)),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].firstName",
                                        equalTo(RESPONDENT_FIRST_NAME)),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].lastName",
                                        equalTo(RESPONDENT_LAST_NAME)),
                                withJsonPath("$.hearings[0].courtApplications[0].applicationType",
                                        equalTo(APPLICATION_TYPE)),
                                withJsonPath("$.hearings[0].courtApplications[0].linkedCaseId",
                                        equalTo(LINKED_CASE_ID.toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].parentApplicationId",
                                        equalTo(LINKED_APPLICATION_ID.toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].restrictFromCourtList",
                                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRestrictFromCourtList())),
                                withJsonPath("$.hearings[0].courtApplications[0].restrictCourtApplicationType",
                                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRestrictCourtApplicationType()))
                        )));
    }

    public void verifyCourtApplicationAddedFromAPI() {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingsData.getHearingData().get(0).getCourtCentreId(), false));

        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(hearingIdFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingsData.getHearingData().get(0).getId().toString())),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.lastName",
                                        Matchers.anyOf(equalTo(APPLICANT_LAST_NAME),
                                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getApplicant().getLastName()))),
                                withJsonPath("$.hearings[0].courtApplications[0].applicant.firstName",
                                        Matchers.anyOf(equalTo(APPLICANT_FIRST_NAME),
                                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getApplicant().getFirstName()))),
                                withJsonPath("$.hearings[0].courtApplications[0].applicationType",
                                        Matchers.anyOf(equalTo(APPLICATION_TYPE),
                                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getType()))),
                                withJsonPath("$.hearings[0].courtApplications[0].linkedCaseId",
                                        Matchers.anyOf(equalTo(LINKED_CASE_ID.toString()),
                                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getLinkedCaseId().toString()))),
                                withJsonPath("$.hearings[0].courtApplications[0].parentApplicationId",
                                        Matchers.anyOf(equalTo(LINKED_APPLICATION_ID.toString()),
                                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getParentApplicationId().toString()))),
                                withJsonPath("$.hearings[0].courtApplications[0].restrictFromCourtList",
                                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRestrictFromCourtList())),
                                withJsonPath("$.hearings[0].courtApplications[0].restrictCourtApplicationType",
                                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRestrictCourtApplicationType())),
                                withJsonPath("$.hearings[0].courtApplications[1].respondents[0].id",
                                        Matchers.anyOf(equalTo(RESPONDENT_ID.toString()),
                                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRespondent().getId().toString()))),
                                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].firstName",
                                        Matchers.anyOf(equalTo(RESPONDENT_FIRST_NAME),
                                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRespondent().getFirstName()))),
                                withJsonPath("$.hearings[0].courtApplications[1].applicant.id",
                                        Matchers.anyOf(equalTo(APPLICANT_ID.toString()),
                                                equalTo(hearingsData.getHearingData().get(1).getCourtApplications().get(0).getApplicant().getId().toString()))),
                                withJsonPath("$.hearings[0].courtApplications[1].applicant.firstName",
                                        Matchers.anyOf(equalTo(APPLICANT_FIRST_NAME),
                                                equalTo(hearingsData.getHearingData().get(1).getCourtApplications().get(0).getApplicant().getFirstName()))),
                                withJsonPath("$.hearings[0].courtApplications[1].applicationType",
                                        Matchers.anyOf(equalTo(APPLICATION_TYPE),
                                                equalTo(hearingsData.getHearingData().get(1).getCourtApplications().get(0).getType()))),
                                withJsonPath("$.hearings[0].courtApplications[1].linkedCaseId",
                                        Matchers.anyOf(equalTo(LINKED_CASE_ID.toString()),
                                                equalTo(hearingsData.getHearingData().get(1).getCourtApplications().get(0).getLinkedCaseId().toString()))),
                                withJsonPath("$.hearings[0].courtApplications[1].parentApplicationId",
                                        Matchers.anyOf(equalTo(LINKED_APPLICATION_ID.toString()),
                                                equalTo(hearingsData.getHearingData().get(1).getCourtApplications().get(0).getParentApplicationId().toString()))),
                                withJsonPath("$.hearings[0].courtApplications[1].respondents[0].lastName",
                                        Matchers.anyOf(equalTo(RESPONDENT_LAST_NAME),
                                                equalTo(hearingsData.getHearingData().get(1).getCourtApplications().get(0).getRespondent().getLastName())))

                        )));
    }

    private CourtApplicationUpdateData getUpdateCourtApplicationForHearingsData(HearingsData hearingsData) {
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        CourtApplication courtApplication = getCourtApplication(courtApplicationData);
        return new CourtApplicationUpdateData(courtApplication);
    }

    private AddCourtApplicationData getCourtApplicationForHearingData(HearingsData hearingsData) {
        UUID hearingId = hearingsData.getHearingData().get(0).getId();
        CourtApplicationData courtApplicationData = hearingsData.getHearingData().get(0).getCourtApplications().get(0);
        CourtApplication courtApplication = getCourtApplication(courtApplicationData);
        return new AddCourtApplicationData(hearingId, courtApplication);
    }

    private CourtApplication getCourtApplication(final CourtApplicationData courtApplicationData) {
        return CourtApplication.courtApplication()
                .withApplicant(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(of(Person.person()
                                .withFirstName(of(APPLICANT_FIRST_NAME))
                                .withLastName(APPLICANT_LAST_NAME)
                                .withGender(Gender.FEMALE)
                                .build()))
                        .withId(APPLICANT_ID)
                        .build())
                .withRespondents(Collections.singletonList(CourtApplicationRespondent.courtApplicationRespondent()
                        .withPartyDetails(CourtApplicationParty.courtApplicationParty()
                                .withPersonDetails(of(Person.person()
                                        .withFirstName(of(RESPONDENT_FIRST_NAME))
                                        .withLastName(RESPONDENT_LAST_NAME)
                                        .withGender(Gender.MALE)
                                        .build()))
                                .withId(RESPONDENT_ID)
                                .build())
                        .build()))
                .withId(courtApplicationData.getId())
                .withLinkedCaseId(of(LINKED_CASE_ID))
                .withParentApplicationId(of(LINKED_APPLICATION_ID))
                .withType(CourtApplicationType.courtApplicationType()
                        .withApplicationType(APPLICATION_TYPE)
                        .withId(UUID.randomUUID())
                        .withApplicationCode(Optional.of(STRING.next()))
                        .withApplicationLegislation(Optional.of(STRING.next()))
                        .withApplicationCategory(STRING.next())
                        .withLinkType(LinkType.EITHER)
                        .withApplicationSummonsTemplateType(Optional.of(ApplicationSummonsTemplateType.GENERIC_SUMMONS))
                        .withApplicationJurisdictionType(ApplicationJurisdictionType.CROWN)
                        .build())
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicationReceivedDate(LocalDate.now().toString())
                .withApplicationReference(Optional.of(STRING.next()))
                .build();
    }

    @Override
    public void close() {
        try {
            publicEventCourtApplicationUpdated.close();
            publicEventMessageConsumerCourtApplicationUpdated.close();
            privateMessageConsumerCourtApplicationUpdatedForHearing.close();
            publicEventCourtApplicationAdded.close();
            publicEventMessageConsumerCourtApplicationAdded.close();
            privateMessageConsumerCourtApplicationAddedForHearing.close();

        } catch (JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
