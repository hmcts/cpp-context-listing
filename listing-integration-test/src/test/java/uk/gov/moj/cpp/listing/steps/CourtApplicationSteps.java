package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.text.MessageFormat.format;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.justice.services.test.utils.core.http.RequestParamsBuilder.requestParams;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.it.util.RestPollerHelper.pollWithDefaults;
import static uk.gov.moj.cpp.listing.utils.FileUtil.getPayload;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.getBaseUri;
import static uk.gov.moj.cpp.listing.utils.PropertyUtil.readConfig;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.retrieveMessage;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ApplicationStatus;
import uk.gov.justice.core.courts.BreachType;
import uk.gov.justice.core.courts.CourtApplication;
import uk.gov.justice.core.courts.CourtApplicationCase;
import uk.gov.justice.core.courts.CourtApplicationParty;
import uk.gov.justice.core.courts.CourtApplicationType;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.Jurisdiction;
import uk.gov.justice.core.courts.LinkType;
import uk.gov.justice.core.courts.OffenceActiveOrder;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.core.courts.ProsecutionCaseIdentifier;
import uk.gov.justice.core.courts.SummonsTemplateType;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageConsumerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.AddCourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationData;
import uk.gov.moj.cpp.listing.steps.data.CourtApplicationUpdateData;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.HearingsData;

