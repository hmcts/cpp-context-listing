package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.Integer.valueOf;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClientProvider.newPublicJmsMessageConsumerClientProvider;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.CourtCentre;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.HearingType;
import uk.gov.justice.core.courts.JurisdictionType;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.ListDefendantRequest;
import uk.gov.justice.core.courts.ListHearingRequest;
import uk.gov.justice.core.courts.Offence;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.AddDefendantForCourtProceedingsData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

public class AddDefendantSteps extends AbstractIT {

    private static final String PUBLIC_EVENT_SELECTOR_PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS = "public.progression.defendants-added-to-court-proceedings";
    private static final String PUBLIC_EVENT_SELECTOR_DEFENDANT_DETAILS_ADDED_FOR_COURT_PROCEEDINGS = "public.listing.new-defendant-added-for-court-proceedings";

    final UUID DEFENDANT_ID = UUID.randomUUID();
    final UUID MASTER_DEFENDANT_ID = UUID.randomUUID();

    private final HearingData hearingData;
    private final UUID caseId;
    private final JmsMessageProducerClient publicEventDefendantAdded;
    private final JmsMessageConsumerClient publicEventsMessageNewDefendantAdded;
    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);
    private String request;

    public AddDefendantSteps(final UUID caseId, final HearingData hearingData) {
        this.caseId = caseId;
        this.hearingData = hearingData;

        publicEventDefendantAdded = newPublicJmsMessageProducerClientProvider().getMessageProducerClient();
        publicEventsMessageNewDefendantAdded = newPublicJmsMessageConsumerClientProvider()
                .withEventNames(PUBLIC_EVENT_SELECTOR_DEFENDANT_DETAILS_ADDED_FOR_COURT_PROCEEDINGS).getMessageConsumerClient();

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseDefendantsAddedPublicEventIsPublished() {
        final AddDefendantForCourtProceedingsData addDefendantForCourtProceedingsData = getAddDefendantDetails(caseId);
        final JsonObject addDefendantDetailsForCourtProceedingsObject = (JsonObject) objectToJsonValueConverter.convert(addDefendantForCourtProceedingsData);

        sendMessage(
                publicEventDefendantAdded,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS,
                addDefendantDetailsForCourtProceedingsObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_ADD_DEFENDANTS_TO_COURT_PROCEEDINGS).withUserId(randomUUID().toString()).build());

        request = addDefendantDetailsForCourtProceedingsObject.toString();
    }

    public void verifyPublicEventDefendantAddedInActiveMQ() {
        final JsonPath jsRequest = new JsonPath(request);

        final JsonPath jsonResponse = retrieveMessage(publicEventsMessageNewDefendantAdded);

        assertThat(jsonResponse.get("caseId"), is(caseId.toString()));
        assertThat(jsonResponse.get("hearingId"), is(hearingData.getId().toString()));
        assertThat(jsonResponse.get("defendantId"), is(jsRequest.getString("defendants[0].id")));
        assertThat(jsonResponse.get("courtCentre.id"), is(hearingData.getCourtCentreId().toString()));
        assertThat(jsonResponse.get("courtCentre.roomId"), is(hearingData.getCourtRoomId().toString()));
        assertThat(jsonResponse.get("hearingDateTime"), notNullValue());
    }

    public void verifyHearingListedFromAPI(final boolean isAllocated) {
        final AddDefendantForCourtProceedingsData addDefendantForCourtProceedingsData = getAddDefendantDetails(caseId);
        final Defendant defendant = addDefendantForCourtProceedingsData.getDefendants().get(0);
        final Person personDetails = defendant.getPersonDefendant().getPersonDetails();

        pollForHearing(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
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
        });
    }

    private AddDefendantForCourtProceedingsData getAddDefendantDetails(final UUID caseId) {


        final List<uk.gov.justice.core.courts.Defendant> defendant = Arrays.asList(Defendant.defendant()
                .withId(DEFENDANT_ID)
                .withMasterDefendantId(MASTER_DEFENDANT_ID)
                .withCourtProceedingsInitiated(ZonedDateTime.now())
                .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                        .withOrganisation(Organisation.organisation()
                                .withName("withOrganisationName")
                                .build())
                        .build())
                .withPersonDefendant(PersonDefendant.personDefendant()
                        .withPersonDetails(Person.person()
                                .withLastName("Last Name")
                                .withFirstName("FIRST NAME")
                                .withDateOfBirth("1980-07-15")
                                .withSpecificRequirements("Screen")
                                .withGender(Gender.FEMALE)
                                .build()
                        )
                        .withBailStatus(new BailStatus.Builder().withCode("C").withDescription("Custody or remanded into custody").withId(UUID.fromString("12e69486-4d01-3403-a50a-7419ca040635")).build())
                        .withCustodyTimeLimit("2017-10-05")
                        .build()
                )
                .withOffences(Arrays.asList(Offence.offence()
                        .withId(UUID.randomUUID())
                        .withOffenceCode("TFL123")
                        .withStartDate("2019-05-01")
                        .withEndDate(null)
                        .withOffenceTitle("TFL Ticket Dodger")
                        .withOffenceDefinitionId(UUID.randomUUID())
                        .withCount(valueOf(0))
                        .withOrderIndex(valueOf(0))
                        .withOffenceLegislation("legislation")
                        .withWording("TFL ticket dodged")
                        .build()))
                .withProsecutionCaseId(caseId)
                .withDefenceOrganisation(organisation()
                        .withName("withOrganisationName")
                        .build())
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
                        .withRoomId(UUID.randomUUID())
                        .build())
                .withHearingType(HearingType.hearingType()
                        .withDescription("Sentence").withId(UUID.randomUUID()).build())
                .withJurisdictionType(JurisdictionType.CROWN)
                .withListDefendantRequests(asList(ListDefendantRequest.listDefendantRequest()
                        .withDefendantId(defendantId)
                        .withDefendantOffences(asList(UUID.randomUUID()))
                        .withProsecutionCaseId(prosecutionCaseId)
                        .build()))
                .build();

    }
}


