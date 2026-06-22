package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.justice.core.courts.Organisation.organisation;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearing;
import static uk.gov.moj.cpp.listing.helper.SearchHearingHelper.pollForHearingWithJmsDelay;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.publicEvents;
import static uk.gov.moj.cpp.listing.utils.QueueUtil.sendMessage;

import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.LegalEntityDefendant;
import uk.gov.justice.core.courts.Organisation;
import uk.gov.justice.core.courts.Person;
import uk.gov.justice.core.courts.PersonDefendant;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.moj.cpp.listing.it.AbstractIT;
import uk.gov.moj.cpp.listing.steps.data.HearingData;
import uk.gov.moj.cpp.listing.steps.data.ListedCaseData;
import uk.gov.moj.cpp.listing.steps.data.UpdateCaseDefendantData;
import uk.gov.moj.cpp.listing.steps.data.UpdatedDefendantData;

import java.util.UUID;

import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Filter;
import org.hamcrest.Matcher;

public class UpdateDefendantSteps extends AbstractIT {
    private static final String PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED = "public.progression.case-defendant-changed";

    private final JmsMessageProducerClient publicEventDefendantUpdated;

    private final HearingData hearingData;
    private final UpdatedDefendantData updatedDefendantData;
    private final ListedCaseData listedCaseData;
    private final UUID caseId;

    private final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();
    private final ObjectToJsonValueConverter objectToJsonValueConverter = new ObjectToJsonValueConverter(objectMapper);

    public UpdateDefendantSteps(final UUID caseId, final HearingData hearingData, final UpdatedDefendantData defendantData) {
        this.caseId = caseId;
        this.hearingData = hearingData;
        this.listedCaseData = hearingData.getListedCases().get(0);
        this.updatedDefendantData = defendantData;

        publicEventDefendantUpdated = publicEvents.createPublicProducer();

        givenAUserHasLoggedInAsAListingOfficer(USER_ID_VALUE);
    }

    public void whenPublicEventProgressionCaseDefendantsUpdatedIsPublished() {
        final UpdateCaseDefendantData updateCaseDefendantDetails = getUpdateCaseDefendantDetails(caseId, updatedDefendantData);
        final JsonObject updateCaseDefendantDetailsObject = (JsonObject) objectToJsonValueConverter.convert(updateCaseDefendantDetails);

        sendMessage(
                publicEventDefendantUpdated,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED,
                updateCaseDefendantDetailsObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED).withUserId(randomUUID().toString()).build());
    }

    public void verifyHearingListedFromAPI(final boolean isAllocated) {

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, updatedDefendantData, updatedDefendantData.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, updatedDefendantData, listedCaseData.getCaseReference());

        pollForHearing(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{

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
//                withJsonPath("$.hearings[0].listedCases[0].defendants[0].custodyTimeLimit",
//                        equalTo(updatedDefendantData.getCustodyTimeLimit())),
//                withJsonPath("$.hearings[0].listedCases[0].defendants[0].dateOfBirth",
//                        equalTo(updatedDefendantData.getDateOfBirth())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].bailStatus.code",
                        equalTo(updatedDefendantData.getBailStatus().getCode())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].bailStatus.id",
                        equalTo(updatedDefendantData.getBailStatus().getId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].firstName",
                        equalTo(updatedDefendantData.getFirstName())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].lastName",
                        equalTo(updatedDefendantData.getLastName())),
//                withJsonPath("$.hearings[0].listedCases[0].defendants[0].specificRequirements",
//                        equalTo(updatedDefendantData.getSpecificRequirements())),
//                withJsonPath("$.hearings[0].listedCases[0].defendants[0].organisationName",
//                        equalTo(updatedDefendantData.getLegalEntityName())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList",
                        equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getRestrictFromCourtList()))
        });

    }

    /**
     * JMS-aware version of verifyHearingListedFromAPI for handling asynchronous message processing timing issues.
     */
    public void verifyHearingListedFromAPIWithJmsDelay(final boolean isAllocated) {

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, updatedDefendantData, updatedDefendantData.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, updatedDefendantData, listedCaseData.getCaseReference());

        // Use JMS-aware polling to handle asynchronous message processing
        pollForHearingWithJmsDelay(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{

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
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].bailStatus.code",
                        equalTo(updatedDefendantData.getBailStatus().getCode())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].bailStatus.id",
                        equalTo(updatedDefendantData.getBailStatus().getId().toString())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].firstName",
                        equalTo(updatedDefendantData.getFirstName())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].lastName",
                        equalTo(updatedDefendantData.getLastName())),
                withJsonPath("$.hearings[0].listedCases[0].defendants[0].restrictFromCourtList",
                        equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getRestrictFromCourtList()))
        });

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
                        .withLegalEntityDefendant(LegalEntityDefendant.legalEntityDefendant()
                                .withOrganisation(Organisation.organisation()
                                        .withName(defendantData.getLegalEntityName())
                                        .build())
                                .build())
                        .withPersonDefendant(PersonDefendant.personDefendant()
                                .withPersonDetails(Person.person()
                                        .withLastName(defendantData.getLastName())
                                        .withFirstName(defendantData.getFirstName())
                                        .withDateOfBirth(defendantData.getDateOfBirth())
                                        .withSpecificRequirements(defendantData.getSpecificRequirements())
                                        .withGender(Gender.FEMALE)
                                        .build()
                                )
                                .withBailStatus(new BailStatus.Builder().withCode(defendantData.getBailStatus().getCode()).withDescription(defendantData.getBailStatus().getDescription()).withId(defendantData.getBailStatus().getId()).build())
                                .withCustodyTimeLimit(defendantData.getCustodyTimeLimit())
                                .build()
                        )
                        .withProsecutionCaseId(caseId)
                        .withDefenceOrganisation(organisation()
                                .withName(defendantData.getOrganisationName())
                                .build())
                        .withPncId(defendantData.getPncId())
                        .withIsYouth(defendantData.getYouth().orElse(null))
                        .withAliases(defendantData.getAliases())
                        .withAssociatedDefenceOrganisation(defendantData.getAssociatedDefenceOrganisation())
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

}
