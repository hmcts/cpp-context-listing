package uk.gov.moj.cpp.listing.event.listener;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static uk.gov.moj.cpp.listing.event.util.ReportingRestrictionHelper.dedupAllReportingRestrictions;
import static uk.gov.moj.cpp.listing.persistence.repository.JsonEntityFinder.using;

import uk.gov.justice.listing.event.CourtApplicationHearingDeleted;
import uk.gov.justice.listing.events.CourtApplication;
import uk.gov.justice.listing.events.CourtApplicationAddedForHearing;
import uk.gov.justice.listing.events.CourtApplicationUpdatedForHearing;
import uk.gov.justice.listing.events.LaaReference;
import uk.gov.justice.listing.events.LaaReferenceForApplicationUpdated;
import uk.gov.justice.listing.events.Offence;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.listing.event.service.HearingSearchSyncService;
import uk.gov.moj.cpp.listing.persistence.entity.Hearing;
import uk.gov.moj.cpp.listing.persistence.repository.HearingRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.EVENT_LISTENER)
public class CourtApplicationEventListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(CourtApplicationEventListener.class);


    private static final String COURT_APPLICATION_FIELD = "courtApplications";

    @Inject
    private HearingSearchSyncService hearingSearchSyncService;
    @Inject
    private HearingRepository hearingRepository;

    @Handles("listing.events.court-application-added-for-hearing")
    public void courtApplicationAdded(final Envelope<CourtApplicationAddedForHearing> event) {
        final CourtApplicationAddedForHearing addedCourtapplicationToHearing = event.payload();
        final UUID hearingId = addedCourtapplicationToHearing.getHearingId();
        final CourtApplication courtApplication = dedupAllReportingRestrictions(addedCourtapplicationToHearing.getCourtApplication());
        final TypeReference<List<CourtApplication>> typeRef = new TypeReference<List<CourtApplication>>() {
        };
        final Hearing hearing = hearingRepository.findBy(hearingId);
        if (nonNull(courtApplication)) {
            filterDuplicateOffencesById(courtApplication.getOffences());
            if (nonNull(hearing)) {
                if (nonNull(hearing.getProperties()) && nonNull(hearing.getProperties().get(COURT_APPLICATION_FIELD))) {
                    using(hearingRepository)
                            .find(hearingId)
                            .putSubList(COURT_APPLICATION_FIELD, typeRef, getCourtApplicationsFunction(courtApplication))
                            .save();
                } else {
                    using(hearingRepository)
                            .find(hearingId)
                            .putObject(COURT_APPLICATION_FIELD, singletonList(courtApplication))
                            .save();
                }
                hearingSearchSyncService.sync(hearingId);
            }
        }
    }

    @Handles("listing.events.court-application-hearing-deleted")
    public void processCourtApplicationDeleted(final Envelope<CourtApplicationHearingDeleted> event) {
        final UUID hearingId = event.payload().getHearingId();
        final Hearing hearingToBeDeleted = hearingRepository.findBy(hearingId);

        if (Objects.nonNull(hearingToBeDeleted)) {
            hearingRepository.remove(hearingToBeDeleted);
            LOGGER.info("Hearing with id {} has been deleted ", hearingId);
        }
    }

    @Handles("listing.events.laa-reference-for-application-updated")
    public void processLaaReferenceUpdate(final Envelope<LaaReferenceForApplicationUpdated> event) {
        final LaaReferenceForApplicationUpdated laaReferenceForApplicationUpdated = event.payload();

        ofNullable(laaReferenceForApplicationUpdated.getHearingIds()).orElse(emptyList())
                .forEach(hearingId -> updateHearingApplicationWithLaaReference(laaReferenceForApplicationUpdated, hearingId));

    }

    private void updateHearingApplicationWithLaaReference(final LaaReferenceForApplicationUpdated laaReferenceForApplicationUpdated, final UUID hearingId) {
        final TypeReference<List<CourtApplication>> typeRef = new TypeReference<List<CourtApplication>>() {
        };

        using(hearingRepository)
                .find(hearingId)
                .putSubList(COURT_APPLICATION_FIELD, typeRef, getCourtApplicationsFunctionToUpdateLaaReference(laaReferenceForApplicationUpdated))
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationsFunctionToUpdateLaaReference(final LaaReferenceForApplicationUpdated laaReferenceForApplicationUpdated) {
        return courtApplications -> getUpdatedLaaReferenceCourtApplications(laaReferenceForApplicationUpdated, courtApplications);
    }

    private void filterDuplicateOffencesById(final List<uk.gov.justice.listing.events.Offence> offences) {
        if (isNull(offences) || offences.isEmpty()) {
            return;
        }
        final Set<UUID> offenceIds = new HashSet<>();
        offences.removeIf(e -> !offenceIds.add(e.getId()));
    }

    @Handles("listing.events.court-application-updated-for-hearing")
    public void courtApplicationUpdated(final Envelope<CourtApplicationUpdatedForHearing> event) {
        final CourtApplicationUpdatedForHearing courtApplicationUpdatedForHearing = event.payload();
        final UUID hearingId = courtApplicationUpdatedForHearing.getHearingId();
        final CourtApplication courtApplication = courtApplicationUpdatedForHearing.getCourtApplication();

        final TypeReference<List<CourtApplication>> typeRef = new TypeReference<List<CourtApplication>>() {
        };

        using(hearingRepository)
                .find(hearingId)
                .putSubList(COURT_APPLICATION_FIELD, typeRef, getCourtApplicationsFunction(courtApplication))
                .save();

        hearingSearchSyncService.sync(hearingId);
    }

    private Function<List<CourtApplication>, List<CourtApplication>> getCourtApplicationsFunction(CourtApplication courtApplication) {
        return courtApplications -> getUpdatedCourtApplications(courtApplication, courtApplications);
    }

    private List<CourtApplication> getUpdatedCourtApplications(CourtApplication updateCourtApplication,
                                                               List<CourtApplication> courtApplications) {

        final Optional<CourtApplication> origCourtApplication = courtApplications.stream().filter(ca -> ca.getId().equals(updateCourtApplication.getId())).findFirst();
        if (origCourtApplication.isPresent()) {
            filterDuplicateOffencesById(updateCourtApplication.getOffences());
            final CourtApplication newCourtApplication = CourtApplication.courtApplication()
                    .withApplicant(updateCourtApplication.getApplicant())
                    .withRespondents(updateCourtApplication.getRespondents())
                    .withApplicationType(updateCourtApplication.getApplicationType())
                    .withId(updateCourtApplication.getId())
                    .withParentApplicationId(updateCourtApplication.getParentApplicationId())
                    .withLinkedCaseIds(updateCourtApplication.getLinkedCaseIds())
                    .withRestrictCourtApplicationType(updateCourtApplication.getRestrictCourtApplicationType())
                    .withRestrictFromCourtList(updateCourtApplication.getRestrictFromCourtList())
                    .withApplicationReference(updateCourtApplication.getApplicationReference())
                    .withApplicationParticulars(updateCourtApplication.getApplicationParticulars())
                    .withOffences(updateCourtApplication.getOffences())
                    .withSubject(updateCourtApplication.getSubject())
                    .build();

            courtApplications.replaceAll(courtApplication -> courtApplication.getId()
                    .equals(newCourtApplication.getId()) ? newCourtApplication : courtApplication);
        } else {
            courtApplications.add(updateCourtApplication);
        }
        return courtApplications;
    }

    private List<CourtApplication> getUpdatedLaaReferenceCourtApplications(final LaaReferenceForApplicationUpdated laaReferenceForApplicationUpdated, final List<CourtApplication> courtApplications) {

        final Optional<CourtApplication> origCourtApplicationOptional = courtApplications.stream().filter(ca -> ca.getId().equals(laaReferenceForApplicationUpdated.getApplicationId())).findFirst();
        if (origCourtApplicationOptional.isPresent()) {
            final CourtApplication origCourtApplication = origCourtApplicationOptional.get();
            final CourtApplication newCourtApplication = CourtApplication.courtApplication()
                    .withValuesFrom(origCourtApplication)
                    .withOffences(getUpdatedOffenceWithLaaReference(origCourtApplication.getOffences(), laaReferenceForApplicationUpdated))
                    .build();

            courtApplications.replaceAll(courtApplication -> courtApplication.getId()
                    .equals(newCourtApplication.getId()) ? newCourtApplication : courtApplication);

        }
        return courtApplications;

    }

    private List<Offence> getUpdatedOffenceWithLaaReference(final List<Offence> offences, final LaaReferenceForApplicationUpdated laaReferenceForApplicationUpdated) {
        if (isEmpty(offences)) {
            return offences;
        }
        return offences.stream().map(offence -> {
                    if (offence.getId().equals(laaReferenceForApplicationUpdated.getOffenceId())) {
                        return Offence.offence()
                                .withValuesFrom(offence)
                                .withLaaApplnReference(buildLaaReference(laaReferenceForApplicationUpdated.getLaaReference()))
                                .build();
                    }
                    return offence;
                }
        ).toList();
    }

    private LaaReference buildLaaReference(final uk.gov.justice.core.courts.LaaReference laaReference) {
        if (isNull(laaReference)) {
            return null;
        }
        return LaaReference.laaReference()
                .withEffectiveEndDate(laaReference.getEffectiveEndDate())
                .withApplicationReference(laaReference.getApplicationReference())
                .withEffectiveStartDate(laaReference.getEffectiveStartDate())
                .withStatusCode(laaReference.getStatusCode())
                .withStatusId(laaReference.getStatusId())
                .withStatusDate(laaReference.getStatusDate())
                .withStatusDescription(laaReference.getStatusDescription())
                .build();
    }
}
