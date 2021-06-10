package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.http.RestPoller.poll;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.privateEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.AddDefendantForCourtProceedingsData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.path.json.JsonPath;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddDefendantSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddDefendantSteps.class);

    private static final String PUBLIC_EVENT_SELECTOR_PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";
    private static final String EVENT_SELECTOR_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS = "listing.events.defendants-to-be-added-for-court-proceedings";
    private static final String EVENT_SELECTOR_DEFENDANT_DETAILS_ADDED_FOR_COURT_PROCEEDINGS = "listing.events.new-defendant-added-for-court-proceedings";
    private static final String PUBLIC_EVENT_SELECTOR_DEFENDANT_DETAILS_ADDED_FOR_COURT_PROCEEDINGS = "public.listing.new-defendant-added-for-court-proceedings";

    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";
    final UUID DEFENDANT_ID = UUID.randomUUID();
    final UUID MASTER_DEFENDANT_ID = UUID.randomUUID();

    private final HearingData hearingData;
    private final ListedCaseData listedCaseData;
    private final UUID caseId;
    private final MessageProducer publicEventDefendantAdded;
    private final MessageConsumer publicEventMessageConsumerDefendantAdded;
    private final MessageConsumer privateEventMessageDefendantsToBeAdded;
    private final MessageConsumer privateEventsMessageDefendantDetailsAdded;
    private final MessageConsumer publicEventsMessageNewDefendantAdded;
    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);
    private String request;

    public AddDefendantSteps(final UUID caseId, final HearingData hearingData) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.listedCaseData = hearingData.getListedCases().get(0);

        publicEventDefendantAdded = QueueUtil.publicEvents.createProducer();
        publicEventMessageConsumerDefendantAdded = QueueUtil.publicEvents.createConsumer(PUBLIC_EVENT_SELECTOR_PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS);
        privateEventMessageDefendantsToBeAdded = privateEvents.createConsumer(EVENT_SELECTOR_DEFENDANTS_TO_BE_ADDED_FOR_COURT_PROCEEDINGS);
        privateEventsMessageDefendantDetailsAdded = privateEvents.createConsumer(EVENT_SELECTOR_DEFENDANT_DETAILS_ADDED_FOR_COURT_PROCEEDINGS);
        publicEventsMessageNewDefendantAdded = publicEvents.createConsumer(PUBLIC_EVENT_SELECTOR_DEFENDANT_DETAILS_ADDED_FOR_COURT_PROCEEDINGS);

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseDefendantsAddedPublicEventIsPublished() {
        final AddDefendantForCourtProceedingsData addDefendantForCourtProceedingsData = getAddDefendantDetails(caseId);
        final JsonObject addDefendantDetailsForCourtProceedingsObject = (JsonObject) objectToJsonValueConverter.convert(addDefendantForCourtProceedingsData);

        QueueUtil.sendMessage(
                publicEventDefendantAdded,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS,
                addDefendantDetailsForCourtProceedingsObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS).withUserId(randomUUID().toString()).build());

        request = addDefendantDetailsForCourtProceedingsObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_EVENT_SELECTOR_PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS, request, getLoggedInHeader());
    }

    public void verifyEventDefendantAddedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicEventMessageConsumerDefendantAdded);
        LOGGER.debug("jsonResponse from publicEventMessageConsumerDefendantAdded: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("defendants.id").toString(), is(jsRequest.getString("defendants.id")));
        assertThat(jsonResponse.get("defendants.personDefendant.custodyTimeLimit").toString(), is(jsRequest.getString("defendants.personDefendant.custodyTimeLimit")));
        assertThat(jsonResponse.get("defendants.personDefendant.personDetails.dateOfBirth").toString(), is(jsRequest.getString("defendants.personDefendant.personDetails.dateOfBirth")));
        assertThat(jsonResponse.get("defendants.defenceOrganisation.name").toString(), is(jsRequest.getString("defendants.defenceOrganisation.name")));
        assertThat(jsonResponse.get("defendants.defenceOrganisation.id").toString(), is(jsRequest.getString("defendants.defenceOrganisation.id")));
        assertThat(jsonResponse.get("defendants.personDefendant.bailStatus"), equalTo(jsRequest.getJsonObject("defendants.personDefendant.bailStatus")));
        assertThat(jsonResponse.get("defendants.personDefendant.personDetails.firstName").toString(), is(jsRequest.getString("defendants.personDefendant.personDetails.firstName")));
        assertThat(jsonResponse.get("defendants.personDefendant.personDetails.lastName").toString(), is(jsRequest.getString("defendants.personDefendant.personDetails.lastName")));
        assertThat(jsonResponse.get("defendants.personDefendant.personDetails.specificRequirements").toString(), is(jsRequest.getString("defendants.personDefendant.personDetails.specificRequirements")));
    }

    public void verifyEventDefendantsToBeAddedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventMessageDefendantsToBeAdded);
        LOGGER.debug("jsonResponse from privateEventMessageDefendantsToBeAdded: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("defendants[0].id").toString(), is(jsRequest.getString("defendants[0].id")));
        assertThat(jsonResponse.get("defendants[0].custodyTimeLimit").toString(), is(jsRequest.getString("defendants[0].personDefendant.custodyTimeLimit")));
        assertThat(jsonResponse.get("defendants[0].dateOfBirth").toString(), is(jsRequest.getString("defendants[0].personDefendant.personDetails.dateOfBirth")));
        assertThat(jsonResponse.get("defendants[0].defenceOrganisation").toString(), is(jsRequest.getString("defendants[0].defenceOrganisation.name")));
        assertThat(jsonResponse.get("defendants[0].bailStatus"), equalTo(jsRequest.getJsonObject("defendants[0].personDefendant.bailStatus")));
        assertThat(jsonResponse.get("defendants[0].firstName").toString(), is(jsRequest.getString("defendants[0].personDefendant.personDetails.firstName")));
        assertThat(jsonResponse.get("defendants[0].lastName").toString(), is(jsRequest.getString("defendants[0].personDefendant.personDetails.lastName")));
        assertThat(jsonResponse.get("defendants[0].specificRequirements").toString(), is(jsRequest.getString("defendants[0].personDefendant.personDetails.specificRequirements")));
        assertThat(jsonResponse.get("defendants[0].organisationName").toString(), is(jsRequest.getString("defendants[0].legalEntityDefendant.organisation.name")));
    }

    public void verifyEventDefendantDetailsAddedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventsMessageDefendantDetailsAdded);
        LOGGER.debug("jsonResponse from privateEventsMessageDefendantDetailsAdded: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("defendant.id"), is(jsRequest.getString("defendants[0].id")));
        assertThat(jsonResponse.get("defendant.custodyTimeLimit"), is(jsRequest.getString("defendants[0].personDefendant.custodyTimeLimit")));
        assertThat(jsonResponse.get("defendant.dateOfBirth"), is(jsRequest.getString("defendants[0].personDefendant.personDetails.dateOfBirth")));
        assertThat(jsonResponse.get("defendant.defenceOrganisation"), is(jsRequest.getString("defendants[0].defenceOrganisation.name")));
        assertThat(jsonResponse.get("defendant.bailStatus"), equalTo(jsRequest.getJsonObject("defendants[0].personDefendant.bailStatus")));
        assertThat(jsonResponse.get("defendant.firstName"), is(jsRequest.getString("defendants[0].personDefendant.personDetails.firstName")));
        assertThat(jsonResponse.get("defendant.specificRequirements"), is(jsRequest.getString("defendants[0].personDefendant.personDetails.specificRequirements")));
        assertThat(jsonResponse.get("defendant.organisationName"), is(jsRequest.getString("defendants[0].legalEntityDefendant.organisation.name")));
    }

    public void verifyPublicEventDefendantAddedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicEventsMessageNewDefendantAdded);

        assertThat(jsonResponse.get("caseId"), is(caseId.toString()));
        assertThat(jsonResponse.get("hearingId"), is(hearingData.getId().toString()));
        assertThat(jsonResponse.get("defendantId"), is(jsRequest.getString("defendants[0].id")));
        assertThat(jsonResponse.get("courtCentre.id"), is(hearingData.getCourtCentreId().toString()));
        assertThat(jsonResponse.get("courtCentre.roomId"), is(hearingData.getCourtRoomId().toString()));
        assertThat(jsonResponse.get("hearingDateTime"), notNullValue());
    }

    public void verifyPublicEventDefendantAddedNotRaisedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicEventsMessageNewDefendantAdded);

        Assert.assertNull("Event should not be raised", jsonResponse);
    }

    public void verifyHearingListedFromAPI(final boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), isAllocated));

        final AddDefendantForCourtProceedingsData addDefendantForCourtProceedingsData = getAddDefendantDetails(caseId);
        final Defendant defendant = addDefendantForCourtProceedingsData.getDefendants().get(0);
        final Person personDetails = defendant.getPersonDefendant().get().getPersonDetails();

        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingData.getId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[2].lastName",
                                        equalTo(personDetails.getLastName())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[2].offences[0].offenceWording",
                                        equalTo(defendant.getOffences().get(0).getWording())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[2].offences[0].offenceCode",
                                        equalTo(defendant.getOffences().get(0).getOffenceCode())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[2].offences[0].offenceCode",
                                        equalTo(defendant.getOffences().get(0).getOffenceCode()))
                        )));
    }


    private AddDefendantForCourtProceedingsData getAddDefendantDetails(final UUID caseId) {


        final List<uk.gov.justice.core.courts.Defendant> defendant = Arrays.asList(Defendant.defendant()
                .withId(DEFENDANT_ID)
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withLegalEntityDefendant(of(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("withOrganisationName")
                                .build())
                        .build()))
                .withPersonDefendant(of(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName("Last Name")
                                .withFirstName(of("FIRST NAME"))
                                .withDateOfBirth(of("1980-07-15"))
                                .withSpecificRequirements(of("Screen"))
                                .withGender(Gender.FEMALE)
                                .build()
                        )
                        .withBailStatus(of(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(UUID.fromString("12e69486-4d01-3403-a50a-7419ca040635")).build()))
                        .withCustodyTimeLimit(of("2017-10-05"))
                        .build())
                )
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(UUID.randomUUID())
                        .withOffenceCode("TFL123")
                        .withStartDate("2019-05-01")
                        .withEndDate(Optional.empty())
                        .withOffenceTitle("TFL Ticket Dodger")
                        .withOffenceDefinitionId(UUID.randomUUID())
                        .withCount(of(Integer.valueOf(0)))
                        .withWording("TFL ticket dodged")

                        .build()))
                .withProsecutionCaseId(caseId)
                .withDefenceOrganisation(of(organisation()
                        .withName("withOrganisationName")
                        .build()))
                .build());
        return AddDefendantForCourtProceedingsData.addDefendantForCourtProceedingsData()
                .withDefendant(defendant)
                .withListHearingRequest(Arrays.asList(getAddHearingRequestData(DEFENDANT_ID, caseId))).build();
    }

    private ListHearingRequest getAddHearingRequestData(final UUID defendantId, final UUID prosecutionCaseId) {

        return ListHearingRequest.listHearingRequest()
                .withCourtCentre(CourtCentre.courtCentre()
                        .withId(UUID.randomUUID())
                        .withName("Carmarthen Magistrates Court")
                        .withRoomId(of(UUID.randomUUID()))
                        .build())
                .withHearingType(HearingType.hearingType()
                        .withDescription("Sentence").withId(UUID.randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(Optional.of(defendantId))
                        .withDefendantOffences(asList(UUID.randomUUID()))
                        .withProsecutionCaseId(Optional.of(prosecutionCaseId))
                        .build()))
                .build();

    }

    @Override
    public void close() {
        try {
            publicEventDefendantAdded.close();
            publicEventMessageConsumerDefendantAdded.close();
            privateEventMessageDefendantsToBeAdded.close();
            privateEventsMessageDefendantDetailsAdded.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}


