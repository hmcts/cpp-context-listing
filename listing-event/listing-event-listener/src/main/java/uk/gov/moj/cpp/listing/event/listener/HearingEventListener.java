package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.core.courts.Defendant;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.listing.events.CaseUpdateDefendantProceedingsUpdated;
import uk.gov.justice.listing.events.HearingAllocatedForListing;
import uk.gov.justice.listing.events.HearingListed;
import uk.gov.justice.listing.events.HearingRescheduled;
import uk.gov.justice.listing.events.HearingTrialVacated;
import uk.gov.justice.listing.events.HearingUnallocatedForListing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.TrialVacated;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
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
    private final JsonEntityFinder jsonEntityFinder;
    private final HearingRepository hearingRepository;
    private final ObjectMapper mapper;

    @Inject
    public HearingEventListener(final HearingRepository hearingRepository,
                                final ObjectMapper mapper) {
        this.hearingRepository = hearingRepository;
        this.jsonEntityFinder = using(hearingRepository);
        this.mapper = mapper;
    }

    @Handles("listing.events.hearing-listed")
    public void hearingListed(final Envelope<HearingListed> event) {
        HearingListed hearingListed = event.payload();
        JsonNode hearingJsonNode = convertToJsonNode(hearingListed.getHearing());
        UUID hearingId = hearingListed.getHearing().getId();
        LOGGER.info("'listing.events.hearing-listed' received hearingId {}", hearingId);
        final Hearing hearing = new Hearing(hearingId, hearingJsonNode);
        hearingRepository.save(hearing);
    }

    @Handles("listing.events.hearing-allocated-for-listing")
    public void hearingAllocated(final Envelope<HearingAllocatedForListing> event) {
        final HearingAllocatedForListing hearingAllocatedForListing = event.payload();
        final UUID hearingId = hearingAllocatedForListing.getHearingId();
        LOGGER.info("'listing.events.hearing-allocated-for-listing' received hearingId {}", hearingId);
        jsonEntityFinder.find(hearingId).put("allocated", ALLOCATED).remove("unscheduled").save();
    }

    @Handles("listing.events.hearing-unallocated-for-listing")
    public void hearingUnallocated(final Envelope<HearingUnallocatedForListing> event) {
        final HearingUnallocatedForListing hearingUnallocatedForListing = event.payload();
        final UUID hearingId = hearingUnallocatedForListing.getHearingId();
        LOGGER.info("'listing.events.hearing-unallocated-for-listing' received hearingId {}", hearingId);
        jsonEntityFinder.find(hearingId).put("allocated", !ALLOCATED).save();

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

    }

    @Handles("listing.events.hearing-rescheduled")
    public void hearingRescheduled(final Envelope<HearingRescheduled> event) {
        final HearingRescheduled hearingRescheduled = event.payload();
        final UUID hearingId = hearingRescheduled.getHearingId();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("'listing.events.hearing-rescheduled' received hearingId {}", hearingId);
        }
        jsonEntityFinder.find(hearingId)
                .put(FIELD_IS_VACATED_TRIAL, !VACATED)
                .put(FIELD_VACATE_TRIAL_REASON, "")
                .save();

    }

    @Handles("listing.events.case-update-defendant-proceedings-updated")
    public void defendantProceedingsConcluded(final Envelope<CaseUpdateDefendantProceedingsUpdated> event) {
        final CaseUpdateDefendantProceedingsUpdated caseUpdateDefendantProceedingsUpdated = event.payload();
        final UUID hearingId = caseUpdateDefendantProceedingsUpdated.getHearingId();
        final ProsecutionCase prosecutionCase = caseUpdateDefendantProceedingsUpdated.getProsecutionCase();
        final List<Defendant> defendants = prosecutionCase.getDefendants();

        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, typeRef, getUpdatedListedCaseWithDefendantProceedingsFunction(prosecutionCase, defendants))
                .save();
    }

    private Function<List<ListedCase>, List<ListedCase>> getUpdatedListedCaseWithDefendantProceedingsFunction(
            final ProsecutionCase prosecutionCase,
            final List<uk.gov.justice.core.courts.Defendant> defendants) {
        return cases -> getUpdatedListedCaseWithCaseStatusAndDefendantProceedings(prosecutionCase, defendants, cases);
    }

    private List<ListedCase> getUpdatedListedCaseWithCaseStatusAndDefendantProceedings(
            final ProsecutionCase prosecutionCase,
            final List<uk.gov.justice.core.courts.Defendant> defendants,
            final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases);
        final ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(prosecutionCase.getId()));
        final List<uk.gov.justice.listing.events.Defendant> listedCaseDefendants = listedCase.getDefendants();
        updateDefendantWithProceedingsIncluded(defendants, listedCaseDefendants);
        updateCaseStatus(prosecutionCase, listedCases, listedCase);
        return listedCases;

    }

    private void updateCaseStatus(final ProsecutionCase prosecutionCase, final List<ListedCase> listedCases, final ListedCase listedCase) {
        final ListedCase newListedCase = ListedCase.listedCase()
                .withCaseIdentifier(listedCase.getCaseIdentifier())
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
            final uk.gov.justice.listing.events.Defendant originalDefendant = Iterables.find(listedCaseDefendants, defendant1 -> defendant1.getId().equals(defendantId));
            final uk.gov.justice.listing.events.Defendant newDefendant = getDefendant(defendant, originalDefendant);
            listedCaseDefendants.replaceAll(defendant1 -> defendant1.getId().equals(newDefendant.getId()) ? newDefendant : defendant1);
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

    private JsonNode convertToJsonNode(Object source) {
        return mapper.valueToTree(source);
    }


}
