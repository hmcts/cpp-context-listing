package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.justice.core.courts.ProsecutionCase.prosecutionCase;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;


import java.util.Collection;
import java.util.stream.Stream;
import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseIdentifier;
import uk.gov.justice.listing.events.CaseIdentifierUpdated;
import uk.gov.justice.listing.events.CaseUpdateDefendantProceedingsUpdated;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdated;
import uk.gov.justice.listing.events.DefendantCourtProceedingsUpdatedV2;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingAllocatedForListingV2;
import uk.gov.justice.listing.events.HearingDay;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingTrialVacated;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.listing.events.OffenceIds;
import uk.gov.justice.listing.events.Prosecutor;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.entity.ListingNumbers;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;
import uk.gov.moj.cpp.listing.persistence.repository.ListingNumbersRepository;

@SuppressWarnings({"squid:S3655", "squid:S1067"})
@ServiceComponent(Component.EVENT_LISTENER)
public class HearingEventListener {

    private static final boolean ALLOCATED = true;
    private static final boolean VACATED = true;
    private static final boolean NON_VACATED = false;
    private static final Logger LOGGER = LoggerFactory.getLogger(HearingEventListener.class);
    private static final String LISTED_CASES_FIELD = "listedCases";
    private static final String FIELD_VACATE_TRIAL_REASON = "vacatedTrialReasonId";
    private static final String FIELD_IS_VACATED_TRIAL = "isVacatedTrial";
    private static final String FIELD_ALLOCATED = "allocated";
    private static final String FIELD_COURT_ROOM_ID = "courtRoomId";
    private final JsonEntityFinder jsonEntityFinder;
    private final HearingRepository hearingRepository;
    private final ObjectMapper mapper;
    private HearingSearchSyncService hearingSearchSyncService;

    @Inject
    private JsonObjectToObjectConverter jsonToObjectConverter;

    @Inject
    private StringToJsonObjectConverter stringToJsonObjectConverter;

    @Inject
    private ListingNumbersRepository listingNumbersRepository;

    @Inject
    public HearingEventListener(final HearingRepository hearingRepository,
                                final ObjectMapper mapper,
                                final HearingSearchSyncService hearingSearchSyncService, ListingNumbersRepository listingNumbersRepository) {
        this.hearingRepository = hearingRepository;
        this.jsonEntityFinder = using(hearingRepository);
        this.mapper = mapper;
        this.hearingSearchSyncService = hearingSearchSyncService;
        this.listingNumbersRepository = listingNumbersRepository;
    }

    @Handles("listing.events.hearing-listed")
    public void hearingListed(final Envelope<HearingListed> event) {
        final HearingListed hearingListed = event.payload();
        final JsonNode hearingJsonNode = convertToObject(hearingListed.getHearing());
        final UUID hearingId = hearingListed.getHearing().getId();
        LOGGER.info("'listing.events.hearing-listed' received hearingId {}", hearingId);
        final Hearing hearing = new Hearing(hearingId, hearingJsonNode);
        hearingSearchSyncService.syncEntity(hearing);
        hearingRepository.save(hearing);
    }

    @Handles("listing.events.hearing-allocated-for-listing")
    public void hearingAllocated(final Envelope<HearingAllocatedForListing> event) {
        final HearingAllocatedForListing hearingAllocatedForListing = event.payload();
        final UUID hearingId = hearingAllocatedForListing.getHearingId();

        if(nonNull(hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds())) {
            final List<ListingNumbers> offencesWithListingNumbers = hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds().stream()
                    .flatMap(prosecutionCaseDefendantOffenceIdsV2 -> prosecutionCaseDefendantOffenceIdsV2.getDefendants().stream())
                    .flatMap(defendant -> defendant.getOffenceIds().stream())
                    .map(listingNumbersRepository::upset)
                    .collect(toList());

            updateViewStoreWithListingNumberAndAllocatedHearing(hearingId, offencesWithListingNumbers);
        }else{
            jsonEntityFinder.find(hearingId).put(FIELD_ALLOCATED, ALLOCATED).remove("unscheduled").save();
        }
        LOGGER.info("'listing.events.hearing-allocated-for-listing' received hearingId {} ", hearingId);
        hearingSearchSyncService.sync(hearingId);
    }

