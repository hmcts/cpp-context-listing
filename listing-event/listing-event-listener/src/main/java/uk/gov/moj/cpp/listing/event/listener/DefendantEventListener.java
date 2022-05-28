package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.DefendantLegalaidStatusUpdatedForHearing;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.NewBaseDefendant;
import uk.gov.justice.listing.events.NewDefendantAddedForCourtProceedings;
import uk.gov.justice.listing.events.NewDefendantDetailsUpdated;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Iterables;

@ServiceComponent(Component.EVENT_LISTENER)
public class DefendantEventListener {


    private static final String LISTED_CASES_FIELD = "listedCases";

    @Inject
    private HearingRepository hearingRepository;

    @Handles("listing.events.new-defendant-details-updated")
    public void defendantDetailsUpdated(final Envelope<NewDefendantDetailsUpdated> event) {
        final NewDefendantDetailsUpdated defendantDetailsUpdated = event.payload();
        final UUID hearingId = defendantDetailsUpdated.getHearingId();
        final UUID caseId = defendantDetailsUpdated.getCaseId();
        final NewBaseDefendant defendant = defendantDetailsUpdated.getDefendant();

        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        if (nonNull(hearingRepository.findBy(hearingId))) {
            using(hearingRepository)
                    .find(hearingId)
                    .putSubList(LISTED_CASES_FIELD, typeRef, getUpdatedListedCaseFunction(caseId, defendant))
                    .save();
        }
    }

    @Handles("listing.events.new-defendant-added-for-court-proceedings")
    public void defendantDetailsAddedForCourtProceedings(final Envelope<NewDefendantAddedForCourtProceedings> event) {
        final NewDefendantAddedForCourtProceedings defendantDetailsAddedForCourtProceedings = event.payload();
        final UUID hearingId = defendantDetailsAddedForCourtProceedings.getHearingId();
        final UUID caseId = defendantDetailsAddedForCourtProceedings.getCaseId();
        final Defendant defendant = defendantDetailsAddedForCourtProceedings.getDefendant();
        filterDuplicateOffencesById(defendant.getOffences());
        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        using(hearingRepository)
                .find(hearingId)
                .putSubList(LISTED_CASES_FIELD, typeRef, getDefendantsAddFunction(caseId, defendant)).save();

    }

    private  void filterDuplicateOffencesById(final List<uk.gov.justice.listing.events.Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
    }

    @Handles("listing.events.defendant-legalaid-status-updated-for-hearing")
    public void defendantLegalStatusUpdatedForHearing(final Envelope<DefendantLegalaidStatusUpdatedForHearing> event) {
        final DefendantLegalaidStatusUpdatedForHearing defendantLegalaidStatusUpdatedForHearing = event.payload();
        final UUID hearingId = defendantLegalaidStatusUpdatedForHearing.getHearingId();
        final UUID caseId = defendantLegalaidStatusUpdatedForHearing.getCaseId();
        final UUID defendantId = defendantLegalaidStatusUpdatedForHearing.getDefendantId();
        final String legalAidStatus = defendantLegalaidStatusUpdatedForHearing.getLegalAidStatus();

        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };

