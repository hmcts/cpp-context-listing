package uk.gov.moj.cpp.listing.event.listener;


import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.listing.events.ApplicantRespondent.applicantRespondent;
import static uk.gov.justice.listing.events.CourtApplication.courtApplication;
import static uk.gov.justice.listing.events.Defendant.defendant;
import static uk.gov.justice.listing.events.ListedCase.listedCase;
import static uk.gov.justice.listing.events.Offence.offence;

import uk.gov.justice.listing.events.ApplicantRespondent;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.CourtListRestricted;
import uk.gov.justice.listing.events.Defendant;
import uk.gov.justice.listing.events.ListedCase;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;
import uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder;
import uk.gov.moj.cpp.listing.persistence.repository.JsonNodeUpdater;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
@SuppressWarnings({"squid:S2384"})
public class RestrictCourtListEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestrictCourtListEventListener.class);
    private static final String LISTED_CASES = "listedCases";
    private static final String COURT_APPLICATIONS_FIELD = "courtApplications";
    private JsonEntityFinder jsonEntityFinder;

    @Inject
    public RestrictCourtListEventListener(final HearingRepository hearingRepository,
                                          final ObjectMapper mapper) {
        this.jsonEntityFinder = JsonEntityFinder.using(hearingRepository);
    }

    @Handles("listing.events.court-list-restricted")
    public void hearingRestrictionForCourt(final Envelope<CourtListRestricted> event) {
        final CourtListRestricted restrictCourtList = event.payload();
        final UUID hearingId = restrictCourtList.getHearingId();
        final Boolean restrictDetailsFromCourt = restrictCourtList.getRestrictCourtList();
        LOGGER.info("'listing.events.court-list-restricted' received hearingId {}", hearingId);
        final TypeReference<List<ListedCase>> typeRef = new TypeReference<List<ListedCase>>() {
        };
        final TypeReference<List<CourtApplication>> typeRefCourtApplication = new TypeReference<List<CourtApplication>>() {
        };
        final JsonNodeUpdater jsonNodeUpdater = jsonEntityFinder.find(hearingId);
        ofNullable(restrictCourtList.getCaseIds()).orElse(newArrayList()).forEach(caseIdToBeRestrict ->
                jsonNodeUpdater.putSubList(LISTED_CASES, typeRef, getCasesFunction(caseIdToBeRestrict, restrictDetailsFromCourt)).save());
        ofNullable(restrictCourtList.getDefendantIds()).orElse(newArrayList()).forEach(defendantIdToBeRestricted ->
                jsonNodeUpdater
                        .putSubList(LISTED_CASES, typeRef, getDefendantsFunction(defendantIdToBeRestricted, restrictDetailsFromCourt)).save());
        ofNullable(restrictCourtList.getOffenceIds()).orElse(newArrayList()).forEach(offenceIdToBeRestricted ->
                jsonNodeUpdater
                        .putSubList(LISTED_CASES, typeRef, getOffencesFunction(offenceIdToBeRestricted, restrictDetailsFromCourt)).save());
        final Optional<String> hasCourtApplicationType = restrictCourtList.getCourtApplicationType();
        if (hasCourtApplicationType.isPresent()) {
            final String appType = hasCourtApplicationType.get();
            restrictCourtList.getCourtApplicationIds().forEach(
                    courtApplicationIdToBeRestricted -> jsonNodeUpdater.putSubList(COURT_APPLICATIONS_FIELD, typeRefCourtApplication,
                            getCourtApplicationTypeFunction(courtApplicationIdToBeRestricted, appType, restrictDetailsFromCourt)).save());
        } else {
            ofNullable(restrictCourtList.getCourtApplicationIds()).orElse(newArrayList()).forEach(
                    courtApplicationIdToBeRestricted -> jsonNodeUpdater.putSubList(COURT_APPLICATIONS_FIELD, typeRefCourtApplication,
                            getCourtApplicationFunction(courtApplicationIdToBeRestricted, restrictDetailsFromCourt)).save()
            );
        }
        ofNullable(restrictCourtList.getCourtApplicationApplicantIds()).orElse(newArrayList()).forEach(
                courtApplicationApplicantIdToBeRestricted -> jsonNodeUpdater.putSubList(COURT_APPLICATIONS_FIELD, typeRefCourtApplication,
                        getCourtApplicationApplicantFunction(courtApplicationApplicantIdToBeRestricted, restrictDetailsFromCourt)).save()
        );
        ofNullable(restrictCourtList.getCourtApplicationRespondentIds()).orElse(newArrayList()).forEach(
                courtApplicationRespondentIdToBeRestricted -> jsonNodeUpdater.putSubList(COURT_APPLICATIONS_FIELD, typeRefCourtApplication,
                        getCourtApplicationRespondentsFunction(courtApplicationRespondentIdToBeRestricted, restrictDetailsFromCourt)).save()
        );
    }

    private Function<List<ListedCase>, List<ListedCase>> getCasesFunction(UUID casesId, Boolean restrictDetailsFromCourt) {
        return cases -> getAndUpdateCases(casesId, cases, restrictDetailsFromCourt);
    }

    private List<ListedCase> getAndUpdateCases(UUID caseId, List<ListedCase> cases, Boolean restrictDetailsFromCourt) {
        final ListedCase listedCase = Iterables.find(cases, lc -> lc.getId().equals(caseId));
        final ListedCase newListedCase = listedCase()
                .withCaseIdentifier(listedCase.getCaseIdentifier())
                .withDefendants(listedCase.getDefendants())
                .withId(listedCase.getId())
                .withRestrictFromCourtList(ofNullable(restrictDetailsFromCourt))
                .withShadowListed(listedCase.getShadowListed()).build();
        cases.replaceAll(lc -> lc.getId().equals(caseId) ? newListedCase : lc);
        return cases;
    }

    private Function<List<ListedCase>, List<ListedCase>> getDefendantsFunction(UUID defendantId, Boolean restrictDetailsFromCourt) {
        return cases -> getAndRestrictDefendantList(defendantId, cases, restrictDetailsFromCourt);
    }

    private Function<List<ListedCase>, List<ListedCase>> getOffencesFunction(UUID offenceId, Boolean restrictDetailsFromCourt) {
        return cases -> getAndRestrictOffencesList(offenceId, cases, restrictDetailsFromCourt);
    }

    private List<ListedCase> getAndRestrictDefendantList(UUID defendantId, List<ListedCase> cases, Boolean restrictDetailsFromCourt) {
        final List<ListedCase> listedCases = cases;
        final ListedCase listedCase = listedCases.stream().filter(caze -> caze.getDefendants().stream().anyMatch(def -> def.getId().equals(defendantId))).findFirst().orElse(null);
        if (listedCase == null) {
            return listedCases;
        }

        final List<Defendant> defendantList = listedCase.getDefendants();
        final Defendant originalDefendant = defendantList.stream().filter(dd -> dd.getId().equals(defendantId)).findFirst().orElse(null);
        if (originalDefendant == null) {
            return listedCases;
        }

        final Defendant newDefendant = defendant()
                .withId(originalDefendant.getId())
                .withBailStatus(originalDefendant.getBailStatus())
                .withCustodyTimeLimit(originalDefendant.getCustodyTimeLimit())
                .withOffences(originalDefendant.getOffences())
                .withDateOfBirth(originalDefendant.getDateOfBirth())
                .withDefenceOrganisation(originalDefendant.getDefenceOrganisation())
                .withFirstName(originalDefendant.getFirstName())
                .withLastName(originalDefendant.getLastName())
                .withOrganisationName(originalDefendant.getOrganisationName())
                .withSpecificRequirements(originalDefendant.getSpecificRequirements())
                .withDatesToAvoid(originalDefendant.getDatesToAvoid())
                .withHearingLanguageNeeds(originalDefendant.getHearingLanguageNeeds())
                .withRestrictFromCourtList(ofNullable(restrictDetailsFromCourt))
                .withIsYouth(originalDefendant.getIsYouth())
                .withAddress(originalDefendant.getAddress())
                .withNationalityDescription(originalDefendant.getNationalityDescription())
                .withProceedingsConcluded(originalDefendant.getProceedingsConcluded())
                .withLegalAidStatus(originalDefendant.getLegalAidStatus())
                .withMasterDefendantId(originalDefendant.getMasterDefendantId())
                .withCourtProceedingsInitiated(originalDefendant.getCourtProceedingsInitiated())
                .build();
        defendantList.replaceAll(defendant -> defendant.getId().equals(defendantId) ? newDefendant : defendant);
        return listedCases;
    }

    private List<ListedCase> getAndRestrictOffencesList(UUID offencesId, List<ListedCase> cases, Boolean restrictDetailsFromCourt) {
        final List<ListedCase> listedCases = cases;
        final ListedCase listedCase = Iterables.find(listedCases, caze -> caze.getDefendants().stream().
                flatMap(def -> def.getOffences().stream()).anyMatch(off -> off.getId()
                .equals(offencesId)));
        final Defendant originalDefendant = Iterables.find(listedCase.getDefendants(), def -> def.getOffences().stream().anyMatch(off -> off.getId().equals(offencesId)));
        final Offence originalOffence = Iterables.find(originalDefendant.getOffences(), off -> off.getId().equals(offencesId));
        final Offence newOffence = offence()
                .withId(originalOffence.getId())
                .withOffenceCode(originalOffence.getOffenceCode())
                .withStartDate(originalOffence.getStartDate())
                .withEndDate(originalOffence.getEndDate())
                .withStatementOfOffence(originalOffence.getStatementOfOffence())
                .withOffenceWording(originalOffence.getOffenceWording())
                .withRestrictFromCourtList(ofNullable(restrictDetailsFromCourt))
                .withShadowListed(originalOffence.getShadowListed())
                .withReportingRestrictions(originalOffence.getReportingRestrictions())
                .withListingNumber(originalOffence.getListingNumber())
                .build();
        originalDefendant.getOffences().replaceAll(offence -> offence.getId().equals(offencesId) ? newOffence : offence);
        return listedCases;
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationFunction(UUID courtApplicationId, Boolean restrictDetailsFromCourt) {
        return courtApplications -> getAndRestrictCourtApplication(courtApplicationId, courtApplications, restrictDetailsFromCourt);
    }

    private List<CourtApplication> getAndRestrictCourtApplication(UUID courtApplicationId, List<CourtApplication> courtApplications, Boolean restrictDetailsFromCourt) {
        final CourtApplication courtApplication = Iterables.find(courtApplications, ca -> ca.getId().equals(courtApplicationId));
        final CourtApplication newCourtApplication = courtApplication()
                .withApplicant(courtApplication.getApplicant())
                .withRespondents(courtApplication.getRespondents())
                .withApplicationType(courtApplication.getApplicationType())
                .withId(courtApplication.getId())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withLinkedCaseIds(courtApplication.getLinkedCaseIds())
                .withRestrictFromCourtList(ofNullable(restrictDetailsFromCourt))
                .withRestrictCourtApplicationType(courtApplication.getRestrictCourtApplicationType())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .build();
        courtApplications.replaceAll(ca -> ca.getId().equals(courtApplicationId) ? newCourtApplication : ca);
        return courtApplications;
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationApplicantFunction(UUID courtApplicationApplicantId, Boolean restrictDetailsFromCourt) {
        return courtApplications -> getAndRestrictCourtApplicationApplicant(courtApplicationApplicantId, courtApplications, restrictDetailsFromCourt);
    }

    private List<CourtApplication> getAndRestrictCourtApplicationApplicant(UUID courtApplicationApplicantId, List<CourtApplication> courtApplications, Boolean restrictDetailsFromCourt) {
        final CourtApplication courtApplication = Iterables.find(courtApplications, ca -> ca.getApplicant().getId().equals(courtApplicationApplicantId));
        final ApplicantRespondent applicantRespondent = courtApplication.getApplicant();
        final ApplicantRespondent newApplicantRespondent = applicantRespondent()
                .withId(applicantRespondent.getId())
                .withFirstName(applicantRespondent.getFirstName())
                .withLastName(applicantRespondent.getLastName())
                .withIsRespondent(applicantRespondent.getIsRespondent())
                .withRestrictFromCourtList(ofNullable(restrictDetailsFromCourt))
                .withCourtApplicationPartyType(applicantRespondent.getCourtApplicationPartyType())
                .withAddress(applicantRespondent.getAddress())
                .build();
        final CourtApplication newCourtApplication = courtApplication()
                .withApplicant(newApplicantRespondent)
                .withRespondents(courtApplication.getRespondents())
                .withApplicationType(courtApplication.getApplicationType())
                .withId(courtApplication.getId())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withLinkedCaseIds(courtApplication.getLinkedCaseIds())
                .withRestrictFromCourtList(courtApplication.getRestrictFromCourtList())
                .withRestrictCourtApplicationType(courtApplication.getRestrictCourtApplicationType())
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .build();
        courtApplications.replaceAll(ca -> ca.getApplicant().getId().equals(courtApplicationApplicantId) ? newCourtApplication : ca);
        return courtApplications;
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationRespondentsFunction(UUID courtApplicationApplicantRespondentId, Boolean restrictDetailsFromCourt) {
        return courtApplications -> getAndRestrictCourtApplicationRespondents(courtApplicationApplicantRespondentId, courtApplications, restrictDetailsFromCourt);
    }

    private List<CourtApplication> getAndRestrictCourtApplicationRespondents(UUID courtApplicationApplicantRespondentId, List<CourtApplication> courtApplications, Boolean restrictDetailsFromCourt) {
        final CourtApplication courtApplication = Iterables.find(courtApplications, ca -> ca.getRespondents().stream()
                .anyMatch(res -> res.getId().equals(courtApplicationApplicantRespondentId)));
        final List<ApplicantRespondent> respondents = courtApplication.getRespondents();
        final ApplicantRespondent applicantRespondent = Iterables.find(respondents, res -> res.getId().equals(courtApplicationApplicantRespondentId));
        final ApplicantRespondent newApplicantRespondent = applicantRespondent()
                .withId(applicantRespondent.getId())
                .withFirstName(applicantRespondent.getFirstName())
                .withLastName(applicantRespondent.getLastName())
                .withIsRespondent(applicantRespondent.getIsRespondent())
                .withRestrictFromCourtList(ofNullable(restrictDetailsFromCourt))
                .withCourtApplicationPartyType(applicantRespondent.getCourtApplicationPartyType())
                .withAddress(applicantRespondent.getAddress())
                .build();
        respondents.replaceAll(res -> res.getId().equals(courtApplicationApplicantRespondentId) ? newApplicantRespondent : res);
        return courtApplications;
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationTypeFunction(UUID courtApplicationId, String courtApplicationType, Boolean restrictDetailsFromCourt) {
        return courtApplications -> getAndRestrictCourtApplicationType(courtApplicationId, courtApplicationType, courtApplications, restrictDetailsFromCourt);
    }

    private List<CourtApplication> getAndRestrictCourtApplicationType(UUID courtApplicationId,
                                                                      String courtApplicationType, List<CourtApplication> courtApplications, Boolean restrictDetailsFromCourt) {
        final CourtApplication courtApplication = Iterables.find(courtApplications, ca -> ca.getId().equals(courtApplicationId) &&
                ca.getApplicationType().equals(courtApplicationType));

        final CourtApplication newCourtApplication = courtApplication()
                .withApplicant(courtApplication.getApplicant())
                .withRespondents(courtApplication.getRespondents())
                .withApplicationType(courtApplication.getApplicationType())
                .withId(courtApplication.getId())
                .withParentApplicationId(courtApplication.getParentApplicationId())
                .withLinkedCaseIds(courtApplication.getLinkedCaseIds())
                .withRestrictFromCourtList(courtApplication.getRestrictFromCourtList())
                .withRestrictCourtApplicationType(ofNullable(restrictDetailsFromCourt))
                .withApplicationReference(courtApplication.getApplicationReference())
                .withApplicationParticulars(courtApplication.getApplicationParticulars())
                .build();
        courtApplications.replaceAll(ca -> ca.getId().equals(courtApplicationId) && ca.getApplicationType().equals(courtApplicationType) ? newCourtApplication : ca);
        return courtApplications;
    }
}