import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import io.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CourtApplicationSteps extends AbstractIT {
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
    private static final String PUBLIC_EVENT_APPLICATION_ADD_COURT_APPLICATION_FOR_HEARING = "public.listing.court-application-added-for-hearing";
    private static final String PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED = "public.progression.court-application-changed";
    private static final String POSTCODE = "CR1 4BX";
    private static final String MEDIA_TYPE_SEARCH_HEARINGS_JSON = "application/vnd.listing.search.hearings+json";

    private JmsMessageProducerClient publicEventCourtApplicationUpdated;
    private JmsMessageProducerClient publicEventCourtApplicationAdded;
    private JmsMessageConsumerClient publicMessageConsumerCourtApplicationAddedForHearing;

    private String request;

    private final HearingsData hearingsData;

    ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter(objectMapper);

    public CourtApplicationSteps(HearingsData hearingsData) {
        this.hearingsData = hearingsData;

        publicEventCourtApplicationUpdated = publicEvents.createPublicProducer();
        publicEventCourtApplicationAdded = publicEvents.createPublicProducer();
        publicMessageConsumerCourtApplicationAddedForHearing = publicEvents.createPublicConsumer(PUBLIC_EVENT_APPLICATION_ADD_COURT_APPLICATION_FOR_HEARING);

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenCaseCourtApplicationIsAddedToListingAndHearingIsExtended() {
        AddCourtApplicationData addCourtApplicationData = getCourtApplicationForHearingData(hearingsData);
        final JsonObject courtApplicationUpdateDataObject = (JsonObject) objectToJsonValueConverter.convert(addCourtApplicationData);
        sendMessage(
                publicEventCourtApplicationAdded,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED,
                courtApplicationUpdateDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
        request = courtApplicationUpdateDataObject.toString();
    }

    public void whenCaseCourtApplicationAndLinkedCaseAreAddedToListingAndHearingIsExtended() {
        AddCourtApplicationData addCourtApplicationData = getCourtApplicationForHearingData(hearingsData);

        final String eventPayloadString = getPayload("prosecution-case.json")
                .replaceAll("HEARING_ID", hearingsData.getHearingData().get(0).getId().toString())
                .replaceAll("CASE_ID_1", randomUUID().toString())
                .replaceAll("DEFENDANT_ID_1", randomUUID().toString())
                .replaceAll("OFFENCE_ID_1", randomUUID().toString())
                .replaceAll("CASE_ID_2", randomUUID().toString())
                .replaceAll("DEFENDANT_ID_2", randomUUID().toString())
                .replaceAll("OFFENCE_ID_2", randomUUID().toString());

        final JsonObject hearingExtendedDataObject = new StringToJsonObjectConverter().convert(eventPayloadString);
        ProsecutionCase prosecutionCase = jsonObjectToObjectConverter.convert(hearingExtendedDataObject.getJsonArray("prosecutionCases").getJsonObject(0), ProsecutionCase.class);

        addCourtApplicationData.setProsecutionCases(Stream.of(prosecutionCase).collect(Collectors.toList()));
        final JsonObject courtApplicationUpdateDataObject = (JsonObject) objectToJsonValueConverter.convert(addCourtApplicationData);
        sendMessage(
                publicEventCourtApplicationAdded,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED,
                courtApplicationUpdateDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_HEARING_EXTENDED).withUserId(randomUUID().toString()).build());
        request = courtApplicationUpdateDataObject.toString();
    }

    public void whenCaseCourtApplicationUpdatedPublicEventIsPublished() {
        CourtApplicationUpdateData courtApplicationUpdateData = getUpdateCourtApplicationForHearingsData(hearingsData);
        final JsonObject courtApplicationUpdateDataObject = (JsonObject) objectToJsonValueConverter.convert(courtApplicationUpdateData);

        sendMessage(
                publicEventCourtApplicationUpdated,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED,
                courtApplicationUpdateDataObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED).withUserId(randomUUID().toString()).build());

        request = courtApplicationUpdateDataObject.toString();
        LOGGER.info("Event published:\n\tMedia type = {} \n\tPayload = {}\n\n, \n\tHeader = {}", PUBLIC_EVENT_SELECTOR_PROGRESSION_COURT_APPLICATION_CHANGED, request, getLoggedInHeader());
    }

    public void verifyPublicEventCourtApplicationAdded() {
        JsonPath jsRequest = new JsonPath(request);

        JsonPath jsonResponse = retrieveMessage(publicMessageConsumerCourtApplicationAddedForHearing);
        LOGGER.debug("jsonResponse from publicMessageConsumerCourtApplicationAddedForHearing: {}", jsonResponse.prettify());
        assertThat(jsonResponse.get("hearingId"), is(jsRequest.getString("hearingId")));
        assertThat(jsonResponse.get("courtApplication.id"), is(jsRequest.getString("courtApplication.id")));
    }

    public void verifyCourtApplicationUpdatedFromAPI() {
        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), false, getLoggedInUser().toString(), new Matcher[]{
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
                withJsonPath("$.hearings[0].courtApplications[0].linkedCaseIds[0]",
                        equalTo(LINKED_CASE_ID.toString())),
                withJsonPath("$.hearings[0].courtApplications[0].id",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getId().toString())),
                withJsonPath("$.hearings[0].courtApplications[0].parentApplicationId",
                        equalTo(LINKED_APPLICATION_ID.toString())),
                withJsonPath("$.hearings[0].courtApplications[0].restrictFromCourtList",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRestrictFromCourtList())),
                withJsonPath("$.hearings[0].courtApplications[0].restrictCourtApplicationType",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRestrictCourtApplicationType()))
        });
    }

    public void verifyCourtApplicationAddedFromAPI(final boolean allocated) {
        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), allocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].courtApplications.size()", is(1)),
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
                withJsonPath("$.hearings[0].courtApplications[0].linkedCaseIds[0]",
                        Matchers.anyOf(equalTo(LINKED_CASE_ID.toString()),
                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getLinkedCaseId().toString()))),
                withJsonPath("$.hearings[0].courtApplications[0].parentApplicationId",
                        Matchers.anyOf(equalTo(LINKED_APPLICATION_ID.toString()),
                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getParentApplicationId().toString()))),
                withJsonPath("$.hearings[0].courtApplications[0].restrictFromCourtList",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRestrictFromCourtList())),
                withJsonPath("$.hearings[0].courtApplications[0].restrictCourtApplicationType",
                        equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRestrictCourtApplicationType())),
                withJsonPath("$.hearings[0].courtApplications[0].respondents[0].firstName",
                        Matchers.anyOf(equalTo(RESPONDENT_FIRST_NAME),
                                equalTo(hearingsData.getHearingData().get(0).getCourtApplications().get(0).getRespondent().getFirstName())))
        });
    }

    public void verifyCaseCountFromAPI(final boolean allocated, final int caseCount) {

        final Filter idFilter = filter(where("id").is(hearingsData.getHearingData().get(0).getId().toString()));
        final com.jayway.jsonpath.JsonPath hearingIdFilter = com.jayway.jsonpath.JsonPath.compile("$.hearings[?]", idFilter);

        pollForHearing(hearingsData.getHearingData().get(0).getCourtCentreId().toString(), allocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(hearingIdFilter),
                withJsonPath("$.hearings[0].listedCases.size()", is(caseCount))
        });
    }

    public void verifyTimeLine(HearingData hearingsData) {
        final String searchHearingUrl = String.format("%s/%s", getBaseUri(),
                format(readConfig().getProperty("listing.unallocated-hearings_for_application"),
                        hearingsData.getCourtApplications().get(0).getId()));

        pollWithDefaults(requestParams(searchHearingUrl, MEDIA_TYPE_SEARCH_HEARINGS_JSON).withHeader(USER_ID, getLoggedInUser()).build())
                .until(
                        status().is(Response.Status.OK),
                        payload().isJson(allOf(
                                        withJsonPath("$.hearings[0].id", is(hearingsData.getId().toString())),
                                        withJsonPath("$.hearings[0].courtApplications.size()", is(1)),
                                        withJsonPath("$.hearings[0].courtApplications[0].id", is(hearingsData.getCourtApplications().get(0).getId().toString()))
                                )
                        ));
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
                        .withPersonDetails(Person.person()
                                .withFirstName(APPLICANT_FIRST_NAME)
                                .withLastName(APPLICANT_LAST_NAME)
                                .withGender(Gender.FEMALE)
                                .withAddress(buildAddress())
                                .build())
                        .withId(APPLICANT_ID)
                        .withSummonsRequired(false)
                        .withNotificationRequired(false)
                        .build())
                .withSubject(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(APPLICANT_FIRST_NAME)
                                .withLastName(APPLICANT_LAST_NAME)
                                .withGender(Gender.FEMALE)
                                .withAddress(buildAddress())
                                .build())
                        .withId(APPLICANT_ID)
                        .withSummonsRequired(false)
                        .withNotificationRequired(false)
                        .build())
                .withRespondents(singletonList(CourtApplicationParty.courtApplicationParty()
                        .withPersonDetails(Person.person()
                                .withFirstName(RESPONDENT_FIRST_NAME)
                                .withLastName(RESPONDENT_LAST_NAME)
                                .withGender(Gender.MALE)
                                .withAddress(buildAddress())
                                .build())
                        .withId(RESPONDENT_ID)
                        .withSummonsRequired(false)
                        .withNotificationRequired(false)
                        .build()))
                .withId(courtApplicationData.getId())
                .withCourtApplicationCases(singletonList(CourtApplicationCase.courtApplicationCase().withProsecutionCaseId(LINKED_CASE_ID)
                        .withProsecutionCaseIdentifier(ProsecutionCaseIdentifier.prosecutionCaseIdentifier()
                                .withCaseURN(STRING.next())
                                .withProsecutionAuthorityId(randomUUID())
                                .withProsecutionAuthorityCode(STRING.next()).build())
                        .withIsSJP(false)
                        .withCaseStatus("ACTIVE")
                        .build()))
                .withParentApplicationId(LINKED_APPLICATION_ID)
                .withType(CourtApplicationType.courtApplicationType()
                        .withType(APPLICATION_TYPE)
                        .withId(UUID.randomUUID())
                        .withCode(STRING.next())
                        .withLegislation(STRING.next())
                        .withCategoryCode(STRING.next())
                        .withLinkType(LinkType.LINKED)
                        .withSummonsTemplateType(SummonsTemplateType.GENERIC_APPLICATION)
                        .withJurisdiction(Jurisdiction.CROWN)
                        .withBreachType(BreachType.GENERIC_BREACH)
                        .withAppealFlag(false)
                        .withApplicantAppellantFlag(false)
                        .withPleaApplicableFlag(false)
                        .withCommrOfOathFlag(false)
                        .withCourtOfAppealFlag(false)
                        .withCourtExtractAvlFlag(false)
                        .withProsecutorThirdPartyFlag(false)
                        .withSpiOutApplicableFlag(false)
                        .withOffenceActiveOrder(OffenceActiveOrder.COURT_ORDER)
                        .build())
                .withApplicationStatus(ApplicationStatus.LISTED)
                .withApplicationReceivedDate(LocalDate.now().toString())
                .withApplicationReference(STRING.next())
                .withApplicationParticulars(STRING.next())
                .build();
    }

    private Address buildAddress() {
        return Address.address()
                .withAddress1(STRING.next())
                .withAddress2(STRING.next())
                .withAddress3(STRING.next())
                .withAddress4(STRING.next())
                .withAddress5(STRING.next())
                .withPostcode(POSTCODE)
                .build();
    }
}