    @Handles("listing.events.hearing-allocated-for-listing-v2")
    public void hearingAllocatedV2(final Envelope<HearingAllocatedForListingV2> event) {
        final HearingAllocatedForListingV2 hearingAllocatedForListing = event.payload();
        final UUID hearingId = hearingAllocatedForListing.getHearingId();

        final List<ListingNumbers> offencesWithListingNumbersForCase = ofNullable(hearingAllocatedForListing.getProsecutionCaseDefendantsOffenceIds()).map(Collection::stream).orElseGet(Stream::empty)
                .flatMap(prosecutionCaseDefendantOffenceIdsV2 -> prosecutionCaseDefendantOffenceIdsV2.getDefendants().stream())
                .flatMap(defendant -> defendant.getOffenceIds().stream())
                .map(OffenceIds::getId)
                .map(listingNumbersRepository::upset)
                .collect(toList());

        updateViewStoreWithListingNumberAndAllocatedHearing(hearingId, offencesWithListingNumbersForCase);
        LOGGER.info("'listing.events.hearing-allocated-for-listing-v2' received hearingId {}", hearingId);
        hearingSearchSyncService.sync(hearingId);
    }

    /**
     * CourtRoom id fields needs to be clean up like allocated field when the hearing is
     * unallocated. Manage Hearing links in the UI  appears regarding whether courtRoomId field  is
     * null or not null in both hearing and hearingDays.
     */
    @Handles("listing.events.hearing-unallocated-for-listing")
    public void hearingUnallocated(final Envelope<HearingUnallocatedForListing> event) {
        final HearingUnallocatedForListing hearingUnallocatedForListing = event.payload();
        final UUID hearingId = hearingUnallocatedForListing.getHearingId();
        final TypeReference<List<HearingDay>> typeHearingDayRef = new TypeReference<List<HearingDay>>() {
        };
        jsonEntityFinder.find(hearingId)
                .put(FIELD_ALLOCATED, !ALLOCATED)
                .remove(FIELD_COURT_ROOM_ID)
                .putSubList("hearingDays", typeHearingDayRef,getHearingDaysWithRemoveCourtRoomIdFunction())
                .save();
        LOGGER.info("'listing.events.hearing-unallocated-for-listing' received hearingId {} ", hearingId);
        hearingSearchSyncService.sync(hearingId);
    }

    @Handles("listing.events.trial-vacated")
    public void trialVacated(final Envelope<TrialVacated> event) {
        final TrialVacated trialVacated = event.payload();
        final UUID hearingId = trialVacated.getHearingId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.events.trial-vacated' received hearingId {}", hearingId);
        }
        jsonEntityFinder.find(hearingId).put(FIELD_IS_VACATED_TRIAL, VACATED)
                .put(FIELD_VACATE_TRIAL_REASON, trialVacated.getVacatedTrialReasonId())
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    @Handles("listing.events.hearing-trial-vacated")
    public void hearingTrialVacated(final Envelope<HearingTrialVacated> event) {
        final HearingTrialVacated hearingTrialVacated = event.payload();
        final UUID hearingId = hearingTrialVacated.getHearingId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.events.hearing-trial-vacated' received hearingId {}", hearingId);
        }
        jsonEntityFinder.find(hearingId)
                .put(FIELD_IS_VACATED_TRIAL, hearingTrialVacated.getVacatedTrialReasonId().isPresent() ? VACATED : NON_VACATED)
                .put(FIELD_VACATE_TRIAL_REASON, hearingTrialVacated.getVacatedTrialReasonId().orElse(null) == null ? "" : hearingTrialVacated.getVacatedTrialReasonId().get().toString())
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    @Handles("listing.events.hearing-rescheduled")
    public void hearingRescheduled(final Envelope<HearingRescheduled> event) {
        final HearingRescheduled hearingRescheduled = event.payload();
        final UUID hearingId = hearingRescheduled.getHearingId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.events.hearing-rescheduled' received hearingId {}", hearingId);
        }