        if (nonNull(hearingRepository.findBy(hearingId))) {
            using(hearingRepository)
                    .find(hearingId)
                    .putSubList(LISTED_CASES_FIELD, typeRef, getListedCaseWithDefendantLegalAidStatusUpdate(caseId, defendantId, legalAidStatus)).save();
        }

    }

    private Function<List<ListedCase>, List<ListedCase>> getListedCaseWithDefendantLegalAidStatusUpdate(final UUID caseId, final UUID defendantId,
                                                                                                        final String legalAidStatus) {
        return cases -> getUpdatedListedCase(caseId, defendantId, legalAidStatus, cases);

    }

    private List<ListedCase> getUpdatedListedCase(final UUID caseId, final UUID defendantId,
                                                  final String legalAidStatus, final List<ListedCase> cases) {
        final List<ListedCase> listedCases = new ArrayList<>(cases);
        final ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId), null);
        if (nonNull(listedCase)) {
            final List<Defendant> defendants = listedCase.getDefendants();
            final Defendant originalDefendant = Iterables.find(defendants, defendant1 -> defendant1.getId().equals(defendantId), null);
            if (nonNull(originalDefendant)) {
                final Defendant newDefendant = Defendant.defendant()
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
                        .withLegalAidStatus("NO_VALUE".equals(legalAidStatus) ? null : legalAidStatus)
                        .withProceedingsConcluded(originalDefendant.getProceedingsConcluded())
                        .withMasterDefendantId(originalDefendant.getMasterDefendantId())
                        .withCourtProceedingsInitiated(originalDefendant.getCourtProceedingsInitiated())
                        .build();

                defendants.replaceAll(defendant -> defendant.getId().equals(newDefendant.getId()) ? newDefendant : defendant);
            }
        }
        return listedCases;

    }


    private Function<List<ListedCase>, List<ListedCase>> getUpdatedListedCaseFunction(UUID caseId, NewBaseDefendant defendant) {
        return cases -> getUpdatedListedCase(caseId, defendant, cases);
    }

    private List<ListedCase> getUpdatedListedCase(UUID caseId, NewBaseDefendant updatedDefendant, List<ListedCase> cases) {
        final List<ListedCase> listedCases = cases;
        final ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getId().equals(caseId), null);
        if (nonNull(listedCase)) {
            final List<Defendant> defendants = listedCase.getDefendants();
            final Defendant originalDefendant = Iterables.find(defendants, defendant1 -> defendant1.getId().equals(updatedDefendant.getId()), null);
            if (nonNull(originalDefendant)) {
                final Defendant newDefendant = Defendant.defendant()
                        .withBailStatus(updatedDefendant.getBailStatus())
                        .withCustodyTimeLimit(updatedDefendant.getCustodyTimeLimit())
                        .withId(updatedDefendant.getId())
                        .withOffences(originalDefendant.getOffences())
                        .withDateOfBirth(updatedDefendant.getDateOfBirth())
                        .withDefenceOrganisation(updatedDefendant.getDefenceOrganisation())
                        .withFirstName(updatedDefendant.getFirstName())
                        .withLastName(updatedDefendant.getLastName())
                        .withOrganisationName(updatedDefendant.getOrganisationName())
                        .withSpecificRequirements(updatedDefendant.getSpecificRequirements())
                        .withDatesToAvoid(originalDefendant.getDatesToAvoid())
                        .withHearingLanguageNeeds(originalDefendant.getHearingLanguageNeeds())
                        .withRestrictFromCourtList(originalDefendant.getRestrictFromCourtList())
                        .withIsYouth(updatedDefendant.getIsYouth())
                        .withAddress(updatedDefendant.getAddress())
                        .withNationalityDescription(updatedDefendant.getNationalityDescription())
                        .withLegalAidStatus(originalDefendant.getLegalAidStatus())
                        .withProceedingsConcluded(originalDefendant.getProceedingsConcluded())
                        .withMasterDefendantId(updatedDefendant.getMasterDefendantId())
                        .withCourtProceedingsInitiated(originalDefendant.getCourtProceedingsInitiated())
                        .build();

                defendants.replaceAll(defendant -> defendant.getId().equals(newDefendant.getId()) ? newDefendant : defendant);
            }
        }
        return listedCases;
    }

    private Function<List<ListedCase>, List<ListedCase>> getDefendantsAddFunction( final UUID caseId, final Defendant defendant) {
        return cases -> getDefendants(caseId, defendant, cases);
    }

    private List<ListedCase> getDefendants(final UUID caseId, final Defendant defendant, final List<ListedCase> cases) {
        Iterables.find(cases, listedCase -> listedCase.getId().equals(caseId)).getDefendants().add(defendant);

        return cases;

    }
}
