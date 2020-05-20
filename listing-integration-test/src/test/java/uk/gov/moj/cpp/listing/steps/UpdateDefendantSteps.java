package uk.gov.moj.cpp.listing.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.listing.courts.Gender;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.UpdateCaseDefendantData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData;
import uk.gov.moj.cpp.listing.utils.QueueUtil;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.json.JsonObject;
import java.time.ZonedDateTime;
import java.util.UUID;

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


public class UpdateDefendantSteps extends AbstractIT implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateDefendantSteps.class);


    private static final String PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";

    private static final String COMMAND_SELECTOR_UPDATE_CASE_DEFENDANT_DETAILS = "listing.command.update-case-defendant-details";
    private static final String EVENT_SELECTOR_DEFENDANTS_TO_BE_UPDATED = "listing.events.defendants-to-be-updated";
    private static final String COMMAND_SELECTOR_UPDATE_DEFENDANTS_FOR_HEARING = "listing.command.update-defendants-for-hearing";
    private static final String EVENT_SELECTOR_DEFENDANT_DETAILS_UPDATED = "listing.events.new-defendant-details-updated";


    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing" +
            ".search.hearings+json";


    private final MessageProducer publicEventDefendantUpdated;
    private final MessageConsumer publicEventMessageConsumerDefendantUpdated;
    private final MessageConsumer privateEventMessageDefendantsToBeUpdated;
    private final MessageConsumer privateEventsMessageDefendantDetailsUpdated;


    private String request;


    private final HearingData hearingData;
    private final UpdatedDefendantData updatedDefendantData;
    private final ListedCaseData listedCaseData;
    private final UUID caseId;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);


    public UpdateDefendantSteps(final UUID caseId, final HearingData hearingData, final UpdatedDefendantData defendantData) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.listedCaseData = hearingData.getListedCases().get(0);
        this.updatedDefendantData = defendantData;


        publicEventDefendantUpdated = QueueUtil.publicEvents.createProducer();
        publicEventMessageConsumerDefendantUpdated = QueueUtil.publicEvents.createConsumer(PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED);
        privateEventMessageDefendantsToBeUpdated = privateEvents.createConsumer(EVENT_SELECTOR_DEFENDANTS_TO_BE_UPDATED);
        privateEventsMessageDefendantDetailsUpdated = privateEvents.createConsumer(EVENT_SELECTOR_DEFENDANT_DETAILS_UPDATED);

        givenAUserHasLoggedInAsAListingOfficers(USER_ID_VALUE);
    }

    public void whenCaseDefendantsUpdatedPublicEventIsPublished() {
        final UpdateCaseDefendantData updateCaseDefendantDetails = getUpdateCaseDefendantDetails(caseId, updatedDefendantData);
        final JsonObject updateCaseDefendantDetailsObject = (JsonObject) objectToJsonValueConverter.convert(updateCaseDefendantDetails);

        QueueUtil.sendMessage(
                publicEventDefendantUpdated,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED,
                updateCaseDefendantDetailsObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED).withUserId(randomUUID().toString()).build());

        request = updateCaseDefendantDetailsObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n", PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED, request, getLoggedInHeader());
    }


    public void verifyEventDefendantUpdatedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(publicEventMessageConsumerDefendantUpdated);
        LOGGER.debug("jsonResponse from publicEventMessageConsumerDefendantUpdated: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("defendant.id"), is(jsRequest.getString("defendant.id")));
        assertThat(jsonResponse.get("defendant.masterDefendantId"), is(jsRequest.getString("defendant.masterDefendantId")));
        assertThat(jsonResponse.get("defendant.personDefendant.custodyTimeLimit"), is(jsRequest.getString("defendant.personDefendant.custodyTimeLimit")));
        assertThat(jsonResponse.get("defendant.personDefendant.personDetails.dateOfBirth"), is(jsRequest.getString("defendant.personDefendant.personDetails.dateOfBirth")));
        assertThat(jsonResponse.get("defendant.defenceOrganisation.name"), is(jsRequest.getString("defendant.defenceOrganisation.name")));
        assertThat(jsonResponse.get("defendant.defenceOrganisation.id"), is(jsRequest.getString("defendant.defenceOrganisation.id")));
        assertThat(jsonResponse.get("defendant.personDefendant.bailStatus"), equalTo(jsRequest.getJsonObject("defendant.personDefendant.bailStatus")));
        assertThat(jsonResponse.get("defendant.personDefendant.personDetails.firstName"), is(jsRequest.getString("defendant.personDefendant.personDetails.firstName")));
        assertThat(jsonResponse.get("defendant.personDefendant.personDetails.lastName"), is(jsRequest.getString("defendant.personDefendant.personDetails.lastName")));
        assertThat(jsonResponse.get("defendant.personDefendant.personDetails.specificRequirements"), is(jsRequest.getString("defendant.personDefendant.personDetails.specificRequirements")));
        assertThat(jsonResponse.get("defendant.personDefendant.personDetails.gender"), is(jsRequest.getString("defendant.personDefendant.personDetails.gender")));
        assertThat(jsonResponse.get("defendant.legalEntityDefendant.organisation.name"), is(jsRequest.getString("defendant.legalEntityDefendant.organisation.name")));
        assertThat(jsonResponse.get("defendant.legalEntityDefendant.organisation.id"), is(jsRequest.getString("defendant.legalEntityDefendant.organisation.id")));
        assertThat(jsonResponse.get("defendant.pncId"), is(jsRequest.getString("defendant.pncId")));
        assertThat(jsonResponse.get("defendant.isYouth"), is(jsRequest.getBoolean("defendant.isYouth")));
        assertThat(jsonResponse.get("defendant.aliases[0].firstName"), is(jsRequest.getString("defendant.aliases[0].firstName")));
        assertThat(jsonResponse.get("defendant.aliases[0].lastName"), is(jsRequest.getString("defendant.aliases[0].lastName")));
        assertThat(jsonResponse.get("defendant.associatedDefenceOrganisation.defenceOrganisation.laaContractNumber"), is(jsRequest.getString("defendant.associatedDefenceOrganisation.defenceOrganisation.laaContractNumber")));
    }

    public void verifyEventDefendantsToBeUpdateInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventMessageDefendantsToBeUpdated);
        LOGGER.debug("jsonResponse from privateEventMessageDefendantsToBeUpdated: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("defendants[0].id"), is(jsRequest.getString("defendant.id")));
        assertThat(jsonResponse.get("defendants[0].masterDefendantId"), is(jsRequest.getString("defendant.masterDefendantId")));
        assertThat(jsonResponse.get("defendants[0].custodyTimeLimit"), is(jsRequest.getString("defendant.personDefendant.custodyTimeLimit")));
        assertThat(jsonResponse.get("defendants[0].dateOfBirth"), is(jsRequest.getString("defendant.personDefendant.personDetails.dateOfBirth")));
        assertThat(jsonResponse.get("defendants[0].defenceOrganisation"), is(jsRequest.getString("defendant.defenceOrganisation.name")));
        assertThat(jsonResponse.get("defendants[0].bailStatus"), equalTo(jsRequest.getJsonObject("defendant.personDefendant.bailStatus")));
        assertThat(jsonResponse.get("defendants[0].firstName"), is(jsRequest.getString("defendant.personDefendant.personDetails.firstName")));
        assertThat(jsonResponse.get("defendants[0].lastName"), is(jsRequest.getString("defendant.personDefendant.personDetails.lastName")));
        assertThat(jsonResponse.get("defendants[0].specificRequirements"), is(jsRequest.getString("defendant.personDefendant.personDetails.specificRequirements")));
        assertThat(jsonResponse.get("defendants[0].organisationName"), is(jsRequest.getString("defendant.legalEntityDefendant.organisation.name")));
        assertThat(jsonResponse.get("defendants[0].isYouth"), is(jsRequest.getBoolean("defendant.isYouth")));
    }

    public void verifyEventDefendantDetailsUpdatedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);
        LOGGER.debug("Request payload: {}", jsRequest.prettify());

        final JsonPath jsonResponse = QueueUtil.retrieveMessage(privateEventsMessageDefendantDetailsUpdated);
        LOGGER.debug("jsonResponse from privateEventsMessageDefendantDetailsUpdated: {}", jsonResponse.prettify());

        assertThat(jsonResponse.get("defendant.id"), is(jsRequest.getString("defendant.id")));
        assertThat(jsonResponse.get("defendant.masterDefendantId"), is(jsRequest.getString("defendant.masterDefendantId")));
        assertThat(jsonResponse.get("defendant.custodyTimeLimit"), is(jsRequest.getString("defendant.personDefendant.custodyTimeLimit")));
        assertThat(jsonResponse.get("defendant.dateOfBirth"), is(jsRequest.getString("defendant.personDefendant.personDetails.dateOfBirth")));
        assertThat(jsonResponse.get("defendant.defenceOrganisation"), is(jsRequest.getString("defendant.defenceOrganisation.name")));
        assertThat(jsonResponse.get("defendant.bailStatus"), equalTo(jsRequest.getJsonObject("defendant.personDefendant.bailStatus")));
        assertThat(jsonResponse.get("defendant.firstName"), is(jsRequest.getString("defendant.personDefendant.personDetails.firstName")));
        assertThat(jsonResponse.get("defendant.specificRequirements"), is(jsRequest.getString("defendant.personDefendant.personDetails.specificRequirements")));
        assertThat(jsonResponse.get("defendant.organisationName"), is(jsRequest.getString("defendant.legalEntityDefendant.organisation.name")));
        assertThat(jsonResponse.get("defendant.isYouth"), is(jsRequest.getBoolean("defendant.isYouth")));
    }

    public void verifyHearingListedFromAPI(final boolean isAllocated) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.range.search.hearings"), hearingData.getCourtCentreId(), isAllocated));

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, updatedDefendantData, updatedDefendantData.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, updatedDefendantData, listedCaseData.getCaseReference());


        poll(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()))
                .until(
                        status().is(OK),
                        payload().isJson(allOf(
                                withJsonPath(lastNameFilter),
                                withJsonPath(caseReferenceFilter),
                                withJsonPath("$.hearings[0].id",
                                        equalTo(hearingData.getId().toString())),
                                withJsonPath("$.hearings[0].jurisdictionType",
                                        equalTo(hearingData.getJurisdictionType())),
                                withJsonPath("$.hearings[0].courtCentreId",
                                        equalTo(hearingData.getCourtCentreId().toString())),
                                withJsonPath("$.hearings[0].type.id",
                                        equalTo(hearingData.getHearingTypeData().getTypeId().toString())),
                                withJsonPath("$.hearings[0].type.description",
                                        equalTo(hearingData.getHearingTypeData().getTypeDescription())),
                                withJsonPath("$.hearings[0].startDate",
                                        equalTo(hearingData.getHearingStartDate().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].id",
                                        equalTo(updatedDefendantData.getDefendantId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].masterDefendantId",
                                        equalTo(updatedDefendantData.getMasterDefendantId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].custodyTimeLimit",
                                        equalTo(updatedDefendantData.getCustodyTimeLimit())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].dateOfBirth",
                                        equalTo(updatedDefendantData.getDateOfBirth())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].bailStatus.code",
                                        equalTo(updatedDefendantData.getBailStatus().getCode())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].bailStatus.id",
                                        equalTo(updatedDefendantData.getBailStatus().getId().toString())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].firstName",
                                        equalTo(updatedDefendantData.getFirstName())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].lastName",
                                        equalTo(updatedDefendantData.getLastName())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].specificRequirements",
                                        equalTo(updatedDefendantData.getSpecificRequirements())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].organisationName",
                                        equalTo(updatedDefendantData.getLegalEntityName())),
                                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList",
                                        equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getRestrictFromCourtList()))
                        )));

    }


    private static com.jayway.jsonpath.JsonPath getJsonPathQueryForDefendantLastName(final HearingData hearing, final ListedCaseData listedCase, final UpdatedDefendantData defendant, final String expectedLastName) {
        final UpdateDefendantSteps.HearingDefendantFilter hearingDefendantFilter = new UpdateDefendantSteps.HearingDefendantFilter(hearing, listedCase, defendant).invoke();
        final Filter hearingFilter = hearingDefendantFilter.getHearingFilter();
        final Filter listingCaseFilter = hearingDefendantFilter.getListingCaseFilter();
        final Filter defendantFilter = hearingDefendantFilter.getDefendantFilter();
        final Filter firstNameFilter = filter(
                where("lastName").eq(expectedLastName)
        );
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].defendants[?][?]", hearingFilter, listingCaseFilter, defendantFilter, firstNameFilter);
    }

    private static com.jayway.jsonpath.JsonPath getJsonPathQueryForCaseReference(final HearingData hearing, final ListedCaseData listedCase, final UpdatedDefendantData defendant, final String expectedCaseReference) {
        final UpdateDefendantSteps.HearingDefendantFilter hearingDefendantFilter = new UpdateDefendantSteps.HearingDefendantFilter(hearing, listedCase, defendant).invoke();
        final Filter hearingFilter = hearingDefendantFilter.getHearingFilter();
        final Filter listingCaseFilter = hearingDefendantFilter.getListingCaseFilter();
        final Filter caseReferenceFilter = filter(
                where("caseReference").eq(expectedCaseReference)
        );
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].caseIdentifier.[?]", hearingFilter, listingCaseFilter, caseReferenceFilter);
    }


    private UpdateCaseDefendantData getUpdateCaseDefendantDetails(final UUID caseId, final UpdatedDefendantData defendantData) {

        return UpdateCaseDefendantData.updateCaseDefendantDetails()
            .withDefendant(Defendant.defendant()
                    .withId(defendantData.getDefendantId())
                    .withMasterDefendantId(defendantData.getMasterDefendantId())
                .withLegalEntityDefendant(of(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName(defendantData.getLegalEntityName())
                                .build())
                        .build()))
                .withPersonDefendant(of(PersonDefendant.personDefendant()
                    .withPersonDetails(Person.person()
                            .withLastName(defendantData.getLastName())
                            .withFirstName(of(defendantData.getFirstName()))
                            .withDateOfBirth(of(defendantData.getDateOfBirth()))
                            .withSpecificRequirements(of(defendantData.getSpecificRequirements()))
                            .withGender(Gender.FEMALE)
                        .build()
                    )
                    .withBailStatus(of(new BailStatus.Builder().withCode(defendantData.getBailStatus().getCode()).withDescription(defendantData.getBailStatus().getDescription()).withId(defendantData.getBailStatus().getId()).build()))
                    .withCustodyTimeLimit(of(defendantData.getCustodyTimeLimit()))
                    .build())
                )
                .withProsecutionCaseId(caseId)
                .withDefenceOrganisation(of(organisation()
                        .withName(defendantData.getOrganisationName())
                        .build()))
                .withPncId(of(defendantData.getPncId()))
                .withIsYouth(defendantData.getYouth())
                .withAliases(defendantData.getAliases())
                    .withAssociatedDefenceOrganisation(of(defendantData.getAssociatedDefenceOrganisation()))
                .build()
            ).build();
    }

    private static class HearingDefendantFilter {
        private final HearingData hearing;
        private final UpdatedDefendantData defendant;
        private final ListedCaseData listedCase;
        private Filter hearingFilter;
        private Filter defendantFilter;
        private Filter listingCaseFilter;

        public HearingDefendantFilter(final HearingData hearing, final ListedCaseData listedCase, final UpdatedDefendantData defendant) {
            this.hearing = hearing;
            this.listedCase = listedCase;
            this.defendant = defendant;
        }

        public Filter getHearingFilter() {
            return hearingFilter;
        }

        public Filter getDefendantFilter() {
            return defendantFilter;
        }

        public Filter getListingCaseFilter() {
            return listingCaseFilter;
        }

        public UpdateDefendantSteps.HearingDefendantFilter invoke() {
            hearingFilter = filter(where("id").is(hearing.getId().toString()));
            listingCaseFilter = filter(where("id").is(listedCase.getCaseId().toString()));
            defendantFilter = filter(where("id").is(defendant.getDefendantId().toString()));
            return this;
        }
    }

    @Override
    public void close() {
        try {
            publicEventDefendantUpdated.close();
            privateEventMessageDefendantsToBeUpdated.close();
            privateEventsMessageDefendantDetailsUpdated.close();
        } catch (final JMSException e) {
            LOGGER.error("Error closing message consumers and producers: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