        if (nonNull(hearingRepository.findBy(hearingId))) {
            jsonEntityFinder.find(hearingId)
                    .put(FIELD_IS_VACATED_TRIAL, !VACATED)
                    .put(FIELD_VACATE_TRIAL_REASON, "")
                    .save();

            hearingSearchSyncService.sync(hearingId);
        }
    }

    @Handles("listing.events.case-update-defendant-proceedings-updated")
    public void defendantProceedingsConcluded(final Envelope<CaseUpdateDefendantProceedingsUpdated> event) {
        final CaseUpdateDefendantProceedingsUpdated caseUpdateDefendantProceedingsUpdated = event.payload();
        final UUID hearingId = caseUpdateDefendantProceedingsUpdated.getHearingId();
        final ProsecutionCase prosecutionCase = caseUpdateDefendantProceedingsUpdated.getProsecutionCase();

        saveHearing(hearingId, prosecutionCase);
    }

    @Handles("listing.events.defendant-court-proceedings-updated")
    public void updateDefendantCourtProceedings(final Envelope<DefendantCourtProceedingsUpdated> event) {
        final DefendantCourtProceedingsUpdated defendantCourtProceedingsUpdated = event.payload();
        final UUID hearingId = defendantCourtProceedingsUpdated.getHearingId();
        final uk.gov.justice.listing.events.ProsecutionCase prosecutionCase = defendantCourtProceedingsUpdated.getProsecutionCase();

        if (isEmpty(prosecutionCase.getDefendants())) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("No defendant provided for case {}, returning...", prosecutionCase.getId());
            }

            return;
        }

        saveHearing(hearingId, convertToCourtProsecutionCase(prosecutionCase));
    }

    @Handles("listing.events.defendant-court-proceedings-updated-v2")
    public void updateDefendantCourtProceedingsV2(final Envelope<DefendantCourtProceedingsUpdatedV2> event) {
        final DefendantCourtProceedingsUpdatedV2 defendantCourtProceedingsUpdated = event.payload();
        final UUID hearingId = defendantCourtProceedingsUpdated.getHearingId();
        final ProsecutionCase prosecutionCase = defendantCourtProceedingsUpdated.getProsecutionCase();

        saveHearing(hearingId, prosecutionCase);
    }

    private void saveHearing(final UUID hearingId, final ProsecutionCase prosecutionCase) {
        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, typeRef, getUpdatedListedCaseWithDefendantProceedingsFunction(prosecutionCase))
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    @Handles("listing.events.case-identifier-updated")
    public void updateCaseIdentifier(final Envelope<CaseIdentifierUpdated> event) {
        final CaseIdentifierUpdated updateCaseIdentifier = event.payload();
        final UUID hearingId = updateCaseIdentifier.getHearingId();
        final UUID prosecutionCaseId = updateCaseIdentifier.getProsecutionCaseId();

        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        final CaseIdentifier caseIdentifier = CaseIdentifier.caseIdentifier()
                .withAuthorityCode(updateCaseIdentifier.getProsecutionAuthorityCode())
                .withAuthorityId(updateCaseIdentifier.getProsecutionAuthorityId())
                .build();
        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, typeRef, getUpdatedListedCaseWithProsecutorProceedingsFunction(prosecutionCaseId, caseIdentifier))
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    private Function<List<HearingDay>, List<HearingDay>> getHearingDaysWithRemoveCourtRoomIdFunction() {
        return this::getHearingDaysWithRemoveCourtRoomId;
    }

    private List<HearingDay> getHearingDaysWithRemoveCourtRoomId(final List<HearingDay> hearingDays) {
        return new ArrayList<>(hearingDays.stream()
                .map(hearingDay -> HearingDay.hearingDay().withValuesFrom(hearingDay).withCourtRoomId(ofNullable(null)).build())
                .collect(toList()));

    }

    private Function<List<ListedCase>, List<ListedCase>> getUpdatedListedCaseWithProsecutorProceedingsFunction(final UUID prosecutionCaseId,
                                                                                                               final CaseIdentifier caseIdentifier) {
        return cases -> getUpdatedListedCaseWithCaseIdentifierProceedings(prosecutionCaseId, caseIdentifier, cases);
    }

    private List<ListedCase> getUpdatedListedCaseWithCaseIdentifierProceedings(final UUID prosecutionCaseID,
                                                                               final CaseIdentifier caseIdentifier,
                                                                               final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases);
        final ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(prosecutionCaseID));
        final Prosecutor prosecutor = Prosecutor.prosecutor()
                .withProsecutorCode(caseIdentifier.getAuthorityCode())
                .withProsecutorId(caseIdentifier.getAuthorityId()).build();
        final ListedCase updatedListedCase = ListedCase.listedCase().withValuesFrom(listedCase)
                .withProsecutor(prosecutor)
                .build();

        listedCases.replaceAll(
                listedCase1 -> listedCase1.getId().equals(updatedListedCase.getId()) ? updatedListedCase : listedCase1);
        return listedCases;
    }

    private Function<List<ListedCase>, List<ListedCase>> getUpdatedListedCaseWithDefendantProceedingsFunction(
            final ProsecutionCase prosecutionCase) {
        return cases -> getUpdatedListedCaseWithCaseStatusAndDefendantProceedings(prosecutionCase, cases);
    }

    private List<ListedCase> getUpdatedListedCaseWithCaseStatusAndDefendantProceedings(
            final ProsecutionCase prosecutionCase,
            final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases);
        final ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(prosecutionCase.getId()));
        final List<uk.gov.justice.listing.events.Defendant> listedCaseDefendants = listedCase.getDefendants();
        updateDefendantWithProceedingsIncluded(prosecutionCase.getDefendants(), listedCaseDefendants);
        updateCaseStatus(prosecutionCase, listedCases, listedCase);
        return listedCases;
    }

    private void updateCaseStatus(final ProsecutionCase prosecutionCase, final List<ListedCase> listedCases, final ListedCase listedCase) {
        final ListedCase newListedCase = ListedCase.listedCase()
                .withCaseIdentifier(listedCase.getCaseIdentifier())
                .withProsecutor(listedCase.getProsecutor())
                .withCaseStatus(prosecutionCase.getCaseStatus())
                .withDefendants(listedCase.getDefendants())
                .withIsEjected(listedCase.getIsEjected())
                .withMarkers(listedCase.getMarkers())
                .withRestrictFromCourtList(listedCase.getRestrictFromCourtList())
                .withId(listedCase.getId())
                .withShadowListed(listedCase.getShadowListed())
                .build();
        listedCases.replaceAll(
                listedCase1 -> listedCase1.getId().equals(newListedCase.getId()) ? newListedCase : listedCase1);
    }

    private void updateDefendantWithProceedingsIncluded(final List<uk.gov.justice.core.courts.Defendant> defendants, final List<uk.gov.justice.listing.events.Defendant> listedCaseDefendants) {
        defendants.forEach(defendant -> {
            final UUID defendantId = defendant.getId();
            final uk.gov.justice.listing.events.Defendant originalDefendant = Iterables.find(listedCaseDefendants, defendant1 -> defendant1.getId().equals(defendantId), null);
            if (nonNull(originalDefendant)) {
                final uk.gov.justice.listing.events.Defendant newDefendant = getDefendant(defendant, originalDefendant);
                listedCaseDefendants.replaceAll(defendant1 -> defendant1.getId().equals(newDefendant.getId()) ? newDefendant : defendant1);
            }
        });
    }

    private uk.gov.justice.listing.events.Defendant getDefendant(final Defendant defendant, final uk.gov.justice.listing.events.Defendant originalDefendant) {
        return uk.gov.justice.listing.events.Defendant.defendant()
                .withBailStatus(originalDefendant.getBailStatus())
                .withCustodyTimeLimit(originalDefendant.getCustodyTimeLimit())
                .withId(originalDefendant.getId())
                .withOffences(originalDefendant.getOffences())
                .withDateOfBirth(originalDefendant.getDateOfBirth())
                .withDefenceOrganisation(originalDefendant.getDefenceOrganisation())
                .withFirstName(originalDefendant.getFirstName())
                .withLastName(originalDefendant.getLastName())
                .withOrganisationName(originalDefendant.getOrganisationName())
                .withSpecificRequirements(originalDefendant.getSpecificRequirements())
                .withDatesToAvoid(originalDefendant.getDatesToAvoid())
                .withHearingLanguageNeeds(originalDefendant.getHearingLanguageNeeds())
                .withRestrictFromCourtList(originalDefendant.getRestrictFromCourtList())
                .withLegalAidStatus(originalDefendant.getLegalAidStatus())
                .withProceedingsConcluded(defendant.getProceedingsConcluded())
                .withIsYouth(originalDefendant.getIsYouth())
                .withAddress(nonNull(originalDefendant.getAddress()) && originalDefendant.getAddress().isPresent() ? buildAddress(originalDefendant.getAddress()) : empty())
                .withNationalityDescription(nonNull(originalDefendant.getNationalityDescription()) && originalDefendant.getNationalityDescription().isPresent() ? originalDefendant.getNationalityDescription() : empty())
                .withMasterDefendantId(originalDefendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(originalDefendant.getCourtProceedingsInitiated())
                .build();
    }

    private Optional<uk.gov.justice.core.courts.Address> buildAddress(Optional<uk.gov.justice.core.courts.Address> a) {

        return Optional.of(uk.gov.justice.core.courts.Address.address().
                withAddress1(a.get().getAddress1())
                .withAddress2(a.get().getAddress2().isPresent() ? a.get().getAddress2() : empty())
                .withAddress3(a.get().getAddress3().isPresent() ? a.get().getAddress3() : empty())
                .withAddress4(a.get().getAddress4().isPresent() ? a.get().getAddress4() : empty())
                .withAddress5(a.get().getAddress5().isPresent() ? a.get().getAddress5() : empty())
                .withPostcode(a.get().getPostcode().isPresent() ? a.get().getPostcode() : empty())
                .build());

    }

    private JsonNode convertToObject(Object source) {
        return mapper.valueToTree(source);
    }

    private ProsecutionCase convertToCourtProsecutionCase(final uk.gov.justice.listing.events.ProsecutionCase listingProsecutionCase) {

        return prosecutionCase()
                .withId(listingProsecutionCase.getId())
                .withProsecutionCaseIdentifier(listingProsecutionCase.getProsecutionCaseIdentifier())
                .withOriginatingOrganisation(listingProsecutionCase.getOriginatingOrganisation())
                .withCpsOrganisation(listingProsecutionCase.getCpsOrganisation())
                .withInitiationCode(listingProsecutionCase.getInitiationCode())
                .withCaseStatus(listingProsecutionCase.getCaseStatus())
                .withPoliceOfficerInCase(listingProsecutionCase.getPoliceOfficerInCase())
                .withStatementOfFacts(listingProsecutionCase.getStatementOfFacts())
                .withStatementOfFactsWelsh(listingProsecutionCase.getStatementOfFactsWelsh())
                .withBreachProceedingsPending(listingProsecutionCase.getBreachProceedingsPending())
                .withRemovalReason(listingProsecutionCase.getRemovalReason())
                .withAppealProceedingsPending(listingProsecutionCase.getAppealProceedingsPending())
                .withDefendants(listingProsecutionCase.getDefendants())
                .withCaseMarkers(listingProsecutionCase.getCaseMarkers())
                .withClassOfCase(listingProsecutionCase.getClassOfCase())
                .withIsCpsOrgVerifyError(listingProsecutionCase.getIsCpsOrgVerifyError())
                .build();
    }

    private Function<List<ListedCase>, List<ListedCase>>  getListedCaseWithUpdateOffencesWithListingNumbersFunction(final List<ListingNumbers> offencesWithListingNumbers) {
        return cases -> getListedCaseWithUpdateOffencesWithListingNumbers(offencesWithListingNumbers, cases);
    }

    private List<ListedCase> getListedCaseWithUpdateOffencesWithListingNumbers(final List<ListingNumbers> offencesWithListingNumbers, final List<ListedCase> cases) {
        return ofNullable(cases).map(Collection::stream).orElseGet(Stream::empty)
                .map(listedCase -> ListedCase.listedCase().withValuesFrom(listedCase)
                        .withDefendants(listedCase.getDefendants().stream()
                                .map(defendant -> uk.gov.justice.listing.events.Defendant.defendant().withValuesFrom(defendant)
                                        .withOffences(defendant.getOffences().stream()
                                                .map(offence -> Offence.offence().withValuesFrom(offence)
                                                        .withListingNumber(offencesWithListingNumbers.stream().filter(listingNumber -> listingNumber.getOffenceId().equals(offence.getId()))
                                                                .findAny().map(ListingNumbers::getListingNumber).orElse(null))
                                                        .build())
                                                .collect(toList()))
                                        .build())
                                .collect(toList()))
                        .build())
                .collect(toList());
    }

    private void updateViewStoreWithListingNumberAndAllocatedHearing(final UUID hearingId, final List<ListingNumbers> offencesWithListingNumbersForCase) {

        final TypeReference<List<ListedCase>> typeRefCase = new TypeReference<List<ListedCase>>() {
        };

        JsonNodeUpdater updater = jsonEntityFinder
                .find(hearingId)
                .put(FIELD_ALLOCATED, ALLOCATED)
                .remove("unscheduled");
        if(! offencesWithListingNumbersForCase.isEmpty()){
            updater = updater.putSubList(LISTED_CASES_FIELD, typeRefCase, getListedCaseWithUpdateOffencesWithListingNumbersFunction(offencesWithListingNumbersForCase));
        }
        updater.save();
    }
}
