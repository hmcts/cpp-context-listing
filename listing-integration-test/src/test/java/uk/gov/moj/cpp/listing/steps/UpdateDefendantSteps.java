package uk.gov.moj.cpp.listing.steps;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.hasItem;
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

import java.util.ArrayList;
import java.util.List;
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
        whenPublicEventProgressionCaseDefendantChangedIsPublished(updatedDefendantData);
    }

    public void whenPublicEventProgressionCaseDefendantChangedIsPublished(final UpdatedDefendantData defendantData) {
        final UpdateCaseDefendantData updateCaseDefendantDetails = getUpdateCaseDefendantDetails(caseId, defendantData);
        final JsonObject updateCaseDefendantDetailsObject = (JsonObject) objectToJsonValueConverter.convert(updateCaseDefendantDetails);

        sendMessage(
                publicEventDefendantUpdated,
                PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED,
                updateCaseDefendantDetailsObject,
                metadataOf(randomUUID(), PUBLIC_EVENT_SELECTOR_PROGRESSION_CASE_DEFENDANT_CHANGED).withUserId(randomUUID().toString()).build());
    }

    /**
     * Polls the hearing view until the first listed defendant's {@code isYouth} flag matches the expected value.
     */
    /**
     * Polls until the target defendant (by hearing, case and defendant id) is present with the expected {@code isYouth} value.
     * Uses a defendant-object filter so missing {@code isYouth} on the payload does not false-pass.
     */
    @SuppressWarnings("rawtypes")
    public void verifyListedDefendantIsYouthWithJmsDelay(final boolean isAllocated, final boolean expectedIsYouth) {
        pollForHearingWithJmsDelay(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(), new Matcher[]{
                withJsonPath(getJsonPathQueryForDefendantWithIsYouth(expectedIsYouth))
        });
    }

    /**
     * Verifies defendant field updates (name, id, etc.) without asserting {@code isYouth}.
     */
    public void verifyDefendantDetailsUpdatedWithJmsDelay(final boolean isAllocated) {
        verifyHearingListedFromAPIWithJmsDelay(isAllocated, null);
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
                withJsonPath(getJsonPathQueryForDefendantAttribute("id"),
                        hasItem(equalTo(updatedDefendantData.getDefendantId().toString()))),
                withJsonPath(getJsonPathQueryForDefendantAttribute("masterDefendantId"),
                        hasItem(equalTo(updatedDefendantData.getMasterDefendantId().toString()))),
                withJsonPath(getJsonPathQueryForDefendantAttribute("bailStatus.code"),
                        hasItem(equalTo(updatedDefendantData.getBailStatus().getCode()))),
                withJsonPath(getJsonPathQueryForDefendantAttribute("bailStatus.id"),
                        hasItem(equalTo(updatedDefendantData.getBailStatus().getId().toString()))),
                withJsonPath(getJsonPathQueryForDefendantAttribute("firstName"),
                        hasItem(equalTo(updatedDefendantData.getFirstName()))),
                withJsonPath(getJsonPathQueryForDefendantAttribute("restrictFromCourtList"),
                        hasItem(equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getRestrictFromCourtList())))
        });

    }

    /**
     * JMS-aware version of verifyHearingListedFromAPI for handling asynchronous message processing timing issues.
     */
    public void verifyHearingListedFromAPIWithJmsDelay(final boolean isAllocated) {
        verifyHearingListedFromAPIWithJmsDelay(isAllocated, null);
    }

    /**
     * Same as {@link #verifyHearingListedFromAPIWithJmsDelay(boolean)} with an optional assertion on the first listed defendant's youth flag.
     */
    @SuppressWarnings("rawtypes")
    public void verifyHearingListedFromAPIWithJmsDelay(final boolean isAllocated, final Boolean expectedIsYouth) {

        final com.jayway.jsonpath.JsonPath lastNameFilter = getJsonPathQueryForDefendantLastName(hearingData, listedCaseData, updatedDefendantData, updatedDefendantData.getLastName());
        final com.jayway.jsonpath.JsonPath caseReferenceFilter = getJsonPathQueryForCaseReference(hearingData, listedCaseData, updatedDefendantData, listedCaseData.getCaseReference());

        final List<Matcher> matchers = new ArrayList<>();
        matchers.add(withJsonPath(lastNameFilter));
        matchers.add(withJsonPath(caseReferenceFilter));
        matchers.add(withJsonPath("$.hearings[0].id",
                equalTo(hearingData.getId().toString())));
        matchers.add(withJsonPath("$.hearings[0].jurisdictionType",
                equalTo(hearingData.getJurisdictionType())));
        matchers.add(withJsonPath("$.hearings[0].courtCentreId",
                equalTo(hearingData.getCourtCentreId().toString())));
        matchers.add(withJsonPath("$.hearings[0].type.id",
                equalTo(hearingData.getHearingTypeData().getTypeId().toString())));
        matchers.add(withJsonPath("$.hearings[0].type.description",
                equalTo(hearingData.getHearingTypeData().getTypeDescription())));
        matchers.add(withJsonPath("$.hearings[0].startDate",
                equalTo(hearingData.getHearingStartDate().toString())));
        matchers.add(withJsonPath(getJsonPathQueryForDefendantAttribute("id"),
                hasItem(equalTo(updatedDefendantData.getDefendantId().toString()))));
        matchers.add(withJsonPath(getJsonPathQueryForDefendantAttribute("masterDefendantId"),
                hasItem(equalTo(updatedDefendantData.getMasterDefendantId().toString()))));
        matchers.add(withJsonPath(getJsonPathQueryForDefendantAttribute("bailStatus.code"),
                hasItem(equalTo(updatedDefendantData.getBailStatus().getCode()))));
        matchers.add(withJsonPath(getJsonPathQueryForDefendantAttribute("bailStatus.id"),
                hasItem(equalTo(updatedDefendantData.getBailStatus().getId().toString()))));
        matchers.add(withJsonPath(getJsonPathQueryForDefendantAttribute("firstName"),
                hasItem(equalTo(updatedDefendantData.getFirstName()))));
        matchers.add(withJsonPath(getJsonPathQueryForDefendantAttribute("restrictFromCourtList"),
                hasItem(equalTo(hearingData.getListedCases().get(0).getDefendants().get(0).getRestrictFromCourtList()))));
        if (expectedIsYouth != null) {
            matchers.add(withJsonPath(getJsonPathQueryForDefendantIsYouth(), hasItem(equalTo(expectedIsYouth))));
        }

        pollForHearingWithJmsDelay(hearingData.getCourtCentreId().toString(), isAllocated, getLoggedInUser().toString(),
                matchers.toArray(new Matcher[0]));

    }

    private com.jayway.jsonpath.JsonPath getJsonPathQueryForDefendantIsYouth() {
        return getJsonPathQueryForDefendantAttribute("isYouth");
    }

    private com.jayway.jsonpath.JsonPath getJsonPathQueryForDefendantWithIsYouth(final boolean expectedIsYouth) {
        final HearingDefendantFilter hearingDefendantFilter = new HearingDefendantFilter(hearingData, listedCaseData, updatedDefendantData).invoke();
        final Filter isYouthFilter = filter(where("isYouth").eq(expectedIsYouth));
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].defendants[?][?]",
                hearingDefendantFilter.getHearingFilter(),
                hearingDefendantFilter.getListingCaseFilter(),
                hearingDefendantFilter.getDefendantFilter(),
                isYouthFilter);
    }

    private com.jayway.jsonpath.JsonPath getJsonPathQueryForDefendantAttribute(final String attribute) {
        final HearingDefendantFilter hearingDefendantFilter = new HearingDefendantFilter(hearingData, listedCaseData, updatedDefendantData).invoke();
        return com.jayway.jsonpath.JsonPath.compile("$.hearings[?].listedCases[?].defendants[?]." + attribute,
                hearingDefendantFilter.getHearingFilter(),
                hearingDefendantFilter.getListingCaseFilter(),
                hearingDefendantFilter.getDefendantFilter());
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
