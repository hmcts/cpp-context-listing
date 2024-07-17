package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Objects.nonNull;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.events.ApplicationEjected;
import uk.gov.justice.listing.events.CaseEjected;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;

@ServiceComponent(Component.EVENT_LISTENER)
public class EjectEventListener {


    private static final String LISTED_CASES = "listedCases";
    private static final String COURT_APPLICATION_FIELD = "courtApplications";

    @Inject
    private HearingRepository hearingRepository;

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;

    @Handles("listing.events.case-ejected")
    public void caseEjected(final Envelope<CaseEjected> event) {
        final CaseEjected caseEjected = event.payload();
        final UUID hearingId = caseEjected.getHearingId();
        final UUID caseId = caseEjected.getProsecutionCaseId();

        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };
        final TypeReference<List<CourtApplication>> typeRefCourtApplication = new TypeReference<List<CourtApplication>>() {
        };
        using(hearingRepository)
                .find(hearingId).putSubList(LISTED_CASES, typeRef, getCasesFunction(caseId)).save();
            using(hearingRepository)
                    .find(hearingId)
                    .putSubList(COURT_APPLICATION_FIELD, typeRefCourtApplication, getCourtApplicationFunctionForLinkedCaseId(caseId)).save();

        hearingSearchSyncService.sync(hearingId);

    }

    @Handles("listing.events.application-ejected")
    public void applicationEjected(final Envelope<ApplicationEjected> event) {
        final ApplicationEjected applicationEjected = event.payload();
        final UUID hearingId = applicationEjected.getHearingId();
        final UUID applicationId = applicationEjected.getApplicationId();

        final TypeReference<List<CourtApplication>> typeRefCourtApplication = new TypeReference<List<CourtApplication>>() {
        };
            using(hearingRepository)
                    .find(hearingId)
                    .putSubList(COURT_APPLICATION_FIELD, typeRefCourtApplication, getCourtApplicationFunction(applicationId)).save();

        hearingSearchSyncService.sync(hearingId);


    }

    private Function<List<ListedCase>, List<ListedCase>> getCasesFunction(UUID caseId) {
        return cases -> getAndUpdateCases(cases, caseId);
    }
    private List<ListedCase> getAndUpdateCases(List<ListedCase> cases, UUID caseId) {
        final ListedCase listedCase = cases.stream().filter(lc -> lc.getId().equals(caseId)).findFirst().orElse(null);

        if (listedCase == null) {
            return cases;
        }

        final ListedCase newListedCase
                = ListedCase.listedCase()
                .withCaseIdentifier(listedCase.getCaseIdentifier())
                .withIsCivil(listedCase.getIsCivil())
                .withGroupId(listedCase.getGroupId())
                .withIsGroupMember(listedCase.getIsGroupMember())
                .withIsGroupMaster(listedCase.getIsGroupMaster())
                .withDefendants(listedCase.getDefendants())
                .withId(listedCase.getId())
                .withIsEjected(true)
                .withShadowListed(listedCase.getShadowListed()).build();
        cases.replaceAll(lc -> lc.getId().equals(caseId) ? newListedCase : lc);
        return cases;
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationFunction(UUID courtApplicationId) {
        return courtApplications -> getAndUpdateCourtApplicationToEject(courtApplicationId, courtApplications);
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationFunctionForLinkedCaseId(UUID caseId) {
        return courtApplications -> getAndUpdateCourtApplicationWithLinkedCaseIdToEject(caseId, courtApplications);
    }

    private List<CourtApplication> getAndUpdateCourtApplicationToEject(UUID courtApplicationId, List<CourtApplication> courtApplications) {
        courtApplications.replaceAll(ca -> ca.getId().equals(courtApplicationId) ||  (nonNull(courtApplicationId) && courtApplicationId.equals(ca.getParentApplicationId())) ? buildCourtApplication(ca) : ca);
        return courtApplications;
    }

    private List<CourtApplication> getAndUpdateCourtApplicationWithLinkedCaseIdToEject(UUID linkedCaseId, List<CourtApplication> courtApplications) {
        final List<CourtApplication> courtApplicationEntities = courtApplications.stream()
                .filter(ca -> nonNull(ca.getLinkedCaseIds()))
                .filter(ca -> ca.getLinkedCaseIds().contains(linkedCaseId))
                .collect(Collectors.toList());
        courtApplicationEntities.forEach(
                courtApplication -> {
                    final UUID courtApplicationId = courtApplication.getId();
                    final CourtApplication newCourtApplication = buildCourtApplication(courtApplication);
                    courtApplications.replaceAll(ca -> ca.getId().equals(courtApplicationId) ? newCourtApplication : ca);
                }
        );

        return courtApplications;
    }

    private CourtApplication buildCourtApplication(final CourtApplication courtApplication) {
        return CourtApplication.courtApplication()
                .withApplicant(courtApplication.getApplicant())
                .withRespondents(courtApplication.getRespondents())
                .withApplicationType(courtApplication.getApplicationType())
                .withId(courtApplication.getId())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withLinkedCaseIds(courtApplication.getLinkedCaseIds())
                .withRestrictFromCourtList((courtApplication.getRestrictFromCourtList()))
                .withRestrictCourtApplicationType(courtApplication.getRestrictCourtApplicationType())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .withApplicationType(courtApplication.getApplicationType())
                .withIsEjected(true)
                .build();
    }

}
